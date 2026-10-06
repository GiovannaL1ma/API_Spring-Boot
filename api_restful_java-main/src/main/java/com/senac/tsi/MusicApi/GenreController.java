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

@RestController
@Tag(name = "Genres", description = "Gerenciamento dos generos musicais (Many-to-Many com Album)")
public class GenreController {

    private static final String GENRE_EXAMPLE = """
            { "name": "Rock", "description": "Musica baseada em guitarra eletrica, baixo e bateria" }""";

    private final GenreRepository repository;
    private final GenreModelAssembler assembler;
    private final PagedResourcesAssembler<Genre> pagedResourcesAssembler;

    public GenreController(GenreRepository repository,
                           GenreModelAssembler assembler,
                           PagedResourcesAssembler<Genre> pagedResourcesAssembler) {
        this.repository = repository;
        this.assembler = assembler;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
    }

    @Operation(summary = "Get all genres", description = "Lista paginada de todos os generos")
    @ApiResponse(responseCode = "200", description = "Returned a paginated list of genres")
    @GetMapping("/genres")
    public ResponseEntity<PagedModel<EntityModel<Genre>>> getAllGenres(
            @ParameterObject @PageableDefault(size = 10, page = 0, sort = "id") Pageable pageable) {
        Page<Genre> genrePage = repository.findAll(pageable);
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(genrePage, assembler));
    }

    @Operation(summary = "Get a genre by its id", description = "Retorna um genero com links para seus albuns e artistas")
    @ApiResponse(responseCode = "200", description = "Returns a valid genre",
            content = @Content(mediaType = "application/hal+json", schema = @Schema(implementation = Genre.class)))
    @ApiResponse(responseCode = "404", description = "Genre not found", content = @Content)
    @ApiResponse(responseCode = "400", description = "Invalid id", content = @Content)
    @GetMapping("/genres/{id}")
    public EntityModel<Genre> getGenreById(@Parameter(description = "Id of the genre", example = "1") @PathVariable Long id) {
        Genre genre = repository.findById(id)
                .orElseThrow(() -> new GenreNotFoundException(id));
        return assembler.toModel(genre);
    }

    @Operation(summary = "Creates a new genre", description = "Para classificar um album no genero, use o campo genres no POST/PUT de /albums")
    @ApiResponse(responseCode = "201", description = "Genre created; Location header points to the new resource")
    @ApiResponse(responseCode = "400", description = "Bad request on the payload", content = @Content)
    @ApiResponse(responseCode = "409", description = "A genre with this name already exists", content = @Content)
    @PostMapping("/genres")
    public ResponseEntity<EntityModel<Genre>> newGenre(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "New genre",
                    required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Genre.class),
                            examples = @ExampleObject(value = GENRE_EXAMPLE)))
            @RequestBody @Valid Genre newGenre) {

        EntityModel<Genre> entityModel = assembler.toModel(repository.save(newGenre));

        return ResponseEntity.created(entityModel
                .getRequiredLink(IanaLinkRelations.SELF)
                .toUri()).body(entityModel);
    }

    @Operation(summary = "Updates a genre", description = "Substitui o nome e a descricao do genero")
    @ApiResponse(responseCode = "200", description = "Genre updated",
            content = @Content(mediaType = "application/hal+json", schema = @Schema(implementation = Genre.class)))
    @ApiResponse(responseCode = "400", description = "Bad request on the payload", content = @Content)
    @ApiResponse(responseCode = "404", description = "Genre not found", content = @Content)
    @ApiResponse(responseCode = "409", description = "A genre with this name already exists", content = @Content)
    @PutMapping("/genres/{id}")
    public ResponseEntity<EntityModel<Genre>> updateGenre(
            @Parameter(description = "Id of the genre", example = "1") @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Updated genre",
                    required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Genre.class),
                            examples = @ExampleObject(value = GENRE_EXAMPLE)))
            @RequestBody @Valid Genre newGenre) {

        Genre updated = repository.findById(id)
                .map(genre -> {
                    genre.setName(newGenre.getName());
                    genre.setDescription(newGenre.getDescription());
                    return repository.save(genre);
                })
                .orElseThrow(() -> new GenreNotFoundException(id));

        return ResponseEntity.ok(assembler.toModel(updated));
    }

    @Operation(summary = "Deletes a genre", description = "Tambem remove a classificacao dos albuns que usavam o genero")
    @ApiResponse(responseCode = "204", description = "Successfully deleted a genre", content = @Content)
    @ApiResponse(responseCode = "404", description = "Genre not found, maybe it's already deleted", content = @Content)
    @Transactional
    @DeleteMapping("/genres/{id}")
    public ResponseEntity<?> deleteGenre(@Parameter(description = "Id of the genre", example = "1") @PathVariable Long id) {
        var genre = repository.findById(id);
        if (genre.isEmpty())
            throw new GenreNotFoundException(id);

        // Album e o dono do Many-to-Many, entao o vinculo e removido pelo lado do album
        genre.get().getAlbums().forEach(album -> album.getGenres().remove(genre.get()));
        repository.delete(genre.get());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Search genres by name", description = "Consulta personalizada: generos cujo nome contem o texto (sem diferenciar maiusculas)")
    @ApiResponse(responseCode = "200", description = "Returned a paginated list of genres")
    @GetMapping("/genres/search")
    public ResponseEntity<PagedModel<EntityModel<Genre>>> getGenresByName(
            @Parameter(description = "Part of the genre's name", example = "rock") @RequestParam String name,
            @ParameterObject @PageableDefault(size = 10, page = 0, sort = "id") Pageable pageable) {
        Page<Genre> genrePage = repository.findByNameContainingIgnoreCase(name, pageable);
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(genrePage, assembler));
    }
}
