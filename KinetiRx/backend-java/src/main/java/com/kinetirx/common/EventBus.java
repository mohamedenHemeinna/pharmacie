package com.kinetirx.common;

import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * In-process publisher for live sync (the Go events.Bus equivalent). Publishing never blocks the
 * mutation that triggered it: a slow subscriber simply misses that event, as in the Go version.
 */
@Component
public class EventBus {

    private final Set<Consumer<String>> subscribers = ConcurrentHashMap.newKeySet();

    public Runnable subscribe(Consumer<String> listener) {
        subscribers.add(listener);
        return () -> subscribers.remove(listener);
    }

    public void publish(String resource) {
        for (Consumer<String> s : subscribers) {
            try {
                s.accept(resource);
            } catch (RuntimeException ignored) {
                // A failing subscriber must not affect the request that published the event.
            }
        }
    }
}
