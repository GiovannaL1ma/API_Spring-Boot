package com.senac.tsi.MusicApi;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.util.List;
import java.util.Objects;

@Entity
@Schema(description = "Gravadora responsavel pelo lancamento dos albuns")
public class RecordLabel {
    private @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) long id;

    @NotBlank
    @Size(min = 2, max = 100)
    @Column(unique = true)
    @Schema(example = "Parlophone")
    private String name;

    @NotBlank
    @Size(min = 2, max = 60)
    @Schema(example = "United Kingdom")
    private String country;

    @NotNull
    @Min(1850)
    @Max(2100)
    @Schema(description = "Ano de fundacao", example = "1896")
    private Integer foundedYear;

    // One-to-Many: lado inverso, navegavel pelo link "albums"
    @JsonIgnore
    @OneToMany(mappedBy = "label")
    private List<Album> albums;

    public RecordLabel() {
    }

    public RecordLabel(String name, String country, Integer foundedYear) {
        this.name = name;
        this.country = country;
        this.foundedYear = foundedYear;
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

    public Integer getFoundedYear() {
        return foundedYear;
    }

    public void setFoundedYear(Integer foundedYear) {
        this.foundedYear = foundedYear;
    }

    public List<Album> getAlbums() {
        return albums;
    }

    public void setAlbums(List<Album> albums) {
        this.albums = albums;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RecordLabel that = (RecordLabel) o;
        return id == that.id && Objects.equals(name, that.name) && Objects.equals(country, that.country) && Objects.equals(foundedYear, that.foundedYear);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, country, foundedYear);
    }

    @Override
    public String toString() {
        return "RecordLabel{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", country='" + country + '\'' +
                ", foundedYear=" + foundedYear +
                '}';
    }
}
