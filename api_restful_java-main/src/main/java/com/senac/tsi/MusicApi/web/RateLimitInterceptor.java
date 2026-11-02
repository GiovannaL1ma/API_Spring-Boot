package com.senac.tsi.MusicApi.web;

import com.senac.tsi.MusicApi.config.ApiProperties;
import com.senac.tsi.MusicApi.exception.RateLimitExceededException;
import com.senac.tsi.MusicApi.model.ApiKey;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Optional;

// primeiro filtro de toda requisicao da API: conta o uso do cliente e informa o limite nos headers
// cliente = a chave de API (se valida) ou o IP; quem tem chave ganha um limite maior
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    public static final String LIMIT_HEADER = "X-RateLimit-Limit";
    public static final String REMAINING_HEADER = "X-RateLimit-Remaining";
    public static final String RESET_HEADER = "X-RateLimit-Reset";

    private final RateLimiter rateLimiter;
    private final ApiKeyResolver apiKeyResolver;
    private final ApiProperties.RateLimit limits;

    public RateLimitInterceptor(RateLimiter rateLimiter, ApiKeyResolver apiKeyResolver, ApiProperties properties) {
        this.rateLimiter = rateLimiter;
        this.apiKeyResolver = apiKeyResolver;
        this.limits = properties.rateLimit();
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // o OPTIONS que o navegador manda antes (preflight de CORS) nao conta
        if (CorsUtils.isPreFlightRequest(request))
            return true;

        // chave invalida conta como anonimo (pelo IP), assim ninguem foge do limite inventando chaves
        Optional<ApiKey> apiKey = apiKeyResolver.resolve(request);
        String client = apiKey.map(key -> "key:" + key.getId()).orElse("ip:" + request.getRemoteAddr());
        int limit = apiKey.isPresent() ? limits.withKeyPerMinute() : limits.anonymousPerMinute();

        RateLimiter.Result result = rateLimiter.tryConsume(client, limit);
        response.setHeader(LIMIT_HEADER, String.valueOf(result.limit()));
        response.setHeader(REMAINING_HEADER, String.valueOf(result.remaining()));
        response.setHeader(RESET_HEADER, String.valueOf(result.resetSeconds()));

        if (!result.allowed())
            throw new RateLimitExceededException(result.limit(), result.resetSeconds());
        return true;
    }
}
