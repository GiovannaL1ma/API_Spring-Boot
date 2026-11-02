package com.senac.tsi.MusicApi.controller;

import com.senac.tsi.MusicApi.assembler.AlbumModelAssembler;
import com.senac.tsi.MusicApi.model.Album;
import com.senac.tsi.MusicApi.model.AlbumType;
import com.senac.tsi.MusicApi.service.AlbumService;
import com.senac.tsi.MusicApi.web.RequiresApiKey;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// CONTROLLER recebe as requisicoes HTTP e devolve as respostas em JSON.
// @RestController: classe que responde requisicoes REST; o retorno dos metodos vira JSON.
// @Tag: so documentacao; agrupa os endpoints no Swagger com um nome e uma descricao.
// as regras de negocio (resolver artistas, generos e gravadora, validar o periodo) ficam no AlbumService
@RestController
@Tag(name = "Albums", description = "Gerenciamento dos albuns (Many-to-Many com Artist e Genre, Many-to-One com RecordLabel, One-to-One com Cover)")
public class AlbumController {
    // JSON de exemplo que aparece ja preenchido no Swagger (no POST e no PUT).
    private static final String ALBUM_EXAMPLE = """
            { "title": "Revolver", "releaseYear": 1966, "type": "STUDIO", "label": { "id": 1 },"artists": [ { "id": 1 } ], "genres": [ { "id": 1 }, { "id": 2 } ] }""";

    //  - service: regras de negocio e acesso ao banco
    //  - assembler: coloca os links HATEOAS em UM album (EntityModel)
    //  - pagedResourcesAssembler: monta a lista paginada com links (PagedModel)
    private final AlbumService service;
    private final AlbumModelAssembler assembler;
    private final PagedResourcesAssembler<Album> pagedResourcesAssembler;
    public AlbumController(AlbumService service,
        AlbumModelAssembler assembler,
        PagedResourcesAssembler<Album> pagedResourcesAssembler) {
        this.service = service;
        this.assembler = assembler;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
    }

    @Operation(summary = "Get all albums", description = "Lista paginada de todos os albuns, com seus artistas, generos e gravadora")
    @ApiResponse(responseCode = "200", description = "Returned a paginated list of albums")
    @ApiResponse(responseCode = "400", description = "Invalid sort - o sort usa um campo que o album nao tem (ex.: sort=abc). Campos validos: id, title, releaseYear, type",
            content = @Content(examples = @ExampleObject(value = "Invalid sort: Album has no field 'abc'")))
    @GetMapping("/albums")
    public ResponseEntity<PagedModel<EntityModel<Album>>> getAllAlbums(
            @ParameterObject @PageableDefault(size = 10, page = 0, sort = "id") Pageable pageable) {
        //pageable é passado para o service, que devolve a pagina de albuns solicitada.
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(service.findAll(pageable), assembler));
    }// esse pedaço de código é responsável pelo get dos albums, fazendo a paginação e retornando o json com os links de navegação.

    @Operation(summary = "Get an album by its id", description = "Retorna um album com seus artistas, generos, gravadora e links para faixas e capa")

    @ApiResponse(responseCode = "200", description = "Returns a valid album",content = @Content(mediaType = "application/hal+json", schema = @Schema(implementation = Album.class)))
    @ApiResponse(responseCode = "400", description = "Invalid id - o id precisa ser um numero inteiro (ex.: 1); textos como abc ou decimais como 1.5 sao recusados",
            content = @Content(examples = @ExampleObject(value = "Invalid value 'abc' for parameter 'id': it must be a whole number")))
    @ApiResponse(responseCode = "404", description = "Album not found - nao existe album com esse id (nunca foi criado ou ja foi excluido)",
            content = @Content(examples = @ExampleObject(value = "Could not find album with ID: 99")))
    @GetMapping("/albums/{id}")

    public EntityModel<Album> getAlbumById(@Parameter(description = "Id of the album", example = "1") @PathVariable Long id) {
        return assembler.toModel(service.findById(id));
    }

    // @Valid: dispara a Bean Validation (@NotBlank, @NotEmpty, @Min...). Se falhar -> 400.
    @Operation(summary = "Creates a new album",description = "Artistas e generos (Many-to-Many) e gravadora (opcional) sao vinculados pelo id. O album precisa de ao menos um artista")

    @ApiResponse(responseCode = "201", description = "Album created; Location header points to the new resource")

    @ApiResponse(responseCode = "400", description = "Bad request on the payload - algum campo falhou na validacao (title vazio ou com mais de 150 caracteres, " +
            "releaseYear fora de 1900 a 2100, lista artists vazia), o type nao e STUDIO, LIVE, COMPILATION, EP ou SOUNDTRACK, " +
            "um campo veio com o tipo errado (ex.: numero num campo de texto, decimal ou texto num campo inteiro) ou o JSON esta mal formado. " +
            "A resposta diz qual campo falhou e por que",
            content = @Content(examples = {
                    @ExampleObject(name = "Campo invalido", value = "{ \"title\": \"não deve estar em branco\", \"artists\": \"não deve estar vazio\" }"),
                    @ExampleObject(name = "Type invalido", value = "Invalid value for field 'type'. Accepted values: [STUDIO, LIVE, COMPILATION, EP, SOUNDTRACK]"),
                    @ExampleObject(name = "Tipo errado", value = "Invalid value for field 'releaseYear': expected Integer")}))

    @ApiResponse(responseCode = "404", description = "Artist, genre or label not found - algum id informado em artists, genres ou label nao existe; " +
            "crie o registro antes ou corrija o id",
            content = @Content(examples = {
                    @ExampleObject(name = "Artista", value = "Could not find artist with ID: 99"),
                    @ExampleObject(name = "Genero", value = "Could not find genre with ID: 99"),
                    @ExampleObject(name = "Gravadora", value = "Could not find record label with ID: 99")}))

    @RequiresApiKey
    @PostMapping("/albums")
    public ResponseEntity<EntityModel<Album>> newAlbum(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(//io. é documentação do swagger
                    description = "New album",
                    required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Album.class),
                            examples = @ExampleObject(value = ALBUM_EXAMPLE)))
            @RequestBody @Valid Album newAlbum) {

        // o service resolve artistas, generos e gravadora pelo id e faz o insert; o assembler add os links hateoas
        EntityModel<Album> entityModel = assembler.toModel(service.create(newAlbum));
        return ResponseEntity.created(entityModel
                .getRequiredLink(IanaLinkRelations.SELF)
                .toUri()).body(entityModel);
    }//retorna 201, com o header Location apontando para o novo album e o JSON do album criado

    @Operation(summary = "Updates an album", description = "Substitui todos os dados do album, inclusive as listas de artistas e generos")
    @ApiResponse(responseCode = "200", description = "Album updated", content = @Content(mediaType = "application/hal+json", schema = @Schema(implementation = Album.class)))

    @ApiResponse(responseCode = "400", description = "Invalid id or payload - o id nao e um numero inteiro, ou algum campo do JSON e invalido (mesmas regras do POST)",
            content = @Content(examples = {
                    @ExampleObject(name = "Id invalido", value = "Invalid value 'abc' for parameter 'id': it must be a whole number"),
                    @ExampleObject(name = "Campo invalido", value = "{ \"releaseYear\": \"deve ser menor que ou igual à 2100\" }"),
                    @ExampleObject(name = "Type invalido", value = "Invalid value for field 'type'. Accepted values: [STUDIO, LIVE, COMPILATION, EP, SOUNDTRACK]")}))

    @ApiResponse(responseCode = "404", description = "Album, artist, genre or label not found - o album com esse id nao existe, " +
            "ou algum id informado em artists, genres ou label nao existe",
            content = @Content(examples = {
                    @ExampleObject(name = "Album", value = "Could not find album with ID: 99"),
                    @ExampleObject(name = "Artista", value = "Could not find artist with ID: 99")}))

    @RequiresApiKey
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
        //se o album nao existir, o service lanca AlbumNotFoundException e a API responde 404.
        return ResponseEntity.ok(assembler.toModel(service.update(id, newAlbum)));
    }

    // Respostas: 204 No Content (sucesso, sem corpo) ou 404 se o id nao existir.
    @Operation(summary = "Deletes an album", description = "Exclui tambem as faixas, a capa e os vinculos do album com artistas e generos")
    @ApiResponse(responseCode = "204", description = "Successfully deleted an album", content = @Content)
    @ApiResponse(responseCode = "400", description = "Invalid id - o id precisa ser um numero inteiro (ex.: 1)",
            content = @Content(examples = @ExampleObject(value = "Invalid value 'abc' for parameter 'id': it must be a whole number")))
    @ApiResponse(responseCode = "404", description = "Album not found, maybe it's already deleted - nao existe album com esse id",
            content = @Content(examples = @ExampleObject(value = "Could not find album with ID: 99")))
    @RequiresApiKey
    @DeleteMapping("/albums/{id}")
    public ResponseEntity<Void> deleteAlbum(@Parameter(description = "Id of the album", example = "1") @PathVariable Long id) {
        // album e o dono dos Many-to-Many (album_artist e album_genre); faixas e capa sao removidas em cascata
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    // as consultas seguem o mesmo formato da listagem: recebem Pageable e devolvem PagedModel
    @Operation(summary = "Search albums by title", description = "Consulta personalizada: albuns cujo titulo contem o texto (sem diferenciar maiusculas)")
    @ApiResponse(responseCode = "200", description = "Returned a paginated list of albums")
    @ApiResponse(responseCode = "400", description = "Missing title or invalid sort - o parametro title e obrigatorio, ou o sort usa um campo que o album nao tem (ex.: sort=abc)",
            content = @Content(examples = {
                    @ExampleObject(name = "Sem title", value = "Required parameter 'title' is missing"),
                    @ExampleObject(name = "Sort invalido", value = "Invalid sort: Album has no field 'abc'")}))
    @GetMapping("/albums/search")
    public ResponseEntity<PagedModel<EntityModel<Album>>> getAlbumsByTitle(
            @Parameter(description = "Part of the album's title", example = "revolver") @RequestParam String title,
            @ParameterObject @PageableDefault(size = 10, page = 0, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(service.searchByTitle(title, pageable), assembler));
    }//esse pedaço de código faz a busca de album pelo titulo, recebendo o parametro title e devolvendo a pagina de albuns que contem o texto do parametro

    @Operation(summary = "Get albums by type", description = "Consulta personalizada: albuns filtrados pelo enum AlbumType (ex.: todos os discos ao vivo - LIVE)")
    @ApiResponse(responseCode = "200", description = "Returned a paginated list of albums")
    @ApiResponse(responseCode = "400", description = "Invalid type or sort - o type precisa ser exatamente STUDIO, LIVE, COMPILATION, EP ou SOUNDTRACK (em maiusculas), " +
            "ou o sort usa um campo que o album nao tem",
            content = @Content(examples = {
                    @ExampleObject(name = "Type invalido", value = "Invalid value 'XYZ' for parameter 'type'. Accepted values: [STUDIO, LIVE, COMPILATION, EP, SOUNDTRACK]"),
                    @ExampleObject(name = "Sort invalido", value = "Invalid sort: Album has no field 'abc'")}))
    @GetMapping("/albums/type/{type}")
    public ResponseEntity<PagedModel<EntityModel<Album>>> getAlbumsByType(
            @Parameter(description = "Type of the album", example = "STUDIO") @PathVariable AlbumType type,
            @ParameterObject @PageableDefault(size = 10, page = 0, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(service.findByType(type, pageable), assembler));
    }//aqui recebe o parametro tipo

    // Se o periodo estiver invertido (from > to) responde 400 Bad Request.
    @Operation(summary = "Get albums released in a period", description = "Consulta personalizada: albuns lancados entre dois anos (inclusive), do mais antigo ao mais recente")
    @ApiResponse(responseCode = "200", description = "Returned a paginated list of albums")
    @ApiResponse(responseCode = "400", description = "Invalid period - from e maior que to (ex.: from=1980&to=1960), algum ano fora de 1900 a 2100, " +
            "faltou from ou to, algum deles nao e um numero inteiro, ou o sort usa um campo que o album nao tem",
            content = @Content(examples = {
                    @ExampleObject(name = "Periodo invertido", value = "Invalid period: from (1980) must be less than or equal to to (1960)"),
                    @ExampleObject(name = "Ano fora do limite", value = "{ \"from\": \"deve ser maior que ou igual à 1900\" }"),
                    @ExampleObject(name = "Sem from", value = "Required parameter 'from' is missing"),
                    @ExampleObject(name = "Ano nao numerico", value = "Invalid value 'abc' for parameter 'from': it must be a whole number")}))
    @GetMapping("/albums/released")
    public ResponseEntity<PagedModel<EntityModel<Album>>> getAlbumsReleasedBetween(
            @Parameter(description = "First year (1900 a 2100)", example = "1960") @RequestParam @Min(1900) @Max(2100) int from,
            @Parameter(description = "Last year (1900 a 2100)", example = "1979") @RequestParam @Min(1900) @Max(2100) int to,
            @ParameterObject @PageableDefault(size = 10, page = 0, sort = "releaseYear") Pageable pageable) {
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(service.findReleasedBetween(from, to, pageable), assembler));
    }//aqui recebe os parametros from e to, e devolve a pagina de albuns que foram lancados entre esses anos, inclusive. Se o from for maior que o to, devolve 400.


    @Operation(summary = "Get albums of an artist", description = "Consulta personalizada: discografia de um artista (Many-to-Many album_artist)")
    @ApiResponse(responseCode = "200", description = "Returned a paginated list of albums of the artist")
    @ApiResponse(responseCode = "400", description = "Invalid artistId or sort - o artistId precisa ser um numero inteiro, ou o sort usa um campo que o album nao tem",
            content = @Content(examples = {
                    @ExampleObject(name = "Id invalido", value = "Invalid value 'abc' for parameter 'artistId': it must be a whole number"),
                    @ExampleObject(name = "Sort invalido", value = "Invalid sort: Album has no field 'abc'")}))
    @ApiResponse(responseCode = "404", description = "Artist not found - nao existe artista com esse id",
            content = @Content(examples = @ExampleObject(value = "Could not find artist with ID: 99")))
    @GetMapping("/albums/artist/{artistId}")
    public ResponseEntity<PagedModel<EntityModel<Album>>> getAlbumsByArtist(
            @Parameter(description = "Id of the artist", example = "1") @PathVariable Long artistId,
            @ParameterObject @PageableDefault(size = 10, page = 0, sort = "releaseYear") Pageable pageable) {
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(service.findByArtist(artistId, pageable), assembler));
    }//aqui recebe o parametro artistId e devolve a pagina de albuns que pertencem a esse artista. se o artista nao existir, devolve 404.

    @Operation(summary = "Get albums of a genre", description = "Consulta personalizada: albuns classificados no genero (Many-to-Many album_genre)")
    @ApiResponse(responseCode = "200", description = "Returned a paginated list of albums of the genre")
    @ApiResponse(responseCode = "400", description = "Invalid genreId or sort - o genreId precisa ser um numero inteiro, ou o sort usa um campo que o album nao tem",
            content = @Content(examples = {
                    @ExampleObject(name = "Id invalido", value = "Invalid value 'abc' for parameter 'genreId': it must be a whole number"),
                    @ExampleObject(name = "Sort invalido", value = "Invalid sort: Album has no field 'abc'")}))
    @ApiResponse(responseCode = "404", description = "Genre not found - nao existe genero com esse id",
            content = @Content(examples = @ExampleObject(value = "Could not find genre with ID: 99")))
    @GetMapping("/albums/genre/{genreId}")
    public ResponseEntity<PagedModel<EntityModel<Album>>> getAlbumsByGenre(
            @Parameter(description = "Id of the genre", example = "1") @PathVariable Long genreId,
            @ParameterObject @PageableDefault(size = 10, page = 0, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(service.findByGenre(genreId, pageable), assembler));
    }//aqui recebe o parametro genreId e devolve a pagina de albuns que pertencem a esse genero. se o genero nao existir, devolve 404.

    @Operation(summary = "Get albums of a record label", description = "Consulta personalizada: catalogo de uma gravadora (One-to-Many)")
    @ApiResponse(responseCode = "200", description = "Returned a paginated list of albums of the label")
    @ApiResponse(responseCode = "400", description = "Invalid labelId or sort - o labelId precisa ser um numero inteiro, ou o sort usa um campo que o album nao tem",
            content = @Content(examples = {
                    @ExampleObject(name = "Id invalido", value = "Invalid value 'abc' for parameter 'labelId': it must be a whole number"),
                    @ExampleObject(name = "Sort invalido", value = "Invalid sort: Album has no field 'abc'")}))
    @ApiResponse(responseCode = "404", description = "Record label not found - nao existe gravadora com esse id",
            content = @Content(examples = @ExampleObject(value = "Could not find record label with ID: 99")))
    @GetMapping("/albums/label/{labelId}")
    public ResponseEntity<PagedModel<EntityModel<Album>>> getAlbumsByLabel(
            @Parameter(description = "Id of the record label", example = "1") @PathVariable Long labelId,
            @ParameterObject @PageableDefault(size = 10, page = 0, sort = "releaseYear") Pageable pageable) {
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(service.findByLabel(labelId, pageable), assembler));
    }//recebe o parametro labelId e devolve a pagina de albuns que pertencem a essa gravadora. se a gravadora nao existir, devolve 404.
}
