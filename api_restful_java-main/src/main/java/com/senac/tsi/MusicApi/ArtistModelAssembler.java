package com.senac.tsi.MusicApi;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

@Component
class ArtistModelAssembler implements RepresentationModelAssembler<Artist, EntityModel<Artist>> {

    @Override
    public EntityModel<Artist> toModel(Artist artist) {
        return EntityModel.of(artist,
                linkTo(methodOn(ArtistController.class).getArtistById(artist.getId())).withSelfRel(),
                linkTo(methodOn(ArtistController.class).updateArtist(artist.getId(), null)).withRel("update"),
                linkTo(methodOn(ArtistController.class).deleteArtist(artist.getId())).withRel("delete"),
                linkTo(methodOn(AlbumController.class).getAlbumsByArtist(artist.getId(), Pageable.unpaged())).withRel("albums"),
                linkTo(methodOn(TrackController.class).getTracksFeaturingArtist(artist.getId(), Pageable.unpaged())).withRel("featuredOn"),
                linkTo(methodOn(ArtistController.class).getAllArtists(Pageable.unpaged())).withRel("artists")
        );
    }
}
