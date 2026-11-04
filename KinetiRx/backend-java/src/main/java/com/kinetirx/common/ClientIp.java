package com.kinetirx.common;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Client address, derived the same way Gin's ClientIP() does: the first
 * X-Forwarded-For entry when present, then X-Real-IP, then the socket address.
 * Those headers are only trustworthy behind the nginx proxy shipped with KinetiRx.
 */
public final class ClientIp {

    private ClientIp() {}

    public static String of(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        String real = request.getHeader("X-Real-IP");
        if (real != null && !real.isBlank()) {
            return real.trim();
        }
        return request.getRemoteAddr();
    }
}
