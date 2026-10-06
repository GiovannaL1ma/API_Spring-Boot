package com.senac.tsi.MusicApi;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AlbumRepository extends JpaRepository<Album, Long> {

    Page<Album> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    Page<Album> findByType(AlbumType type, Pageable pageable);

    Page<Album> findByReleaseYearBetween(int from, int to, Pageable pageable);

    Page<Album> findByLabelId(long labelId, Pageable pageable);

    // Many-to-Many: consulta pela tabela pivot album_artist
    Page<Album> findByArtistsId(long artistId, Pageable pageable);

    // Many-to-Many: consulta pela tabela pivot album_genre
    Page<Album> findByGenresId(long genreId, Pageable pageable);
}
