package com.senac.tsi.MusicApi.service;

import com.senac.tsi.MusicApi.exception.GenreNotFoundException;
import com.senac.tsi.MusicApi.model.Genre;
import com.senac.tsi.MusicApi.repository.GenreRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// regras de negocio dos generos musicais
@Service
@Transactional(readOnly = true)
public class GenreService {

    private final GenreRepository repository;

    public GenreService(GenreRepository repository) {
        this.repository = repository;
    }

    public Page<Genre> findAll(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public Genre findById(long id) {
        return repository.findById(id).orElseThrow(() -> new GenreNotFoundException(id));
    }

    @Transactional
    public Genre create(Genre genre) {
        // o id e gerado pelo banco; se vier no JSON e ignorado (senao o save sobrescreveria o genero com esse id)
        genre.setId(0);
        return repository.save(genre);
    }

    @Transactional
    public Genre update(long id, Genre data) {
        Genre genre = findById(id);
        genre.setName(data.getName());
        genre.setDescription(data.getDescription());
        return repository.saveAndFlush(genre);
    }

    @Transactional
    public void delete(long id) {
        Genre genre = findById(id);
        // o album e o dono do vinculo album_genre, entao o genero sai de cada album antes de ser excluido
        genre.getAlbums().forEach(album -> album.getGenres().remove(genre));
        repository.delete(genre);
    }

    public Page<Genre> searchByName(String name, Pageable pageable) {
        return repository.findByNameContainingIgnoreCase(name, pageable);
    }
}
