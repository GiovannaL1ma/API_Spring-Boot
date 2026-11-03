package com.senac.tsi.MusicApi.config;

import com.senac.tsi.MusicApi.model.ApiKeyRole;
import com.senac.tsi.MusicApi.web.ApiKeyInterceptor;
import com.senac.tsi.MusicApi.web.ApiKeyResolver;
import com.senac.tsi.MusicApi.web.IdempotencyInterceptor;
import com.senac.tsi.MusicApi.web.RateLimitInterceptor;
import com.senac.tsi.MusicApi.web.RequiresApiKey;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MapSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.GlobalOpenApiCustomizer;
import org.springdoc.core.customizers.GlobalOperationCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import tools.jackson.databind.json.JsonMapper;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// tudo o que aparece no Swagger alem das anotacoes dos controllers:
// cabecalho (titulo, termos, licenca), um grupo por versao da API e a documentacao automatica de chave, idempotencia e rate limit
@Configuration
public class OpenApiConfig {

    // versao do produto: aparece no Swagger e na raiz (/); tem que bater com o <version> do pom.xml
    // MAJOR muda quando quebra quem ja usa a API, MINOR quando acrescenta algo, PATCH quando so corrige
    public static final String VERSION = "2.1.0";

    private static final String SECURITY_SCHEME = "ApiKey";
    private static final String ANY_MEDIA_TYPE = "*/*";
    private static final String PROBLEM_MEDIA_TYPE = "application/problem+json";
    private static final String PROBLEM_SCHEMA = "ProblemDetail";

    private final ApiProperties properties;
    private final JsonMapper jsonMapper;

    public OpenApiConfig(ApiProperties properties, JsonMapper jsonMapper) {
        this.properties = properties;
        this.jsonMapper = jsonMapper;
    }

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Music Catalog API")
                        .version(VERSION)
                        .description(description())
                        // termos e licenca sao servidos pela propria API (src/main/resources/static)
                        // o link e relativo: o Swagger completa com o endereco do servidor, entao funciona local e no Render
                        .termsOfService("/terms.html")
                        .license(new License().name("MIT").url("/license.txt"))
                        .contact(new Contact().name("Giovanna Lima - TSI Senac").url("https://www.sp.senac.br/")))
                // o botao Authorize do Swagger: guarda a chave e manda no header X-API-Key
                .components(new Components().addSecuritySchemes(SECURITY_SCHEME, new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.HEADER)
                        .name(ApiKeyResolver.HEADER)
                        .description("Chave gerada no POST /api-keys. Exigida para criar, alterar e excluir dados")));
    }

    private String description() {
        ApiProperties.RateLimit limits = properties.rateLimit();
        return "API RESTful para gerenciar um catalogo de discos: artistas, albuns, faixas, capas, generos e gravadoras, " +
                "com albuns colaborativos e participacoes especiais (feat.). Todas as listagens sao paginadas (page, size, sort) " +
                "e as respostas seguem HATEOAS (HAL).\n\n" +
                "**Dados de exemplo:** a API ja sobe com 5 artistas do rap nacional, cada um com um album, as faixas e a capa.\n\n" +
                "**Erros:** toda resposta de erro segue o formato padrao Problem Details (RFC 9457), em `application/problem+json`: " +
                "`title` e `status` (o status HTTP), `detail` (o motivo) e `instance` (a rota). Nos erros de validacao, " +
                "`errors` traz cada campo com o problema dele.\n\n" +
                "**Versoes (header `X-API-Version`):** escolha a versao no seletor \"Select a definition\", no topo. " +
                "Sem o header a API responde a versao 1. Na versao 2 as consultas de faixas trazem a duracao formatada (\"3:32\").\n\n" +
                "**Chave de API (header `X-API-Key`):** ler e livre; criar, alterar e excluir exige chave. Gere a sua em POST /api-keys " +
                "e cole no botao **Authorize**. Mandar uma chave invalida responde 401 em qualquer endpoint.\n\n" +
                "**Idempotencia (header `X-Idempotency-Key`):** nos POST, repetir a mesma chave devolve a resposta da primeira vez " +
                "em vez de criar outro registro.\n\n" +
                "**Limite de requisicoes:** " + limits.anonymousPerMinute() + " por minuto sem chave e " + limits.withKeyPerMinute() +
                " com chave; passou disso, a resposta e 429 com o header Retry-After.";
    }

    // um grupo por versao: o seletor do Swagger troca entre eles e cada um ja manda o X-API-Version certo
    @Bean
    public GroupedOpenApi v1Api() {
        return versionGroup("1", false);
    }

    @Bean
    public GroupedOpenApi v2Api() {
        return versionGroup("2", true);
    }

    private static GroupedOpenApi versionGroup(String version, boolean headerRequired) {
        return GroupedOpenApi.builder()
                .group("v" + version)
                .displayName("v" + version + " (" + WebConfig.API_VERSION_HEADER + ": " + version + ")")
                .addOpenApiMethodFilter(method -> belongsToVersion(method, version))
                .addOperationCustomizer((operation, handlerMethod) -> {
                    // o springdoc ja poe um X-API-Version generico nos endpoints versionados; troca pelo deste grupo
                    if (operation.getParameters() != null)
                        operation.getParameters().removeIf(parameter -> "header".equals(parameter.getIn())
                                && WebConfig.API_VERSION_HEADER.equalsIgnoreCase(parameter.getName()));
                    operation.addParametersItem(new HeaderParameter()
                            .name(WebConfig.API_VERSION_HEADER)
                            .required(headerRequired)
                            .description(headerRequired
                                    ? "Versao da API. Mande " + version + " para receber a versao " + version
                                    : "Versao da API. Opcional: sem o header a API ja responde a versao " + version)
                            .schema(new StringSchema()._default(version)._enum(List.of(version))));
                    return operation;
                })
                .build();
    }

    // endpoint sem version no mapping responde a todas as versoes, entao aparece em todos os grupos
    private static boolean belongsToVersion(Method method, String version) {
        RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
        return mapping == null || mapping.version().isEmpty() || mapping.version().equals(version);
    }

    // vale para todos os endpoints de todos os grupos
    @Bean
    public GlobalOperationCustomizer apiConventionsCustomizer() {
        return (operation, handlerMethod) -> {
            RequiresApiKey required = ApiKeyInterceptor.findRequirement(handlerMethod);
            if (required != null)
                documentApiKey(operation, required.value());
            if (AnnotatedElementUtils.hasAnnotation(handlerMethod.getMethod(), PostMapping.class))
                documentIdempotency(operation);
            documentRateLimit(operation);
            return operation;
        };
    }

    private static void documentApiKey(Operation operation, ApiKeyRole role) {
        operation.addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME));
        operation.getResponses().addApiResponse("401", errorResponse(
                "Missing or invalid API key - este endpoint exige uma chave de API no header X-API-Key " +
                        "(gere uma no POST /api-keys e use o botao Authorize); chave errada ou desativada tambem cai aqui",
                "Missing API key: send it in the X-API-Key header (create one with POST /api-keys)"));
        if (role == ApiKeyRole.ADMIN)
            operation.getResponses().addApiResponse("403", errorResponse(
                    "API key without permission - a chave e valida, mas e de nivel USER; esta operacao exige uma chave ADMIN",
                    "This operation requires an API key with role ADMIN"));
    }

    private static void documentIdempotency(Operation operation) {
        operation.addParametersItem(new HeaderParameter()
                .name(IdempotencyInterceptor.HEADER)
                .required(false)
                .description("Opcional e recomendado: uma chave unica por operacao (ex.: um UUID). Se o mesmo POST for repetido " +
                        "com a mesma chave, a API devolve a resposta da primeira vez, com o header X-Idempotency-Replayed: true, " +
                        "em vez de criar outro registro. A chave vale por 24 horas")
                .schema(new StringSchema().maxLength(255).example("3f1c2a9e-7b1d-4c55-9a51-0d6c2f9b8e11")));

        addExample(operation, "400", "X-Idempotency-Key invalida",
                "Invalid X-Idempotency-Key: it must have between 1 and 255 characters",
                "Bad request - a X-Idempotency-Key precisa ter de 1 a 255 caracteres");
        addExample(operation, "409", "Requisicao em andamento",
                "A request with X-Idempotency-Key 'abc-123' is still being processed; try again in a moment",
                "Idempotency key in progress - a primeira requisicao com essa X-Idempotency-Key ainda nao terminou");
        operation.getResponses().addApiResponse("422", errorResponse(
                "Idempotency key reused - a X-Idempotency-Key ja foi usada com outra requisicao (outro corpo ou outra rota); " +
                        "use uma chave nova para uma operacao nova",
                "X-Idempotency-Key 'abc-123' was already used with a different request; use a new key for a new operation"));
    }

    private void documentRateLimit(Operation operation) {
        ApiProperties.RateLimit limits = properties.rateLimit();
        // todo 2xx informa o limite, quanto sobra e em quantos segundos a janela recomeca
        operation.getResponses().forEach((status, response) -> {
            if (status.startsWith("2")) {
                response.addHeaderObject(RateLimitInterceptor.LIMIT_HEADER, intHeader("Requisicoes permitidas por minuto"));
                response.addHeaderObject(RateLimitInterceptor.REMAINING_HEADER, intHeader("Requisicoes que ainda restam neste minuto"));
                response.addHeaderObject(RateLimitInterceptor.RESET_HEADER, intHeader("Segundos ate o limite recomecar"));
            }
        });
        ApiResponse tooMany = errorResponse(
                "Too many requests - o cliente passou do limite de " + limits.anonymousPerMinute() + " requisicoes por minuto sem chave (ou " +
                        limits.withKeyPerMinute() + " com chave); o header Retry-After diz em quantos segundos tentar de novo",
                "Too many requests: the limit is " + limits.anonymousPerMinute() + " requests per minute. Try again in 23 seconds");
        tooMany.addHeaderObject(HttpHeaders.RETRY_AFTER, intHeader("Segundos ate poder tentar de novo"));
        operation.getResponses().addApiResponse("429", tooMany);
    }

    // os controllers documentam so a mensagem de cada erro (o detail); aqui cada exemplo vira o JSON completo
    // que a API devolve, com o schema ProblemDetail. Roda por ultimo, depois que todos os erros ja foram documentados
    @Bean
    public GlobalOpenApiCustomizer problemDetailCustomizer() {
        return openApi -> {
            openApi.getComponents().addSchemas(PROBLEM_SCHEMA, problemSchema());
            openApi.getPaths().forEach((path, item) -> item.readOperations().forEach(operation ->
                    operation.getResponses().forEach((status, response) -> {
                        if (status.startsWith("4") || status.startsWith("5"))
                            response.setContent(problemContent(path, Integer.parseInt(status), response.getContent()));
                    })));
        };
    }

    private Content problemContent(String path, int status, Content documented) {
        MediaType problem = new MediaType().schema(new Schema<>().$ref("#/components/schemas/" + PROBLEM_SCHEMA));
        if (documented != null)
            documented.values().forEach(mediaType -> {
                if (mediaType.getExample() != null)
                    problem.setExample(problemExample(path, status, mediaType.getExample()));
                if (mediaType.getExamples() != null)
                    mediaType.getExamples().forEach((name, example) ->
                            problem.addExamples(name, new Example().value(problemExample(path, status, example.getValue()))));
            });
        return new Content().addMediaType(PROBLEM_MEDIA_TYPE, problem);
    }

    // mesmo JSON que o ApiExceptionAdvice monta; o instance mostra a rota do endpoint (ex.: /artists/{id})
    private Map<String, Object> problemExample(String path, int status, Object message) {
        Map<String, Object> errors = validationErrors(message);
        Map<String, Object> example = new LinkedHashMap<>();
        example.put("title", HttpStatus.valueOf(status).getReasonPhrase());
        example.put("status", status);
        example.put("detail", errors == null ? message : "Validation failed for: " + String.join(", ", errors.keySet()));
        example.put("instance", path);
        if (errors != null)
            example.put("errors", errors);
        return example;
    }

    // exemplo de erro de validacao (um JSON com cada campo e o motivo) vira o "errors"; mensagem comum devolve null
    @SuppressWarnings("unchecked")
    private Map<String, Object> validationErrors(Object message) {
        if (message instanceof Map<?, ?> map)
            return (Map<String, Object>) map;
        String text = String.valueOf(message).trim();
        return text.startsWith("{") ? jsonMapper.readValue(text, LinkedHashMap.class) : null;
    }

    private static Schema<?> problemSchema() {
        return new ObjectSchema()
                .description("Formato padrao das respostas de erro (RFC 9457 - Problem Details)")
                .addProperty("type", new StringSchema().description("Tipo do erro. Vem omitido quando e about:blank (o status ja explica)"))
                .addProperty("title", new StringSchema().description("Nome do status HTTP").example("Not Found"))
                .addProperty("status", new IntegerSchema().description("Codigo do status HTTP").example(404))
                .addProperty("detail", new StringSchema().description("O motivo do erro").example("Could not find artist with ID: 99"))
                .addProperty("instance", new StringSchema().description("Rota que deu o erro").example("/artists/99"))
                .addProperty("errors", new MapSchema().additionalProperties(new StringSchema())
                        .description("So nos erros de validacao: cada campo (ou parametro) com o motivo"));
    }

    private static ApiResponse errorResponse(String description, String example) {
        return new ApiResponse().description(description)
                .content(new Content().addMediaType(ANY_MEDIA_TYPE, new MediaType().example(example)));
    }

    private static Header intHeader(String description) {
        return new Header().description(description).schema(new IntegerSchema());
    }

    // junta um exemplo novo a uma resposta que o controller ja documentou (ex.: o 409 de nome repetido)
    private static void addExample(Operation operation, String status, String name, String example, String descriptionIfNew) {
        ApiResponse response = operation.getResponses().get(status);
        if (response == null) {
            operation.getResponses().addApiResponse(status, errorResponse(descriptionIfNew, example));
            return;
        }
        response.setDescription(response.getDescription() + ". Ou: " + descriptionIfNew);
        if (response.getContent() == null)
            response.setContent(new Content());
        MediaType mediaType = response.getContent().computeIfAbsent(ANY_MEDIA_TYPE, type -> new MediaType());
        if (mediaType.getExamples() == null)
            mediaType.setExamples(new LinkedHashMap<>());
        // um exemplo unico vira o primeiro da lista, para os dois aparecerem no seletor de exemplos do Swagger
        if (mediaType.getExample() != null) {
            mediaType.getExamples().put("Exemplo", new Example().value(mediaType.getExample()));
            mediaType.setExample(null);
        }
        mediaType.getExamples().put(name, new Example().value(example));
    }
}
