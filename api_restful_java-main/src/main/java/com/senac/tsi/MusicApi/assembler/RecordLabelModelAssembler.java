package com.senac.tsi.MusicApi.assembler;

import com.senac.tsi.MusicApi.controller.AlbumController;
import com.senac.tsi.MusicApi.controller.RecordLabelController;
import com.senac.tsi.MusicApi.model.RecordLabel;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

@Component
public class RecordLabelModelAssembler implements RepresentationModelAssembler<RecordLabel, EntityModel<RecordLabel>> {

    @Override
    public EntityModel<RecordLabel> toModel(RecordLabel label) {
        return EntityModel.of(label,
                linkTo(methodOn(RecordLabelController.class).getLabelById(label.getId())).withSelfRel(),
                linkTo(methodOn(RecordLabelController.class).updateLabel(label.getId(), null)).withRel("update"),
                linkTo(methodOn(RecordLabelController.class).deleteLabel(label.getId())).withRel("delete"),
                linkTo(methodOn(AlbumController.class).getAlbumsByLabel(label.getId(), Pageable.unpaged())).withRel("albums"),
                linkTo(methodOn(RecordLabelController.class).getAllLabels(Pageable.unpaged())).withRel("labels")
        );
    }
}
