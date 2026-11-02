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
@Tag(name = "Covers", description = "Gerenciamento das capas dos albuns (relacionamento One-to-One com Album)")
public class CoverController {

    private static final String COVER_EXAMPLE = """
            { "designer": "Klaus Voormann", "description": "Colagem com desenhos a traco e fotografias dos quatro integrantes",
              "imageUrl": "https://example.com/covers/revolver.jpg", "album": { "id": 1 } }""";

    private final CoverRepository repository;
    private final AlbumRepository albumRepository;
    private final CoverModelAssembler assembler;
    private final PagedResourcesAssembler<Cover> pagedResourcesAssembler;

    public CoverController(CoverRepository repository,
                           AlbumRepository albumRepository,
                           CoverModelAssembler assembler,
                           PagedResourcesAssembler<Cover> pagedResourcesAssembler) {
        this.repository = repository;
        this.albumRepository = albumRepository;
        this.assembler = assembler;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
    }

    @Operation(summary = "Get all covers", description = "Lista paginada de todas as capas cadastradas")
    @ApiResponse(responseCode = "200", description = "Returned a paginated list of covers")
    @ApiResponse(responseCode = "400", description = "Invalid sort - o sort usa um campo que a capa nao tem (ex.: sort=abc). Campos validos: id, designer, description, imageUrl",
            content = @Content(examples = @ExampleObject(value = "Invalid sort: Cover has no field 'abc'")))
    @GetMapping("/covers")
    public ResponseEntity<PagedModel<EntityModel<Cover>>> getAllCovers(
            @ParameterObject @PageableDefault(size = 10, page = 0, sort = "id") Pageable pageable) {
        Page<Cover> coverPage = repository.findAll(pageable);
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(coverPage, assembler));
    }

    @Operation(summary = "Get a cover by its id", description = "Retorna uma capa com o album a que pertence")
    @ApiResponse(responseCode = "200", description = "Returns a valid cover",
            content = @Content(mediaType = "application/hal+json", schema = @Schema(implementation = Cover.class)))
    @ApiResponse(responseCode = "400", description = "Invalid id - o id precisa ser um numero inteiro (ex.: 1); textos como abc ou decimais como 1.5 sao recusados",
            content = @Content(examples = @ExampleObject(value = "Invalid value 'abc' for parameter 'id': it must be a whole number")))
    @ApiResponse(responseCode = "404", description = "Cover not found - nao existe capa com esse id (nunca foi criada ou ja foi excluida)",
            content = @Content(examples = @ExampleObject(value = "Could not find cover with ID: 99")))
    @GetMapping("/covers/{id}")
    public EntityModel<Cover> getCoverById(@Parameter(description = "Id of the cover", example = "1") @PathVariable Long id) {
        Cover cover = repository.findById(id)
                .orElseThrow(() -> new CoverNotFoundException(id));
        return assembler.toModel(cover);
    }

    @Operation(summary = "Creates a new cover", description = "O album e vinculado pelo id; cada album so pode ter uma capa (One-to-One)")
    @ApiResponse(responseCode = "201", description = "Cover created; Location header points to the new resource")
    @ApiResponse(responseCode = "400", description = "Bad request on the payload - algum campo falhou na validacao (designer vazio ou fora de 2 a 100 caracteres, " +
            "imageUrl vazia ou que nao e uma URL valida, description com mais de 255 caracteres de texto, album ausente), " +
            "um campo veio com o tipo errado (ex.: \"description\": 300 - o campo e texto, entao numero e recusado) ou o JSON esta mal formado. " +
            "A resposta diz qual campo falhou e por que",
            content = @Content(examples = {
                    @ExampleObject(name = "Campo invalido", value = "{ \"imageUrl\": \"deve ser uma URL válida\", \"album\": \"não deve ser nulo\" }"),
                    @ExampleObject(name = "Tipo errado", value = "Invalid value for field 'description': expected String"),
                    @ExampleObject(name = "JSON mal formado", value = "Malformed JSON: check quotes, commas and braces")}))
    @ApiResponse(responseCode = "404", description = "Album not found - o id informado em album nao existe",
            content = @Content(examples = @ExampleObject(value = "Could not find album with ID: 99")))
    @ApiResponse(responseCode = "409", description = "The album already has a cover - cada album so pode ter uma capa (One-to-One); " +
            "para trocar a capa use PUT /v1/covers/{id} na capa existente",
            content = @Content(examples = @ExampleObject(value = "Operation conflicts with existing data: a value that must be unique is already in use")))
    @PostMapping("/covers")
    public ResponseEntity<EntityModel<Cover>> newCover(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "New cover",
                    required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Cover.class),
                            examples = @ExampleObject(value = COVER_EXAMPLE)))
            @RequestBody @Valid Cover newCover) {

        // o id e gerado pelo banco; se vier no JSON e ignorado (senao o save sobrescreveria a capa com esse id)
        newCover.setId(0);
        newCover.setAlbum(resolveAlbum(newCover.getAlbum()));
        EntityModel<Cover> entityModel = assembler.toModel(repository.save(newCover));

        return ResponseEntity.created(entityModel
                .getRequiredLink(IanaLinkRelations.SELF)
                .toUri()).body(entityModel);
    }

    @Operation(summary = "Updates a cover", description = "Substitui todos os dados da capa, inclusive o album vinculado")
    @ApiResponse(responseCode = "200", description = "Cover updated",
            content = @Content(mediaType = "application/hal+json", schema = @Schema(implementation = Cover.class)))
    @ApiResponse(responseCode = "400", description = "Invalid id or payload - o id nao e um numero inteiro, ou algum campo do JSON e invalido (mesmas regras do POST)",
            content = @Content(examples = {
                    @ExampleObject(name = "Id invalido", value = "Invalid value 'abc' for parameter 'id': it must be a whole number"),
                    @ExampleObject(name = "Campo invalido", value = "{ \"imageUrl\": \"deve ser uma URL válida\" }")}))
    @ApiResponse(responseCode = "404", description = "Cover or album not found - a capa com esse id nao existe, ou o id informado em album nao existe",
            content = @Content(examples = {
                    @ExampleObject(name = "Capa", value = "Could not find cover with ID: 99"),
                    @ExampleObject(name = "Album", value = "Could not find album with ID: 99")}))
    @ApiResponse(responseCode = "409", description = "The album already has another cover - o album informado ja tem outra capa (cada album so pode ter uma)",
            content = @Content(examples = @ExampleObject(value = "Operation conflicts with existing data: a value that must be unique is already in use")))
    @PutMapping("/covers/{id}")
    public ResponseEntity<EntityModel<Cover>> updateCover(
            @Parameter(description = "Id of the cover", example = "1") @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Updated cover",
                    required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Cover.class),
                            examples = @ExampleObject(value = COVER_EXAMPLE)))
            @RequestBody @Valid Cover newCover) {

        Cover updated = repository.findById(id)
                .map(cover -> {
                    cover.setDesigner(newCover.getDesigner());
                    cover.setDescription(newCover.getDescription());
                    cover.setImageUrl(newCover.getImageUrl());
                    cover.setAlbum(resolveAlbum(newCover.getAlbum()));
                    return repository.save(cover);
                })
                .orElseThrow(() -> new CoverNotFoundException(id));

        return ResponseEntity.ok(assembler.toModel(updated));
    }

    @Operation(summary = "Deletes a cover", description = "Remove a capa; o album continua cadastrado, apenas sem capa")
    @ApiResponse(responseCode = "204", description = "Successfully deleted a cover", content = @Content)
    @ApiResponse(responseCode = "400", description = "Invalid id - o id precisa ser um numero inteiro (ex.: 1)",
            content = @Content(examples = @ExampleObject(value = "Invalid value 'abc' for parameter 'id': it must be a whole number")))
    @ApiResponse(responseCode = "404", description = "Cover not found, maybe it's already deleted - nao existe capa com esse id",
            content = @Content(examples = @ExampleObject(value = "Could not find cover with ID: 99")))
    @Transactional
    @DeleteMapping("/covers/{id}")
    public ResponseEntity<?> deleteCover(@Parameter(description = "Id of the cover", example = "1") @PathVariable Long id) {
        var cover = repository.findById(id);
        if (cover.isEmpty())
            throw new CoverNotFoundException(id);

        cover.get().getAlbum().setCover(null);
        repository.delete(cover.get());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Search covers by designer", description = "Consulta personalizada: capas cujo designer contem o texto (sem diferenciar maiusculas)")
    @ApiResponse(responseCode = "200", description = "Returned a paginated list of covers")
    @ApiResponse(responseCode = "400", description = "Missing designer or invalid sort - o parametro designer e obrigatorio, ou o sort usa um campo que a capa nao tem (ex.: sort=abc)",
            content = @Content(examples = {
                    @ExampleObject(name = "Sem designer", value = "Required parameter 'designer' is missing"),
                    @ExampleObject(name = "Sort invalido", value = "Invalid sort: Cover has no field 'abc'")}))
    @GetMapping("/covers/search")
    public ResponseEntity<PagedModel<EntityModel<Cover>>> getCoversByDesigner(
            @Parameter(description = "Part of the designer's name", example = "voormann") @RequestParam String designer,
            @ParameterObject @PageableDefault(size = 10, page = 0, sort = "id") Pageable pageable) {
        Page<Cover> coverPage = repository.findByDesignerContainingIgnoreCase(designer, pageable);
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(coverPage, assembler));
    }

    @Operation(summary = "Get the cover of an album", description = "Consulta personalizada: capa de um album (One-to-One)")
    @ApiResponse(responseCode = "200", description = "Returns the cover of the album",
            content = @Content(mediaType = "application/hal+json", schema = @Schema(implementation = Cover.class)))
    @ApiResponse(responseCode = "400", description = "Invalid albumId - o albumId precisa ser um numero inteiro (ex.: 1)",
            content = @Content(examples = @ExampleObject(value = "Invalid value 'abc' for parameter 'albumId': it must be a whole number")))
    @ApiResponse(responseCode = "404", description = "Album not found or album has no cover - o album nao existe, ou existe mas ainda nao tem capa cadastrada",
            content = @Content(examples = {
                    @ExampleObject(name = "Album nao existe", value = "Could not find album with ID: 99"),
                    @ExampleObject(name = "Album sem capa", value = "Album with ID: 1 has no cover")}))
    @GetMapping("/covers/album/{albumId}")
    public ResponseEntity<EntityModel<Cover>> getCoverByAlbum(
            @Parameter(description = "Id of the album", example = "1") @PathVariable Long albumId) {
        if (!albumRepository.existsById(albumId))
            throw new AlbumNotFoundException(albumId);

        return repository.findByAlbumId(albumId)
                .map(cover -> ResponseEntity.ok(assembler.toModel(cover)))
                .orElseThrow(() -> new CoverNotFoundException("Album with ID: " + albumId + " has no cover"));
    }

    private Album resolveAlbum(Album album) {
        return albumRepository.findById(album.getId())
                .orElseThrow(() -> new AlbumNotFoundException(album.getId()));
    }
}
