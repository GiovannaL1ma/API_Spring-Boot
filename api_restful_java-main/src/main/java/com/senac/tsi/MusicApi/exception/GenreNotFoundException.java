package com.senac.tsi.MusicApi.exception;

public class GenreNotFoundException extends RuntimeException {

    public GenreNotFoundException(long id) {
        super("Could not find genre with ID: " + id);
    }
}
