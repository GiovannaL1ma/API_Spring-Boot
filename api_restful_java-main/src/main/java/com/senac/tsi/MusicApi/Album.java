package com.senac.tsi.MusicApi;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.util.*;

@Entity
@Schema(description = "Album (disco) lancado por um ou mais artistas")
public class Album {
    private @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) long id;

    @NotBlank
    @Size(min = 1, max = 150)
    @Schema(example = "Revolver")
    private String title;

    @NotNull
    @Min(1900)
    @Max(2100)
    @Schema(description = "Ano de lancamento", example = "1966")
    private Integer releaseYear;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Schema(example = "STUDIO")
    private AlbumType type;

    // Many-to-One opcional: albuns independentes nao tem gravadora
    // No payload basta enviar o id: "label": { "id": 1 }
    @ManyToOne
    @JoinColumn(name = "label_id")
    private RecordLabel label;

    // Many-to-Many: um album tem um ou mais artistas principais e um artista lanca varios albuns
    // No payload basta enviar os ids: "artists": [{ "id": 1 }]
    @NotEmpty
    @ManyToMany
    @JoinTable(name = "album_artist",
            joinColumns = @JoinColumn(name = "album_id"),
            inverseJoinColumns = @JoinColumn(name = "artist_id"))
    private Set<Artist> artists = new HashSet<>();

    // Many-to-Many: um album pode ter varios generos e um genero reune varios albuns
    // No payload basta enviar os ids: "genres": [{ "id": 1 }]
    @ManyToMany
    @JoinTable(name = "album_genre",
            joinColumns = @JoinColumn(name = "album_id"),
            inverseJoinColumns = @JoinColumn(name = "genre_id"))
    private Set<Genre> genres = new HashSet<>();

    // One-to-Many: as faixas pertencem ao album e sao excluidas junto com ele
    @JsonIgnore
    @OneToMany(mappedBy = "album", cascade = CascadeType.REMOVE)
    @OrderBy("trackNumber")
    private List<Track> tracks = new ArrayList<>();

    // One-to-One: lado inverso, navegavel pelo link "cover"; a capa e excluida junto com o album
    @JsonIgnore
    @OneToOne(mappedBy = "album", cascade = CascadeType.REMOVE)
    private Cover cover;

    public Album() {
    }

    public Album(String title, Integer releaseYear, AlbumType type, RecordLabel label) {
        this.title = title;
        this.releaseYear = releaseYear;
        this.type = type;
        this.label = label;
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

    public Integer getReleaseYear() {
        return releaseYear;
    }

    public void setReleaseYear(Integer releaseYear) {
        this.releaseYear = releaseYear;
    }

    public AlbumType getType() {
        return type;
    }

    public void setType(AlbumType type) {
        this.type = type;
    }

    public RecordLabel getLabel() {
        return label;
    }

    public void setLabel(RecordLabel label) {
        this.label = label;
    }

    public Set<Artist> getArtists() {
        return artists;
    }

    public void setArtists(Set<Artist> artists) {
        this.artists = artists;
    }

    public Set<Genre> getGenres() {
        return genres;
    }

    public void setGenres(Set<Genre> genres) {
        this.genres = genres;
    }

    public List<Track> getTracks() {
        return tracks;
    }

    public void setTracks(List<Track> tracks) {
        this.tracks = tracks;
    }

    public Cover getCover() {
        return cover;
    }

    public void setCover(Cover cover) {
        this.cover = cover;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Album album = (Album) o;
        return id == album.id && Objects.equals(title, album.title) && Objects.equals(releaseYear, album.releaseYear) && type == album.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, releaseYear, type);
    }

    @Override
    public String toString() {
        return "Album{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", releaseYear=" + releaseYear +
                ", type=" + type +
                '}';
    }
}
