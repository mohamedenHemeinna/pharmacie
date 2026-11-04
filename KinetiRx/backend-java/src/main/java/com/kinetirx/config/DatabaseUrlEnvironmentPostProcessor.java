package com.kinetirx.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Accepts the Go backend's DATABASE_URL format (postgres://user:pass@host:port/db?sslmode=disable)
 * and translates it to the JDBC properties Spring expects. Existing deployment
 * configuration therefore keeps working unchanged.
 *
 * If DATABASE_URL is absent, Spring's own spring.datasource.* properties apply.
 */
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String raw = environment.getProperty("DATABASE_URL");
        if (raw == null || raw.isBlank()) {
            return;
        }
        Map<String, Object> props = new HashMap<>();
        String jdbc = toJdbc(raw.trim(), props);
        props.put("spring.datasource.url", jdbc);
        environment.getPropertySources().addFirst(new MapPropertySource("databaseUrl", props));
    }

    static String toJdbc(String databaseUrl, Map<String, Object> props) {
        URI uri = URI.create(databaseUrl.replaceFirst("^postgres(ql)?://", "http://"));
        String userInfo = uri.getRawUserInfo();
        if (userInfo != null) {
            int colon = userInfo.indexOf(':');
            String user = colon >= 0 ? userInfo.substring(0, colon) : userInfo;
            String pass = colon >= 0 ? userInfo.substring(colon + 1) : "";
            props.put("spring.datasource.username", URLDecoder.decode(user, StandardCharsets.UTF_8));
            props.put("spring.datasource.password", URLDecoder.decode(pass, StandardCharsets.UTF_8));
        }
        int port = uri.getPort() == -1 ? 5432 : uri.getPort();
        String path = uri.getPath() == null ? "" : uri.getPath();
        StringBuilder jdbc = new StringBuilder("jdbc:postgresql://")
                .append(uri.getHost()).append(':').append(port).append(path);
        String query = uri.getQuery();
        if (query != null && !query.isBlank()) {
            // sslmode=disable is the only parameter the Go deployment uses in practice; pass others through.
            jdbc.append('?').append(query);
        }
        return jdbc.toString();
    }
}
