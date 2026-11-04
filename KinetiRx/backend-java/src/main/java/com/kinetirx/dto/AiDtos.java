package com.kinetirx.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/** AI assistant, OCR, and provider-config wire shapes. */
public final class AiDtos {

    private AiDtos() {}

    public record AskRequest(String prompt, String medicineContext) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record AskResponse(boolean success, Boolean fallback, String message, String response) {}

    public record ParseBillRequest(String imageBase64, String mimeType, String textContent, String distributorHint) {}

    public record ProviderInput(String baseUrl, String model, String apiKey) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ProviderStatus(boolean configured, String baseUrl, String model, Instant updatedAt) {}
}
