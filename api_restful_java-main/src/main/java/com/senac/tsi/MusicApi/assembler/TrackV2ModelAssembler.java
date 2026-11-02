package com.senac.tsi.MusicApi.assembler;

import com.senac.tsi.MusicApi.controller.AlbumController;
import com.senac.tsi.MusicApi.controller.TrackController;
import com.senac.tsi.MusicApi.dto.TrackV2;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

// links HATEOAS da faixa na versao 2; as URLs sao as mesmas da v1, o que muda e o header X-API-Version
@Component
public class TrackV2ModelAssembler implements RepresentationModelAssembler<TrackV2, EntityModel<TrackV2>> {

    @Override
    public EntityModel<TrackV2> toModel(TrackV2 track) {
        return EntityModel.of(track,
                linkTo(methodOn(TrackController.class).getTrackByIdV2(track.id())).withSelfRel(),
                linkTo(methodOn(TrackController.class).updateTrack(track.id(), null)).withRel("update"),
                linkTo(methodOn(TrackController.class).deleteTrack(track.id())).withRel("delete"),
                linkTo(methodOn(AlbumController.class).getAlbumById(track.album().id())).withRel("album"),
                linkTo(methodOn(TrackController.class).getAllTracksV2(Pageable.unpaged())).withRel("tracks")
        );
    }
}
