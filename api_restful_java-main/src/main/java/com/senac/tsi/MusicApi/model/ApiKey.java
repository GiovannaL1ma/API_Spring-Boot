package com.senac.tsi.MusicApi.model;

import jakarta.persistence.*;

import java.time.Instant;

// chave de API de um usuario; a chave em si nunca e guardada, so o hash SHA-256 dela
@Entity
public class ApiKey {

    private @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) long id;

    @Column(nullable = false, length = 100)
    private String ownerName;

    @Column(nullable = false, length = 150)
    private String ownerEmail;

    // hash da chave: se o banco vazar, as chaves continuam secretas
    @Column(nullable = false, unique = true, length = 64)
    private String keyHash;

    // comeco da chave (ex.: "mca_Ab12"), so para o dono reconhecer qual e
    @Column(nullable = false, length = 12)
    private String keyPrefix;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ApiKeyRole role;

    // chave desativada continua cadastrada, mas para de autenticar
    private boolean active = true;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant lastUsedAt;

    public ApiKey() {
    }

    public ApiKey(String ownerName, String ownerEmail, String keyHash, String keyPrefix, ApiKeyRole role) {
        this.ownerName = ownerName;
        this.ownerEmail = ownerEmail;
        this.keyHash = keyHash;
        this.keyPrefix = keyPrefix;
        this.role = role;
        this.createdAt = Instant.now();
    }

    public long getId() {
        return id;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public String getOwnerEmail() {
        return ownerEmail;
    }

    public String getKeyHash() {
        return keyHash;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public ApiKeyRole getRole() {
        return role;
    }

    public void setRole(ApiKeyRole role) {
        this.role = role;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastUsedAt() {
        return lastUsedAt;
    }

    public void setLastUsedAt(Instant lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }
}
