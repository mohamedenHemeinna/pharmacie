package com.kinetirx.controller;

import com.kinetirx.common.SecretCipher;
import com.kinetirx.dto.BackupDtos;
import com.kinetirx.exception.ApiException;
import com.kinetirx.service.BackupService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/** /api/backup/*: admin only (SecurityConfig). Every route can expose or overwrite the full database. */
@RestController
@RequestMapping("/api/backup")
public class BackupController {

    private final BackupService backups;
    private final SecretCipher cipher;

    public BackupController(BackupService backups, SecretCipher cipher) {
        this.backups = backups;
        this.cipher = cipher;
    }

    @GetMapping("/config")
    public BackupDtos.ConfigStatus getConfig() {
        return backups.status();
    }

    @PostMapping("/config")
    public BackupDtos.ConfigStatus saveConfig(@RequestBody BackupDtos.ConfigInput in) {
        return backups.save(in);
    }

    @DeleteMapping("/config")
    public ResponseEntity<Void> deleteConfig() {
        backups.deleteConfig();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/status")
    public BackupDtos.Status status() {
        return backups.overallStatus();
    }

    @GetMapping
    public Map<String, Object> list() {
        java.util.Map<String, Object> out = new java.util.LinkedHashMap<>();
        out.put("backups", backups.list());
        return out;
    }

    @PostMapping
    public BackupDtos.Record trigger() {
        cipher.requireConfigured();
        BackupService.Destination dest;
        try {
            dest = backups.destination();
        } catch (BackupService.NotConfiguredException e) {
            throw ApiException.badRequest("No S3 backup destination configured yet — save one first.");
        }
        return backups.run(dest, "manual");
    }

    @PostMapping("/{id}/restore")
    public Map<String, Object> restore(@PathVariable String id, @RequestBody(required = false) BackupDtos.RestoreRequest req) {
        if (req == null || !"RESTORE".equals(req.confirm())) {
            throw ApiException.badRequest("To confirm this destructive action, send {\"confirm\":\"RESTORE\"}");
        }
        cipher.requireConfigured();
        BackupDtos.Record rec = backups.get(id);
        if (rec == null) throw ApiException.notFound("Backup not found");
        if (!"verified".equals(rec.status())) throw ApiException.badRequest("Only a verified backup can be restored");
        BackupService.Destination dest = backups.destination();
        backups.restoreFromS3(dest, rec);
        return Map.of("restored", true, "backupId", rec.id());
    }

    @GetMapping("/download")
    public void download(HttpServletResponse response) throws IOException {
        String filename = "kinetirx-backup-" + DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
                .withZone(ZoneOffset.UTC).format(Instant.now()) + ".dump";
        response.setHeader("Content-Disposition", ContentDisposition.attachment().filename(filename).build().toString());
        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        backups.streamDump(response.getOutputStream());
    }

    @PostMapping(value = "/restore-upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> restoreUpload(@RequestParam(value = "confirm", required = false) String confirm,
                                             @RequestParam(value = "file", required = false) MultipartFile file) throws IOException {
        if (!"RESTORE".equals(confirm)) {
            throw ApiException.badRequest("To confirm this destructive action, include a \"confirm\" form field set to RESTORE");
        }
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("No backup file was uploaded");
        }
        backups.restoreFromUpload(file.getInputStream());
        return Map.of("restored", true);
    }
}
