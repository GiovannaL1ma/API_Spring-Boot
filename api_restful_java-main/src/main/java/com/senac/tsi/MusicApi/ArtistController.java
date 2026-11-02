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
import org.springframework.data.core.PropertyPath;
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
@RequestMapping("/v1") // versao 1 da API: todas as rotas daqui comecam com /v1 (a v2 fica ao lado, em /v2)
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
    @ApiResponse(responseCode = "400", description = "Invalid sort - o sort usa um campo que o artista nao tem (ex.: sort=abc). Campos validos: id, name, country, startYear, type",
            content = @Content(examples = @ExampleObject(value = "Invalid sort: Artist has no field 'abc'")))
    @GetMapping("/artists")
    public ResponseEntity<PagedModel<EntityModel<Artist>>> getAllArtists(
            @ParameterObject @PageableDefault(size = 10, page = 0, sort = "id") Pageable pageable) {
        Page<Artist> artistPage = repository.findAll(pageable);
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(artistPage, assembler));
    }

    @Operation(summary = "Get an artist by its id", description = "Retorna um artista com links para seus albuns e participacoes especiais")
    @ApiResponse(responseCode = "200", description = "Returns a valid artist",
            content = @Content(mediaType = "application/hal+json", schema = @Schema(implementation = Artist.class)))
    @ApiResponse(responseCode = "400", description = "Invalid id - o id precisa ser um numero inteiro (ex.: 1); textos como abc ou decimais como 1.5 sao recusados",
            content = @Content(examples = @ExampleObject(value = "Invalid value 'abc' for parameter 'id': it must be a whole number")))
    @ApiResponse(responseCode = "404", description = "Artist not found - nao existe artista com esse id (nunca foi criado ou ja foi excluido)",
            content = @Content(examples = @ExampleObject(value = "Could not find artist with ID: 99")))
    @GetMapping("/artists/{id}")
    public EntityModel<Artist> getArtistById(@Parameter(description = "Id of the artist", example = "1") @PathVariable Long id) {
        Artist artist = repository.findById(id)
                .orElseThrow(() -> new ArtistNotFoundException(id));
        return assembler.toModel(artist);
    }

    @Operation(summary = "Creates a new artist", description = "Os albuns sao vinculados ao artista pelo campo artists de /v1/albums")
    @ApiResponse(responseCode = "201", description = "Artist created; Location header points to the new resource")
    @ApiResponse(responseCode = "400", description = "Bad request on the payload - algum campo falhou na validacao (name vazio ou com mais de 100 caracteres, " +
            "country fora de 2 a 60 caracteres, startYear fora de 1900 a 2100), o type nao e SOLO, DUO, BAND ou ORCHESTRA, " +
            "um campo veio com o tipo errado (ex.: numero num campo de texto, decimal ou texto num campo inteiro) ou o JSON esta mal formado. " +
            "A resposta diz qual campo falhou e por que",
            content = @Content(examples = {
                    @ExampleObject(name = "Campo invalido", value = "{ \"name\": \"não deve estar em branco\", \"startYear\": \"deve ser maior que ou igual à 1900\" }"),
                    @ExampleObject(name = "Type invalido", value = "Invalid value for field 'type'. Accepted values: [SOLO, DUO, BAND, ORCHESTRA]"),
                    @ExampleObject(name = "Tipo errado", value = "Invalid value for field 'name': expected String"),
                    @ExampleObject(name = "JSON mal formado", value = "Malformed JSON: check quotes, commas and braces")}))
    @ApiResponse(responseCode = "409", description = "An artist with this name already exists - o name do artista e unico e ja existe outro artista com esse nome",
            content = @Content(examples = @ExampleObject(value = "Operation conflicts with existing data: a value that must be unique is already in use")))
    @PostMapping("/artists")
    public ResponseEntity<EntityModel<Artist>> newArtist(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "New artist",
                    required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Artist.class),
                            examples = @ExampleObject(value = ARTIST_EXAMPLE)))
            @RequestBody @Valid Artist newArtist) {

        // o id e gerado pelo banco; se vier no JSON e ignorado (senao o save sobrescreveria o artista com esse id)
        newArtist.setId(0);
        EntityModel<Artist> entityModel = assembler.toModel(repository.save(newArtist));

        return ResponseEntity.created(entityModel
                .getRequiredLink(IanaLinkRelations.SELF)
                .toUri()).body(entityModel);
    }

    @Operation(summary = "Updates an artist", description = "Substitui todos os dados do artista")
    @ApiResponse(responseCode = "200", description = "Artist updated",
            content = @Content(mediaType = "application/hal+json", schema = @Schema(implementation = Artist.class)))
    @ApiResponse(responseCode = "400", description = "Invalid id or payload - o id nao e um numero inteiro, ou algum campo do JSON e invalido (mesmas regras do POST)",
            content = @Content(examples = {
                    @ExampleObject(name = "Id invalido", value = "Invalid value 'abc' for parameter 'id': it must be a whole number"),
                    @ExampleObject(name = "Campo invalido", value = "{ \"country\": \"tamanho deve ser entre 2 e 60\" }"),
                    @ExampleObject(name = "Type invalido", value = "Invalid value for field 'type'. Accepted values: [SOLO, DUO, BAND, ORCHESTRA]")}))
    @ApiResponse(responseCode = "404", description = "Artist not found - nao existe artista com esse id para atualizar",
            content = @Content(examples = @ExampleObject(value = "Could not find artist with ID: 99")))
    @ApiResponse(responseCode = "409", description = "An artist with this name already exists - o novo name ja pertence a outro artista",
            content = @Content(examples = @ExampleObject(value = "Operation conflicts with existing data: a value that must be unique is already in use")))
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
    @ApiResponse(responseCode = "400", description = "Invalid id - o id precisa ser um numero inteiro (ex.: 1)",
            content = @Content(examples = @ExampleObject(value = "Invalid value 'abc' for parameter 'id': it must be a whole number")))
    @ApiResponse(responseCode = "404", description = "Artist not found, maybe it's already deleted - nao existe artista com esse id",
            content = @Content(examples = @ExampleObject(value = "Could not find artist with ID: 99")))
    @ApiResponse(responseCode = "409", description = "Artist still has albums - o artista ainda aparece em algum album; " +
            "tire-o do album (PUT /v1/albums/{id}) ou exclua o album antes",
            content = @Content(examples = @ExampleObject(value = "Artist with ID: 1 still has albums; remove the artist from them or delete them first")))
    @Transactional
    @DeleteMapping("/artists/{id}")
    public ResponseEntity<?> deleteArtist(@Parameter(description = "Id of the artist", example = "1") @PathVariable Long id) {
        var artist = repository.findById(id);
        if (artist.isEmpty())
            throw new ArtistNotFoundException(id);

        if (!artist.get().getAlbums().isEmpty())
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Artist with ID: " + id + " still has albums; remove the artist from them or delete them first");

        artist.get().getFeaturedOn().forEach(track -> track.getFeaturedArtists().remove(artist.get()));
        repository.delete(artist.get());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Search artists by name", description = "Consulta personalizada: artistas cujo nome contem o texto (sem diferenciar maiusculas)")
    @ApiResponse(responseCode = "200", description = "Returned a paginated list of artists")
    @ApiResponse(responseCode = "400", description = "Missing name or invalid sort - o parametro name e obrigatorio, ou o sort usa um campo que o artista nao tem (ex.: sort=abc)",
            content = @Content(examples = {
                    @ExampleObject(name = "Sem name", value = "Required parameter 'name' is missing"),
                    @ExampleObject(name = "Sort invalido", value = "Invalid sort: Artist has no field 'abc'")}))
    @GetMapping("/artists/search")
    public ResponseEntity<PagedModel<EntityModel<Artist>>> getArtistsByName(
            @Parameter(description = "Part of the artist's name", example = "elis") @RequestParam String name,
            @ParameterObject @PageableDefault(size = 10, page = 0, sort = "id") Pageable pageable) {
        Page<Artist> artistPage = repository.findByNameContainingIgnoreCase(name, pageable);
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(artistPage, assembler));
    }

    @Operation(summary = "Get artists by type", description = "Consulta personalizada: artistas filtrados pelo enum ArtistType (ex.: todas as bandas - BAND)")
    @ApiResponse(responseCode = "200", description = "Returned a paginated list of artists")
    @ApiResponse(responseCode = "400", description = "Invalid type or sort - o type precisa ser exatamente SOLO, DUO, BAND ou ORCHESTRA (em maiusculas), " +
            "ou o sort usa um campo que o artista nao tem",
            content = @Content(examples = {
                    @ExampleObject(name = "Type invalido", value = "Invalid value 'ROCK' for parameter 'type'. Accepted values: [SOLO, DUO, BAND, ORCHESTRA]"),
                    @ExampleObject(name = "Sort invalido", value = "Invalid sort: Artist has no field 'abc'")}))
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
    @ApiResponse(responseCode = "400", description = "Invalid genreId or sort - o genreId precisa ser um numero inteiro, ou o sort usa um campo que o artista nao tem",
            content = @Content(examples = {
                    @ExampleObject(name = "Id invalido", value = "Invalid value 'abc' for parameter 'genreId': it must be a whole number"),
                    @ExampleObject(name = "Sort invalido", value = "Invalid sort: Artist has no field 'abc'")}))
    @ApiResponse(responseCode = "404", description = "Genre not found - nao existe genero com esse id",
            content = @Content(examples = @ExampleObject(value = "Could not find genre with ID: 99")))
    @GetMapping("/artists/genre/{genreId}")
    public ResponseEntity<PagedModel<EntityModel<Artist>>> getArtistsByGenre(
            @Parameter(description = "Id of the genre", example = "1") @PathVariable Long genreId,
            @ParameterObject @PageableDefault(size = 10, page = 0, sort = "id") Pageable pageable) {
        if (!genreRepository.existsById(genreId))
            throw new GenreNotFoundException(genreId);

        // essa consulta usa @Query, entao o Spring Data nao confere o sort sozinho; sem isso campo inexistente virava 500
        pageable.getSort().forEach(order -> PropertyPath.from(order.getProperty(), Artist.class));

        Page<Artist> artistPage = repository.findByGenreId(genreId, pageable);
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(artistPage, assembler));
    }
}
