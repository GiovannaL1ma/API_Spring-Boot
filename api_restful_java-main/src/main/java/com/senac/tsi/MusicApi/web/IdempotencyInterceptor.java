package com.senac.tsi.MusicApi.web;

import com.senac.tsi.MusicApi.exception.IdempotencyException;
import com.senac.tsi.MusicApi.model.IdempotencyRecord;
import com.senac.tsi.MusicApi.service.IdempotencyService;
import com.senac.tsi.MusicApi.util.Hashing;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.ContentCachingResponseWrapper;
import org.springframework.web.util.WebUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

// idempotencia nos POST: o cliente manda X-Idempotency-Key e, se repetir a mesma requisicao
// (ex.: a internet caiu e ele nao sabe se o POST chegou), recebe a resposta da primeira vez em vez de criar outro registro
@Component
public class IdempotencyInterceptor implements HandlerInterceptor {

    public static final String HEADER = "X-Idempotency-Key";
    public static final String REPLAYED_HEADER = "X-Idempotency-Replayed";
    private static final String RECORD_ATTRIBUTE = "idempotencyRecordId";

    private final IdempotencyService idempotencyService;
    private final ApiKeyResolver apiKeyResolver;

    public IdempotencyInterceptor(IdempotencyService idempotencyService, ApiKeyResolver apiKeyResolver) {
        this.idempotencyService = idempotencyService;
        this.apiKeyResolver = apiKeyResolver;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        String key = request.getHeader(HEADER);
        if (!"POST".equals(request.getMethod()) || key == null || !(handler instanceof HandlerMethod))
            return true;

        key = key.trim();
        if (key.isEmpty() || key.length() > 255)
            throw IdempotencyException.invalidKey();

        // o IdempotencyFilter ja guardou o corpo; sem ele nao da para comparar as requisicoes
        CachedBodyHttpServletRequest cached = WebUtils.getNativeRequest(request, CachedBodyHttpServletRequest.class);
        if (cached == null)
            return true;

        // a mesma chave so vale dentro do mesmo cliente
        String scope = apiKeyResolver.resolve(request)
                .map(apiKey -> "key:" + apiKey.getId())
                .orElse("ip:" + request.getRemoteAddr());
        String requestHash = Hashing.sha256(request.getMethod() + " " + request.getRequestURI() + "\n"
                + new String(cached.getBody(), StandardCharsets.UTF_8));

        IdempotencyService.Decision decision = idempotencyService.begin(scope, key, requestHash);
        if (decision instanceof IdempotencyService.Replay replay) {
            writeStoredResponse(response, replay.record());
            return false; // o controller nem e chamado: nada e criado de novo
        }

        request.setAttribute(RECORD_ATTRIBUTE, ((IdempotencyService.Proceed) decision).recordId());
        response.setHeader(REPLAYED_HEADER, "false");
        return true;
    }

    // depois que o controller respondeu: guarda a resposta se deu certo, ou libera a chave se deu erro
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        Object recordId = request.getAttribute(RECORD_ATTRIBUTE);
        if (recordId == null)
            return;

        ContentCachingResponseWrapper cachedResponse = WebUtils.getNativeResponse(response, ContentCachingResponseWrapper.class);
        int status = response.getStatus();
        if (cachedResponse != null && status >= 200 && status < 300) {
            idempotencyService.complete((long) recordId, status, response.getContentType(),
                    response.getHeader(HttpHeaders.LOCATION),
                    new String(cachedResponse.getContentAsByteArray(), StandardCharsets.UTF_8));
        } else {
            idempotencyService.abandon((long) recordId);
        }
    }

    private void writeStoredResponse(HttpServletResponse response, IdempotencyRecord record) throws IOException {
        response.setStatus(record.getResponseStatus());
        if (record.getResponseContentType() != null)
            response.setContentType(record.getResponseContentType());
        if (record.getResponseLocation() != null)
            response.setHeader(HttpHeaders.LOCATION, record.getResponseLocation());
        response.setHeader(REPLAYED_HEADER, "true");
        if (record.getResponseBody() != null)
            response.getOutputStream().write(record.getResponseBody().getBytes(StandardCharsets.UTF_8));
    }
}
