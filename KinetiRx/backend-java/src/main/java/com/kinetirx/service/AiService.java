package com.kinetirx.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinetirx.ai.AiClient;
import com.kinetirx.ai.OcrPrompt;
import com.kinetirx.common.SecretCipher;
import com.kinetirx.config.KinetiRxProperties;
import com.kinetirx.dto.AiDtos;
import com.kinetirx.entity.AiProviderConfig;
import com.kinetirx.exception.ApiException;
import com.kinetirx.repository.AiProviderConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AI assistant and bill OCR. Backend priority, as in Go:
 * 1) the operator-configured OpenAI-compatible endpoint, 2) the legacy GEMINI_API_KEY, 3) offline fallback.
 */
@Service
public class AiService {

    private static final Logger log = LoggerFactory.getLogger(AiService.class);
    private static final String DEFAULT_CONTEXT = "Standard Indian Pharmacy Formulary";

    /** Thrown when neither an operator config nor GEMINI_API_KEY exists. */
    public static class NotConfiguredException extends RuntimeException {}

    private final AiProviderConfigRepository configs;
    private final SecretCipher cipher;
    private final AiClient client;
    private final ObjectMapper mapper;
    private final KinetiRxProperties props;

    public AiService(AiProviderConfigRepository configs, SecretCipher cipher, AiClient client,
                     ObjectMapper mapper, KinetiRxProperties props) {
        this.configs = configs;
        this.cipher = cipher;
        this.client = client;
        this.mapper = mapper;
        this.props = props;
    }

    /** Runs a prompt on whichever backend is configured; throws NotConfiguredException when none is. */
    public String run(String prompt, String imageBase64, String mime, boolean jsonMode) {
        var stored = configs.findById(Boolean.TRUE);
        if (stored.isPresent() && cipher.isConfigured()) {
            try {
                AiClient.Config cfg = new AiClient.Config(stored.get().getBaseUrl(), stored.get().getModel(),
                        cipher.decrypt(stored.get().getApiKeyCipher()));
                return client.chatCompletion(cfg, prompt, imageBase64, mime, jsonMode, Duration.ofSeconds(60));
            } catch (RuntimeException e) {
                if (e instanceof AiClient.AiException) throw e;
                // A decryption/store problem: log it and fall through to the env-var backend, as Go does.
                log.warn("ai provider config unusable: {}", e.getMessage());
            }
        }
        if (props.geminiApiKey() == null || props.geminiApiKey().isEmpty()) {
            throw new NotConfiguredException();
        }
        return client.gemini(prompt, imageBase64, mime, jsonMode);
    }

    public AiDtos.AskResponse ask(AiDtos.AskRequest req) {
        if (req.prompt() == null || req.prompt().isBlank()) {
            throw ApiException.badRequest("Invalid AI request payload: prompt is required");
        }
        String context = req.medicineContext() == null || req.medicineContext().isEmpty() ? DEFAULT_CONTEXT : req.medicineContext();
        String system = "You are Pharma Care Pro's clinical AI pharmacist assistant.\n"
                + "Provide concise, accurate clinical guidance on medicine indications, dosage precautions, salt substitutions, interactions, and inventory recommendations.\n"
                + "Current inventory context: " + context + ".";
        try {
            String text = run(system + "\n\nUser Question: " + req.prompt(), null, null, false);
            return new AiDtos.AskResponse(true, null, null, text);
        } catch (NotConfiguredException e) {
            return new AiDtos.AskResponse(false, true, "AI assistant is operating in offline mode.",
                    "Clinical Pharmacist Advisory: For " + req.prompt()
                            + ", recommend checking inventory for standard broad-spectrum or symptomatic medications.");
        } catch (RuntimeException e) {
            throw new ApiException(500, "ai_request_failed", "AI request failed: " + e.getMessage());
        }
    }

    /**
     * Builds the same key set the Go gin.H maps did: fallback, raw-text, and parsed-data branches
     * each carry different keys, so this returns a map rather than a fixed record.
     */
    public Map<String, Object> parseBill(AiDtos.ParseBillRequest req) {
        String prompt;
        if (req.imageBase64() != null && !req.imageBase64().isEmpty()) {
            prompt = OcrPrompt.BILL;
        } else if (req.textContent() != null && !req.textContent().isEmpty()) {
            prompt = OcrPrompt.BILL + "\n\nInvoice Text Content:\n" + req.textContent();
        } else {
            throw ApiException.badRequest("No imageBase64 or textContent provided");
        }
        String responseText;
        try {
            responseText = run(prompt, req.imageBase64(), req.mimeType(), true);
        } catch (NotConfiguredException e) {
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("success", false);
            out.put("fallback", true);
            out.put("message", "No AI provider configured on server. Using built-in pharmaceutical OCR engine.");
            return out;
        } catch (RuntimeException e) {
            throw new ApiException(500, "ocr_extraction_failed", "Failed to process purchase bill OCR: " + e.getMessage());
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("success", true);
        try {
            out.put("data", mapper.readValue(responseText, new TypeReference<Object>() {}));
        } catch (Exception notJson) {
            out.put("rawText", responseText);
            out.put("data", null);
        }
        return out;
    }

    // ---- provider configuration -----------------------------------------

    @Transactional(readOnly = true)
    public AiDtos.ProviderStatus status() {
        return configs.findById(Boolean.TRUE)
                .map(c -> new AiDtos.ProviderStatus(true, c.getBaseUrl(), c.getModel(), c.getUpdatedAt()))
                .orElseGet(() -> new AiDtos.ProviderStatus(false, null, null, null));
    }

    @Transactional
    public AiDtos.ProviderStatus save(AiDtos.ProviderInput in) {
        cipher.requireConfigured();
        if (isBlank(in.baseUrl()) || isBlank(in.model()) || isBlank(in.apiKey())) {
            throw ApiException.badRequest("Invalid AI provider config payload: baseUrl, model and apiKey are required");
        }
        String baseUrl = in.baseUrl().trim().replaceAll("/+$", "");
        AiClient.Config cfg = new AiClient.Config(baseUrl, in.model().trim(), in.apiKey().trim());
        try {
            client.chatCompletion(cfg, "Reply with the single word: OK", null, null, false, Duration.ofSeconds(20));
        } catch (AiClient.AiException e) {
            throw ApiException.badRequest("Could not reach this endpoint with these details: " + e.getMessage());
        }
        AiProviderConfig row = configs.findById(Boolean.TRUE).orElseGet(AiProviderConfig::new);
        row.setBaseUrl(baseUrl);
        row.setModel(cfg.model());
        row.setApiKeyCipher(cipher.encrypt(cfg.apiKey()));
        configs.saveAndFlush(row);
        return status();
    }

    @Transactional
    public void delete() {
        configs.deleteById(Boolean.TRUE);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
