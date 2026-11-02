package com.senac.tsi.MusicApi.web;

import com.senac.tsi.MusicApi.model.ApiKeyRole;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// marca um endpoint (ou um controller inteiro) que so funciona com X-API-Key valida
// o ApiKeyInterceptor confere a chave e o nivel; o Swagger mostra o cadeado nesses endpoints
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresApiKey {

    // nivel minimo de acesso; ADMIN tambem passa onde se pede USER
    ApiKeyRole value() default ApiKeyRole.USER;
}
