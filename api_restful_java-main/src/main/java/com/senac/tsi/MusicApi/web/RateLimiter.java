package com.senac.tsi.MusicApi.web;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// conta as requisicoes de cada cliente em janelas fixas de 1 minuto
// ex.: limite 60 -> da 1a a 60a requisicao do minuto passam, a 61a recebe 429 ate a janela virar
@Component
public class RateLimiter {

    private static final long WINDOW_MILLIS = 60_000;

    // resultado de uma tentativa: os numeros vao nos headers X-RateLimit-* da resposta
    public record Result(boolean allowed, int limit, int remaining, long resetSeconds) {
    }

    private record Window(long resetAt, int count) {
    }

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public Result tryConsume(String clientId, int limit) {
        long now = System.currentTimeMillis();
        // compute e atomico por cliente: duas requisicoes ao mesmo tempo nao contam como uma so
        Window window = windows.compute(clientId, (id, current) ->
                current == null || now >= current.resetAt()
                        ? new Window(now + WINDOW_MILLIS, 1)
                        : new Window(current.resetAt(), current.count() + 1));

        // evita o mapa crescer para sempre com IPs que nunca voltam
        if (windows.size() > 10_000)
            windows.values().removeIf(old -> now >= old.resetAt());

        long resetSeconds = Math.max(1, (window.resetAt() - now + 999) / 1000);
        return new Result(window.count() <= limit, limit, Math.max(0, limit - window.count()), resetSeconds);
    }
}
