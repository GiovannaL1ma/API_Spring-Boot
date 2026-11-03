package com.senac.tsi.MusicApi.web;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// marca campos da resposta de um POST que a idempotencia NAO guarda (ex.: um segredo que so pode aparecer uma vez)
// o IdempotencyInterceptor tira esses campos antes de salvar, entao a repeticao com a mesma X-Idempotency-Key volta sem eles
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OmitOnReplay {

    // nomes dos campos do JSON de resposta (ex.: "apiKey")
    String[] value();
}
