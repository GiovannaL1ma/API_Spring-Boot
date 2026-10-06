package com.senac.tsi.MusicApi;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

@Component
class GenreModelAssembler implements RepresentationModelAssembler<Genre, EntityModel<Genre>> {

    @Override
    public EntityModel<Genre> toModel(Genre genre) {
        return EntityModel.of(genre,
                linkTo(methodOn(GenreController.class).getGenreById(genre.getId())).withSelfRel(),
                linkTo(methodOn(GenreController.class).updateGenre(genre.getId(), null)).withRel("update"),
                linkTo(methodOn(GenreController.class).deleteGenre(genre.getId())).withRel("delete"),
                linkTo(methodOn(AlbumController.class).getAlbumsByGenre(genre.getId(), Pageable.unpaged())).withRel("albums"),
                linkTo(methodOn(ArtistController.class).getArtistsByGenre(genre.getId(), Pageable.unpaged())).withRel("artists"),
                linkTo(methodOn(GenreController.class).getAllGenres(Pageable.unpaged())).withRel("genres")
        );
    }
}
