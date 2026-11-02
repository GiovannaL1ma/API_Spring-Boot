package com.senac.tsi.MusicApi.exception;

public class CoverNotFoundException extends RuntimeException {

    public CoverNotFoundException(long id) {
        super("Could not find cover with ID: " + id);
    }

    // Usado quando o album existe, mas ainda nao tem capa
    public CoverNotFoundException(String message) {
        super(message);
    }
}
