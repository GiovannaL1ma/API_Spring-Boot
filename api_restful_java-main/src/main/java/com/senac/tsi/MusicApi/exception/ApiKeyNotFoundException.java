package com.senac.tsi.MusicApi.exception;

public class ApiKeyNotFoundException extends RuntimeException {

    public ApiKeyNotFoundException(long id) {
        super("Could not find API key with ID: " + id);
    }
}
