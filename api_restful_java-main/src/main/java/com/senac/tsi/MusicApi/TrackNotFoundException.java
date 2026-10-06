package com.senac.tsi.MusicApi;

public class TrackNotFoundException extends RuntimeException {

    TrackNotFoundException(long id) {
        super("Could not find track with ID: " + id);
    }
}
