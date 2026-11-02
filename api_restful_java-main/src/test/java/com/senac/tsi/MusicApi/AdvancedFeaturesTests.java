package com.senac.tsi.MusicApi;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// testa os recursos da fase 2 de ponta a ponta: chave de API, idempotencia, versionamento e CORS
// cada teste cria os proprios dados com nomes unicos, porque o banco H2 e compartilhado entre eles
@SpringBootTest(properties = "api.security.admin-key=" + AdvancedFeaturesTests.ADMIN_KEY)
@AutoConfigureMockMvc
class AdvancedFeaturesTests {

    static final String ADMIN_KEY = "mca_admin_key_for_tests";

    @Autowired
    private MockMvc mvc;

    @Test
    void readingIsPublicAndReportsRateLimit() throws Exception {
        mvc.perform(get("/artists"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-RateLimit-Limit"))
                .andExpect(header().exists("X-RateLimit-Remaining"))
                .andExpect(header().string("X-API-Version", "1"));
    }

    @Test
    void writingWithoutApiKeyIsUnauthorized() throws Exception {
        mvc.perform(post("/genres").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Sem chave\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists(HttpHeaders.WWW_AUTHENTICATE));
    }

    @Test
    void invalidApiKeyIsUnauthorizedEvenOnPublicEndpoints() throws Exception {
        mvc.perform(get("/artists").header("X-API-Key", "mca_chave_que_nao_existe"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void generatedKeyAllowsWritingButNotManagingKeys() throws Exception {
        String userKey = createUserKey();

        mvc.perform(post("/genres").header("X-API-Key", userKey)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Bossa Nova\"}"))
                .andExpect(status().isCreated());

        mvc.perform(get("/api-keys").header("X-API-Key", userKey))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api-keys").header("X-API-Key", ADMIN_KEY))
                .andExpect(status().isOk());
    }

    @Test
    void repeatedPostWithSameIdempotencyKeyIsNotProcessedTwice() throws Exception {
        String userKey = createUserKey();
        String body = "{\"name\":\"Idempotente\"}";

        MvcResult first = mvc.perform(post("/genres").header("X-API-Key", userKey).header("X-Idempotency-Key", "teste-1")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("X-Idempotency-Replayed", "false"))
                .andReturn();

        mvc.perform(post("/genres").header("X-API-Key", userKey).header("X-Idempotency-Key", "teste-1")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("X-Idempotency-Replayed", "true"))
                .andExpect(content().string(first.getResponse().getContentAsString()));

        // a repeticao nao criou outro genero
        mvc.perform(get("/genres/search").param("name", "Idempotente"))
                .andExpect(jsonPath("$.page.totalElements").value(1));

        // a mesma chave com outra requisicao e erro do cliente
        mvc.perform(post("/genres").header("X-API-Key", userKey).header("X-Idempotency-Key", "teste-1")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Outro\"}"))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void tracksHaveTwoVersionsSelectedByHeader() throws Exception {
        String userKey = createUserKey();
        long artistId = createAndGetId("/artists", userKey,
                "{\"name\":\"Versionado\",\"country\":\"Brazil\",\"startYear\":1990,\"type\":\"SOLO\"}");
        long albumId = createAndGetId("/albums", userKey,
                "{\"title\":\"Album V\",\"releaseYear\":2000,\"type\":\"STUDIO\",\"artists\":[{\"id\":" + artistId + "}]}");
        long trackId = createAndGetId("/tracks", userKey,
                "{\"title\":\"Faixa V\",\"trackNumber\":1,\"durationSeconds\":159,\"album\":{\"id\":" + albumId + "}}");

        // sem header a API responde a versao 1: duracao em segundos
        mvc.perform(get("/tracks/{id}", trackId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.durationSeconds").value(159));

        // versao 2: duracao formatada e album resumido
        mvc.perform(get("/tracks/{id}", trackId).header("X-API-Version", "2"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-API-Version", "2"))
                .andExpect(jsonPath("$.duration").value("2:39"))
                .andExpect(jsonPath("$.album.title").value("Album V"));

        mvc.perform(get("/tracks/{id}", trackId).header("X-API-Version", "3"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Supported versions: 1, 2")));
    }

    @Test
    void corsAllowsOnlyConfiguredOrigins() throws Exception {
        mvc.perform(options("/artists")
                        .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "x-api-key"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000"));

        mvc.perform(options("/artists")
                        .header(HttpHeaders.ORIGIN, "http://site-nao-autorizado.com")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden());
    }

    private String createUserKey() throws Exception {
        MvcResult result = mvc.perform(post("/api-keys").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ownerName\":\"Teste\",\"ownerEmail\":\"teste@example.com\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.apiKey");
    }

    private long createAndGetId(String path, String apiKey, String body) throws Exception {
        MvcResult result = mvc.perform(post(path).header("X-API-Key", apiKey)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
    }
}
