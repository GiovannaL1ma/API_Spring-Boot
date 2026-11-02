package com.senac.tsi.MusicApi.dto;

import com.senac.tsi.MusicApi.model.Artist;
import com.senac.tsi.MusicApi.model.Track;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

// faixa na versao 2 da API (X-API-Version: 2)
// muda o formato em relacao a v1, por isso virou uma versao nova em vez de alterar a v1:
//  - durationSeconds (numero) -> duration (texto "m:ss", como aparece num player)
//  - o album vem resumido (id e titulo) em vez do album inteiro
//  - os convidados vem so pelo nome
@Schema(description = "Faixa na versao 2: duracao formatada e album e convidados resumidos")
public record TrackV2(
        @Schema(example = "1") long id,
        @Schema(example = "Taxman") String title,
        @Schema(example = "1") int trackNumber,
        @Schema(description = "Duracao formatada (m:ss, ou h:mm:ss a partir de 1 hora)", example = "2:39") String duration,
        @Schema(description = "Album da faixa, resumido") AlbumSummary album,
        @Schema(description = "Nomes dos artistas convidados (feat.)", example = "[\"Daft Punk\"]") List<String> featuredArtists) {

    @Schema(description = "Album resumido")
    public record AlbumSummary(@Schema(example = "1") long id, @Schema(example = "Revolver") String title) {
    }

    public static TrackV2 from(Track track) {
        List<String> featured = track.getFeaturedArtists().stream().map(Artist::getName).sorted().toList();
        return new TrackV2(track.getId(), track.getTitle(), track.getTrackNumber(), formatDuration(track.getDurationSeconds()),
                new AlbumSummary(track.getAlbum().getId(), track.getAlbum().getTitle()), featured);
    }

    // 159 -> "2:39" | 3725 -> "1:02:05"
    static String formatDuration(int totalSeconds) {
        int hours = totalSeconds / 3600, minutes = (totalSeconds % 3600) / 60, seconds = totalSeconds % 60;
        return hours > 0 ? "%d:%02d:%02d".formatted(hours, minutes, seconds) : "%d:%02d".formatted(minutes, seconds);
    }
}
