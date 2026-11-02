package com.senac.tsi.MusicApi.controller;

import com.senac.tsi.MusicApi.assembler.TrackModelAssembler;
import com.senac.tsi.MusicApi.assembler.TrackV2ModelAssembler;
import com.senac.tsi.MusicApi.dto.TrackV2;
import com.senac.tsi.MusicApi.model.Track;
import com.senac.tsi.MusicApi.service.TrackService;
import com.senac.tsi.MusicApi.web.RequiresApiKey;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// so cuida do HTTP (rota, status, links); as regras ficam no TrackService
// as consultas de faixa existem em duas versoes, escolhidas pelo header X-API-Version:
//   version = "1" -> formato original (durationSeconds em segundos, album completo)
//   version = "2" -> formato novo (duration "m:ss", album e convidados resumidos), veja dto/TrackV2
// endpoints sem version (POST, PUT, DELETE, busca) respondem as duas versoes do mesmo jeito
@RestController
@Tag(name = "Tracks", description = "Gerenciamento das faixas (Many-to-One com Album e Many-to-Many com Artist nas participacoes especiais). " +
        "As consultas por id, a listagem e a tracklist tem versao 2 (header X-API-Version: 2)")
public class TrackController {

    private static final String TRACK_EXAMPLE = """
            { "title": "Get Lucky", "trackNumber": 8, "durationSeconds": 369, "album": { "id": 2 },
              "featuredArtists": [ { "id": 3 }, { "id": 4 } ] }""";

    private final TrackService service;
    private final TrackModelAssembler assembler;
    private final TrackV2ModelAssembler v2Assembler;
    private final PagedResourcesAssembler<Track> pagedResourcesAssembler;
    private final PagedResourcesAssembler<TrackV2> pagedV2Assembler;

    public TrackController(TrackService service,
                           TrackModelAssembler assembler,
                           TrackV2ModelAssembler v2Assembler,
                           PagedResourcesAssembler<Track> pagedResourcesAssembler,
                           PagedResourcesAssembler<TrackV2> pagedV2Assembler) {
        this.service = service;
        this.assembler = assembler;
        this.v2Assembler = v2Assembler;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
        this.pagedV2Assembler = pagedV2Assembler;
    }

    @Operation(summary = "Get all tracks (v1)", description = "Lista paginada de todas as faixas, com a duracao em segundos (durationSeconds)")
    @ApiResponse(responseCode = "200", description = "Returned a paginated list of tracks")
    @ApiResponse(responseCode = "400", description = "Invalid sort - o sort usa um campo que a faixa nao tem (ex.: sort=abc). Campos validos: id, title, trackNumber, durationSeconds",
            content = @Content(examples = @ExampleObject(value = "Invalid sort: Track has no field 'abc'")))
    @GetMapping(path = "/tracks", version = "1")
    public ResponseEntity<PagedModel<EntityModel<Track>>> getAllTracks(
            @ParameterObject @PageableDefault(size = 10, page = 0, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(service.findAll(pageable), assembler));
    }

    @Operation(summary = "Get all tracks (v2)", description = "Versao 2 da listagem: cada faixa vem com a duracao formatada (duration \"m:ss\"), " +
            "o album resumido (id e titulo) e os nomes dos convidados")
    @ApiResponse(responseCode = "200", description = "Returned a paginated list of tracks in the version 2 format")
    @ApiResponse(responseCode = "400", description = "Invalid sort - o sort usa um campo que a faixa nao tem (ex.: sort=abc). Campos validos: id, title, trackNumber, durationSeconds",
            content = @Content(examples = @ExampleObject(value = "Invalid sort: Track has no field 'abc'")))
    @GetMapping(path = "/tracks", version = "2")
    public ResponseEntity<PagedModel<EntityModel<TrackV2>>> getAllTracksV2(
            @ParameterObject @PageableDefault(size = 10, page = 0, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(pagedV2Assembler.toModel(service.findAll(pageable).map(TrackV2::from), v2Assembler));
    }

    @Operation(summary = "Get a track by its id (v1)", description = "Retorna uma faixa com seu album e seus artistas convidados; a duracao vem em segundos")
    @ApiResponse(responseCode = "200", description = "Returns a valid track",
            content = @Content(mediaType = "application/hal+json", schema = @Schema(implementation = Track.class)))
    @ApiResponse(responseCode = "400", description = "Invalid id - o id precisa ser um numero inteiro (ex.: 1); textos como abc ou decimais como 1.5 sao recusados",
            content = @Content(examples = @ExampleObject(value = "Invalid value 'abc' for parameter 'id': it must be a whole number")))
    @ApiResponse(responseCode = "404", description = "Track not found - nao existe faixa com esse id (nunca foi criada ou ja foi excluida)",
            content = @Content(examples = @ExampleObject(value = "Could not find track with ID: 99")))
    @GetMapping(path = "/tracks/{id}", version = "1")
    public EntityModel<Track> getTrackById(@Parameter(description = "Id of the track", example = "1") @PathVariable Long id) {
        return assembler.toModel(service.findById(id));
    }

    @Operation(summary = "Get a track by its id (v2)", description = "Versao 2: duracao formatada (duration \"m:ss\"), " +
            "album resumido (id e titulo) e nomes dos convidados, em vez do album inteiro e da duracao em segundos")
    @ApiResponse(responseCode = "200", description = "Returns a valid track in the version 2 format",
            content = @Content(mediaType = "application/hal+json", schema = @Schema(implementation = TrackV2.class)))
    @ApiResponse(responseCode = "400", description = "Invalid id - o id precisa ser um numero inteiro (ex.: 1); textos como abc ou decimais como 1.5 sao recusados",
            content = @Content(examples = @ExampleObject(value = "Invalid value 'abc' for parameter 'id': it must be a whole number")))
    @ApiResponse(responseCode = "404", description = "Track not found - nao existe faixa com esse id (nunca foi criada ou ja foi excluida)",
            content = @Content(examples = @ExampleObject(value = "Could not find track with ID: 99")))
    @GetMapping(path = "/tracks/{id}", version = "2")
    public EntityModel<TrackV2> getTrackByIdV2(@Parameter(description = "Id of the track", example = "1") @PathVariable Long id) {
        return v2Assembler.toModel(TrackV2.from(service.findById(id)));
    }

    @Operation(summary = "Creates a new track",
            description = "O album e os artistas convidados (feat., Many-to-Many) sao vinculados pelo id. O numero da faixa e unico dentro do album")
    @ApiResponse(responseCode = "201", description = "Track created; Location header points to the new resource")
    @ApiResponse(responseCode = "400", description = "Bad request on the payload - algum campo falhou na validacao (title vazio ou com mais de 150 caracteres, " +
            "trackNumber fora de 1 a 99, durationSeconds fora de 1 a 7200, album ausente), " +
            "um campo veio com o tipo errado (ex.: numero num campo de texto, decimal ou texto num campo inteiro) ou o JSON esta mal formado. " +
            "A resposta diz qual campo falhou e por que",
            content = @Content(examples = {
                    @ExampleObject(name = "Campo invalido", value = "{ \"trackNumber\": \"deve ser menor que ou igual à 99\", \"album\": \"não deve ser nulo\" }"),
                    @ExampleObject(name = "Tipo errado", value = "Invalid value for field 'durationSeconds': expected Integer"),
                    @ExampleObject(name = "JSON mal formado", value = "Malformed JSON: check quotes, commas and braces")}))
    @ApiResponse(responseCode = "404", description = "Album or artist not found - o id informado em album ou algum id em featuredArtists nao existe",
            content = @Content(examples = {
                    @ExampleObject(name = "Album", value = "Could not find album with ID: 99"),
                    @ExampleObject(name = "Artista", value = "Could not find artist with ID: 99")}))
    @ApiResponse(responseCode = "409", description = "Track number already used on this album - ja existe outra faixa com esse trackNumber no mesmo album",
            content = @Content(examples = @ExampleObject(value = "Operation conflicts with existing data: a value that must be unique is already in use")))
    @RequiresApiKey
    @PostMapping("/tracks")
    public ResponseEntity<EntityModel<Track>> newTrack(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "New track",
                    required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Track.class),
                            examples = @ExampleObject(value = TRACK_EXAMPLE)))
            @RequestBody @Valid Track newTrack) {

        EntityModel<Track> entityModel = assembler.toModel(service.create(newTrack));
        return ResponseEntity.created(entityModel
                .getRequiredLink(IanaLinkRelations.SELF)
                .toUri()).body(entityModel);
    }

    @Operation(summary = "Updates a track", description = "Substitui todos os dados da faixa, inclusive a lista de participacoes especiais")
    @ApiResponse(responseCode = "200", description = "Track updated",
            content = @Content(mediaType = "application/hal+json", schema = @Schema(implementation = Track.class)))
    @ApiResponse(responseCode = "400", description = "Invalid id or payload - o id nao e um numero inteiro, ou algum campo do JSON e invalido (mesmas regras do POST)",
            content = @Content(examples = {
                    @ExampleObject(name = "Id invalido", value = "Invalid value 'abc' for parameter 'id': it must be a whole number"),
                    @ExampleObject(name = "Campo invalido", value = "{ \"durationSeconds\": \"deve ser maior que ou igual à 1\" }")}))
    @ApiResponse(responseCode = "404", description = "Track, album or artist not found - a faixa com esse id nao existe, " +
            "ou o id informado em album ou em featuredArtists nao existe",
            content = @Content(examples = {
                    @ExampleObject(name = "Faixa", value = "Could not find track with ID: 99"),
                    @ExampleObject(name = "Album", value = "Could not find album with ID: 99")}))
    @ApiResponse(responseCode = "409", description = "Track number already used on this album - ja existe outra faixa com esse trackNumber no mesmo album",
            content = @Content(examples = @ExampleObject(value = "Operation conflicts with existing data: a value that must be unique is already in use")))
    @RequiresApiKey
    @PutMapping("/tracks/{id}")
    public ResponseEntity<EntityModel<Track>> updateTrack(
            @Parameter(description = "Id of the track", example = "1") @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Updated track",
                    required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Track.class),
                            examples = @ExampleObject(value = TRACK_EXAMPLE)))
            @RequestBody @Valid Track newTrack) {
        return ResponseEntity.ok(assembler.toModel(service.update(id, newTrack)));
    }

    @Operation(summary = "Deletes a track", description = "Exclui a faixa e suas participacoes especiais; o album e os artistas continuam")
    @ApiResponse(responseCode = "204", description = "Successfully deleted a track", content = @Content)
    @ApiResponse(responseCode = "400", description = "Invalid id - o id precisa ser um numero inteiro (ex.: 1)",
            content = @Content(examples = @ExampleObject(value = "Invalid value 'abc' for parameter 'id': it must be a whole number")))
    @ApiResponse(responseCode = "404", description = "Track not found, maybe it's already deleted - nao existe faixa com esse id",
            content = @Content(examples = @ExampleObject(value = "Could not find track with ID: 99")))
    @RequiresApiKey
    @DeleteMapping("/tracks/{id}")
    public ResponseEntity<Void> deleteTrack(@Parameter(description = "Id of the track", example = "1") @PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Search tracks by title", description = "Consulta personalizada: faixas cujo titulo contem o texto (sem diferenciar maiusculas)")
    @ApiResponse(responseCode = "200", description = "Returned a paginated list of tracks")
    @ApiResponse(responseCode = "400", description = "Missing title or invalid sort - o parametro title e obrigatorio, ou o sort usa um campo que a faixa nao tem (ex.: sort=abc)",
            content = @Content(examples = {
                    @ExampleObject(name = "Sem title", value = "Required parameter 'title' is missing"),
                    @ExampleObject(name = "Sort invalido", value = "Invalid sort: Track has no field 'abc'")}))
    @GetMapping("/tracks/search")
    public ResponseEntity<PagedModel<EntityModel<Track>>> getTracksByTitle(
            @Parameter(description = "Part of the track's title", example = "lucky") @RequestParam String title,
            @ParameterObject @PageableDefault(size = 10, page = 0, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(service.searchByTitle(title, pageable), assembler));
    }

    @Operation(summary = "Get the tracklist of an album (v1)", description = "Consulta personalizada: faixas de um album na ordem do disco, com a duracao em segundos")
    @ApiResponse(responseCode = "200", description = "Returned a paginated list of tracks of the album")
    @ApiResponse(responseCode = "400", description = "Invalid albumId or sort - o albumId precisa ser um numero inteiro, ou o sort usa um campo que a faixa nao tem",
            content = @Content(examples = {
                    @ExampleObject(name = "Id invalido", value = "Invalid value 'abc' for parameter 'albumId': it must be a whole number"),
                    @ExampleObject(name = "Sort invalido", value = "Invalid sort: Track has no field 'abc'")}))
    @ApiResponse(responseCode = "404", description = "Album not found - nao existe album com esse id",
            content = @Content(examples = @ExampleObject(value = "Could not find album with ID: 99")))
    @GetMapping(path = "/tracks/album/{albumId}", version = "1")
    public ResponseEntity<PagedModel<EntityModel<Track>>> getTracksByAlbum(
            @Parameter(description = "Id of the album", example = "1") @PathVariable Long albumId,
            @ParameterObject @PageableDefault(size = 20, page = 0, sort = "trackNumber") Pageable pageable) {
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(service.findByAlbum(albumId, pageable), assembler));
    }

    @Operation(summary = "Get the tracklist of an album (v2)", description = "Versao 2 da tracklist: faixas na ordem do disco, " +
            "com a duracao formatada (duration \"m:ss\") e os nomes dos convidados")
    @ApiResponse(responseCode = "200", description = "Returned a paginated list of tracks of the album in the version 2 format")
    @ApiResponse(responseCode = "400", description = "Invalid albumId or sort - o albumId precisa ser um numero inteiro, ou o sort usa um campo que a faixa nao tem",
            content = @Content(examples = {
                    @ExampleObject(name = "Id invalido", value = "Invalid value 'abc' for parameter 'albumId': it must be a whole number"),
                    @ExampleObject(name = "Sort invalido", value = "Invalid sort: Track has no field 'abc'")}))
    @ApiResponse(responseCode = "404", description = "Album not found - nao existe album com esse id",
            content = @Content(examples = @ExampleObject(value = "Could not find album with ID: 99")))
    @GetMapping(path = "/tracks/album/{albumId}", version = "2")
    public ResponseEntity<PagedModel<EntityModel<TrackV2>>> getTracksByAlbumV2(
            @Parameter(description = "Id of the album", example = "1") @PathVariable Long albumId,
            @ParameterObject @PageableDefault(size = 20, page = 0, sort = "trackNumber") Pageable pageable) {
        return ResponseEntity.ok(pagedV2Assembler.toModel(service.findByAlbum(albumId, pageable).map(TrackV2::from), v2Assembler));
    }

    @Operation(summary = "Get tracks featuring an artist", description = "Consulta personalizada: participacoes especiais de um artista (Many-to-Many track_featured_artist)")
    @ApiResponse(responseCode = "200", description = "Returned a paginated list of tracks featuring the artist")
    @ApiResponse(responseCode = "400", description = "Invalid artistId or sort - o artistId precisa ser um numero inteiro, ou o sort usa um campo que a faixa nao tem",
            content = @Content(examples = {
                    @ExampleObject(name = "Id invalido", value = "Invalid value 'abc' for parameter 'artistId': it must be a whole number"),
                    @ExampleObject(name = "Sort invalido", value = "Invalid sort: Track has no field 'abc'")}))
    @ApiResponse(responseCode = "404", description = "Artist not found - nao existe artista com esse id",
            content = @Content(examples = @ExampleObject(value = "Could not find artist with ID: 99")))
    @GetMapping("/tracks/featuring/{artistId}")
    public ResponseEntity<PagedModel<EntityModel<Track>>> getTracksFeaturingArtist(
            @Parameter(description = "Id of the artist", example = "1") @PathVariable Long artistId,
            @ParameterObject @PageableDefault(size = 10, page = 0, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(service.findFeaturingArtist(artistId, pageable), assembler));
    }
}
