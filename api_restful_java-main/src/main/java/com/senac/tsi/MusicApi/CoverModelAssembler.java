package com.senac.tsi.MusicApi;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

@Component
class CoverModelAssembler implements RepresentationModelAssembler<Cover, EntityModel<Cover>> {

    @Override
    public EntityModel<Cover> toModel(Cover cover) {
        return EntityModel.of(cover,
                linkTo(methodOn(CoverController.class).getCoverById(cover.getId())).withSelfRel(),
                linkTo(methodOn(CoverController.class).updateCover(cover.getId(), null)).withRel("update"),
                linkTo(methodOn(CoverController.class).deleteCover(cover.getId())).withRel("delete"),
                linkTo(methodOn(AlbumController.class).getAlbumById(cover.getAlbum().getId())).withRel("album"),
                linkTo(methodOn(CoverController.class).getAllCovers(Pageable.unpaged())).withRel("covers")
        );
    }
}
