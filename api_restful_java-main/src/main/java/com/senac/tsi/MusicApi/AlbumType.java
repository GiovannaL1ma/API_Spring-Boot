package com.senac.tsi.MusicApi;

// enum é uma lista fixa de valores, usada no "type" do album. se mandar um valor fora da lista a api responde 400
public enum AlbumType {
    STUDIO,         // album de estudio
    LIVE,           // gravado ao vivo
    COMPILATION,    // coletanea
    EP,             // extended play
    SOUNDTRACK      // trilha sonora
}
