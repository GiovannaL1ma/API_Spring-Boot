package com.senac.tsi.MusicApi;

public class ArtistNotFoundException extends RuntimeException {

    ArtistNotFoundException(long id) {
        super("Could not find artist with ID: " + id);
    }
}
