package com.kinetirx.common;

import com.kinetirx.exception.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fixed-window per-IP limiter, same semantics as the Go RateLimit middleware:
 * a window starts at the first request and resets once it has elapsed.
 * In-memory, so each instance keeps its own counters (as the Go service did).
 */
@Component
public class RateLimiter {

    private record Window(int count, long startMillis) {}

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    /** Throws 429 once the caller exceeds {@code max} requests in {@code window} for this route. */
    public void check(String route, HttpServletRequest request, int max, Duration window) {
        String key = route + "|" + ClientIp.of(request);
        long now = System.currentTimeMillis();
        Window result = windows.compute(key, (k, w) -> {
            if (w == null || now - w.startMillis() >= window.toMillis()) {
                return new Window(1, now);
            }
            return new Window(w.count() + 1, w.startMillis());
        });
        if (result.count() > max) {
            throw new ApiException(429, "rate_limited", "Too many attempts — please wait a moment and try again");
        }
    }
}
