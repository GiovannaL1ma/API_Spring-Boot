package com.senac.tsi.MusicApi.model;

import jakarta.persistence.*;

import java.time.Instant;

// guarda o resultado de um POST feito com X-Idempotency-Key, para devolver a mesma resposta se ele for repetido
// a mesma chave so vale dentro do mesmo cliente (scope = chave de API ou IP)
@Entity
@Table(uniqueConstraints = @UniqueConstraint(name = "uq_idempotency_scope_key", columnNames = {"scope", "idempotency_key"}))
public class IdempotencyRecord {

    public enum Status {
        IN_PROGRESS, // a primeira requisicao ainda esta sendo processada
        COMPLETED    // terminou com sucesso e a resposta esta guardada
    }

    private @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) long id;

    @Column(nullable = false, length = 80)
    private String scope;

    @Column(name = "idempotency_key", nullable = false, length = 255)
    private String idempotencyKey;

    // hash de metodo + rota + corpo: mesma chave com outra requisicao e erro do cliente
    @Column(nullable = false, length = 64)
    private String requestHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private Status status = Status.IN_PROGRESS;

    private int responseStatus;

    private String responseContentType;

    private String responseLocation;

    @Lob
    private String responseBody;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant expiresAt;

    public IdempotencyRecord() {
    }

    public IdempotencyRecord(String scope, String idempotencyKey, String requestHash, Instant expiresAt) {
        this.scope = scope;
        this.idempotencyKey = idempotencyKey;
        this.requestHash = requestHash;
        this.createdAt = Instant.now();
        this.expiresAt = expiresAt;
    }

    // guarda a resposta da primeira execucao
    public void complete(int status, String contentType, String location, String body) {
        this.status = Status.COMPLETED;
        this.responseStatus = status;
        this.responseContentType = contentType;
        this.responseLocation = location;
        this.responseBody = body;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public long getId() {
        return id;
    }

    public String getRequestHash() {
        return requestHash;
    }

    public Status getStatus() {
        return status;
    }

    public int getResponseStatus() {
        return responseStatus;
    }

    public String getResponseContentType() {
        return responseContentType;
    }

    public String getResponseLocation() {
        return responseLocation;
    }

    public String getResponseBody() {
        return responseBody;
    }
}
