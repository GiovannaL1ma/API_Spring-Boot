package com.senac.tsi.MusicApi.repository;

import com.senac.tsi.MusicApi.model.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {

    // a chave chega em texto no header; a busca e pelo hash dela
    Optional<ApiKey> findByKeyHash(String keyHash);
}
