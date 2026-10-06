package com.senac.tsi.MusicApi;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

@Component
class AlbumModelAssembler implements RepresentationModelAssembler<Album, EntityModel<Album>> {

    @Override
    public EntityModel<Album> toModel(Album album) {
        EntityModel<Album> model = EntityModel.of(album,
                linkTo(methodOn(AlbumController.class).getAlbumById(album.getId())).withSelfRel(),
                linkTo(methodOn(AlbumController.class).updateAlbum(album.getId(), null)).withRel("update"),
                linkTo(methodOn(AlbumController.class).deleteAlbum(album.getId())).withRel("delete"),
                linkTo(methodOn(TrackController.class).getTracksByAlbum(album.getId(), Pageable.unpaged())).withRel("tracks"),
                linkTo(methodOn(CoverController.class).getCoverByAlbum(album.getId())).withRel("cover"),
                linkTo(methodOn(AlbumController.class).getAllAlbums(Pageable.unpaged())).withRel("albums")
        );
        // Albuns independentes nao tem gravadora
        if (album.getLabel() != null)
            model.add(linkTo(methodOn(RecordLabelController.class).getLabelById(album.getLabel().getId())).withRel("label"));
        return model;
    }
}
