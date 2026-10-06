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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name = "Artists", description = "Gerenciamento dos artistas (Many-to-Many com Album e com Track nas participacoes)")
public class ArtistController {

    private static final String ARTIST_EXAMPLE = """
            { "name": "The Beatles", "country": "United Kingdom", "startYear": 1960, "type": "BAND" }""";

    private final ArtistRepository repository;
    private final GenreRepository genreRepository;
    private final ArtistModelAssembler assembler;
    private final PagedResourcesAssembler<Artist> pagedResourcesAssembler;

    public ArtistController(ArtistRepository repository,
                            GenreRepository genreRepository,
                            ArtistModelAssembler assembler,
                            PagedResourcesAssembler<Artist> pagedResourcesAssembler) {
        this.repository = repository;
        this.genreRepository = genreRepository;
        this.assembler = assembler;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
    }

    @Operation(summary = "Get all artists", description = "Lista paginada de todos os artistas")
    @ApiResponse(responseCode = "200", description = "Returned a paginated list of artists")
    @GetMapping("/artists")
    public ResponseEntity<PagedModel<EntityModel<Artist>>> getAllArtists(
            @ParameterObject @PageableDefault(size = 10, page = 0, sort = "id") Pageable pageable) {
        Page<Artist> artistPage = repository.findAll(pageable);
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(artistPage, assembler));
    }

    @Operation(summary = "Get an artist by its id", description = "Retorna um artista com links para seus albuns e participacoes especiais")
    @ApiResponse(responseCode = "200", description = "Returns a valid artist",
            content = @Content(mediaType = "application/hal+json", schema = @Schema(implementation = Artist.class)))
    @ApiResponse(responseCode = "404", description = "Artist not found", content = @Content)
    @ApiResponse(responseCode = "400", description = "Invalid id", content = @Content)
    @GetMapping("/artists/{id}")
    public EntityModel<Artist> getArtistById(@Parameter(description = "Id of the artist", example = "1") @PathVariable Long id) {
        Artist artist = repository.findById(id)
                .orElseThrow(() -> new ArtistNotFoundException(id));
        return assembler.toModel(artist);
    }

    @Operation(summary = "Creates a new artist", description = "Os albuns sao vinculados ao artista pelo campo artists de /albums")
    @ApiResponse(responseCode = "201", description = "Artist created; Location header points to the new resource")
    @ApiResponse(responseCode = "400", description = "Bad request on the payload (ex.: type fora de SOLO, DUO, BAND, ORCHESTRA)", content = @Content)
    @ApiResponse(responseCode = "409", description = "An artist with this name already exists", content = @Content)
    @PostMapping("/artists")
    public ResponseEntity<EntityModel<Artist>> newArtist(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "New artist",
                    required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Artist.class),
                            examples = @ExampleObject(value = ARTIST_EXAMPLE)))
            @RequestBody @Valid Artist newArtist) {

        EntityModel<Artist> entityModel = assembler.toModel(repository.save(newArtist));

        return ResponseEntity.created(entityModel
                .getRequiredLink(IanaLinkRelations.SELF)
                .toUri()).body(entityModel);
    }

    @Operation(summary = "Updates an artist", description = "Substitui todos os dados do artista")
    @ApiResponse(responseCode = "200", description = "Artist updated",
            content = @Content(mediaType = "application/hal+json", schema = @Schema(implementation = Artist.class)))
    @ApiResponse(responseCode = "400", description = "Bad request on the payload", content = @Content)
    @ApiResponse(responseCode = "404", description = "Artist not found", content = @Content)
    @ApiResponse(responseCode = "409", description = "An artist with this name already exists", content = @Content)
    @PutMapping("/artists/{id}")
    public ResponseEntity<EntityModel<Artist>> updateArtist(
            @Parameter(description = "Id of the artist", example = "1") @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Updated artist",
                    required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Artist.class),
                            examples = @ExampleObject(value = ARTIST_EXAMPLE)))
            @RequestBody @Valid Artist newArtist) {

        Artist updated = repository.findById(id)
                .map(artist -> {
                    artist.setName(newArtist.getName());
                    artist.setCountry(newArtist.getCountry());
                    artist.setStartYear(newArtist.getStartYear());
                    artist.setType(newArtist.getType());
                    return repository.save(artist);
                })
                .orElseThrow(() -> new ArtistNotFoundException(id));

        return ResponseEntity.ok(assembler.toModel(updated));
    }

    @Operation(summary = "Deletes an artist",
            description = "So e possivel excluir artistas sem albuns; as participacoes especiais em faixas sao desfeitas")
    @ApiResponse(responseCode = "204", description = "Successfully deleted an artist", content = @Content)
    @ApiResponse(responseCode = "404", description = "Artist not found, maybe it's already deleted", content = @Content)
    @ApiResponse(responseCode = "409", description = "Artist still has albums", content = @Content)
    @Transactional
    @DeleteMapping("/artists/{id}")
    public ResponseEntity<?> deleteArtist(@Parameter(description = "Id of the artist", example = "1") @PathVariable Long id) {
        var artist = repository.findById(id);
        if (artist.isEmpty())
            return ResponseEntity.notFound().build();

        if (!artist.get().getAlbums().isEmpty())
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Artist still has albums");

        // Track e o dono do Many-to-Many das participacoes, entao o vinculo e removido pelo lado da faixa
        artist.get().getFeaturedOn().forEach(track -> track.getFeaturedArtists().remove(artist.get()));
        repository.delete(artist.get());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Search artists by name", description = "Consulta personalizada: artistas cujo nome contem o texto (sem diferenciar maiusculas)")
    @ApiResponse(responseCode = "200", description = "Returned a paginated list of artists")
    @GetMapping("/artists/search")
    public ResponseEntity<PagedModel<EntityModel<Artist>>> getArtistsByName(
            @Parameter(description = "Part of the artist's name", example = "elis") @RequestParam String name,
            @ParameterObject @PageableDefault(size = 10, page = 0, sort = "id") Pageable pageable) {
        Page<Artist> artistPage = repository.findByNameContainingIgnoreCase(name, pageable);
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(artistPage, assembler));
    }

    @Operation(summary = "Get artists by type", description = "Consulta personalizada: artistas filtrados pelo enum ArtistType (ex.: todas as bandas - BAND)")
    @ApiResponse(responseCode = "200", description = "Returned a paginated list of artists")
    @ApiResponse(responseCode = "400", description = "Invalid type", content = @Content)
    @GetMapping("/artists/type/{type}")
    public ResponseEntity<PagedModel<EntityModel<Artist>>> getArtistsByType(
            @Parameter(description = "Type of the artist", example = "BAND") @PathVariable ArtistType type,
            @ParameterObject @PageableDefault(size = 10, page = 0, sort = "id") Pageable pageable) {
        Page<Artist> artistPage = repository.findByType(type, pageable);
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(artistPage, assembler));
    }

    @Operation(summary = "Get artists of a genre",
            description = "Consulta personalizada: artistas que lancaram albuns do genero (atravessa album_genre e album_artist)")
    @ApiResponse(responseCode = "200", description = "Returned a paginated list of artists of the genre")
    @ApiResponse(responseCode = "404", description = "Genre not found", content = @Content)
    @GetMapping("/artists/genre/{genreId}")
    public ResponseEntity<PagedModel<EntityModel<Artist>>> getArtistsByGenre(
            @Parameter(description = "Id of the genre", example = "1") @PathVariable Long genreId,
            @ParameterObject @PageableDefault(size = 10, page = 0, sort = "id") Pageable pageable) {
        if (!genreRepository.existsById(genreId))
            throw new GenreNotFoundException(genreId);

        Page<Artist> artistPage = repository.findByGenreId(genreId, pageable);
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(artistPage, assembler));
    }
}
