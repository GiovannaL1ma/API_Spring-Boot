package com.senac.tsi.MusicApi.web;

import com.senac.tsi.MusicApi.model.ApiKey;
import com.senac.tsi.MusicApi.service.ApiKeyService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.Optional;

// descobre qual chave de API veio no header X-API-Key, uma vez por requisicao
// o rate limit e a autenticacao usam o mesmo resultado, entao o banco e consultado so uma vez
@Component
public class ApiKeyResolver {

    public static final String HEADER = "X-API-Key";

    // o controller recebe a chave autenticada com @RequestAttribute(ApiKeyResolver.API_KEY_ATTRIBUTE)
    public static final String API_KEY_ATTRIBUTE = "authenticatedApiKey";
    private static final String RESOLVED_ATTRIBUTE = "apiKeyResolved";

    private final ApiKeyService apiKeyService;

    public ApiKeyResolver(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    public Optional<ApiKey> resolve(HttpServletRequest request) {
        if (request.getAttribute(RESOLVED_ATTRIBUTE) == null) {
            request.setAttribute(RESOLVED_ATTRIBUTE, true);
            if (hasKeyHeader(request))
                apiKeyService.authenticate(request.getHeader(HEADER).trim())
                        .ifPresent(key -> request.setAttribute(API_KEY_ATTRIBUTE, key));
        }
        return Optional.ofNullable((ApiKey) request.getAttribute(API_KEY_ATTRIBUTE));
    }

    public boolean hasKeyHeader(HttpServletRequest request) {
        String header = request.getHeader(HEADER);
        return header != null && !header.isBlank();
    }
}
