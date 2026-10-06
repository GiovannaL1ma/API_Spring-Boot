package com.senac.tsi.MusicApi;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionAdvice {

    // 404 - recurso nao encontrado
    @ExceptionHandler({ArtistNotFoundException.class, AlbumNotFoundException.class, TrackNotFoundException.class,
            GenreNotFoundException.class, RecordLabelNotFoundException.class, CoverNotFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    String notFoundHandler(RuntimeException ex) {
        return ex.getMessage();
    }

    // 400 - payload nao passou nas validacoes (Bean Validation)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    Map<String, String> validationHandler(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
        return errors;
    }

    // 400 - JSON mal formatado ou valor de enum invalido
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    String unreadableHandler(HttpMessageNotReadableException ex) {
        return "Malformed request body";
    }

    // 409 - violacao de unicidade (nome, numero da faixa no album, capa do album) ou exclusao de registro ainda referenciado
    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    String conflictHandler(DataIntegrityViolationException ex) {
        return "Operation conflicts with existing data (duplicated value or record still referenced)";
    }
}
