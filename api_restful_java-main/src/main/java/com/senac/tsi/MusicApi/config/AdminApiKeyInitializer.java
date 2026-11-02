package com.senac.tsi.MusicApi.config;

import com.senac.tsi.MusicApi.model.ApiKeyRole;
import com.senac.tsi.MusicApi.service.ApiKeyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

// o banco comeca vazio a cada execucao, entao alguem precisa nascer ADMIN para gerenciar as chaves
// a chave vem da variavel de ambiente API_ADMIN_KEY; sem ela, uma chave nova e gerada e mostrada no log
@Component
public class AdminApiKeyInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminApiKeyInitializer.class);

    private final ApiKeyService apiKeyService;
    private final ApiProperties properties;

    public AdminApiKeyInitializer(ApiKeyService apiKeyService, ApiProperties properties) {
        this.apiKeyService = apiKeyService;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        String adminKey = properties.security().adminKey();
        if (adminKey.isBlank()) {
            adminKey = apiKeyService.generateRawKey();
            log.warn("API_ADMIN_KEY nao definida. Chave ADMIN gerada para esta execucao: {}", adminKey);
        } else {
            log.info("Chave ADMIN carregada da variavel de ambiente API_ADMIN_KEY");
        }
        apiKeyService.register("Administrador", "admin@musiccatalog.local", ApiKeyRole.ADMIN, adminKey);
    }
}
