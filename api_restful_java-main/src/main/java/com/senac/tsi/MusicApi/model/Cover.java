package com.senac.tsi.MusicApi.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.validator.constraints.URL;

import java.util.Objects;

@Entity
@Schema(description = "Capa (arte grafica) de um album")
public class Cover {
    private @Id
    @Schema(description = "Gerado pelo banco: e ignorado se vier no corpo do POST ou do PUT", example = "1")
    @GeneratedValue(strategy = GenerationType.IDENTITY) long id;

    @NotBlank
    @Size(min = 2, max = 100)
    @Schema(description = "Artista grafico, designer ou fotografo responsavel pela capa: texto de 2 a 100 caracteres", example = "Klaus Voormann")
    private String designer;

    @Size(max = 255)
    @Schema(description = "Texto livre opcional com no maximo 255 caracteres (o 255 e o tamanho do texto, nao um valor numerico)",
            example = "Colagem com desenhos a traco e fotografias dos quatro integrantes")
    private String description;

    // min = 1 so para o Swagger nao mostrar "minimo 0" num campo obrigatorio (o @NotBlank ja barra vazio)
    @NotBlank
    @URL
    @Size(min = 1, max = 255)
    @Schema(description = "Endereco da imagem da capa: URL valida (ex.: https://...) com no maximo 255 caracteres",
            example = "https://example.com/covers/revolver.jpg")
    private String imageUrl;

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
