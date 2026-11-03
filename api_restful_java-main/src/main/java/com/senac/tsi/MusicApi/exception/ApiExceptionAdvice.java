package com.senac.tsi.MusicApi.exception;

import com.senac.tsi.MusicApi.config.WebConfig;
import com.senac.tsi.MusicApi.web.ApiKeyResolver;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.accept.InvalidApiVersionException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
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
// cada metodo devolve uma mensagem dizendo O MOTIVO do erro, para quem testa no Swagger entender o que mandou errado
@RestControllerAdvice
public class ApiExceptionAdvice {

    // 404 - qualquer um dos NotFoundException, devolve a mensagem da exception
    @ExceptionHandler({ArtistNotFoundException.class, AlbumNotFoundException.class, TrackNotFoundException.class,
            GenreNotFoundException.class, RecordLabelNotFoundException.class, CoverNotFoundException.class,
            ApiKeyNotFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    String notFoundHandler(RuntimeException ex) {
        return ex.getMessage();
    }

    // 404 - rota que nao existe (ex.: GET /artistas)
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    String noEndpointHandler(NoResourceFoundException ex) {
        return "No endpoint " + ex.getHttpMethod() + " /" + ex.getResourcePath() + ": check the URL in the Swagger documentation";
    }

    // 400 - o JSON nao passou nas validacoes (@NotBlank, @Size, @Min...), devolve cada campo com o erro dele
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    Map<String, String> validationHandler(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
        ex.getBindingResult().getGlobalErrors()
                .forEach(error -> errors.put(error.getObjectName(), error.getDefaultMessage()));
        return errors;
    }

    // 400 - parametro da URL fora da regra (ex.: ?from=1800 em /albums/released), devolve cada parametro com o erro dele
    @ExceptionHandler(HandlerMethodValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    Map<String, String> parameterValidationHandler(HandlerMethodValidationException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getParameterValidationResults().forEach(result -> errors.put(
                result.getMethodParameter().getParameterName(),
                result.getResolvableErrors().stream().map(error -> error.getDefaultMessage()).collect(Collectors.joining("; "))));
        return errors;
    }

    // 400 - corpo vazio, JSON quebrado ou campo com valor do tipo errado (ex.: "type": "ROCK", "releaseYear": "abc" ou "description": 300)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    String unreadableHandler(HttpMessageNotReadableException ex) {
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
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    String typeMismatchHandler(MethodArgumentTypeMismatchException ex) {
        Class<?> type = ex.getRequiredType();
        if (type != null && type.isEnum())
            return "Invalid value '" + ex.getValue() + "' for parameter '" + ex.getName() + "'. Accepted values: " + Arrays.toString(type.getEnumConstants());
        return "Invalid value '" + ex.getValue() + "' for parameter '" + ex.getName() + "': it must be a whole number";
    }

    // 400 - faltou um parametro obrigatorio (ex.: /artists/search sem o ?name=)
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    String missingParameterHandler(MissingServletRequestParameterException ex) {
        return "Required parameter '" + ex.getParameterName() + "' is missing";
    }

    // 400 - o sort pediu um campo que a entidade nao tem (ex.: ?sort=abc); sem esse handler virava 500
    @ExceptionHandler(PropertyReferenceException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    String sortHandler(PropertyReferenceException ex) {
        return "Invalid sort: " + ex.getType().getType().getSimpleName() + " has no field '" + ex.getPropertyName() + "'";
    }

    // 400 - parametros que nao fazem sentido juntos (ex.: periodo com from maior que to)
    @ExceptionHandler(InvalidSearchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    String invalidSearchHandler(InvalidSearchException ex) {
        return ex.getMessage();
    }

    // 400 - X-API-Version com uma versao que a API nao tem (ex.: 3)
    @ExceptionHandler(InvalidApiVersionException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    String apiVersionHandler(InvalidApiVersionException ex) {
        return "Unsupported API version '" + ex.getVersion() + "' in the " + WebConfig.API_VERSION_HEADER
                + " header. Supported versions: " + String.join(", ", WebConfig.SUPPORTED_API_VERSIONS);
    }

    // 401 - chave de API ausente, invalida ou desativada; o header WWW-Authenticate diz como se autenticar
    @ExceptionHandler(ApiKeyAuthenticationException.class)
    ResponseEntity<String> authenticationHandler(ApiKeyAuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .header(HttpHeaders.WWW_AUTHENTICATE, "ApiKey header=\"" + ApiKeyResolver.HEADER + "\"")
                .body(ex.getMessage());
    }

    // 403 - a chave e valida, mas o nivel dela nao permite a operacao
    @ExceptionHandler(InsufficientApiKeyRoleException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    String forbiddenHandler(InsufficientApiKeyRoleException ex) {
        return ex.getMessage();
    }

    // 405 - metodo HTTP que a rota nao aceita (ex.: PATCH /artists/1); o header Allow lista os aceitos
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<String> methodNotAllowedHandler(HttpRequestMethodNotSupportedException ex) {
        String allowed = ex.getSupportedHttpMethods() == null ? "" : ex.getSupportedHttpMethods().stream()
                .map(HttpMethod::name).collect(Collectors.joining(", "));
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .header(HttpHeaders.ALLOW, allowed)
                .body("Method " + ex.getMethod() + " is not supported for this endpoint. Supported methods: " + allowed);
    }

    // 409 - excluir um registro que outro ainda usa (ex.: artista com albuns)
    @ExceptionHandler(ResourceInUseException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    String resourceInUseHandler(ResourceInUseException ex) {
        return ex.getMessage();
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
    @ResponseStatus(HttpStatus.CONFLICT)
    String conflictHandler(DataIntegrityViolationException ex) {
        String detail = String.valueOf(ex.getMostSpecificCause().getMessage()).toUpperCase();
        return UNIQUE_VIOLATIONS.entrySet().stream()
                .filter(violation -> detail.contains(violation.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse("Operation conflicts with existing data: a value that must be unique is already in use");
    }

    // 400, 409 ou 422 - problema com a X-Idempotency-Key (cada caso ja traz o status certo)
    @ExceptionHandler(IdempotencyException.class)
    ResponseEntity<String> idempotencyHandler(IdempotencyException ex) {
        return ResponseEntity.status(ex.getStatus()).body(ex.getMessage());
    }

    // 415 - corpo enviado em outro formato (ex.: text/plain)
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    String mediaTypeHandler(HttpMediaTypeNotSupportedException ex) {
        return "Content-Type '" + ex.getContentType() + "' is not supported: send the body as application/json";
    }

    // 429 - passou do limite de requisicoes; Retry-After diz em quantos segundos tentar de novo
    @ExceptionHandler(RateLimitExceededException.class)
    ResponseEntity<String> rateLimitHandler(RateLimitExceededException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(ex.getRetryAfterSeconds()))
                .body(ex.getMessage());
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
