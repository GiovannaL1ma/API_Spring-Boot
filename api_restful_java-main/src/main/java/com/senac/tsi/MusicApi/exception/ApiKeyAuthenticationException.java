package com.senac.tsi.MusicApi.exception;

// chave de API ausente, invalida ou desativada; o ApiExceptionAdvice devolve 401
public class ApiKeyAuthenticationException extends RuntimeException {

    public ApiKeyAuthenticationException(String message) {
        super(message);
    }
}
