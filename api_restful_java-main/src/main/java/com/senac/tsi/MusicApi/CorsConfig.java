package com.senac.tsi.MusicApi;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// libera o CORS para que qualquer front-end (ex.: um site hospedado em outro dominio) consiga chamar a API publicada
@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**") // vale para todos os endpoints
                        .allowedOrigins("*") // API publica: qualquer site pode chamar
                        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .maxAge(3600); // guarda o preflight em cache por 1 hora
            }
        };
    }
}
