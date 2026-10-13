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
        // Se a rota do metodo mudar, o link muda junto (nao ha URL escrita a mao).
        // withSelfRel() cria o link "self"; withRel("nome") cria um link com esse nome.
        EntityModel<Album> model = EntityModel.of(album,
                linkTo(methodOn(AlbumController.class).getAlbumById(album.getId())).withSelfRel(),          // o proprio album
                linkTo(methodOn(AlbumController.class).updateAlbum(album.getId(), null)).withRel("update"), // PUT /albums/{id}
                linkTo(methodOn(AlbumController.class).deleteAlbum(album.getId())).withRel("delete"),       // DELETE /albums/{id}
                linkTo(methodOn(TrackController.class).getTracksByAlbum(album.getId(), Pageable.unpaged())).withRel("tracks"), // faixas (1:N)
                linkTo(methodOn(CoverController.class).getCoverByAlbum(album.getId())).withRel("cover"),    // capa (1:1)
                linkTo(methodOn(AlbumController.class).getAllAlbums(Pageable.unpaged())).withRel("albums")  // lista de albuns
        );
        // Link condicional: albuns independentes nao tem gravadora, entao so adiciona se existir
        if (album.getLabel() != null)
            model.add(linkTo(methodOn(RecordLabelController.class).getLabelById(album.getLabel().getId())).withRel("label"));
        return model;
    }
}

//basicamente o assembler é responsavel por add os links heteoas no json de resposta, e o controller é responsavel por chamar o assembler e devolver a resposta.