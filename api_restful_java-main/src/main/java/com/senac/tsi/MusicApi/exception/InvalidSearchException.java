package com.senac.tsi.MusicApi.exception;

// lancada quando os parametros de uma consulta nao fazem sentido juntos (ex.: from maior que to), o ApiExceptionAdvice devolve 400
public class InvalidSearchException extends RuntimeException {

    public InvalidSearchException(String message) {
        super(message);
    }
}
