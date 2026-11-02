package com.senac.tsi.MusicApi.controller;

import com.senac.tsi.MusicApi.assembler.ApiKeyModelAssembler;
import com.senac.tsi.MusicApi.dto.ApiKeyRequest;
import com.senac.tsi.MusicApi.dto.ApiKeyResponse;
import com.senac.tsi.MusicApi.dto.ApiKeyUpdateRequest;
import com.senac.tsi.MusicApi.model.ApiKey;
import com.senac.tsi.MusicApi.model.ApiKeyRole;
import com.senac.tsi.MusicApi.service.ApiKeyService;
import com.senac.tsi.MusicApi.web.ApiKeyResolver;
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

// geracao e gerenciamento das chaves de API
// qualquer pessoa gera uma chave USER; quem tem chave ve e apaga a propria; so ADMIN gerencia as chaves de todos
@RestController
@RequestMapping("/api-keys")
@Tag(name = "API Keys", description = "Geracao e gerenciamento das chaves de API (header X-API-Key). Gere a sua no POST, " +
        "guarde a chave e use no botao Authorize do Swagger para liberar os endpoints que criam, alteram e excluem dados")
public class ApiKeyController {

    private static final String KEY_REQUEST_EXAMPLE = """
            { "ownerName": "Giovanna Lima", "ownerEmail": "giovanna@example.com" }""";

    private final ApiKeyService service;
    private final ApiKeyModelAssembler assembler;
    private final PagedResourcesAssembler<ApiKeyResponse> pagedResourcesAssembler;

    public ApiKeyController(ApiKeyService service,
                            ApiKeyModelAssembler assembler,
                            PagedResourcesAssembler<ApiKeyResponse> pagedResourcesAssembler) {
        this.service = service;
        this.assembler = assembler;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
    }

    @Operation(summary = "Creates a new API key", description = "Publico: gera uma chave com nivel USER. A chave completa (campo apiKey) " +
            "so aparece nesta resposta, porque a API guarda apenas o hash dela; se perder, gere outra")
    @ApiResponse(responseCode = "201", description = "API key created; the apiKey field has the key to send in the X-API-Key header")
    @ApiResponse(responseCode = "400", description = "Bad request on the payload - ownerName vazio ou fora de 2 a 100 caracteres, " +
            "ownerEmail vazio ou que nao e um e-mail valido, ou o JSON esta mal formado",
            content = @Content(examples = @ExampleObject(name = "Campo invalido",
                    value = "{ \"ownerEmail\": \"deve ser um endereço de e-mail bem formado\" }")))
    @PostMapping
    public ResponseEntity<EntityModel<ApiKeyResponse>> createKey(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Who is asking for the key",
                    required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiKeyRequest.class),
                            examples = @ExampleObject(value = KEY_REQUEST_EXAMPLE)))
            @RequestBody @Valid ApiKeyRequest request) {

        ApiKeyService.CreatedApiKey created = service.create(request.ownerName(), request.ownerEmail(), ApiKeyRole.USER);
        EntityModel<ApiKeyResponse> entityModel = assembler.toModel(ApiKeyResponse.withSecret(created.apiKey(), created.rawKey()));
        return ResponseEntity.created(entityModel
                .getRequiredLink(IanaLinkRelations.SELF)
                .toUri()).body(entityModel);
    }

    @Operation(summary = "Get the API key in use", description = "Mostra os dados da chave enviada no header X-API-Key (sem a chave completa)")
    @ApiResponse(responseCode = "200", description = "Returns the API key that made the request",
            content = @Content(mediaType = "application/hal+json", schema = @Schema(implementation = ApiKeyResponse.class)))
    @RequiresApiKey
    @GetMapping("/me")
    public EntityModel<ApiKeyResponse> getMyKey(
            @Parameter(hidden = true) @RequestAttribute(ApiKeyResolver.API_KEY_ATTRIBUTE) ApiKey caller) {
        return assembler.toModel(ApiKeyResponse.from(service.findById(caller.getId())));
    }

    @Operation(summary = "Deletes the API key in use", description = "Apaga a chave enviada no header X-API-Key; depois disso ela para de funcionar")
    @ApiResponse(responseCode = "204", description = "API key deleted", content = @Content)
    @RequiresApiKey
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMyKey(
            @Parameter(hidden = true) @RequestAttribute(ApiKeyResolver.API_KEY_ATTRIBUTE) ApiKey caller) {
        service.delete(caller.getId());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get all API keys", description = "Somente ADMIN: lista paginada de todas as chaves cadastradas")
    @ApiResponse(responseCode = "200", description = "Returned a paginated list of API keys")
    @ApiResponse(responseCode = "400", description = "Invalid sort - o sort usa um campo que a chave nao tem (ex.: sort=abc). " +
            "Campos validos: id, ownerName, ownerEmail, role, active, createdAt, lastUsedAt",
            content = @Content(examples = @ExampleObject(value = "Invalid sort: ApiKey has no field 'abc'")))
    @RequiresApiKey(ApiKeyRole.ADMIN)
    @GetMapping
    public ResponseEntity<PagedModel<EntityModel<ApiKeyResponse>>> getAllKeys(
            @ParameterObject @PageableDefault(size = 10, page = 0, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(service.findAll(pageable).map(ApiKeyResponse::from), assembler));
    }

    @Operation(summary = "Get an API key by its id", description = "Somente ADMIN")
    @ApiResponse(responseCode = "200", description = "Returns a valid API key",
            content = @Content(mediaType = "application/hal+json", schema = @Schema(implementation = ApiKeyResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid id - o id precisa ser um numero inteiro (ex.: 1)",
            content = @Content(examples = @ExampleObject(value = "Invalid value 'abc' for parameter 'id': it must be a whole number")))
    @ApiResponse(responseCode = "404", description = "API key not found - nao existe chave com esse id",
            content = @Content(examples = @ExampleObject(value = "Could not find API key with ID: 99")))
    @RequiresApiKey(ApiKeyRole.ADMIN)
    @GetMapping("/{id}")
    public EntityModel<ApiKeyResponse> getKeyById(@Parameter(description = "Id of the API key", example = "2") @PathVariable Long id) {
        return assembler.toModel(ApiKeyResponse.from(service.findById(id)));
    }

    @Operation(summary = "Updates an API key", description = "Somente ADMIN: ativa ou desativa uma chave (active) e/ou muda o nivel dela (role). " +
            "So os campos enviados mudam")
    @ApiResponse(responseCode = "200", description = "API key updated",
            content = @Content(mediaType = "application/hal+json", schema = @Schema(implementation = ApiKeyResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid id or payload - o id nao e um numero inteiro, o corpo nao trouxe active nem role, " +
            "ou o role nao e USER ou ADMIN",
            content = @Content(examples = {
                    @ExampleObject(name = "Corpo vazio", value = "{ \"anyFieldPresent\": \"send at least one field: active or role\" }"),
                    @ExampleObject(name = "Role invalido", value = "Invalid value for field 'role'. Accepted values: [USER, ADMIN]")}))
    @ApiResponse(responseCode = "404", description = "API key not found - nao existe chave com esse id",
            content = @Content(examples = @ExampleObject(value = "Could not find API key with ID: 99")))
    @RequiresApiKey(ApiKeyRole.ADMIN)
    @PatchMapping("/{id}")
    public EntityModel<ApiKeyResponse> updateKey(
            @Parameter(description = "Id of the API key", example = "2") @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Fields to change",
                    required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiKeyUpdateRequest.class),
                            examples = @ExampleObject(value = "{ \"active\": false }")))
            @RequestBody @Valid ApiKeyUpdateRequest request) {
        return assembler.toModel(ApiKeyResponse.from(service.update(id, request.active(), request.role())));
    }

    @Operation(summary = "Deletes an API key", description = "Somente ADMIN: apaga uma chave; ela para de funcionar na hora")
    @ApiResponse(responseCode = "204", description = "API key deleted", content = @Content)
    @ApiResponse(responseCode = "400", description = "Invalid id - o id precisa ser um numero inteiro (ex.: 1)",
            content = @Content(examples = @ExampleObject(value = "Invalid value 'abc' for parameter 'id': it must be a whole number")))
    @ApiResponse(responseCode = "404", description = "API key not found - nao existe chave com esse id",
            content = @Content(examples = @ExampleObject(value = "Could not find API key with ID: 99")))
    @RequiresApiKey(ApiKeyRole.ADMIN)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteKey(@Parameter(description = "Id of the API key", example = "2") @PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
