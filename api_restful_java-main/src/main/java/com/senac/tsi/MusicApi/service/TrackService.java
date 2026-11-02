package com.senac.tsi.MusicApi.service;

import com.senac.tsi.MusicApi.exception.AlbumNotFoundException;
import com.senac.tsi.MusicApi.exception.ArtistNotFoundException;
import com.senac.tsi.MusicApi.exception.TrackNotFoundException;
import com.senac.tsi.MusicApi.model.Album;
import com.senac.tsi.MusicApi.model.Artist;
import com.senac.tsi.MusicApi.model.Track;
import com.senac.tsi.MusicApi.repository.AlbumRepository;
import com.senac.tsi.MusicApi.repository.ArtistRepository;
import com.senac.tsi.MusicApi.repository.TrackRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

// regras de negocio das faixas: resolve o album e os artistas convidados (feat.) pelo id
@Service
@Transactional(readOnly = true)
public class TrackService {

    private final TrackRepository repository;
    private final AlbumRepository albumRepository;
    private final ArtistRepository artistRepository;

    public TrackService(TrackRepository repository, AlbumRepository albumRepository, ArtistRepository artistRepository) {
        this.repository = repository;
        this.albumRepository = albumRepository;
        this.artistRepository = artistRepository;
    }

    public Page<Track> findAll(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public Track findById(long id) {
        return repository.findById(id).orElseThrow(() -> new TrackNotFoundException(id));
    }

    @Transactional
    public Track create(Track track) {
        // o id e gerado pelo banco; se vier no JSON e ignorado (senao o save sobrescreveria a faixa com esse id)
        track.setId(0);
        track.setAlbum(resolveAlbum(track.getAlbum()));
        track.setFeaturedArtists(resolveArtists(track.getFeaturedArtists()));
        return repository.save(track);
    }

    @Transactional
    public Track update(long id, Track data) {
        Track track = findById(id);
        track.setTitle(data.getTitle());
        track.setTrackNumber(data.getTrackNumber());
        track.setDurationSeconds(data.getDurationSeconds());
        track.setAlbum(resolveAlbum(data.getAlbum()));
        track.setFeaturedArtists(resolveArtists(data.getFeaturedArtists()));
        return repository.saveAndFlush(track);
    }

    @Transactional
    public void delete(long id) {
        // a faixa e a dona do Many-to-Many das participacoes, entao os vinculos saem junto
        repository.delete(findById(id));
    }

    public Page<Track> searchByTitle(String title, Pageable pageable) {
        return repository.findByTitleContainingIgnoreCase(title, pageable);
    }

    public Page<Track> findByAlbum(long albumId, Pageable pageable) {
        if (!albumRepository.existsById(albumId))
            throw new AlbumNotFoundException(albumId);
        return repository.findByAlbumId(albumId, pageable);
    }

    public Page<Track> findFeaturingArtist(long artistId, Pageable pageable) {
        if (!artistRepository.existsById(artistId))
            throw new ArtistNotFoundException(artistId);
        return repository.findByFeaturedArtistsId(artistId, pageable);
    }

    private Album resolveAlbum(Album album) {
        return albumRepository.findById(album.getId())
                .orElseThrow(() -> new AlbumNotFoundException(album.getId()));
    }

    private Set<Artist> resolveArtists(Set<Artist> artists) {
        Set<Artist> resolved = new HashSet<>();
        if (artists == null)
            return resolved;

        for (Artist artist : artists)
            resolved.add(artistRepository.findById(artist.getId())
                    .orElseThrow(() -> new ArtistNotFoundException(artist.getId())));
        return resolved;
    }
}
