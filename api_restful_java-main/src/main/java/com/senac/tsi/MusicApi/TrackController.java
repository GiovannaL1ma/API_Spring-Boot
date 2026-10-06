package com.senac.tsi.MusicApi;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.Set;

@RestController
@Tag(name = "Tracks", description = "Gerenciamento das faixas (Many-to-One com Album e Many-to-Many com Artist nas participacoes especiais)")
public class TrackController {

    private static final String TRACK_EXAMPLE = """
            { "title": "Get Lucky", "trackNumber": 8, "durationSeconds": 369, "album": { "id": 2 },
              "featuredArtists": [ { "id": 3 }, { "id": 4 } ] }""";

    private final TrackRepository repository;
    private final AlbumRepository albumRepository;
    private final ArtistRepository artistRepository;
    private final TrackModelAssembler assembler;
    private final PagedResourcesAssembler<Track> pagedResourcesAssembler;

    public TrackController(TrackRepository repository,
                           AlbumRepository albumRepository,
                           ArtistRepository artistRepository,
                           TrackModelAssembler assembler,
                           PagedResourcesAssembler<Track> pagedResourcesAssembler) {
        this.repository = repository;
        this.albumRepository = albumRepository;
        this.artistRepository = artistRepository;
        this.assembler = assembler;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
    }

    @Operation(summary = "Get all tracks", description = "Lista paginada de todas as faixas")
    @ApiResponse(responseCode = "200", description = "Returned a paginated list of tracks")
    @GetMapping("/tracks")
    public ResponseEntity<PagedModel<EntityModel<Track>>> getAllTracks(
            @ParameterObject @PageableDefault(size = 10, page = 0, sort = "id") Pageable pageable) {
        Page<Track> trackPage = repository.findAll(pageable);
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(trackPage, assembler));
    }

    @Operation(summary = "Get a track by its id", description = "Retorna uma faixa com seu album e seus artistas convidados")
    @ApiResponse(responseCode = "200", description = "Returns a valid track",
            content = @Content(mediaType = "application/hal+json", schema = @Schema(implementation = Track.class)))
    @ApiResponse(responseCode = "404", description = "Track not found", content = @Content)
    @ApiResponse(responseCode = "400", description = "Invalid id", content = @Content)
    @GetMapping("/tracks/{id}")
    public EntityModel<Track> getTrackById(@Parameter(description = "Id of the track", example = "1") @PathVariable Long id) {
        Track track = repository.findById(id)
                .orElseThrow(() -> new TrackNotFoundException(id));
        return assembler.toModel(track);
    }

    @Operation(summary = "Creates a new track",
            description = "O album e os artistas convidados (feat., Many-to-Many) sao vinculados pelo id. O numero da faixa e unico dentro do album")
    @ApiResponse(responseCode = "201", description = "Track created; Location header points to the new resource")
    @ApiResponse(responseCode = "400", description = "Bad request on the payload", content = @Content)
    @ApiResponse(responseCode = "404", description = "Referenced album or artist not found", content = @Content)
    @ApiResponse(responseCode = "409", description = "Track number already used on this album", content = @Content)
    @PostMapping("/tracks")
    public ResponseEntity<EntityModel<Track>> newTrack(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "New track",
                    required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Track.class),
                            examples = @ExampleObject(value = TRACK_EXAMPLE)))
            @RequestBody @Valid Track newTrack) {

        newTrack.setAlbum(resolveAlbum(newTrack.getAlbum()));
        newTrack.setFeaturedArtists(resolveArtists(newTrack.getFeaturedArtists()));

        EntityModel<Track> entityModel = assembler.toModel(repository.save(newTrack));

        return ResponseEntity.created(entityModel
                .getRequiredLink(IanaLinkRelations.SELF)
                .toUri()).body(entityModel);
    }

    @Operation(summary = "Updates a track", description = "Substitui todos os dados da faixa, inclusive a lista de participacoes especiais")
    @ApiResponse(responseCode = "200", description = "Track updated",
            content = @Content(mediaType = "application/hal+json", schema = @Schema(implementation = Track.class)))
    @ApiResponse(responseCode = "400", description = "Bad request on the payload", content = @Content)
    @ApiResponse(responseCode = "404", description = "Track, album or artist not found", content = @Content)
    @ApiResponse(responseCode = "409", description = "Track number already used on this album", content = @Content)
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

        Track updated = repository.findById(id)
                .map(track -> {
                    track.setTitle(newTrack.getTitle());
                    track.setTrackNumber(newTrack.getTrackNumber());
                    track.setDurationSeconds(newTrack.getDurationSeconds());
                    track.setAlbum(resolveAlbum(newTrack.getAlbum()));
                    track.setFeaturedArtists(resolveArtists(newTrack.getFeaturedArtists()));
                    return repository.save(track);
                })
                .orElseThrow(() -> new TrackNotFoundException(id));

        return ResponseEntity.ok(assembler.toModel(updated));
    }

    @Operation(summary = "Deletes a track", description = "Exclui a faixa e suas participacoes especiais; o album e os artistas continuam")
    @ApiResponse(responseCode = "204", description = "Successfully deleted a track", content = @Content)
    @ApiResponse(responseCode = "404", description = "Track not found, maybe it's already deleted", content = @Content)
    @DeleteMapping("/tracks/{id}")
    public ResponseEntity<?> deleteTrack(@Parameter(description = "Id of the track", example = "1") @PathVariable Long id) {
        var track = repository.findById(id);
        if (track.isEmpty())
            return ResponseEntity.notFound().build();

        // Track e o dono do Many-to-Many, entao as linhas de track_featured_artist saem junto
        repository.delete(track.get());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Search tracks by title", description = "Consulta personalizada: faixas cujo titulo contem o texto (sem diferenciar maiusculas)")
    @ApiResponse(responseCode = "200", description = "Returned a paginated list of tracks")
    @GetMapping("/tracks/search")
    public ResponseEntity<PagedModel<EntityModel<Track>>> getTracksByTitle(
            @Parameter(description = "Part of the track's title", example = "lucky") @RequestParam String title,
            @ParameterObject @PageableDefault(size = 10, page = 0, sort = "id") Pageable pageable) {
        Page<Track> trackPage = repository.findByTitleContainingIgnoreCase(title, pageable);
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(trackPage, assembler));
    }

    @Operation(summary = "Get the tracklist of an album", description = "Consulta personalizada: faixas de um album na ordem do disco")
    @ApiResponse(responseCode = "200", description = "Returned a paginated list of tracks of the album")
    @ApiResponse(responseCode = "404", description = "Album not found", content = @Content)
    @GetMapping("/tracks/album/{albumId}")
    public ResponseEntity<PagedModel<EntityModel<Track>>> getTracksByAlbum(
            @Parameter(description = "Id of the album", example = "1") @PathVariable Long albumId,
            @ParameterObject @PageableDefault(size = 20, page = 0, sort = "trackNumber") Pageable pageable) {
        if (!albumRepository.existsById(albumId))
            throw new AlbumNotFoundException(albumId);

        Page<Track> trackPage = repository.findByAlbumId(albumId, pageable);
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(trackPage, assembler));
    }

    @Operation(summary = "Get tracks featuring an artist", description = "Consulta personalizada: participacoes especiais de um artista (Many-to-Many track_featured_artist)")
    @ApiResponse(responseCode = "200", description = "Returned a paginated list of tracks featuring the artist")
    @ApiResponse(responseCode = "404", description = "Artist not found", content = @Content)
    @GetMapping("/tracks/featuring/{artistId}")
    public ResponseEntity<PagedModel<EntityModel<Track>>> getTracksFeaturingArtist(
            @Parameter(description = "Id of the artist", example = "1") @PathVariable Long artistId,
            @ParameterObject @PageableDefault(size = 10, page = 0, sort = "id") Pageable pageable) {
        if (!artistRepository.existsById(artistId))
            throw new ArtistNotFoundException(artistId);

        Page<Track> trackPage = repository.findByFeaturedArtistsId(artistId, pageable);
        return ResponseEntity.ok(pagedResourcesAssembler.toModel(trackPage, assembler));
    }

    // Trocam os objetos recebidos (so com id) pelas entidades do banco
    private Album resolveAlbum(Album album) {
        return albumRepository.findById(album.getId())
                .orElseThrow(() -> new AlbumNotFoundException(album.getId()));
    }

    private Set<Artist> resolveArtists(Set<Artist> artists) {
        Set<Artist> resolved = new HashSet<>();
        if (artists == null)
            return resolved;

        for (Artist artist : artists)
            resolved.add(findArtist(artist.getId()));
        return resolved;
    }

    private Artist findArtist(long id) {
        return artistRepository.findById(id).orElseThrow(() -> new ArtistNotFoundException(id));
    }
}
