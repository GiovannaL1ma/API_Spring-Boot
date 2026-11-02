package com.senac.tsi.MusicApi.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.senac.tsi.MusicApi.model.ApiKey;
import com.senac.tsi.MusicApi.model.ApiKeyRole;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

// o que a API mostra de uma chave; a chave completa so aparece uma vez, na resposta do POST /api-keys
@Schema(description = "Chave de API")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiKeyResponse(
        @Schema(example = "2") long id,
        @Schema(example = "Giovanna Lima") String ownerName,
        @Schema(example = "giovanna@example.com") String ownerEmail,
        @Schema(description = "Comeco da chave, para reconhecer qual e sem expor ela inteira", example = "mca_Q1w2") String keyPrefix,
        @Schema(example = "USER") ApiKeyRole role,
        @Schema(example = "true") boolean active,
        @Schema(example = "2026-11-02T18:30:00Z") Instant createdAt,
        @Schema(description = "Ultima requisicao feita com a chave", example = "2026-11-02T18:42:10Z") Instant lastUsedAt,
        @Schema(description = "A chave completa. So vem na criacao: guarde, porque ela nao pode ser recuperada depois",
                example = "mca_Q1w2E3r4T5y6U7i8O9p0AsDfGhJkLzXcVbNm1234")
        String apiKey) {

    public static ApiKeyResponse from(ApiKey key) {
        return withSecret(key, null);
    }

    // usado so no POST /api-keys, unica vez em que a chave completa e devolvida
    public static ApiKeyResponse withSecret(ApiKey key, String rawKey) {
        return new ApiKeyResponse(key.getId(), key.getOwnerName(), key.getOwnerEmail(), key.getKeyPrefix(),
                key.getRole(), key.isActive(), key.getCreatedAt(), key.getLastUsedAt(), rawKey);
    }
}
