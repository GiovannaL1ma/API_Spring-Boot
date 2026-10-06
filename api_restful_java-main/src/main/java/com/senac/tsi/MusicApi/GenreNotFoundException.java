package com.senac.tsi.MusicApi;

public class GenreNotFoundException extends RuntimeException {

    GenreNotFoundException(long id) {
        super("Could not find genre with ID: " + id);
    }
}
