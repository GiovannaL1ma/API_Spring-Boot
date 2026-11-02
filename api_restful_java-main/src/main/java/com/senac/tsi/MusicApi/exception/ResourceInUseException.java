package com.senac.tsi.MusicApi.exception;

// lancada quando tentam excluir um registro que outro ainda usa (ex.: artista com albuns), o ApiExceptionAdvice devolve 409
public class ResourceInUseException extends RuntimeException {

    public ResourceInUseException(String message) {
        super(message);
    }
}
