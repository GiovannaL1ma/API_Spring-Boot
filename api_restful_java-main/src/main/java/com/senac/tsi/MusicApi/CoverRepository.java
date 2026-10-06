package com.senac.tsi.MusicApi;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CoverRepository extends JpaRepository<Cover, Long> {

    Page<Cover> findByDesignerContainingIgnoreCase(String designer, Pageable pageable);

    Optional<Cover> findByAlbumId(long albumId);
}
