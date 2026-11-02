package com.senac.tsi.MusicApi.service;

import com.senac.tsi.MusicApi.exception.ArtistNotFoundException;
import com.senac.tsi.MusicApi.exception.GenreNotFoundException;
import com.senac.tsi.MusicApi.exception.ResourceInUseException;
import com.senac.tsi.MusicApi.model.Artist;
import com.senac.tsi.MusicApi.model.ArtistType;
import com.senac.tsi.MusicApi.repository.ArtistRepository;
import com.senac.tsi.MusicApi.repository.GenreRepository;
import org.springframework.data.core.PropertyPath;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// regras de negocio dos artistas; o controller so cuida do HTTP e chama daqui
@Service
@Transactional(readOnly = true)
public class ArtistService {

    private final ArtistRepository repository;
    private final GenreRepository genreRepository;

    public ArtistService(ArtistRepository repository, GenreRepository genreRepository) {
        this.repository = repository;
        this.genreRepository = genreRepository;
    }

    public Page<Artist> findAll(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public Artist findById(long id) {
        return repository.findById(id).orElseThrow(() -> new ArtistNotFoundException(id));
    }

    @Transactional
    public Artist create(Artist artist) {
        // o id e gerado pelo banco; se vier no JSON e ignorado (senao o save sobrescreveria o artista com esse id)
        artist.setId(0);
        return repository.save(artist);
    }

    @Transactional
    public Artist update(long id, Artist data) {
        Artist artist = findById(id);
        artist.setName(data.getName());
        artist.setCountry(data.getCountry());
        artist.setStartYear(data.getStartYear());
        artist.setType(data.getType());
        return repository.saveAndFlush(artist);
    }

    @Transactional
    public void delete(long id) {
        Artist artist = findById(id);
        if (!artist.getAlbums().isEmpty())
            throw new ResourceInUseException("Artist with ID: " + id + " still has albums; remove the artist from them or delete them first");

        // as participacoes especiais sao desfeitas, as faixas continuam
        artist.getFeaturedOn().forEach(track -> track.getFeaturedArtists().remove(artist));
        repository.delete(artist);
    }

    public Page<Artist> searchByName(String name, Pageable pageable) {
        return repository.findByNameContainingIgnoreCase(name, pageable);
    }

    public Page<Artist> findByType(ArtistType type, Pageable pageable) {
        return repository.findByType(type, pageable);
    }

    public Page<Artist> findByGenre(long genreId, Pageable pageable) {
        if (!genreRepository.existsById(genreId))
            throw new GenreNotFoundException(genreId);

        // essa consulta usa @Query, entao o Spring Data nao confere o sort sozinho; sem isso campo inexistente virava 500
        pageable.getSort().forEach(order -> PropertyPath.from(order.getProperty(), Artist.class));
        return repository.findByGenreId(genreId, pageable);
    }
}
