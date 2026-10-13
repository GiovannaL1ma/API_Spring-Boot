package com.senac.tsi.MusicApi;

// essa excecao é lancada quando nao acha o album pelo id, o ApiExceptionAdvice pega ela e devolve 404
public class AlbumNotFoundException extends RuntimeException { //runtime p nao precisar colocar throws nos metodos

    AlbumNotFoundException(long id) {
        super("Could not find album with ID: " + id); 
    }
}
