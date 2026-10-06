package com.senac.tsi.MusicApi;

public class CoverNotFoundException extends RuntimeException {

    CoverNotFoundException(long id) {
        super("Could not find cover with ID: " + id);
    }

    // Usado quando o album existe, mas ainda nao tem capa
    CoverNotFoundException(String message) {
        super(message);
    }
}
