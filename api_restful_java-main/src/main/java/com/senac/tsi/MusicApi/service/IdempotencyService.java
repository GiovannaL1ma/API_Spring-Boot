package com.senac.tsi.MusicApi.service;

import com.senac.tsi.MusicApi.config.ApiProperties;
import com.senac.tsi.MusicApi.exception.IdempotencyException;
import com.senac.tsi.MusicApi.model.IdempotencyRecord;
import com.senac.tsi.MusicApi.repository.IdempotencyRecordRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

// decide o que fazer com um POST que chegou com X-Idempotency-Key:
// chave nova -> executa e guarda a resposta | chave repetida com a mesma requisicao -> devolve a resposta guardada
// sem @Transactional na classe de proposito: cada operacao no banco fecha sozinha, assim a corrida entre
// duas requisicoes iguais e resolvida pela restricao unica (scope, idempotency_key)
@Service
public class IdempotencyService {

    // o que o interceptor deve fazer com a requisicao
    public sealed interface Decision permits Proceed, Replay {
    }

    public record Proceed(long recordId) implements Decision {
    }

    public record Replay(IdempotencyRecord record) implements Decision {
    }

    private final IdempotencyRecordRepository repository;
    private final Duration ttl;

    public IdempotencyService(IdempotencyRecordRepository repository, ApiProperties properties) {
        this.repository = repository;
        this.ttl = properties.idempotency().ttl();
    }

    public Decision begin(String scope, String key, String requestHash) {
        Optional<IdempotencyRecord> existing = repository.findByScopeAndIdempotencyKey(scope, key);
        if (existing.isPresent()) {
            IdempotencyRecord record = existing.get();
            if (record.isExpired()) {
                repository.delete(record);
            } else {
                if (!record.getRequestHash().equals(requestHash))
                    throw IdempotencyException.reused(key);
                if (record.getStatus() == IdempotencyRecord.Status.IN_PROGRESS)
                    throw IdempotencyException.inProgress(key);
                return new Replay(record);
            }
        }

        try {
            IdempotencyRecord record = repository.saveAndFlush(
                    new IdempotencyRecord(scope, key, requestHash, Instant.now().plus(ttl)));
            return new Proceed(record.getId());
        } catch (DataIntegrityViolationException e) {
            // outra requisicao com a mesma chave chegou junto e registrou primeiro
            throw IdempotencyException.inProgress(key);
        }
    }

    // a primeira execucao deu certo: guarda a resposta para as repeticoes
    public void complete(long recordId, int status, String contentType, String location, String body) {
        repository.findById(recordId).ifPresent(record -> {
            record.complete(status, contentType, location, body);
            repository.save(record);
        });
    }

    // a primeira execucao falhou (ex.: 400): libera a chave para o cliente tentar de novo com os dados corrigidos
    public void abandon(long recordId) {
        repository.deleteById(recordId);
    }

    // de hora em hora apaga as chaves vencidas
    @Scheduled(fixedRate = 60 * 60 * 1000)
    public void deleteExpired() {
        repository.deleteExpired(Instant.now());
    }
}
