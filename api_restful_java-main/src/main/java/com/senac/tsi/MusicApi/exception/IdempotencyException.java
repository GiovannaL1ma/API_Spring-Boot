package com.senac.tsi.MusicApi.exception;

import org.springframework.http.HttpStatus;

// problemas com a X-Idempotency-Key; cada caso carrega o status HTTP certo e o ApiExceptionAdvice so repassa
public class IdempotencyException extends RuntimeException {

    private final HttpStatus status;

    private IdempotencyException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    // 400: chave vazia ou grande demais
    public static IdempotencyException invalidKey() {
        return new IdempotencyException(HttpStatus.BAD_REQUEST,
                "Invalid X-Idempotency-Key: it must have between 1 and 255 characters");
    }

    // 409: a primeira requisicao com essa chave ainda nao terminou
    public static IdempotencyException inProgress(String key) {
        return new IdempotencyException(HttpStatus.CONFLICT,
                "A request with X-Idempotency-Key '" + key + "' is still being processed; try again in a moment");
    }

    // 422: a chave ja foi usada com outra requisicao (outra rota ou outro corpo)
    public static IdempotencyException reused(String key) {
        return new IdempotencyException(HttpStatus.UNPROCESSABLE_CONTENT,
                "X-Idempotency-Key '" + key + "' was already used with a different request; use a new key for a new operation");
    }

    public HttpStatus getStatus() {
        return status;
    }
}
