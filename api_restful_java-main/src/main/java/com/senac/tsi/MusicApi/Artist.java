package com.senac.tsi.MusicApi;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Schema(description = "Artista: solo, dupla, banda ou orquestra")
public class Artist {
    private @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) long id;

    @NotBlank
    @Size(min = 1, max = 100)
    @Column(unique = true)
    @Schema(example = "Elis Regina")
    private String name;

    @NotBlank
    @Size(min = 2, max = 60)
    @Schema(description = "Pais de origem", example = "Brazil")
    private String country;

    @NotNull
    @Min(1900)
    @Max(2100)
    @Schema(description = "Ano de inicio da carreira", example = "1961")
    private Integer startYear;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Schema(example = "SOLO")
    private ArtistType type;

    // Many-to-Many: lado inverso, navegavel pelo link "albums"
    @JsonIgnore
    @ManyToMany(mappedBy = "artists")
    private Set<Album> albums = new HashSet<>();

    // Many-to-Many: lado inverso das participacoes especiais, navegavel pelo link "featuredOn"
    @JsonIgnore
    @ManyToMany(mappedBy = "featuredArtists")
    private Set<Track> featuredOn = new HashSet<>();

    public Artist() {
    }

    public Artist(String name, String country, Integer startYear, ArtistType type) {
        this.name = name;
        this.country = country;
        this.startYear = startYear;
        this.type = type;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public Integer getStartYear() {
        return startYear;
    }

    public void setStartYear(Integer startYear) {
        this.startYear = startYear;
    }

    public ArtistType getType() {
        return type;
    }

    public void setType(ArtistType type) {
        this.type = type;
    }

    public Set<Album> getAlbums() {
        return albums;
    }

    public void setAlbums(Set<Album> albums) {
        this.albums = albums;
    }

    public Set<Track> getFeaturedOn() {
        return featuredOn;
    }

    public void setFeaturedOn(Set<Track> featuredOn) {
        this.featuredOn = featuredOn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Artist artist = (Artist) o;
        return id == artist.id && Objects.equals(name, artist.name) && Objects.equals(country, artist.country) && Objects.equals(startYear, artist.startYear) && type == artist.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, country, startYear, type);
    }

    @Override
    public String toString() {
        return "Artist{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", country='" + country + '\'' +
                ", startYear=" + startYear +
                ", type=" + type +
                '}';
    }
}
