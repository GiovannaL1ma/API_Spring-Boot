package com.senac.tsi.MusicApi;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// limite baixo (3 por minuto) so neste teste, para chegar no 429 rapido
@SpringBootTest(properties = "api.rate-limit.anonymous-per-minute=3")
@AutoConfigureMockMvc
class RateLimitTests {

    @Autowired
    private MockMvc mvc;

    @Test
    void exceedingTheLimitReturns429WithRetryAfter() throws Exception {
        for (int request = 1; request <= 3; request++)
            mvc.perform(get("/genres"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("X-RateLimit-Limit", "3"))
                    .andExpect(header().string("X-RateLimit-Remaining", String.valueOf(3 - request)));

        mvc.perform(get("/genres"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists(HttpHeaders.RETRY_AFTER))
                .andExpect(header().string("X-RateLimit-Remaining", "0"));
    }
}
