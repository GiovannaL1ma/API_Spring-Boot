package com.senac.tsi.MusicApi.config;

import com.senac.tsi.MusicApi.model.Album;
import com.senac.tsi.MusicApi.model.AlbumType;
import com.senac.tsi.MusicApi.model.Artist;
import com.senac.tsi.MusicApi.model.ArtistType;
import com.senac.tsi.MusicApi.model.Cover;
import com.senac.tsi.MusicApi.model.Genre;
import com.senac.tsi.MusicApi.model.RecordLabel;
import com.senac.tsi.MusicApi.model.Track;
import com.senac.tsi.MusicApi.repository.AlbumRepository;
import com.senac.tsi.MusicApi.repository.ArtistRepository;
import com.senac.tsi.MusicApi.repository.CoverRepository;
import com.senac.tsi.MusicApi.repository.GenreRepository;
import com.senac.tsi.MusicApi.repository.RecordLabelRepository;
import com.senac.tsi.MusicApi.repository.TrackRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// o banco e em memoria, entao a API ja sobe com um catalogo de exemplo para testar sem cadastrar nada antes
// 5 artistas do rap nacional, um album de cada; faixas, duracoes, anos, tipos e gravadoras conferidos no Spotify
// para subir vazia, defina a variavel de ambiente API_SAMPLE_DATA=false
@Component
@ConditionalOnProperty(name = "api.sample-data.enabled", havingValue = "true", matchIfMissing = true)
public class SampleDataLoader implements ApplicationRunner {

    // o Spotify nao diz quem fez as capas
    private static final String UNKNOWN_DESIGNER = "Não informado";

    private final RecordLabelRepository labels;
    private final GenreRepository genres;
    private final ArtistRepository artists;
    private final AlbumRepository albums;
    private final TrackRepository tracks;
    private final CoverRepository covers;

    public SampleDataLoader(RecordLabelRepository labels, GenreRepository genres, ArtistRepository artists,
                            AlbumRepository albums, TrackRepository tracks, CoverRepository covers) {
        this.labels = labels;
        this.genres = genres;
        this.artists = artists;
        this.albums = albums;
        this.tracks = tracks;
        this.covers = covers;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        RecordLabel empire = labels.save(new RecordLabel("EMPIRE", "United States", 2010));
        RecordLabel boogieNaipe = labels.save(new RecordLabel("Boogie Naipe", "Brazil", 2013));

        Genre rap = genres.save(new Genre("Rap", "Rimas faladas sobre uma base ritmica, nascido no hip hop"));
        Genre trap = genres.save(new Genre("Trap", "Vertente do rap com graves pesados e chimbal acelerado"));

        // startYear: inicio da carreira segundo a Wikipedia e entrevistas das artistas
        Artist ajulliacosta = artists.save(new Artist("AJULLIACOSTA", "Brazil", 2022, ArtistType.SOLO));
        Artist mcLuanna = artists.save(new Artist("Mc Luanna", "Brazil", 2020, ArtistType.SOLO));
        Artist tashaTracie = artists.save(new Artist("Tasha & Tracie", "Brazil", 2019, ArtistType.DUO));
        Artist duquesa = artists.save(new Artist("Duquesa", "Brazil", 2015, ArtistType.SOLO));
        Artist nandaTsunami = artists.save(new Artist("NandaTsunami", "Brazil", 2017, ArtistType.SOLO));

        Album brutas = album("Brutas Amam, Choram e Sentem Raiva", 2023, AlbumType.STUDIO, empire, ajulliacosta, rap, trap);
        track(brutas, 1, "Tão Gostoso", "4:22");
        track(brutas, 2, "Amo Te Ver de Juju", "2:40");
        track(brutas, 3, "Brutas Também Choram", "3:03");
        track(brutas, 4, "Roteiro Mudado", "2:16");
        track(brutas, 5, "Você Não É Meu Homem", "2:31");
        track(brutas, 6, "Pq a Policia Smp Acaba Com a Festa?", "2:25");
        track(brutas, 7, "Empresário", "3:07");
        track(brutas, 8, "Queen Chavosa", "2:40");
        track(brutas, 9, "Mil e uma Treta", "1:59");
        track(brutas, 10, "Cigana", "2:19");
        cover(brutas, "https://i.scdn.co/image/ab67616d0000b273a69603c14ab1fae423abf228");

        Album quarentaEQuatro = album("44", 2022, AlbumType.STUDIO, null, mcLuanna, rap, trap);
        track(quarentaEQuatro, 1, "Íntimo", "1:39");
        track(quarentaEQuatro, 2, "Rotina", "3:16");
        track(quarentaEQuatro, 3, "Direito de Voz", "3:46");
        track(quarentaEQuatro, 4, "44", "2:46");
        track(quarentaEQuatro, 5, "Maldita", "3:07");
        track(quarentaEQuatro, 6, "Atacante", "1:47");
        track(quarentaEQuatro, 7, "Rude", "2:44");
        track(quarentaEQuatro, 8, "Surtada", "2:44");
        cover(quarentaEQuatro, "https://i.scdn.co/image/ab67616d0000b273a3b77d2c157fe775b7eaaaa1");

        Album yinYang = album("Yin Yang", 2023, AlbumType.EP, null, tashaTracie, rap, trap);
        track(yinYang, 1, "DIA DE BAILE", "1:51");
        track(yinYang, 2, "FORTAL", "2:57");
        track(yinYang, 3, "TOM SOBRE TOM", "3:42");
        track(yinYang, 4, "FECHAMENTO", "3:02");
        track(yinYang, 5, "DESCE LICOR", "2:34");
        track(yinYang, 6, "DRAKE DA CAPITAL", "4:28");
        cover(yinYang, "https://i.scdn.co/image/ab67616d0000b273b3d3bca612e9fa6ab3933d16");

        Album taurus = album("TAURUS", 2023, AlbumType.STUDIO, boogieNaipe, duquesa, rap, trap);
        track(taurus, 1, "Intro", "0:39");
        track(taurus, 2, "Classudona", "2:31");
        track(taurus, 3, "Taurus", "2:06");
        track(taurus, 4, "Única", "1:53");
        track(taurus, 5, "99 Problemas", "3:10", mcLuanna); // participacao especial (feat.) da Mc Luanna
        track(taurus, 6, "Big D!!!!!", "3:29");
        track(taurus, 7, "Glória", "3:28");
        track(taurus, 8, "Parece um Vampiro", "3:08");
        cover(taurus, "https://i.scdn.co/image/ab67616d0000b27329bcd8f7f2d572f868bcfbe6");

        Album alimento = album("É Disso Que Eu Me Alimento", 2025, AlbumType.STUDIO, null, nandaTsunami, rap, trap);
        track(alimento, 1, "Segredo e Feitiço", "2:53");
        track(alimento, 2, "Pisca Duas Vezes", "2:21");
        track(alimento, 3, "Meu Afeto É Obsceno", "2:42");
        track(alimento, 4, "Solta", "2:46");
        track(alimento, 5, "Pq Vc Não Me Liga?", "2:58");
        track(alimento, 6, "Te Amei Infinito (Interlúdio)", "2:22");
        track(alimento, 7, "Vai no Seu Progresso", "2:55");
        track(alimento, 8, "Oi Linda", "3:02");
        track(alimento, 9, "Por Todo o Amor Que Eu Já Senti", "3:35");
        cover(alimento, "https://i.scdn.co/image/ab67616d0000b27385ba6128079f023a7e089cb3");
    }

    // label null = lancamento independente (sem gravadora)
    private Album album(String title, int releaseYear, AlbumType type, RecordLabel label, Artist artist, Genre... albumGenres) {
        Album album = new Album(title, releaseYear, type, label);
        album.getArtists().add(artist);
        album.getGenres().addAll(List.of(albumGenres));
        return albums.save(album);
    }

    // a duracao vem como no Spotify ("2:39") e e guardada em segundos (159)
    private void track(Album album, int trackNumber, String title, String duration, Artist... featuredArtists) {
        String[] minutesAndSeconds = duration.split(":");
        int seconds = Integer.parseInt(minutesAndSeconds[0]) * 60 + Integer.parseInt(minutesAndSeconds[1]);
        Track track = new Track(title, trackNumber, seconds, album);
        track.getFeaturedArtists().addAll(List.of(featuredArtists));
        tracks.save(track);
    }

    private void cover(Album album, String imageUrl) {
        covers.save(new Cover(UNKNOWN_DESIGNER, "Capa do álbum " + album.getTitle() + " (imagem do Spotify)", imageUrl, album));
    }
}
