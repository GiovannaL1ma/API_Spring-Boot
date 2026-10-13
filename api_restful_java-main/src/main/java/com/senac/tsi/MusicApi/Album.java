package com.senac.tsi.MusicApi;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.util.*;

// @Entity é para a classe virar uma entidade do banco, ai cada objeto Album vira uma linha da tabela "album" e cada atributo vira uma coluna. o hibernate faz esse "mapeamento" e a tabela é criada automaticamente (jpa) quando a aplicacao sobe
// @Schema: so documentacao. O Swagger usa para
// descrever o modelo e mostrar exemplos.
@Entity
@Schema(description = "Album (disco) lancado por um ou mais artistas")
public class Album {
    private @Id
    //generatedvalue serve para o banco gerar o id automaticamente
    @GeneratedValue(strategy = GenerationType.IDENTITY) long id;
    // bean validatiom, se alguma falhar, a API responde 400 com a lista de erros (ver ApiExceptionAdvice) valor padrao.
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
    @Enumerated(EnumType.STRING)//grava no banco o TEXTO ("STUDIO") e nao a posicao (0, 1, 2...), assim o dado continua certo mesmo se a ordem do enum mudar.
    @Schema(example = "STUDIO")
    private AlbumType type;

    // MANY-TO-ONE (lado "muitos" do One-to-Many Gravadora -> Album): muitos albuns podem ser da mesma gravadora.
    @ManyToOne
    @JoinColumn(name = "label_id") //o join column serve para mudar o nome da coluna no banco (padrao seria "label_id" mesmo, mas poderia ser outro). A coluna é a FK (foreign key) que referencia a gravadora.
    private RecordLabel label;
    @NotEmpty // pelo menos 1 artista, senao 400
    @ManyToMany
    @JoinTable(name = "album_artist", joinColumns = @JoinColumn(name = "album_id"),inverseJoinColumns = @JoinColumn(name = "artist_id"))

    private Set<Artist> artists = new HashSet<>();
    @ManyToMany
    @JoinTable(name = "album_genre", joinColumns = @JoinColumn(name = "album_id"), inverseJoinColumns = @JoinColumn(name = "genre_id"))

    private Set<Genre> genres = new HashSet<>(); //set para nao ter repetidos, mesmo que o front envie 2x o mesmo genero.
    @JsonIgnore // @JsonIgnore: nao aparece no JSON do album
    @OneToMany(mappedBy = "album", cascade = CascadeType.REMOVE) //mappedBy diz que o dono do relacionamento e track.album
    @OrderBy("trackNumber")

    private List<Track> tracks = new ArrayList<>();
    @JsonIgnore
    @OneToOne(mappedBy = "album", cascade = CascadeType.REMOVE)
    private Cover cover;// p nao aparecer o json

    public Album() {
        //o construtor vazio existe para o Hibernate e o Jackson conseguirem criar um Album vazio e depois preencher sozinhos.
    }

    // construtor de conveniencia, para criar um album ja preenchido no codigo
    public Album(String title, Integer releaseYear, AlbumType type, RecordLabel label) {
        this.title = title;
        this.releaseYear = releaseYear;
        this.type = type;
        this.label = label;
    }

    
    // O Jackson usa os getters para gerar o JSON de resposta e os setters para ler o JSON recebido.
    // Campos com @JsonIgnore (tracks, cover) ficam fora do JSON mesmo tendo getter.
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

    // ---------- equals, hashCode e toString ----------

    // equals: define quando dois objetos Album sao "iguais" (mesmo id e mesmos dados).
    // E usado, por exemplo, pelos Sets dos relacionamentos para nao guardar repetidos.
    // Os relacionamentos ficam de fora de proposito, para evitar loops entre entidades.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Album album = (Album) o;
        return id == album.id && Objects.equals(title, album.title) && Objects.equals(releaseYear, album.releaseYear) && type == album.type;
    }

    // hashCode são usados por sets e maps p organizar objetos. 
    @Override
    public int hashCode() {
        return Objects.hash(id, title, releaseYear, type);
    }

    // toString: texto do objeto, util para logs e depuracao
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
