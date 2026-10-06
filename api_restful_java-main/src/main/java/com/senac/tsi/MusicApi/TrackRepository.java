package com.senac.tsi.MusicApi;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TrackRepository extends JpaRepository<Track, Long> {

    Page<Track> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    Page<Track> findByAlbumId(long albumId, Pageable pageable);

    // Many-to-Many: consulta pela tabela pivot track_featured_artist
    Page<Track> findByFeaturedArtistsId(long artistId, Pageable pageable);
}
