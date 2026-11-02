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
@RequestMapping("/v1") // versao 1 da API: todas as rotas daqui comecam com /v1 (a v2 fica ao lado, em /v2)
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
    @ApiResponse(responseCode = "400", description = "Invalid sort - o sort usa um campo que o genero nao tem (ex.: sort=abc). Campos validos: id, name, description",
            content = @Content(examples = @ExampleObject(value = "Invalid sort: Genre has no field 'abc'")))
    @GetMapping("/genres")
    public ResponseEntity<PagedModel<EntityModel<Genre>>> getAllGenres(
            @ParameterObject @PageableDefault(size = 10, page = 0, sort = "id") Pageable pageable) {
        Page<Genre> genrePage = repository.findAll(pageable);
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(genrePage, assembler));
    }

    @Operation(summary = "Get a genre by its id", description = "Retorna um genero com links para seus albuns e artistas")
    @ApiResponse(responseCode = "200", description = "Returns a valid genre",
            content = @Content(mediaType = "application/hal+json", schema = @Schema(implementation = Genre.class)))
    @ApiResponse(responseCode = "400", description = "Invalid id - o id precisa ser um numero inteiro (ex.: 1); textos como abc ou decimais como 1.5 sao recusados",
            content = @Content(examples = @ExampleObject(value = "Invalid value 'abc' for parameter 'id': it must be a whole number")))
    @ApiResponse(responseCode = "404", description = "Genre not found - nao existe genero com esse id (nunca foi criado ou ja foi excluido)",
            content = @Content(examples = @ExampleObject(value = "Could not find genre with ID: 99")))
    @GetMapping("/genres/{id}")
    public EntityModel<Genre> getGenreById(@Parameter(description = "Id of the genre", example = "1") @PathVariable Long id) {
        Genre genre = repository.findById(id)
                .orElseThrow(() -> new GenreNotFoundException(id));
        return assembler.toModel(genre);
    }

    @Operation(summary = "Creates a new genre", description = "Para classificar um album no genero, use o campo genres no POST/PUT de /v1/albums")
    @ApiResponse(responseCode = "201", description = "Genre created; Location header points to the new resource")
    @ApiResponse(responseCode = "400", description = "Bad request on the payload - algum campo falhou na validacao (name vazio ou fora de 2 a 50 caracteres, " +
            "description com mais de 255 caracteres de texto), um campo veio com o tipo errado (ex.: \"description\": 300 - o campo e texto, " +
            "entao numero e recusado) ou o JSON esta mal formado. A resposta diz qual campo falhou e por que",
            content = @Content(examples = {
                    @ExampleObject(name = "Campo invalido", value = "{ \"name\": \"tamanho deve ser entre 2 e 50\" }"),
                    @ExampleObject(name = "Tipo errado", value = "Invalid value for field 'description': expected String"),
                    @ExampleObject(name = "JSON mal formado", value = "Malformed JSON: check quotes, commas and braces")}))
    @ApiResponse(responseCode = "409", description = "A genre with this name already exists - o name do genero e unico e ja existe outro genero com esse nome",
            content = @Content(examples = @ExampleObject(value = "Operation conflicts with existing data: a value that must be unique is already in use")))
    @PostMapping("/genres")
    public ResponseEntity<EntityModel<Genre>> newGenre(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "New genre",
                    required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Genre.class),
                            examples = @ExampleObject(value = GENRE_EXAMPLE)))
            @RequestBody @Valid Genre newGenre) {

        // o id e gerado pelo banco; se vier no JSON e ignorado (senao o save sobrescreveria o genero com esse id)
        newGenre.setId(0);
        EntityModel<Genre> entityModel = assembler.toModel(repository.save(newGenre));

        return ResponseEntity.created(entityModel
                .getRequiredLink(IanaLinkRelations.SELF)
                .toUri()).body(entityModel);
    }

    @Operation(summary = "Updates a genre", description = "Substitui o nome e a descricao do genero")
    @ApiResponse(responseCode = "200", description = "Genre updated",
            content = @Content(mediaType = "application/hal+json", schema = @Schema(implementation = Genre.class)))
    @ApiResponse(responseCode = "400", description = "Invalid id or payload - o id nao e um numero inteiro, ou algum campo do JSON e invalido (mesmas regras do POST)",
            content = @Content(examples = {
                    @ExampleObject(name = "Id invalido", value = "Invalid value 'abc' for parameter 'id': it must be a whole number"),
                    @ExampleObject(name = "Campo invalido", value = "{ \"name\": \"não deve estar em branco\" }")}))
    @ApiResponse(responseCode = "404", description = "Genre not found - nao existe genero com esse id para atualizar",
            content = @Content(examples = @ExampleObject(value = "Could not find genre with ID: 99")))
    @ApiResponse(responseCode = "409", description = "A genre with this name already exists - o novo name ja pertence a outro genero",
            content = @Content(examples = @ExampleObject(value = "Operation conflicts with existing data: a value that must be unique is already in use")))
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
    @ApiResponse(responseCode = "400", description = "Invalid id - o id precisa ser um numero inteiro (ex.: 1)",
            content = @Content(examples = @ExampleObject(value = "Invalid value 'abc' for parameter 'id': it must be a whole number")))
    @ApiResponse(responseCode = "404", description = "Genre not found, maybe it's already deleted - nao existe genero com esse id",
            content = @Content(examples = @ExampleObject(value = "Could not find genre with ID: 99")))
    @Transactional
    @DeleteMapping("/genres/{id}")
    public ResponseEntity<?> deleteGenre(@Parameter(description = "Id of the genre", example = "1") @PathVariable Long id) {
        var genre = repository.findById(id);
        if (genre.isEmpty())
            throw new GenreNotFoundException(id);

        genre.get().getAlbums().forEach(album -> album.getGenres().remove(genre.get()));
        repository.delete(genre.get());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Search genres by name", description = "Consulta personalizada: generos cujo nome contem o texto (sem diferenciar maiusculas)")
    @ApiResponse(responseCode = "200", description = "Returned a paginated list of genres")
    @ApiResponse(responseCode = "400", description = "Missing name or invalid sort - o parametro name e obrigatorio, ou o sort usa um campo que o genero nao tem (ex.: sort=abc)",
            content = @Content(examples = {
                    @ExampleObject(name = "Sem name", value = "Required parameter 'name' is missing"),
                    @ExampleObject(name = "Sort invalido", value = "Invalid sort: Genre has no field 'abc'")}))
    @GetMapping("/genres/search")
    public ResponseEntity<PagedModel<EntityModel<Genre>>> getGenresByName(
            @Parameter(description = "Part of the genre's name", example = "rock") @RequestParam String name,
            @ParameterObject @PageableDefault(size = 10, page = 0, sort = "id") Pageable pageable) {
        Page<Genre> genrePage = repository.findByNameContainingIgnoreCase(name, pageable);
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(genrePage, assembler));
    }
}
