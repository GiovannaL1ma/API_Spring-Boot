package com.senac.tsi.MusicApi;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ArtistRepository extends JpaRepository<Artist, Long> {

    Page<Artist> findByNameContainingIgnoreCase(String name, Pageable pageable);

    Page<Artist> findByType(ArtistType type, Pageable pageable);

    @Query("select a from Artist a where exists " +
            "(select 1 from Album al join al.artists aa join al.genres g where aa = a and g.id = :genreId)")
    Page<Artist> findByGenreId(@Param("genreId") long genreId, Pageable pageable);
}
