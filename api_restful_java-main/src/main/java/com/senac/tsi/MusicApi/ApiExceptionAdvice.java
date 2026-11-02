package com.senac.tsi.MusicApi;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import tools.jackson.core.JacksonException;
import tools.jackson.core.exc.InputCoercionException;
import tools.jackson.databind.exc.MismatchedInputException;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

// trata os erros de todos os controllers num lugar so
// cada metodo devolve uma mensagem dizendo O MOTIVO do erro, para quem testa no Swagger entender o que mandou errado
@RestControllerAdvice
public class ApiExceptionAdvice {

    // 404 - qualquer um dos NotFoundException, devolve a mensagem da exception
    @ExceptionHandler({ArtistNotFoundException.class, AlbumNotFoundException.class, TrackNotFoundException.class,
            GenreNotFoundException.class, RecordLabelNotFoundException.class, CoverNotFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    String notFoundHandler(RuntimeException ex) {
        return ex.getMessage();
    }

    // 400 - o JSON nao passou nas validacoes (@NotBlank, @Size, @Min...), devolve cada campo com o erro dele
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    Map<String, String> validationHandler(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
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

    // 409 - o banco barrou um valor repetido num campo unico (nome, numero da faixa no album, capa do album)
    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    String conflictHandler(DataIntegrityViolationException ex) {
        return "Operation conflicts with existing data: a value that must be unique is already in use";
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
