package com.senac.tsi.MusicApi.exception;

public class ArtistNotFoundException extends RuntimeException {

    public ArtistNotFoundException(long id) {
        super("Could not find artist with ID: " + id);
    }
}
