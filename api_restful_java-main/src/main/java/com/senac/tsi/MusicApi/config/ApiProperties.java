package com.senac.tsi.MusicApi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;
import java.util.List;

// configuracoes proprias da API (prefixo "api." no application.properties), lidas com tipo certo
@ConfigurationProperties("api")
public record ApiProperties(@DefaultValue Security security, @DefaultValue RateLimit rateLimit,
                            @DefaultValue Cors cors, @DefaultValue Idempotency idempotency) {

    // adminKey vazia = a API gera uma chave ADMIN nova a cada execucao e mostra no log
    public record Security(@DefaultValue("") String adminKey) {
    }

    // quantas requisicoes cada cliente pode fazer por minuto
    public record RateLimit(@DefaultValue("60") int anonymousPerMinute, @DefaultValue("120") int withKeyPerMinute) {
    }

    // sites (origens) que podem chamar a API pelo navegador
    public record Cors(@DefaultValue("http://localhost:3000") List<String> allowedOrigins) {
    }

    // por quanto tempo uma X-Idempotency-Key fica guardada
    public record Idempotency(@DefaultValue("24h") Duration ttl) {
    }
}
