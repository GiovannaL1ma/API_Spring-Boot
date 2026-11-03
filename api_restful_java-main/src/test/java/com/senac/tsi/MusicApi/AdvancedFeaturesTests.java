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
    void repeatedKeyCreationDoesNotRevealTheKeyAgain() throws Exception {
        String body = "{\"ownerName\":\"Repetida\",\"ownerEmail\":\"repetida@example.com\"}";

        MvcResult first = mvc.perform(post("/api-keys").header("X-Idempotency-Key", "chave-repetida")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.apiKey").exists())
                .andReturn();
        long id = ((Number) JsonPath.read(first.getResponse().getContentAsString(), "$.id")).longValue();

        // a repeticao nao cria outra chave e nao devolve a chave completa (ela nao fica guardada)
        mvc.perform(post("/api-keys").header("X-Idempotency-Key", "chave-repetida")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("X-Idempotency-Replayed", "true"))
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.keyPrefix").exists())
                .andExpect(jsonPath("$.apiKey").doesNotExist());
    }

    @Test
    void nullInsideListsIsBadRequestNotServerError() throws Exception {
        String userKey = createUserKey();
        long artistId = createAndGetId("/artists", userKey,
                "{\"name\":\"Lista Nula\",\"country\":\"Brazil\",\"startYear\":1990,\"type\":\"SOLO\"}");
        long albumId = createAndGetId("/albums", userKey,
                "{\"title\":\"Album Lista\",\"releaseYear\":2000,\"type\":\"STUDIO\",\"artists\":[{\"id\":" + artistId + "}]}");

        mvc.perform(post("/albums").header("X-API-Key", userKey).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"X\",\"releaseYear\":2000,\"type\":\"STUDIO\",\"artists\":[null]}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("artists")));
        mvc.perform(post("/albums").header("X-API-Key", userKey).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"X\",\"releaseYear\":2000,\"type\":\"STUDIO\",\"artists\":[{\"id\":" + artistId + "}],\"genres\":[null]}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("genres")));
        mvc.perform(post("/tracks").header("X-API-Key", userKey).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"X\",\"trackNumber\":1,\"durationSeconds\":10,\"album\":{\"id\":" + albumId + "},\"featuredArtists\":[null]}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("featuredArtists")));
    }

    @Test
    void conflictsSayWhichValueIsRepeated() throws Exception {
        String userKey = createUserKey();
        String label = "{\"name\":\"Gravadora Repetida\",\"country\":\"Brazil\",\"foundedYear\":1990}";
        String genre = "{\"name\":\"Genero Repetido\"}";
        String artist = "{\"name\":\"Artista Repetido\",\"country\":\"Brazil\",\"startYear\":1990,\"type\":\"SOLO\"}";
        createAndGetId("/labels", userKey, label);
        createAndGetId("/genres", userKey, genre);
        long artistId = createAndGetId("/artists", userKey, artist);
        long albumId = createAndGetId("/albums", userKey,
                "{\"title\":\"Album Repetido\",\"releaseYear\":2000,\"type\":\"STUDIO\",\"artists\":[{\"id\":" + artistId + "}]}");
        String track = "{\"title\":\"Faixa\",\"trackNumber\":1,\"durationSeconds\":10,\"album\":{\"id\":" + albumId + "}}";
        String cover = "{\"designer\":\"Designer\",\"description\":\"Capa\",\"imageUrl\":\"https://example.com/c.jpg\",\"album\":{\"id\":" + albumId + "}}";
        createAndGetId("/tracks", userKey, track);
        createAndGetId("/covers", userKey, cover);

        expectConflict("/labels", userKey, label, "A record label with this name already exists");
        expectConflict("/genres", userKey, genre, "A genre with this name already exists");
        expectConflict("/artists", userKey, artist, "An artist with this name already exists");
        expectConflict("/tracks", userKey, track, "This album already has a track with this trackNumber");
        expectConflict("/covers", userKey, cover, "This album already has a cover");
    }

    @Test
    void errorsFollowTheProblemDetailFormat() throws Exception {
        mvc.perform(get("/artists/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Invalid value 'abc' for parameter 'id': it must be a whole number"))
                .andExpect(jsonPath("$.instance").value("/artists/abc"));

        // na validacao, o "errors" traz cada campo com o motivo
        mvc.perform(post("/artists").header("X-API-Key", createUserKey()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"country\":\"Brazil\",\"startYear\":1800,\"type\":\"SOLO\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("startYear")))
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.startYear").exists());
    }

    @Test
    void swaggerDocumentsErrorsInTheSameFormat() throws Exception {
        mvc.perform(get("/v3/api-docs/v1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.schemas.ProblemDetail").exists())
                .andExpect(jsonPath("$.paths['/artists/{id}'].get.responses['404'].content['application/problem+json'].example.detail")
                        .value("Could not find artist with ID: 99"));
    }

    @Test
    void startsWithSampleData() throws Exception {
        mvc.perform(get("/artists/search").param("name", "AJULLIACOSTA"))
                .andExpect(jsonPath("$.page.totalElements").value(1));
        mvc.perform(get("/albums/search").param("title", "Brutas Amam"))
                .andExpect(jsonPath("$.page.totalElements").value(1));
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

    private void expectConflict(String path, String apiKey, String body, String reason) throws Exception {
        mvc.perform(post(path).header("X-API-Key", apiKey).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(content().string(containsString(reason)));
    }

    private long createAndGetId(String path, String apiKey, String body) throws Exception {
        MvcResult result = mvc.perform(post(path).header("X-API-Key", apiKey)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
    }
}
