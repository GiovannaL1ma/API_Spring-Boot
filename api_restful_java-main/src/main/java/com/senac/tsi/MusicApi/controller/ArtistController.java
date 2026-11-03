package com.senac.tsi.MusicApi.controller;

import com.senac.tsi.MusicApi.assembler.ArtistModelAssembler;
import com.senac.tsi.MusicApi.model.Artist;
import com.senac.tsi.MusicApi.model.ArtistType;
import com.senac.tsi.MusicApi.service.ArtistService;
import com.senac.tsi.MusicApi.web.RequiresApiKey;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// so cuida do HTTP (rota, status, links); as regras ficam no ArtistService
@RestController
@Tag(name = "Artists", description = "Gerenciamento dos artistas (Many-to-Many com Album e com Track nas participacoes)")
public class ArtistController {

    private static final String ARTIST_EXAMPLE = """
            { "name": "The Beatles", "country": "United Kingdom", "startYear": 1960, "type": "BAND" }""";

    private final ArtistService service;
    private final ArtistModelAssembler assembler;
    private final PagedResourcesAssembler<Artist> pagedResourcesAssembler;

    public ArtistController(ArtistService service,
                            ArtistModelAssembler assembler,
                            PagedResourcesAssembler<Artist> pagedResourcesAssembler) {
        this.service = service;
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
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(service.findAll(pageable), assembler));
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
        return assembler.toModel(service.findById(id));
    }

    @Operation(summary = "Creates a new artist", description = "Os albuns sao vinculados ao artista pelo campo artists de /albums")
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
            content = @Content(examples = @ExampleObject(value = "An artist with this name already exists")))
    @RequiresApiKey
    @PostMapping("/artists")
    public ResponseEntity<EntityModel<Artist>> newArtist(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "New artist",
                    required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Artist.class),
                            examples = @ExampleObject(value = ARTIST_EXAMPLE)))
            @RequestBody @Valid Artist newArtist) {

        EntityModel<Artist> entityModel = assembler.toModel(service.create(newArtist));
        // 201 com o header Location apontando para o artista novo
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
            content = @Content(examples = @ExampleObject(value = "An artist with this name already exists")))
    @RequiresApiKey
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
        return ResponseEntity.ok(assembler.toModel(service.update(id, newArtist)));
    }

    @Operation(summary = "Deletes an artist",
            description = "So e possivel excluir artistas sem albuns; as participacoes especiais em faixas sao desfeitas")
    @ApiResponse(responseCode = "204", description = "Successfully deleted an artist", content = @Content)
    @ApiResponse(responseCode = "400", description = "Invalid id - o id precisa ser um numero inteiro (ex.: 1)",
            content = @Content(examples = @ExampleObject(value = "Invalid value 'abc' for parameter 'id': it must be a whole number")))
    @ApiResponse(responseCode = "404", description = "Artist not found, maybe it's already deleted - nao existe artista com esse id",
            content = @Content(examples = @ExampleObject(value = "Could not find artist with ID: 99")))
    @ApiResponse(responseCode = "409", description = "Artist still has albums - o artista ainda aparece em algum album; " +
            "tire-o do album (PUT /albums/{id}) ou exclua o album antes",
            content = @Content(examples = @ExampleObject(value = "Artist with ID: 1 still has albums; remove the artist from them or delete them first")))
    @RequiresApiKey
    @DeleteMapping("/artists/{id}")
    public ResponseEntity<Void> deleteArtist(@Parameter(description = "Id of the artist", example = "1") @PathVariable Long id) {
        service.delete(id);
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
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(service.searchByName(name, pageable), assembler));
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
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(service.findByType(type, pageable), assembler));
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
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(service.findByGenre(genreId, pageable), assembler));
    }
}
