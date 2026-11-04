package com.kinetirx.service;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Runs pg_dump and pg_restore with the same flags as the Go backend. Both binaries must be on
 * PATH; the Docker image installs the PostgreSQL client for this. DATABASE_URL is passed as the connection string.
 */
@Component
public class DbTools {

    private final String databaseUrl;

    public DbTools(Environment env) {
        this.databaseUrl = env.getProperty("DATABASE_URL", "");
    }

    /** pg_dump --format=custom, which pg_restore can apply regardless of object creation order. */
    public void dumpTo(Path dest) throws IOException {
        run(List.of("pg_dump", "--format=custom", "--no-owner", "--no-privileges",
                "--file=" + dest.toAbsolutePath(), databaseUrl), "pg_dump failed");
    }

    /** pg_restore --clean --if-exists: idempotent against a database that already has this schema. */
    public void restoreFrom(Path src) throws IOException {
        run(List.of("pg_restore", "--clean", "--if-exists", "--no-owner", "--no-privileges",
                "--dbname=" + databaseUrl, src.toAbsolutePath().toString()), "pg_restore failed");
    }

    private static void run(List<String> command, String label) throws IOException {
        Process p = new ProcessBuilder(command).redirectErrorStream(true).start();
        String output;
        try (var in = p.getInputStream()) {
            output = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        try {
            if (!p.waitFor(30, TimeUnit.MINUTES)) {
                p.destroyForcibly();
                throw new IOException(label + ": timed out");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(label + ": interrupted", e);
        }
        if (p.exitValue() != 0) {
            throw new IOException(label + ": " + output.strip());
        }
    }
}
