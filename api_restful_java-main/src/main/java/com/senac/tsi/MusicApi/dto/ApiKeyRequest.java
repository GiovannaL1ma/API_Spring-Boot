package com.senac.tsi.MusicApi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// corpo do POST /api-keys: quem esta pedindo a chave
@Schema(description = "Dados de quem esta pedindo a chave de API")
public record ApiKeyRequest(

        @NotBlank
        @Size(min = 2, max = 100)
        @Schema(description = "Nome do dono da chave: texto de 2 a 100 caracteres", example = "Giovanna Lima")
        String ownerName,

        @NotBlank
        @Email
        @Size(max = 150)
        @Schema(description = "E-mail do dono da chave, usado so para identificar quem a pediu", example = "giovanna@example.com")
        String ownerEmail) {
}
