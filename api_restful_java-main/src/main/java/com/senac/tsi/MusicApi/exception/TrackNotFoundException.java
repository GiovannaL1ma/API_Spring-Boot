package com.senac.tsi.MusicApi.exception;

public class TrackNotFoundException extends RuntimeException {

    public TrackNotFoundException(long id) {
        super("Could not find track with ID: " + id);
    }
}
