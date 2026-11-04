package com.kinetirx.common;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks open live-sync streams. Each stream receives "data: {"resource":...}" frames from the
 * EventBus, and a ": ping" comment every 25 seconds, the same cadence as the Go handler.
 */
@Component
public class SseHub {

    private final Map<SseEmitter, Runnable> streams = new ConcurrentHashMap<>();

    public void register(SseEmitter emitter, EventBus bus) {
        Runnable unsubscribe = bus.subscribe(resource -> sendData(emitter, "{\"resource\":\"" + resource + "\"}"));
        streams.put(emitter, unsubscribe);
        Runnable cleanup = () -> {
            Runnable u = streams.remove(emitter);
            if (u != null) u.run();
        };
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(e -> cleanup.run());
    }

    @Scheduled(fixedRate = 25_000)
    public void heartbeat() {
        for (SseEmitter e : Set.copyOf(streams.keySet())) {
            try {
                e.send(SseEmitter.event().comment("ping"));
            } catch (IOException | IllegalStateException ex) {
                drop(e);
            }
        }
    }

    private void sendData(SseEmitter emitter, String json) {
        try {
            emitter.send(SseEmitter.event().data(" " + json));
        } catch (IOException | IllegalStateException ex) {
            drop(emitter);
        }
    }

    private void drop(SseEmitter emitter) {
        Runnable u = streams.remove(emitter);
        if (u != null) u.run();
    }
}
