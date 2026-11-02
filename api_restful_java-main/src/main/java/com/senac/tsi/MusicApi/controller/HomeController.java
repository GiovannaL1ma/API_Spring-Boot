package com.senac.tsi.MusicApi.controller;

import com.senac.tsi.MusicApi.config.OpenApiConfig;
import com.senac.tsi.MusicApi.config.WebConfig;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Tag(name = "Home", description = "Ponto de entrada da API, com links para todas as colecoes")
public class HomeController {

    @Operation(summary = "API root", description = "Raiz da API: links HATEOAS para todas as colecoes, para as chaves de API e para a documentacao")
    @ApiResponse(responseCode = "200", description = "Returned the links to all collections")
    @GetMapping("/")
    public ApiRoot root() {
        ApiRoot model = new ApiRoot();

        model.add(linkTo(methodOn(HomeController.class).root()).withSelfRel());
        model.add(linkTo(methodOn(ArtistController.class).getAllArtists(Pageable.unpaged())).withRel("artists"));
        model.add(linkTo(methodOn(AlbumController.class).getAllAlbums(Pageable.unpaged())).withRel("albums"));
        model.add(linkTo(methodOn(TrackController.class).getAllTracks(Pageable.unpaged())).withRel("tracks"));
        model.add(linkTo(methodOn(GenreController.class).getAllGenres(Pageable.unpaged())).withRel("genres"));
        model.add(linkTo(methodOn(RecordLabelController.class).getAllLabels(Pageable.unpaged())).withRel("labels"));
        model.add(linkTo(methodOn(CoverController.class).getAllCovers(Pageable.unpaged())).withRel("covers"));

        // onde gerar a chave de API (POST) para criar, alterar e excluir dados
        model.add(linkTo(methodOn(ApiKeyController.class).createKey(null)).withRel("api-keys"));

        // Documentacao: Swagger UI e o JSON do OpenAPI de cada versao
        model.add(linkTo(HomeController.class).slash("swagger-ui.html").withRel("docs"));
        model.add(linkTo(HomeController.class).slash("v3").slash("api-docs").slash("v1").withRel("openapi-v1"));
        model.add(linkTo(HomeController.class).slash("v3").slash("api-docs").slash("v2").withRel("openapi-v2"));

        // Termos de servico e licenca (arquivos em src/main/resources/static), os mesmos do topo do Swagger
        model.add(linkTo(HomeController.class).slash("terms.html").withRel("terms-of-service"));
        model.add(linkTo(HomeController.class).slash("license.txt").withRel("license"));
        return model;
    }

    // Corpo da resposta da raiz: nome e versao da API, como escolher a versao dos endpoints, mais os _links
    public static class ApiRoot extends RepresentationModel<ApiRoot> {

        public String getName() {
            return "Music Catalog API";
        }

        public String getVersion() {
            return OpenApiConfig.VERSION;
        }

        public String getApiVersionHeader() {
            return WebConfig.API_VERSION_HEADER;
        }

        public List<String> getApiVersions() {
            return List.of(WebConfig.SUPPORTED_API_VERSIONS);
        }
    }
}
