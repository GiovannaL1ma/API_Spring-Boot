package com.senac.tsi.MusicApi.model;

// niveis de acesso das chaves de API
public enum ApiKeyRole {
    USER,   // cria, altera e exclui registros do catalogo
    ADMIN;  // tudo o que o USER faz e ainda gerencia as chaves de todo mundo

    // ADMIN pode tudo; USER so o que exige USER
    public boolean includes(ApiKeyRole required) {
        return this == ADMIN || this == required;
    }
}
