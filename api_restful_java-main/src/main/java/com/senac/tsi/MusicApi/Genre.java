package com.senac.tsi.MusicApi;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Schema(description = "Genero musical")
public class Genre {
    private @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) long id;

    @NotBlank
    @Size(min = 2, max = 50)
    @Column(unique = true)
    @Schema(example = "Bossa Nova")
    private String name;

    @Size(max = 255)
    @Schema(example = "Fusao de samba e jazz surgida no Rio de Janeiro no fim dos anos 1950")
    private String description;

    // Many-to-Many: lado inverso, navegavel pelo link "albums"
    @JsonIgnore
    @ManyToMany(mappedBy = "genres")
    private Set<Album> albums = new HashSet<>();

    public Genre() {
    }

    public Genre(String name, String description) {
        this.name = name;
        this.description = description;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Set<Album> getAlbums() {
        return albums;
    }

    public void setAlbums(Set<Album> albums) {
        this.albums = albums;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Genre genre = (Genre) o;
        return id == genre.id && Objects.equals(name, genre.name) && Objects.equals(description, genre.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, description);
    }

    @Override
    public String toString() {
        return "Genre{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
