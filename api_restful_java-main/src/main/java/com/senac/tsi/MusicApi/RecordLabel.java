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
    @Schema(description = "Gerado pelo banco: e ignorado se vier no corpo do POST ou do PUT. " +
            "Para apontar para uma gravadora em outro recurso, mande so o id (ex.: \"label\": { \"id\": 1 })", example = "1")
    @GeneratedValue(strategy = GenerationType.IDENTITY) long id;

    @NotBlank
    @Size(min = 2, max = 100)
    @Column(unique = true)
    @Schema(description = "Nome unico da gravadora: texto de 2 a 100 caracteres", example = "Parlophone")
    private String name;

    @NotBlank
    @Size(min = 2, max = 60)
    @Schema(description = "Pais da gravadora: texto de 2 a 60 caracteres", example = "United Kingdom")
    private String country;

    @NotNull
    @Min(1850)
    @Max(2100)
    @Schema(description = "Ano de fundacao: numero inteiro de 1850 a 2100", example = "1896")
    private Integer foundedYear;

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
