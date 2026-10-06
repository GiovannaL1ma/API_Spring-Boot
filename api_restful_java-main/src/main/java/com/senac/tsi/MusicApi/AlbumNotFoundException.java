package com.senac.tsi.MusicApi;

public class AlbumNotFoundException extends RuntimeException {

    AlbumNotFoundException(long id) {
        super("Could not find album with ID: " + id);
    }
}
