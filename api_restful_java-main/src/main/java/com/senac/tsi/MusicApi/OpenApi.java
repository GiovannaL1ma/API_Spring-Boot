package com.senac.tsi.MusicApi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApi {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Music Catalog API")
                        .version("1.0.0")
                        .description("API RESTful para gerenciar um catalogo de discos: artistas, albuns, faixas, generos " +
                                "e gravadoras, com albuns colaborativos (varios artistas principais), participacoes especiais (feat.), " +
                                "capas e consultas que cruzam esses vinculos, como os artistas que lancaram discos de um genero. " +
                                "Todas as listagens sao paginadas (page, size, sort) e as respostas seguem HATEOAS (HAL). " +
                                "A raiz / traz links para todas as colecoes.")
                        .termsOfService("https://swagger.io/terms/")
                        .license(new License().name("MIT").url("https://mit-license.org/"))
                        .contact(new Contact().name("TSI")
                                .url("https://www.senac.com.br")
                                .email("****@sp.senac.br"))
                );
    }
}
