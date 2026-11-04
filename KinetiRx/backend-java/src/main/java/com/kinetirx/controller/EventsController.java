package com.kinetirx.controller;

import com.kinetirx.common.EventBus;
import com.kinetirx.common.SseHub;
import com.kinetirx.exception.ApiException;
import com.kinetirx.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * GET /api/events/stream: Server-Sent Events for live sync. Carries only resource names; clients
 * re-fetch through the normal authorized REST endpoints. Token comes from ?token= (EventSource
 * cannot set headers) or from a Bearer header, and is fully verified.
 */
@RestController
public class EventsController {

    private final JwtService jwt;
    private final EventBus bus;
    private final SseHub hub;

    public EventsController(JwtService jwt, EventBus bus, SseHub hub) {
        this.jwt = jwt;
        this.bus = bus;
        this.hub = hub;
    }

    @GetMapping(value = "/api/events/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(HttpServletRequest request, HttpServletResponse response) {
        String token = request.getParameter("token");
        if (token == null || token.isEmpty()) {
            String h = request.getHeader("Authorization");
            if (h != null && h.startsWith("Bearer ")) token = h.substring(7);
        }
        if (token == null || token.isEmpty()) {
            throw new ApiException(401, "unauthorized", "Missing access token");
        }
        if (jwt.verify(token).isEmpty()) {
            throw new ApiException(401, "unauthorized", "Invalid or expired access token");
        }
        response.setHeader("X-Accel-Buffering", "no");   // stop nginx buffering this route
        SseEmitter emitter = new SseEmitter(0L);          // no server-side timeout; the heartbeat keeps it open
        hub.register(emitter, bus);
        return emitter;
    }
}
