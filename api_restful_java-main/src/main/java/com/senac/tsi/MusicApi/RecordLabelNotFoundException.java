package com.senac.tsi.MusicApi;

public class RecordLabelNotFoundException extends RuntimeException {

    RecordLabelNotFoundException(long id) {
        super("Could not find record label with ID: " + id);
    }
}
