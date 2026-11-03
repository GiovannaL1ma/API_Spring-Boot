package com.senac.tsi.MusicApi.exception;

import com.senac.tsi.MusicApi.config.WebConfig;
import com.senac.tsi.MusicApi.web.ApiKeyResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.accept.InvalidApiVersionException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import tools.jackson.core.JacksonException;
import tools.jackson.core.exc.InputCoercionException;
import tools.jackson.databind.exc.MismatchedInputException;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

// trata os erros de todos os controllers (e dos interceptors) num lugar so
// toda resposta de erro segue o formato padrao Problem Details (RFC 9457): title, status, detail e instance
// (o type so aparece quando nao e o padrao about:blank); o detail diz O MOTIVO do erro, para quem testa no Swagger entender o que mandou errado
@RestControllerAdvice
public class ApiExceptionAdvice {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionAdvice.class);

    // 404 - qualquer um dos NotFoundException, devolve a mensagem da exception
    @ExceptionHandler({ArtistNotFoundException.class, AlbumNotFoundException.class, TrackNotFoundException.class,
            GenreNotFoundException.class, RecordLabelNotFoundException.class, CoverNotFoundException.class,
            ApiKeyNotFoundException.class})
    ProblemDetail notFoundHandler(RuntimeException ex) {
        return problem(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    // 404 - rota que nao existe (ex.: GET /artistas)
    @ExceptionHandler(NoResourceFoundException.class)
    ProblemDetail noEndpointHandler(NoResourceFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "No endpoint " + ex.getHttpMethod() + " /" + ex.getResourcePath() + ": check the URL in the Swagger documentation");
    }

    // 400 - o JSON nao passou nas validacoes (@NotBlank, @Size, @Min...); o campo "errors" traz cada campo com o erro dele
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validationHandler(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
        ex.getBindingResult().getGlobalErrors()
                .forEach(error -> errors.put(error.getObjectName(), error.getDefaultMessage()));
        return validationProblem(errors);
    }

    // 400 - parametro da URL fora da regra (ex.: ?from=1800 em /albums/released); o "errors" traz cada parametro com o erro dele
    @ExceptionHandler(HandlerMethodValidationException.class)
    ProblemDetail parameterValidationHandler(HandlerMethodValidationException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getParameterValidationResults().forEach(result -> errors.put(
                result.getMethodParameter().getParameterName(),
                result.getResolvableErrors().stream().map(error -> error.getDefaultMessage()).collect(Collectors.joining("; "))));
        return validationProblem(errors);
    }

    // 400 - corpo vazio, JSON quebrado ou campo com valor do tipo errado (ex.: "type": "ROCK", "releaseYear": "abc" ou "description": 300)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail unreadableHandler(HttpMessageNotReadableException ex) {
        return problem(HttpStatus.BAD_REQUEST, unreadableReason(ex));
    }

    private static String unreadableReason(HttpMessageNotReadableException ex) {
        // o Jackson diz qual campo falhou e qual tipo esperava
        if (ex.getCause() instanceof MismatchedInputException mismatch && !mismatch.getPath().isEmpty()) {
            Class<?> type = mismatch.getTargetType();
            if (type != null && type.isEnum())
                return "Invalid value for field '" + fieldPath(mismatch) + "'. Accepted values: " + Arrays.toString(type.getEnumConstants());
            return "Invalid value for field '" + fieldPath(mismatch) + "': expected " + (type != null ? type.getSimpleName() : "another type");
        }
        // numero grande demais para caber no campo (ex.: "trackNumber": 99999999999)
        if (ex.getCause() instanceof InputCoercionException overflow && !overflow.getPath().isEmpty())
            return "Invalid value for field '" + fieldPath(overflow) + "': number out of range";
        // sem causa = o Spring nem chegou a ler o JSON porque o corpo veio vazio
        if (ex.getCause() == null)
            return "Request body is missing: send the resource as JSON";
        return "Malformed JSON: check quotes, commas and braces";
    }

    // 400 - parametro da URL com valor do tipo errado: id que nao e numero (/artists/abc) ou valor fora do enum (/artists/type/ROCK)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ProblemDetail typeMismatchHandler(MethodArgumentTypeMismatchException ex) {
        Class<?> type = ex.getRequiredType();
        if (type != null && type.isEnum())
            return problem(HttpStatus.BAD_REQUEST, "Invalid value '" + ex.getValue() + "' for parameter '" + ex.getName() + "'. Accepted values: " + Arrays.toString(type.getEnumConstants()));
        return problem(HttpStatus.BAD_REQUEST, "Invalid value '" + ex.getValue() + "' for parameter '" + ex.getName() + "': it must be a whole number");
    }

    // 400 - faltou um parametro obrigatorio (ex.: /artists/search sem o ?name=)
    @ExceptionHandler(MissingServletRequestParameterException.class)
    ProblemDetail missingParameterHandler(MissingServletRequestParameterException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Required parameter '" + ex.getParameterName() + "' is missing");
    }

    // 400 - o sort pediu um campo que a entidade nao tem (ex.: ?sort=abc); sem esse handler virava 500
    @ExceptionHandler(PropertyReferenceException.class)
    ProblemDetail sortHandler(PropertyReferenceException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid sort: " + ex.getType().getType().getSimpleName() + " has no field '" + ex.getPropertyName() + "'");
    }

    // 400 - parametros que nao fazem sentido juntos (ex.: periodo com from maior que to)
    @ExceptionHandler(InvalidSearchException.class)
    ProblemDetail invalidSearchHandler(InvalidSearchException ex) {
        return problem(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // 400 - X-API-Version com uma versao que a API nao tem (ex.: 3)
    @ExceptionHandler(InvalidApiVersionException.class)
    ProblemDetail apiVersionHandler(InvalidApiVersionException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Unsupported API version '" + ex.getVersion() + "' in the " + WebConfig.API_VERSION_HEADER
                + " header. Supported versions: " + String.join(", ", WebConfig.SUPPORTED_API_VERSIONS));
    }

    // 401 - chave de API ausente, invalida ou desativada; o header WWW-Authenticate diz como se autenticar
    @ExceptionHandler(ApiKeyAuthenticationException.class)
    ResponseEntity<ProblemDetail> authenticationHandler(ApiKeyAuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .header(HttpHeaders.WWW_AUTHENTICATE, "ApiKey header=\"" + ApiKeyResolver.HEADER + "\"")
                .body(problem(HttpStatus.UNAUTHORIZED, ex.getMessage()));
    }

    // 403 - a chave e valida, mas o nivel dela nao permite a operacao
    @ExceptionHandler(InsufficientApiKeyRoleException.class)
    ProblemDetail forbiddenHandler(InsufficientApiKeyRoleException ex) {
        return problem(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    // 405 - metodo HTTP que a rota nao aceita (ex.: PATCH /artists/1); o header Allow lista os aceitos
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ProblemDetail> methodNotAllowedHandler(HttpRequestMethodNotSupportedException ex) {
        String allowed = ex.getSupportedHttpMethods() == null ? "" : ex.getSupportedHttpMethods().stream()
                .map(HttpMethod::name).collect(Collectors.joining(", "));
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .header(HttpHeaders.ALLOW, allowed)
                .body(problem(HttpStatus.METHOD_NOT_ALLOWED, "Method " + ex.getMethod() + " is not supported for this endpoint. Supported methods: " + allowed));
    }

    // 409 - excluir um registro que outro ainda usa (ex.: artista com albuns)
    @ExceptionHandler(ResourceInUseException.class)
    ProblemDetail resourceInUseHandler(ResourceInUseException ex) {
        return problem(HttpStatus.CONFLICT, ex.getMessage());
    }

    // indice unico, como aparece na mensagem do banco -> motivo do 409
    private static final Map<String, String> UNIQUE_VIOLATIONS = Map.of(
            "PUBLIC.TRACK(ALBUM_ID", "This album already has a track with this trackNumber",
            "PUBLIC.COVER(ALBUM_ID", "This album already has a cover: update or delete it instead of creating another",
            "PUBLIC.ARTIST(NAME", "An artist with this name already exists",
            "PUBLIC.GENRE(NAME", "A genre with this name already exists",
            "PUBLIC.RECORD_LABEL(NAME", "A record label with this name already exists");

    // 409 - o banco barrou um valor repetido num campo unico (nome, numero da faixa no album, capa do album)
    // a mensagem do banco traz a tabela e a coluna do indice, e por elas a resposta diz qual valor repetiu
    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail conflictHandler(DataIntegrityViolationException ex) {
        String detail = String.valueOf(ex.getMostSpecificCause().getMessage()).toUpperCase();
        return problem(HttpStatus.CONFLICT, UNIQUE_VIOLATIONS.entrySet().stream()
                .filter(violation -> detail.contains(violation.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse("Operation conflicts with existing data: a value that must be unique is already in use"));
    }

    // 400, 409 ou 422 - problema com a X-Idempotency-Key (cada caso ja traz o status certo)
    @ExceptionHandler(IdempotencyException.class)
    ProblemDetail idempotencyHandler(IdempotencyException ex) {
        return ProblemDetail.forStatusAndDetail(ex.getStatus(), ex.getMessage());
    }

    // 415 - corpo enviado em outro formato (ex.: text/plain)
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ProblemDetail mediaTypeHandler(HttpMediaTypeNotSupportedException ex) {
        return problem(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Content-Type '" + ex.getContentType() + "' is not supported: send the body as application/json");
    }

    // 429 - passou do limite de requisicoes; Retry-After diz em quantos segundos tentar de novo
    @ExceptionHandler(RateLimitExceededException.class)
    ResponseEntity<ProblemDetail> rateLimitHandler(RateLimitExceededException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(ex.getRetryAfterSeconds()))
                .body(problem(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage()));
    }

    // qualquer outro erro: as excecoes do proprio Spring (ex.: 406) ja trazem o status e o motivo;
    // o resto e falha inesperada (500), registrada no log sem mostrar detalhes internos para o cliente
    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> unexpectedHandler(Exception ex) {
        if (ex instanceof ErrorResponse error)
            return ResponseEntity.status(error.getStatusCode()).headers(error.getHeaders()).body(error.getBody());
        log.error("Erro inesperado", ex);
        return ResponseEntity.internalServerError().body(problem(HttpStatus.INTERNAL_SERVER_ERROR,
                "Unexpected error: the request could not be completed. Try again later"));
    }

    // o title (nome do status) e o instance (rota que deu erro) o Spring preenche sozinho
    private static ProblemDetail problem(HttpStatus status, String detail) {
        return ProblemDetail.forStatusAndDetail(status, detail);
    }

    // erros de validacao: o detail lista os campos e o "errors" traz o motivo de cada um
    private static ProblemDetail validationProblem(Map<String, String> errors) {
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Validation failed for: " + String.join(", ", errors.keySet()));
        problem.setProperty("errors", errors);
        return problem;
    }

    // monta o caminho do campo que deu erro no JSON, ex.: "type" ou "artists[0].id"
    private static String fieldPath(JacksonException ex) {
        StringBuilder path = new StringBuilder();
        for (JacksonException.Reference ref : ex.getPath()) {
            if (ref.getPropertyName() != null)
                path.append(path.isEmpty() ? "" : ".").append(ref.getPropertyName());
            else
                path.append('[').append(ref.getIndex()).append(']');
        }
        return path.toString();
    }
}
