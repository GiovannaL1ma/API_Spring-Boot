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
    @GetMapping("/labels")
    public ResponseEntity<PagedModel<EntityModel<RecordLabel>>> getAllLabels(
            @ParameterObject @PageableDefault(size = 10, page = 0, sort = "id") Pageable pageable) {
        Page<RecordLabel> labelPage = repository.findAll(pageable);
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(labelPage, assembler));
    }

    @Operation(summary = "Get a record label by its id", description = "Retorna uma gravadora com link para seu catalogo de albuns")
    @ApiResponse(responseCode = "200", description = "Returns a valid record label",
            content = @Content(mediaType = "application/hal+json", schema = @Schema(implementation = RecordLabel.class)))
    @ApiResponse(responseCode = "404", description = "Record label not found", content = @Content)
    @ApiResponse(responseCode = "400", description = "Invalid id", content = @Content)
    @GetMapping("/labels/{id}")
    public EntityModel<RecordLabel> getLabelById(@Parameter(description = "Id of the record label", example = "1") @PathVariable Long id) {
        RecordLabel label = repository.findById(id)
                .orElseThrow(() -> new RecordLabelNotFoundException(id));
        return assembler.toModel(label);
    }

    @Operation(summary = "Creates a new record label", description = "Para vincular albuns a gravadora, use o campo label no POST/PUT de /albums")
    @ApiResponse(responseCode = "201", description = "Record label created; Location header points to the new resource")
    @ApiResponse(responseCode = "400", description = "Bad request on the payload", content = @Content)
    @ApiResponse(responseCode = "409", description = "A record label with this name already exists", content = @Content)
    @PostMapping("/labels")
    public ResponseEntity<EntityModel<RecordLabel>> newLabel(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "New record label",
                    required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = RecordLabel.class),
                            examples = @ExampleObject(value = LABEL_EXAMPLE)))
            @RequestBody @Valid RecordLabel newLabel) {

        EntityModel<RecordLabel> entityModel = assembler.toModel(repository.save(newLabel));

        return ResponseEntity.created(entityModel
                .getRequiredLink(IanaLinkRelations.SELF)
                .toUri()).body(entityModel);
    }

    @Operation(summary = "Updates a record label", description = "Substitui todos os dados da gravadora")
    @ApiResponse(responseCode = "200", description = "Record label updated",
            content = @Content(mediaType = "application/hal+json", schema = @Schema(implementation = RecordLabel.class)))
    @ApiResponse(responseCode = "400", description = "Bad request on the payload", content = @Content)
    @ApiResponse(responseCode = "404", description = "Record label not found", content = @Content)
    @ApiResponse(responseCode = "409", description = "A record label with this name already exists", content = @Content)
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
    @ApiResponse(responseCode = "404", description = "Record label not found, maybe it's already deleted", content = @Content)
    @ApiResponse(responseCode = "409", description = "Record label still has albums", content = @Content)
    @DeleteMapping("/labels/{id}")
    public ResponseEntity<?> deleteLabel(@Parameter(description = "Id of the record label", example = "1") @PathVariable Long id) {
        var label = repository.findById(id);
        if (label.isEmpty())
            throw new RecordLabelNotFoundException(id);

        if (!label.get().getAlbums().isEmpty())
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Record label still has albums");

        repository.delete(label.get());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Search record labels", description = "Consulta personalizada: gravadoras por pais ou por parte do nome (sem diferenciar maiusculas)")
    @ApiResponse(responseCode = "200", description = "Returned a paginated list of record labels")
    @ApiResponse(responseCode = "400", description = "Neither country nor name was informed", content = @Content)
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
            return ResponseEntity.badRequest().build();

        return ResponseEntity.ok(pagedResourcesAssembler.toModel(labelPage, assembler));
    }
}
