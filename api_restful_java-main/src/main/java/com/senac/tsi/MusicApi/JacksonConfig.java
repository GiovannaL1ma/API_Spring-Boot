package com.senac.tsi.MusicApi;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.cfg.CoercionInputShape;
import tools.jackson.databind.type.LogicalType;

// deixa a leitura do JSON estrita: cada campo tem que vir com o tipo que o Swagger mostra
// sem isso o Jackson convertia sozinho e o erro passava batido:
// "description": 300 virava o texto "300", "trackNumber": 5.7 virava 5 e "type": 1 virava DUO
@Configuration
public class JacksonConfig {

    @Bean
    public JsonMapperBuilderCustomizer strictTypes() {
        return builder -> builder
                // campos de texto so aceitam texto (nada de numero ou true/false)
                .withCoercionConfig(LogicalType.Textual, config -> config
                        .setCoercion(CoercionInputShape.Integer, CoercionAction.Fail)
                        .setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
                        .setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail))
                // campos inteiros so aceitam numero inteiro (nem "5" entre aspas, nem 5.7)
                .withCoercionConfig(LogicalType.Integer, config -> config
                        .setCoercion(CoercionInputShape.String, CoercionAction.Fail)
                        .setCoercion(CoercionInputShape.Float, CoercionAction.Fail))
                // enums so aceitam o nome (ex.: "BAND"), nao a posicao na lista
                .withCoercionConfig(LogicalType.Enum, config -> config
                        .setCoercion(CoercionInputShape.Integer, CoercionAction.Fail));
    }
}
