package com.senac.tsi.MusicApi;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.Set;

@RestController
@Tag(name = "Albums", description = "Gerenciamento dos albuns (Many-to-Many com Artist e Genre, Many-to-One com RecordLabel, One-to-One com Cover)")
public class AlbumController {

    private static final String ALBUM_EXAMPLE = """
            { "title": "Revolver", "releaseYear": 1966, "type": "STUDIO", "label": { "id": 1 },
              "artists": [ { "id": 1 } ], "genres": [ { "id": 1 }, { "id": 2 } ] }""";

    private final AlbumRepository repository;
    private final ArtistRepository artistRepository;
    private final GenreRepository genreRepository;
    private final RecordLabelRepository labelRepository;
    private final AlbumModelAssembler assembler;
    private final PagedResourcesAssembler<Album> pagedResourcesAssembler;

    public AlbumController(AlbumRepository repository,
                           ArtistRepository artistRepository,
                           GenreRepository genreRepository,
                           RecordLabelRepository labelRepository,
                           AlbumModelAssembler assembler,
                           PagedResourcesAssembler<Album> pagedResourcesAssembler) {
        this.repository = repository;
        this.artistRepository = artistRepository;
        this.genreRepository = genreRepository;
        this.labelRepository = labelRepository;
        this.assembler = assembler;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
    }

    @Operation(summary = "Get all albums", description = "Lista paginada de todos os albuns, com seus artistas, generos e gravadora")
    @ApiResponse(responseCode = "200", description = "Returned a paginated list of albums")
    @GetMapping("/albums")
    public ResponseEntity<PagedModel<EntityModel<Album>>> getAllAlbums(
            @ParameterObject @PageableDefault(size = 10, page = 0, sort = "id") Pageable pageable) {
        Page<Album> albumPage = repository.findAll(pageable);
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(albumPage, assembler));
    }

    @Operation(summary = "Get an album by its id", description = "Retorna um album com seus artistas, generos, gravadora e links para faixas e capa")
    @ApiResponse(responseCode = "200", description = "Returns a valid album",
            content = @Content(mediaType = "application/hal+json", schema = @Schema(implementation = Album.class)))
    @ApiResponse(responseCode = "404", description = "Album not found", content = @Content)
    @ApiResponse(responseCode = "400", description = "Invalid id", content = @Content)
    @GetMapping("/albums/{id}")
    public EntityModel<Album> getAlbumById(@Parameter(description = "Id of the album", example = "1") @PathVariable Long id) {
        Album album = repository.findById(id)
                .orElseThrow(() -> new AlbumNotFoundException(id));
        return assembler.toModel(album);
    }

    @Operation(summary = "Creates a new album",
            description = "Artistas e generos (Many-to-Many) e gravadora (opcional) sao vinculados pelo id. O album precisa de ao menos um artista")
    @ApiResponse(responseCode = "201", description = "Album created; Location header points to the new resource")
    @ApiResponse(responseCode = "400", description = "Bad request on the payload (ex.: lista de artistas vazia, type fora de STUDIO, LIVE, COMPILATION, EP, SOUNDTRACK)", content = @Content)
    @ApiResponse(responseCode = "404", description = "A referenced artist, genre or label was not found", content = @Content)
    @PostMapping("/albums")
    public ResponseEntity<EntityModel<Album>> newAlbum(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "New album",
                    required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Album.class),
                            examples = @ExampleObject(value = ALBUM_EXAMPLE)))
            @RequestBody @Valid Album newAlbum) {

        newAlbum.setArtists(resolveArtists(newAlbum.getArtists()));
        newAlbum.setGenres(resolveGenres(newAlbum.getGenres()));
        newAlbum.setLabel(resolveLabel(newAlbum.getLabel()));
        EntityModel<Album> entityModel = assembler.toModel(repository.save(newAlbum));

        return ResponseEntity.created(entityModel
                .getRequiredLink(IanaLinkRelations.SELF)
                .toUri()).body(entityModel);
    }

    @Operation(summary = "Updates an album", description = "Substitui todos os dados do album, inclusive as listas de artistas e generos")
    @ApiResponse(responseCode = "200", description = "Album updated",
            content = @Content(mediaType = "application/hal+json", schema = @Schema(implementation = Album.class)))
    @ApiResponse(responseCode = "400", description = "Bad request on the payload", content = @Content)
    @ApiResponse(responseCode = "404", description = "Album, artist, genre or label not found", content = @Content)
    @PutMapping("/albums/{id}")
    public ResponseEntity<EntityModel<Album>> updateAlbum(
            @Parameter(description = "Id of the album", example = "1") @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Updated album",
                    required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Album.class),
                            examples = @ExampleObject(value = ALBUM_EXAMPLE)))
            @RequestBody @Valid Album newAlbum) {

        Album updated = repository.findById(id)
                .map(album -> {
                    album.setTitle(newAlbum.getTitle());
                    album.setReleaseYear(newAlbum.getReleaseYear());
                    album.setType(newAlbum.getType());
                    album.setLabel(resolveLabel(newAlbum.getLabel()));
                    album.setArtists(resolveArtists(newAlbum.getArtists()));
                    album.setGenres(resolveGenres(newAlbum.getGenres()));
                    return repository.save(album);
                })
                .orElseThrow(() -> new AlbumNotFoundException(id));

        return ResponseEntity.ok(assembler.toModel(updated));
    }

    @Operation(summary = "Deletes an album", description = "Exclui tambem as faixas, a capa e os vinculos do album com artistas e generos")
    @ApiResponse(responseCode = "204", description = "Successfully deleted an album", content = @Content)
    @ApiResponse(responseCode = "404", description = "Album not found, maybe it's already deleted", content = @Content)
    @Transactional
    @DeleteMapping("/albums/{id}")
    public ResponseEntity<?> deleteAlbum(@Parameter(description = "Id of the album", example = "1") @PathVariable Long id) {
        var album = repository.findById(id);
        if (album.isEmpty())
            throw new AlbumNotFoundException(id);

        // Album e o dono dos Many-to-Many (album_artist e album_genre); faixas e capa sao removidas em cascata
        repository.delete(album.get());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Search albums by title", description = "Consulta personalizada: albuns cujo titulo contem o texto (sem diferenciar maiusculas)")
    @ApiResponse(responseCode = "200", description = "Returned a paginated list of albums")
    @GetMapping("/albums/search")
    public ResponseEntity<PagedModel<EntityModel<Album>>> getAlbumsByTitle(
            @Parameter(description = "Part of the album's title", example = "revolver") @RequestParam String title,
            @ParameterObject @PageableDefault(size = 10, page = 0, sort = "id") Pageable pageable) {
        Page<Album> albumPage = repository.findByTitleContainingIgnoreCase(title, pageable);
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(albumPage, assembler));
    }

    @Operation(summary = "Get albums by type", description = "Consulta personalizada: albuns filtrados pelo enum AlbumType (ex.: todos os discos ao vivo - LIVE)")
    @ApiResponse(responseCode = "200", description = "Returned a paginated list of albums")
    @ApiResponse(responseCode = "400", description = "Invalid type", content = @Content)
    @GetMapping("/albums/type/{type}")
    public ResponseEntity<PagedModel<EntityModel<Album>>> getAlbumsByType(
            @Parameter(description = "Type of the album", example = "STUDIO") @PathVariable AlbumType type,
            @ParameterObject @PageableDefault(size = 10, page = 0, sort = "id") Pageable pageable) {
        Page<Album> albumPage = repository.findByType(type, pageable);
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(albumPage, assembler));
    }

    @Operation(summary = "Get albums released in a period", description = "Consulta personalizada: albuns lancados entre dois anos (inclusive), do mais antigo ao mais recente")
    @ApiResponse(responseCode = "200", description = "Returned a paginated list of albums")
    @ApiResponse(responseCode = "400", description = "Start year is after end year", content = @Content)
    @GetMapping("/albums/released")
    public ResponseEntity<PagedModel<EntityModel<Album>>> getAlbumsReleasedBetween(
            @Parameter(description = "First year", example = "1960") @RequestParam int from,
            @Parameter(description = "Last year", example = "1979") @RequestParam int to,
            @ParameterObject @PageableDefault(size = 10, page = 0, sort = "releaseYear") Pageable pageable) {
        if (from > to)
            return ResponseEntity.badRequest().build();

        Page<Album> albumPage = repository.findByReleaseYearBetween(from, to, pageable);
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(albumPage, assembler));
    }

    @Operation(summary = "Get albums of an artist", description = "Consulta personalizada: discografia de um artista (Many-to-Many album_artist)")
    @ApiResponse(responseCode = "200", description = "Returned a paginated list of albums of the artist")
    @ApiResponse(responseCode = "404", description = "Artist not found", content = @Content)
    @GetMapping("/albums/artist/{artistId}")
    public ResponseEntity<PagedModel<EntityModel<Album>>> getAlbumsByArtist(
            @Parameter(description = "Id of the artist", example = "1") @PathVariable Long artistId,
            @ParameterObject @PageableDefault(size = 10, page = 0, sort = "releaseYear") Pageable pageable) {
        if (!artistRepository.existsById(artistId))
            throw new ArtistNotFoundException(artistId);

        Page<Album> albumPage = repository.findByArtistsId(artistId, pageable);
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(albumPage, assembler));
    }

    @Operation(summary = "Get albums of a genre", description = "Consulta personalizada: albuns classificados no genero (Many-to-Many album_genre)")
    @ApiResponse(responseCode = "200", description = "Returned a paginated list of albums of the genre")
    @ApiResponse(responseCode = "404", description = "Genre not found", content = @Content)
    @GetMapping("/albums/genre/{genreId}")
    public ResponseEntity<PagedModel<EntityModel<Album>>> getAlbumsByGenre(
            @Parameter(description = "Id of the genre", example = "1") @PathVariable Long genreId,
            @ParameterObject @PageableDefault(size = 10, page = 0, sort = "id") Pageable pageable) {
        if (!genreRepository.existsById(genreId))
            throw new GenreNotFoundException(genreId);

        Page<Album> albumPage = repository.findByGenresId(genreId, pageable);
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(albumPage, assembler));
    }

    @Operation(summary = "Get albums of a record label", description = "Consulta personalizada: catalogo de uma gravadora (One-to-Many)")
    @ApiResponse(responseCode = "200", description = "Returned a paginated list of albums of the label")
    @ApiResponse(responseCode = "404", description = "Record label not found", content = @Content)
    @GetMapping("/albums/label/{labelId}")
    public ResponseEntity<PagedModel<EntityModel<Album>>> getAlbumsByLabel(
            @Parameter(description = "Id of the record label", example = "1") @PathVariable Long labelId,
            @ParameterObject @PageableDefault(size = 10, page = 0, sort = "releaseYear") Pageable pageable) {
        if (!labelRepository.existsById(labelId))
            throw new RecordLabelNotFoundException(labelId);

        Page<Album> albumPage = repository.findByLabelId(labelId, pageable);
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(albumPage, assembler));
    }

    // Trocam os objetos recebidos (so com id) pelas entidades do banco
    private Set<Artist> resolveArtists(Set<Artist> artists) {
        Set<Artist> resolved = new HashSet<>();
        for (Artist artist : artists)
            resolved.add(artistRepository.findById(artist.getId())
                    .orElseThrow(() -> new ArtistNotFoundException(artist.getId())));
        return resolved;
    }

    private Set<Genre> resolveGenres(Set<Genre> genres) {
        Set<Genre> resolved = new HashSet<>();
        if (genres == null)
            return resolved;

        for (Genre genre : genres)
            resolved.add(genreRepository.findById(genre.getId())
                    .orElseThrow(() -> new GenreNotFoundException(genre.getId())));
        return resolved;
    }

    private RecordLabel resolveLabel(RecordLabel label) {
        if (label == null)
            return null;
        return labelRepository.findById(label.getId())
                .orElseThrow(() -> new RecordLabelNotFoundException(label.getId()));
    }
}
