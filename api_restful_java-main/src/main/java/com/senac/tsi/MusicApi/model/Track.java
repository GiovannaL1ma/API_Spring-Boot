package com.senac.tsi.MusicApi.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(name = "uq_track_album_number", columnNames = {"album_id", "track_number"}))
@Schema(description = "Faixa (musica) de um album")
public class Track {
    private @Id
    @Schema(description = "Gerado pelo banco: e ignorado se vier no corpo do POST ou do PUT", example = "1")
    @GeneratedValue(strategy = GenerationType.IDENTITY) long id;

    @NotBlank
    @Size(min = 1, max = 150)
    @Schema(description = "Titulo: texto de 1 a 150 caracteres", example = "Águas de Março")
    private String title;

    @NotNull
    @Min(1)
    @Max(99)
    @Schema(description = "Posicao da faixa no album: numero inteiro de 1 a 99, unico dentro do album", example = "1")
    private Integer trackNumber;

    @NotNull
    @Min(1)
    @Max(7200)
    @Schema(description = "Duracao em segundos: numero inteiro de 1 a 7200 (2 horas)", example = "212")
    private Integer durationSeconds;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "album_id")
    private Album album;

    @ManyToMany
    @JoinTable(name = "track_featured_artist",
            joinColumns = @JoinColumn(name = "track_id"),
            inverseJoinColumns = @JoinColumn(name = "artist_id"))
    private Set<Artist> featuredArtists = new HashSet<>();

    public Track() {
    }

    public Track(String title, Integer trackNumber, Integer durationSeconds, Album album) {
        this.title = title;
        this.trackNumber = trackNumber;
        this.durationSeconds = durationSeconds;
        this.album = album;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getTrackNumber() {
        return trackNumber;
    }

    public void setTrackNumber(Integer trackNumber) {
        this.trackNumber = trackNumber;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public Album getAlbum() {
        return album;
    }

    public void setAlbum(Album album) {
        this.album = album;
    }

    public Set<Artist> getFeaturedArtists() {
        return featuredArtists;
    }

    public void setFeaturedArtists(Set<Artist> featuredArtists) {
        this.featuredArtists = featuredArtists;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Track track = (Track) o;
        return id == track.id && Objects.equals(title, track.title) && Objects.equals(trackNumber, track.trackNumber) && Objects.equals(durationSeconds, track.durationSeconds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, trackNumber, durationSeconds);
    }

    @Override
    public String toString() {
        return "Track{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", trackNumber=" + trackNumber +
                ", durationSeconds=" + durationSeconds +
                '}';
    }
}
