package com.senac.tsi.MusicApi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// informacoes que aparecem no topo do Swagger (titulo, versao, descricao, termos, contato e licenca)
@Configuration
public class OpenApi {

    // versao da v1 em um lugar so: aparece no Swagger e na raiz (/); tem que bater com o <version> do pom.xml
    // MAJOR muda quando quebra quem ja usa a API (e ganha rota nova: /v2), MINOR quando acrescenta algo, PATCH quando so corrige
    public static final String V1 = "1.0.0";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Music Catalog API")
                        .version(V1)
                        .description("API RESTful para gerenciar um catalogo de discos: artistas, albuns, faixas, capas, generos " +
                                "e gravadoras, com albuns colaborativos e participacoes especiais (feat.). " +
                                "Todas as listagens sao paginadas (page, size, sort) e as respostas seguem HATEOAS (HAL).\n\n" +
                                "**Versoes:** a versao fica na URL e todas as rotas desta comecam com /v1. " +
                                "Uma versao nova sai ao lado (ex.: /v2) sem tirar a anterior do ar; " +
                                "troque a versao exibida no seletor \"Select a definition\", no topo da pagina.")
                        // termos e licenca sao servidos pela propria API (src/main/resources/static)
                        // o link e relativo: o Swagger completa com o endereco do servidor, entao funciona local e no Render
                        .termsOfService("/terms.html")
                        .license(new License().name("MIT").url("/license.txt"))
                        .contact(new Contact().name("TSI")
                                .url("https://www.sp.senac.br/"))
                );
    }

    // cada versao da API vira uma opcao no seletor do Swagger; a v2 entra aqui como outro GroupedOpenApi com /v2/**
    @Bean
    public GroupedOpenApi v1Api() {
        return GroupedOpenApi.builder()
                .group("v1")
                .displayName("v1 (" + V1 + ")")
                .pathsToMatch("/", "/v1/**")
                .build();
    }
}
