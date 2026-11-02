package com.senac.tsi.MusicApi.service;

import com.senac.tsi.MusicApi.exception.AlbumNotFoundException;
import com.senac.tsi.MusicApi.exception.ArtistNotFoundException;
import com.senac.tsi.MusicApi.exception.GenreNotFoundException;
import com.senac.tsi.MusicApi.exception.InvalidSearchException;
import com.senac.tsi.MusicApi.exception.RecordLabelNotFoundException;
import com.senac.tsi.MusicApi.model.Album;
import com.senac.tsi.MusicApi.model.AlbumType;
import com.senac.tsi.MusicApi.model.Artist;
import com.senac.tsi.MusicApi.model.Genre;
import com.senac.tsi.MusicApi.model.RecordLabel;
import com.senac.tsi.MusicApi.repository.AlbumRepository;
import com.senac.tsi.MusicApi.repository.ArtistRepository;
import com.senac.tsi.MusicApi.repository.GenreRepository;
import com.senac.tsi.MusicApi.repository.RecordLabelRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

// regras de negocio dos albuns: resolve artistas, generos e gravadora pelo id antes de salvar
@Service
@Transactional(readOnly = true)
public class AlbumService {

    private final AlbumRepository repository;
    private final ArtistRepository artistRepository;
    private final GenreRepository genreRepository;
    private final RecordLabelRepository labelRepository;

    public AlbumService(AlbumRepository repository, ArtistRepository artistRepository,
                        GenreRepository genreRepository, RecordLabelRepository labelRepository) {
        this.repository = repository;
        this.artistRepository = artistRepository;
        this.genreRepository = genreRepository;
        this.labelRepository = labelRepository;
    }

    public Page<Album> findAll(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public Album findById(long id) {
        return repository.findById(id).orElseThrow(() -> new AlbumNotFoundException(id));
    }

    @Transactional
    public Album create(Album album) {
        // o id do album e gerado pelo banco; se vier no JSON e ignorado (senao o save sobrescreveria o album com esse id)
        album.setId(0);
        // o JSON so traz os ids ({ "id": 1 }); aqui trocamos esses objetos "vazios" pelas entidades reais do banco
        album.setArtists(resolveArtists(album.getArtists()));
        album.setGenres(resolveGenres(album.getGenres()));
        album.setLabel(resolveLabel(album.getLabel()));
        return repository.save(album);
    }

    @Transactional
    public Album update(long id, Album data) {
        Album album = findById(id);
        album.setTitle(data.getTitle());
        album.setReleaseYear(data.getReleaseYear());
        album.setType(data.getType());
        album.setLabel(resolveLabel(data.getLabel()));
        album.setArtists(resolveArtists(data.getArtists()));
        album.setGenres(resolveGenres(data.getGenres()));
        return repository.saveAndFlush(album);
    }

    @Transactional
    public void delete(long id) {
        // album e o dono dos Many-to-Many (album_artist e album_genre); faixas e capa saem em cascata
        repository.delete(findById(id));
    }

    public Page<Album> searchByTitle(String title, Pageable pageable) {
        return repository.findByTitleContainingIgnoreCase(title, pageable);
    }

    public Page<Album> findByType(AlbumType type, Pageable pageable) {
        return repository.findByType(type, pageable);
    }

    public Page<Album> findReleasedBetween(int from, int to, Pageable pageable) {
        if (from > to)
            throw new InvalidSearchException("Invalid period: from (" + from + ") must be less than or equal to to (" + to + ")");
        return repository.findByReleaseYearBetween(from, to, pageable);
    }

    public Page<Album> findByArtist(long artistId, Pageable pageable) {
        if (!artistRepository.existsById(artistId))
            throw new ArtistNotFoundException(artistId);
        return repository.findByArtistsId(artistId, pageable);
    }

    public Page<Album> findByGenre(long genreId, Pageable pageable) {
        if (!genreRepository.existsById(genreId))
            throw new GenreNotFoundException(genreId);
        return repository.findByGenresId(genreId, pageable);
    }

    public Page<Album> findByLabel(long labelId, Pageable pageable) {
        if (!labelRepository.existsById(labelId))
            throw new RecordLabelNotFoundException(labelId);
        return repository.findByLabelId(labelId, pageable);
    }

    // recebem os objetos "vazios" com so o id e devolvem os reais do banco; id que nao existe vira 404
    private Set<Artist> resolveArtists(Set<Artist> artists) {
        Set<Artist> resolved = new HashSet<>();
        for (Artist artist : artists)
            resolved.add(artistRepository.findById(artist.getId())
                    .orElseThrow(() -> new ArtistNotFoundException(artist.getId())));
        return resolved;
    }

    private Set<Genre> resolveGenres(Set<Genre> genres) {
        Set<Genre> resolved = new HashSet<>();
        if (genres == null)
            return resolved;

        for (Genre genre : genres)
            resolved.add(genreRepository.findById(genre.getId())
                    .orElseThrow(() -> new GenreNotFoundException(genre.getId())));
        return resolved;
    }

    private RecordLabel resolveLabel(RecordLabel label) {
        if (label == null)
            return null;
        return labelRepository.findById(label.getId())
                .orElseThrow(() -> new RecordLabelNotFoundException(label.getId()));
    }
}
