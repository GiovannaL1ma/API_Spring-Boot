package com.senac.tsi.MusicApi.service;

import com.senac.tsi.MusicApi.exception.ApiKeyNotFoundException;
import com.senac.tsi.MusicApi.model.ApiKey;
import com.senac.tsi.MusicApi.model.ApiKeyRole;
import com.senac.tsi.MusicApi.repository.ApiKeyRepository;
import com.senac.tsi.MusicApi.util.Hashing;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

// gera, autentica e gerencia as chaves de API
@Service
@Transactional(readOnly = true)
public class ApiKeyService {

    // toda chave comeca com "mca_" (Music Catalog API), assim fica facil reconhecer uma chave vazada num log ou no git
    private static final String KEY_PREFIX = "mca_";

    private final ApiKeyRepository repository;
    private final SecureRandom random = new SecureRandom();

    public ApiKeyService(ApiKeyRepository repository) {
        this.repository = repository;
    }

    // chave recem-criada: a entidade salva e a chave em texto, que so existe neste momento
    public record CreatedApiKey(ApiKey apiKey, String rawKey) {
    }

    @Transactional
    public CreatedApiKey create(String ownerName, String ownerEmail, ApiKeyRole role) {
        String rawKey = generateRawKey();
        return new CreatedApiKey(register(ownerName, ownerEmail, role, rawKey), rawKey);
    }

    // cadastra uma chave que ja existe em texto (usado na chave ADMIN que vem da variavel de ambiente)
    @Transactional
    public ApiKey register(String ownerName, String ownerEmail, ApiKeyRole role, String rawKey) {
        String prefix = rawKey.substring(0, Math.min(8, rawKey.length()));
        return repository.save(new ApiKey(ownerName, ownerEmail, hash(rawKey), prefix, role));
    }

    // devolve a chave so se ela existir e estiver ativa; aproveita para registrar o ultimo uso
    @Transactional
    public Optional<ApiKey> authenticate(String rawKey) {
        return repository.findByKeyHash(hash(rawKey))
                .filter(ApiKey::isActive)
                .map(key -> {
                    key.setLastUsedAt(Instant.now());
                    return key;
                });
    }

    public Page<ApiKey> findAll(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public ApiKey findById(long id) {
        return repository.findById(id).orElseThrow(() -> new ApiKeyNotFoundException(id));
    }

    // PATCH: so muda o que veio preenchido
    @Transactional
    public ApiKey update(long id, Boolean active, ApiKeyRole role) {
        ApiKey key = findById(id);
        if (active != null)
            key.setActive(active);
        if (role != null)
            key.setRole(role);
        return key;
    }

    @Transactional
    public void delete(long id) {
        repository.delete(findById(id));
    }

    public String generateRawKey() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return KEY_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hash(String rawKey) {
        return Hashing.sha256(rawKey);
    }
}
