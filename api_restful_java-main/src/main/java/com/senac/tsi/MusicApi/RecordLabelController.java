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
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1") // versao 1 da API: todas as rotas daqui comecam com /v1 (a v2 fica ao lado, em /v2)
@Tag(name = "Record Labels", description = "Gerenciamento das gravadoras (One-to-Many com Album)")
public class RecordLabelController {

    private static final String LABEL_EXAMPLE = """
            { "name": "Parlophone", "country": "United Kingdom", "foundedYear": 1896 }""";

    private final RecordLabelRepository repository;
    private final RecordLabelModelAssembler assembler;
    private final PagedResourcesAssembler<RecordLabel> pagedResourcesAssembler;

    public RecordLabelController(RecordLabelRepository repository,
                                 RecordLabelModelAssembler assembler,
                                 PagedResourcesAssembler<RecordLabel> pagedResourcesAssembler) {
        this.repository = repository;
        this.assembler = assembler;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
    }

    @Operation(summary = "Get all record labels", description = "Lista paginada de todas as gravadoras")
    @ApiResponse(responseCode = "200", description = "Returned a paginated list of record labels")
    @ApiResponse(responseCode = "400", description = "Invalid sort - o sort usa um campo que a gravadora nao tem (ex.: sort=abc). Campos validos: id, name, country, foundedYear",
            content = @Content(examples = @ExampleObject(value = "Invalid sort: RecordLabel has no field 'abc'")))
    @GetMapping("/labels")
    public ResponseEntity<PagedModel<EntityModel<RecordLabel>>> getAllLabels(
            @ParameterObject @PageableDefault(size = 10, page = 0, sort = "id") Pageable pageable) {
        Page<RecordLabel> labelPage = repository.findAll(pageable);
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(labelPage, assembler));
    }

    @Operation(summary = "Get a record label by its id", description = "Retorna uma gravadora com link para seu catalogo de albuns")
    @ApiResponse(responseCode = "200", description = "Returns a valid record label",
            content = @Content(mediaType = "application/hal+json", schema = @Schema(implementation = RecordLabel.class)))
    @ApiResponse(responseCode = "400", description = "Invalid id - o id precisa ser um numero inteiro (ex.: 1); textos como abc ou decimais como 1.5 sao recusados",
            content = @Content(examples = @ExampleObject(value = "Invalid value 'abc' for parameter 'id': it must be a whole number")))
    @ApiResponse(responseCode = "404", description = "Record label not found - nao existe gravadora com esse id (nunca foi criada ou ja foi excluida)",
            content = @Content(examples = @ExampleObject(value = "Could not find record label with ID: 99")))
    @GetMapping("/labels/{id}")
    public EntityModel<RecordLabel> getLabelById(@Parameter(description = "Id of the record label", example = "1") @PathVariable Long id) {
        RecordLabel label = repository.findById(id)
                .orElseThrow(() -> new RecordLabelNotFoundException(id));
        return assembler.toModel(label);
    }

    @Operation(summary = "Creates a new record label", description = "Para vincular albuns a gravadora, use o campo label no POST/PUT de /v1/albums")
    @ApiResponse(responseCode = "201", description = "Record label created; Location header points to the new resource")
    @ApiResponse(responseCode = "400", description = "Bad request on the payload - algum campo falhou na validacao (name vazio ou fora de 2 a 100 caracteres, " +
            "country fora de 2 a 60 caracteres, foundedYear fora de 1850 a 2100), um campo veio com o tipo errado " +
            "(ex.: numero num campo de texto, decimal ou texto num campo inteiro) ou o JSON esta mal formado. A resposta diz qual campo falhou e por que",
            content = @Content(examples = {
                    @ExampleObject(name = "Campo invalido", value = "{ \"name\": \"não deve estar em branco\", \"foundedYear\": \"deve ser maior que ou igual à 1850\" }"),
                    @ExampleObject(name = "Tipo errado", value = "Invalid value for field 'foundedYear': expected Integer"),
                    @ExampleObject(name = "JSON mal formado", value = "Malformed JSON: check quotes, commas and braces")}))
    @ApiResponse(responseCode = "409", description = "A record label with this name already exists - o name da gravadora e unico e ja existe outra gravadora com esse nome",
            content = @Content(examples = @ExampleObject(value = "Operation conflicts with existing data: a value that must be unique is already in use")))
    @PostMapping("/labels")
    public ResponseEntity<EntityModel<RecordLabel>> newLabel(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "New record label",
                    required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = RecordLabel.class),
                            examples = @ExampleObject(value = LABEL_EXAMPLE)))
            @RequestBody @Valid RecordLabel newLabel) {

        // o id e gerado pelo banco; se vier no JSON e ignorado (senao o save sobrescreveria a gravadora com esse id)
        newLabel.setId(0);
        EntityModel<RecordLabel> entityModel = assembler.toModel(repository.save(newLabel));

        return ResponseEntity.created(entityModel
                .getRequiredLink(IanaLinkRelations.SELF)
                .toUri()).body(entityModel);
    }

    @Operation(summary = "Updates a record label", description = "Substitui todos os dados da gravadora")
    @ApiResponse(responseCode = "200", description = "Record label updated",
            content = @Content(mediaType = "application/hal+json", schema = @Schema(implementation = RecordLabel.class)))
    @ApiResponse(responseCode = "400", description = "Invalid id or payload - o id nao e um numero inteiro, ou algum campo do JSON e invalido (mesmas regras do POST)",
            content = @Content(examples = {
                    @ExampleObject(name = "Id invalido", value = "Invalid value 'abc' for parameter 'id': it must be a whole number"),
                    @ExampleObject(name = "Campo invalido", value = "{ \"country\": \"tamanho deve ser entre 2 e 60\" }")}))
    @ApiResponse(responseCode = "404", description = "Record label not found - nao existe gravadora com esse id para atualizar",
            content = @Content(examples = @ExampleObject(value = "Could not find record label with ID: 99")))
    @ApiResponse(responseCode = "409", description = "A record label with this name already exists - o novo name ja pertence a outra gravadora",
            content = @Content(examples = @ExampleObject(value = "Operation conflicts with existing data: a value that must be unique is already in use")))
    @PutMapping("/labels/{id}")
    public ResponseEntity<EntityModel<RecordLabel>> updateLabel(
            @Parameter(description = "Id of the record label", example = "1") @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Updated record label",
                    required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = RecordLabel.class),
                            examples = @ExampleObject(value = LABEL_EXAMPLE)))
            @RequestBody @Valid RecordLabel newLabel) {

        RecordLabel updated = repository.findById(id)
                .map(label -> {
                    label.setName(newLabel.getName());
                    label.setCountry(newLabel.getCountry());
                    label.setFoundedYear(newLabel.getFoundedYear());
                    return repository.save(label);
                })
                .orElseThrow(() -> new RecordLabelNotFoundException(id));

        return ResponseEntity.ok(assembler.toModel(updated));
    }

    @Operation(summary = "Deletes a record label", description = "So e possivel excluir gravadoras sem albuns no catalogo")
    @ApiResponse(responseCode = "204", description = "Successfully deleted a record label", content = @Content)
    @ApiResponse(responseCode = "400", description = "Invalid id - o id precisa ser um numero inteiro (ex.: 1)",
            content = @Content(examples = @ExampleObject(value = "Invalid value 'abc' for parameter 'id': it must be a whole number")))
    @ApiResponse(responseCode = "404", description = "Record label not found, maybe it's already deleted - nao existe gravadora com esse id",
            content = @Content(examples = @ExampleObject(value = "Could not find record label with ID: 99")))
    @ApiResponse(responseCode = "409", description = "Record label still has albums - algum album ainda aponta para essa gravadora; " +
            "troque o label desses albuns (PUT /v1/albums/{id}) ou exclua-os antes",
            content = @Content(examples = @ExampleObject(value = "Record label with ID: 1 still has albums; move them to another label or delete them first")))
    @DeleteMapping("/labels/{id}")
    public ResponseEntity<?> deleteLabel(@Parameter(description = "Id of the record label", example = "1") @PathVariable Long id) {
        var label = repository.findById(id);
        if (label.isEmpty())
            throw new RecordLabelNotFoundException(id);

        if (!label.get().getAlbums().isEmpty())
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Record label with ID: " + id + " still has albums; move them to another label or delete them first");

        repository.delete(label.get());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Search record labels", description = "Consulta personalizada: gravadoras por pais (nome completo do pais) ou por parte do nome, " +
            "sem diferenciar maiusculas. Informe um dos dois; se vierem os dois, a busca usa so o country")
    @ApiResponse(responseCode = "200", description = "Returned a paginated list of record labels")
    @ApiResponse(responseCode = "400", description = "Missing search parameter or invalid sort - nenhum dos dois (country ou name) foi informado, " +
            "ou o sort usa um campo que a gravadora nao tem",
            content = @Content(examples = {
                    @ExampleObject(name = "Sem country e sem name", value = "Inform country or name to search record labels"),
                    @ExampleObject(name = "Sort invalido", value = "Invalid sort: RecordLabel has no field 'abc'")}))
    @GetMapping("/labels/search")
    public ResponseEntity<PagedModel<EntityModel<RecordLabel>>> searchLabels(
            @Parameter(description = "Country of the label", example = "United Kingdom") @RequestParam(required = false) String country,
            @Parameter(description = "Part of the label's name", example = "records") @RequestParam(required = false) String name,
            @ParameterObject @PageableDefault(size = 10, page = 0, sort = "id") Pageable pageable) {
        Page<RecordLabel> labelPage;
        if (country != null && !country.isBlank())
            labelPage = repository.findByCountryIgnoreCase(country, pageable);
        else if (name != null && !name.isBlank())
            labelPage = repository.findByNameContainingIgnoreCase(name, pageable);
        else
            throw new InvalidSearchException("Inform country or name to search record labels");

        return ResponseEntity.ok(pagedResourcesAssembler.toModel(labelPage, assembler));
    }
}
