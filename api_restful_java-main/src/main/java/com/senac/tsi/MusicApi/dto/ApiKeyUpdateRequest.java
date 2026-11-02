package com.senac.tsi.MusicApi.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.senac.tsi.MusicApi.model.ApiKeyRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;

// corpo do PATCH /api-keys/{id}: so os campos enviados mudam
@Schema(description = "Alteracao parcial de uma chave: envie active, role ou os dois")
public record ApiKeyUpdateRequest(

        @Schema(description = "false desativa a chave (ela para de autenticar); true reativa", example = "false")
        Boolean active,

        @Schema(description = "Novo nivel de acesso", example = "ADMIN")
        ApiKeyRole role) {

    // sem nenhum dos dois campos o PATCH nao teria o que alterar
    @JsonIgnore
    @AssertTrue(message = "send at least one field: active or role")
    public boolean isAnyFieldPresent() {
        return active != null || role != null;
    }
}
