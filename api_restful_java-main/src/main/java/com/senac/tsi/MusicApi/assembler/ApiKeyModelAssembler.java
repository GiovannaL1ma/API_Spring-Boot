package com.senac.tsi.MusicApi.assembler;

import com.senac.tsi.MusicApi.controller.ApiKeyController;
import com.senac.tsi.MusicApi.dto.ApiKeyResponse;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

@Component
public class ApiKeyModelAssembler implements RepresentationModelAssembler<ApiKeyResponse, EntityModel<ApiKeyResponse>> {

    @Override
    public EntityModel<ApiKeyResponse> toModel(ApiKeyResponse key) {
        return EntityModel.of(key,
                linkTo(methodOn(ApiKeyController.class).getKeyById(key.id())).withSelfRel(),
                linkTo(methodOn(ApiKeyController.class).getMyKey(null)).withRel("me"),
                linkTo(methodOn(ApiKeyController.class).getAllKeys(Pageable.unpaged())).withRel("api-keys")
        );
    }
}
