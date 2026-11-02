package com.senac.tsi.MusicApi.service;

import com.senac.tsi.MusicApi.exception.AlbumNotFoundException;
import com.senac.tsi.MusicApi.exception.CoverNotFoundException;
import com.senac.tsi.MusicApi.model.Album;
import com.senac.tsi.MusicApi.model.Cover;
import com.senac.tsi.MusicApi.repository.AlbumRepository;
import com.senac.tsi.MusicApi.repository.CoverRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// regras de negocio das capas: cada album tem no maximo uma (One-to-One)
@Service
@Transactional(readOnly = true)
public class CoverService {

    private final CoverRepository repository;
    private final AlbumRepository albumRepository;

    public CoverService(CoverRepository repository, AlbumRepository albumRepository) {
        this.repository = repository;
        this.albumRepository = albumRepository;
    }

    public Page<Cover> findAll(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public Cover findById(long id) {
        return repository.findById(id).orElseThrow(() -> new CoverNotFoundException(id));
    }

    @Transactional
    public Cover create(Cover cover) {
        // o id e gerado pelo banco; se vier no JSON e ignorado (senao o save sobrescreveria a capa com esse id)
        cover.setId(0);
        cover.setAlbum(resolveAlbum(cover.getAlbum()));
        return repository.save(cover);
    }

    @Transactional
    public Cover update(long id, Cover data) {
        Cover cover = findById(id);
        cover.setDesigner(data.getDesigner());
        cover.setDescription(data.getDescription());
        cover.setImageUrl(data.getImageUrl());
        cover.setAlbum(resolveAlbum(data.getAlbum()));
        return repository.saveAndFlush(cover);
    }

    @Transactional
    public void delete(long id) {
        Cover cover = findById(id);
        // o album continua cadastrado, so fica sem capa
        cover.getAlbum().setCover(null);
        repository.delete(cover);
    }

    public Page<Cover> searchByDesigner(String designer, Pageable pageable) {
        return repository.findByDesignerContainingIgnoreCase(designer, pageable);
    }

    public Cover findByAlbum(long albumId) {
        if (!albumRepository.existsById(albumId))
            throw new AlbumNotFoundException(albumId);
        return repository.findByAlbumId(albumId)
                .orElseThrow(() -> new CoverNotFoundException("Album with ID: " + albumId + " has no cover"));
    }

    private Album resolveAlbum(Album album) {
        return albumRepository.findById(album.getId())
                .orElseThrow(() -> new AlbumNotFoundException(album.getId()));
    }
}
