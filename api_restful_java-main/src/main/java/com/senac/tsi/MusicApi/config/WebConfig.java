package com.senac.tsi.MusicApi.config;

import com.senac.tsi.MusicApi.web.ApiKeyInterceptor;
import com.senac.tsi.MusicApi.web.ApiKeyResolver;
import com.senac.tsi.MusicApi.web.ApiVersionInterceptor;
import com.senac.tsi.MusicApi.web.IdempotencyInterceptor;
import com.senac.tsi.MusicApi.web.RateLimitInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.config.annotation.ApiVersionConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// configuracao do Spring MVC: versionamento por header, CORS e a ordem dos interceptors
@Configuration
public class WebConfig implements WebMvcConfigurer {

    public static final String API_VERSION_HEADER = "X-API-Version";
    public static final String DEFAULT_API_VERSION = "1";
    public static final String[] SUPPORTED_API_VERSIONS = {"1", "2"};

    // rotas da API; Swagger, termos e licenca ficam de fora do limite e da autenticacao
    private static final String[] API_PATHS = {
            "/", "/artists/**", "/albums/**", "/tracks/**", "/covers/**", "/genres/**", "/labels/**", "/api-keys/**"};

    private final RateLimitInterceptor rateLimitInterceptor;
    private final ApiKeyInterceptor apiKeyInterceptor;
    private final IdempotencyInterceptor idempotencyInterceptor;
    private final ApiVersionInterceptor apiVersionInterceptor;
    private final ApiProperties properties;

    public WebConfig(RateLimitInterceptor rateLimitInterceptor, ApiKeyInterceptor apiKeyInterceptor,
                     IdempotencyInterceptor idempotencyInterceptor, ApiVersionInterceptor apiVersionInterceptor,
                     ApiProperties properties) {
        this.rateLimitInterceptor = rateLimitInterceptor;
        this.apiKeyInterceptor = apiKeyInterceptor;
        this.idempotencyInterceptor = idempotencyInterceptor;
        this.apiVersionInterceptor = apiVersionInterceptor;
        this.properties = properties;
    }

    // a versao vem no header X-API-Version; sem o header a requisicao e atendida pela versao 1
    // um endpoint com version = "2" so responde a quem pedir a 2; endpoints sem version respondem as duas
    @Override
    public void configureApiVersioning(ApiVersionConfigurer configurer) {
        configurer.useRequestHeader(API_VERSION_HEADER)
                .setDefaultVersion(DEFAULT_API_VERSION)
                .addSupportedVersions(SUPPORTED_API_VERSIONS);
    }

    // a ordem importa: primeiro o limite (barra abuso antes de tudo), depois a chave, depois a idempotencia
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor).addPathPatterns(API_PATHS);
        registry.addInterceptor(apiKeyInterceptor).addPathPatterns(API_PATHS);
        registry.addInterceptor(idempotencyInterceptor).addPathPatterns(API_PATHS);
        registry.addInterceptor(apiVersionInterceptor).addPathPatterns(API_PATHS);
    }

    // so os sites listados em api.cors.allowed-origins podem chamar a API pelo navegador
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(properties.cors().allowedOrigins().toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders(HttpHeaders.CONTENT_TYPE, HttpHeaders.ACCEPT, ApiKeyResolver.HEADER,
                        API_VERSION_HEADER, IdempotencyInterceptor.HEADER)
                // headers que o JavaScript do site pode ler na resposta
                .exposedHeaders(HttpHeaders.LOCATION, HttpHeaders.RETRY_AFTER, API_VERSION_HEADER,
                        RateLimitInterceptor.LIMIT_HEADER, RateLimitInterceptor.REMAINING_HEADER,
                        RateLimitInterceptor.RESET_HEADER, IdempotencyInterceptor.REPLAYED_HEADER)
                .maxAge(3600); // o navegador guarda a resposta do preflight por 1 hora
    }
}
