package com.senac.tsi.MusicApi.exception;

public class RecordLabelNotFoundException extends RuntimeException {

    public RecordLabelNotFoundException(long id) {
        super("Could not find record label with ID: " + id);
    }
}
