package com.senac.tsi.MusicApi.web;

import com.senac.tsi.MusicApi.exception.ApiKeyAuthenticationException;
import com.senac.tsi.MusicApi.exception.InsufficientApiKeyRoleException;
import com.senac.tsi.MusicApi.model.ApiKey;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Optional;

// roda antes de cada endpoint: confere a X-API-Key quando ele tem @RequiresApiKey
// as excecoes lancadas aqui caem no ApiExceptionAdvice (401 ou 403)
@Component
public class ApiKeyInterceptor implements HandlerInterceptor {

    private final ApiKeyResolver resolver;

    public ApiKeyInterceptor(ApiKeyResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod method))
            return true;

        Optional<ApiKey> apiKey = resolver.resolve(request);

        // mandou uma chave errada: recusa mesmo em endpoint publico, para o erro nao passar despercebido
        if (resolver.hasKeyHeader(request) && apiKey.isEmpty())
            throw new ApiKeyAuthenticationException("Invalid or inactive API key in the " + ApiKeyResolver.HEADER + " header");

        RequiresApiKey required = findRequirement(method);
        if (required == null)
            return true;

        if (apiKey.isEmpty())
            throw new ApiKeyAuthenticationException("Missing API key: send it in the " + ApiKeyResolver.HEADER
                    + " header (create one with POST /api-keys)");

        if (!apiKey.get().getRole().includes(required.value()))
            throw new InsufficientApiKeyRoleException(required.value());

        return true;
    }

    // a anotacao pode estar no metodo ou no controller inteiro (o Swagger usa o mesmo metodo para mostrar o cadeado)
    public static RequiresApiKey findRequirement(HandlerMethod method) {
        RequiresApiKey onMethod = AnnotatedElementUtils.findMergedAnnotation(method.getMethod(), RequiresApiKey.class);
        return onMethod != null ? onMethod : AnnotatedElementUtils.findMergedAnnotation(method.getBeanType(), RequiresApiKey.class);
    }
}
