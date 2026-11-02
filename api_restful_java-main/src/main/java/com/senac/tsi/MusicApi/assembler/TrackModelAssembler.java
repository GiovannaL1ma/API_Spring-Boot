package com.senac.tsi.MusicApi.assembler;

import com.senac.tsi.MusicApi.controller.AlbumController;
import com.senac.tsi.MusicApi.controller.TrackController;
import com.senac.tsi.MusicApi.model.Track;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

@Component
public class TrackModelAssembler implements RepresentationModelAssembler<Track, EntityModel<Track>> {

    @Override
    public EntityModel<Track> toModel(Track track) {
        return EntityModel.of(track,
                linkTo(methodOn(TrackController.class).getTrackById(track.getId())).withSelfRel(),
                linkTo(methodOn(TrackController.class).updateTrack(track.getId(), null)).withRel("update"),
                linkTo(methodOn(TrackController.class).deleteTrack(track.getId())).withRel("delete"),
                linkTo(methodOn(AlbumController.class).getAlbumById(track.getAlbum().getId())).withRel("album"),
                linkTo(methodOn(TrackController.class).getAllTracks(Pageable.unpaged())).withRel("tracks")
        );
    }
}
