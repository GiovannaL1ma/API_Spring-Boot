package com.senac.tsi.MusicApi;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.validator.constraints.URL;

import java.util.Objects;

@Entity
@Schema(description = "Capa (arte grafica) de um album")
public class Cover {
    private @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) long id;

    @NotBlank
    @Size(min = 2, max = 100)
    @Schema(description = "Artista grafico, designer ou fotografo responsavel pela capa", example = "Klaus Voormann")
    private String designer;

    @Size(max = 255)
    @Schema(example = "Colagem com desenhos a traco e fotografias dos quatro integrantes")
    private String description;

    @NotBlank
    @URL
    @Size(max = 255)
    @Schema(description = "Endereco da imagem da capa", example = "https://example.com/covers/revolver.jpg")
    private String imageUrl;

    // One-to-One: cada album tem uma unica capa (unique garante isso no banco)
    // No payload basta enviar o id: "album": { "id": 1 }
    @NotNull
    @OneToOne
    @JoinColumn(name = "album_id", unique = true)
    private Album album;

    public Cover() {
    }

    public Cover(String designer, String description, String imageUrl, Album album) {
        this.designer = designer;
        this.description = description;
        this.imageUrl = imageUrl;
        this.album = album;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getDesigner() {
        return designer;
    }

    public void setDesigner(String designer) {
        this.designer = designer;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Album getAlbum() {
        return album;
    }

    public void setAlbum(Album album) {
        this.album = album;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Cover cover = (Cover) o;
        return id == cover.id && Objects.equals(designer, cover.designer) && Objects.equals(description, cover.description) && Objects.equals(imageUrl, cover.imageUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, designer, description, imageUrl);
    }

    @Override
    public String toString() {
        return "Cover{" +
                "id=" + id +
                ", designer='" + designer + '\'' +
                ", description='" + description + '\'' +
                ", imageUrl='" + imageUrl + '\'' +
                '}';
    }
}
