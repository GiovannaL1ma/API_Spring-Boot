package com.senac.tsi.MusicApi;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

// o repository é a parte que conversa com o banco. é so uma interface, o spring cria a implementacao sozinho
@Repository
public interface AlbumRepository extends JpaRepository<Album, Long> { //extends JpaRepository ja da o findAll, findById, save, delete... de graca

    // o spring le o nome do metodo e monta o sql sozinho, todos recebem pageable e devolvem page (paginado)
    Page<Album> findByTitleContainingIgnoreCase(String title, Pageable pageable);
    Page<Album> findByType(AlbumType type, Pageable pageable); //filtra pelo enum AlbumType
    Page<Album> findByReleaseYearBetween(int from, int to, Pageable pageable); //albuns lancados entre dois anos
    Page<Album> findByLabelId(long labelId, Pageable pageable); //albuns de uma gravadora (one-to-many)
    // many-to-many: o spring faz o join com a tabela album_artist e album_genre sozinho
    Page<Album> findByArtistsId(long artistId, Pageable pageable);
    Page<Album> findByGenresId(long genreId, Pageable pageable);
}
