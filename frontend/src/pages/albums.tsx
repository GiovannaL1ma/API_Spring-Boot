import { useMemo, useState } from "react"
import { Link, useNavigate, useParams } from "react-router-dom"
import { ArtistLinks, ConfirmDelete, Empty, ErrorState, PageHeader, Pager, WriteButton } from "@/components/common"
import { AlbumForm, CoverForm, TrackForm } from "@/components/forms"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Skeleton } from "@/components/ui/skeleton"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import {
  ALBUM_TYPES,
  ApiError,
  api,
  formatDuration,
  list,
  type Album,
  type Cover,
  type Track,
} from "@/lib/api"
import { useDebounced, useFetch } from "@/lib/hooks"

function CoverImg({ url, title, className }: { url?: string; title: string; className?: string }) {
  return url ? (
    <img src={url} alt={`Capa de ${title}`} loading="lazy" className={`aspect-square w-full object-cover ${className ?? ""}`} />
  ) : (
    <div
      className={`aspect-square w-full bg-muted flex items-end p-3 text-xs text-muted-foreground ${className ?? ""}`}
    >
      sem capa
    </div>
  )
}

export function AlbumsPage() {
  const [q, setQ] = useState("")
  const [page, setPage] = useState(0)
  const [creating, setCreating] = useState(false)
  const query = useDebounced(q)
  const navigate = useNavigate()

  const albums = useFetch(
    () =>
      list<Album>(
        query
          ? `/albums/search?title=${encodeURIComponent(query)}&page=${page}&size=24`
          : `/albums?page=${page}&size=24`,
      ),
    [query, page],
  )
  // capas vem de outro recurso; junta por id do album
  const covers = useFetch(() => list<Cover>("/covers?size=200"), [])
  const coverByAlbum = useMemo(
    () => new Map(covers.data?.items.map((c) => [c.album.id, c.imageUrl])),
    [covers.data],
  )

  return (
    <>
      <PageHeader title="Álbuns" count={albums.data?.page.totalElements}>
        <Input
          placeholder="Buscar por título"
          value={q}
          onChange={(e) => {
            setQ(e.target.value)
            setPage(0)
          }}
          className="w-56"
        />
        <WriteButton onClick={() => setCreating(true)}>Novo álbum</WriteButton>
      </PageHeader>

      {albums.error ? (
        <ErrorState error={albums.error} onRetry={albums.reload} />
      ) : albums.loading && !albums.data ? (
        <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-x-5 gap-y-8">
          {Array.from({ length: 8 }).map((_, i) => (
            <div key={i} className="space-y-2">
              <Skeleton className="aspect-square w-full" />
              <Skeleton className="h-4 w-3/4" />
            </div>
          ))}
        </div>
      ) : albums.data?.items.length === 0 ? (
        <Empty>{query ? `Nenhum álbum com "${query}".` : "Nenhum álbum cadastrado."}</Empty>
      ) : (
        <ul className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-x-5 gap-y-8">
          {albums.data?.items.map((a) => (
            <li key={a.id}>
              <Link to={`/albuns/${a.id}`} className="group block">
                <CoverImg
                  url={coverByAlbum.get(a.id)}
                  title={a.title}
                  className="rounded-sm border shadow-xs transition group-hover:brightness-95"
                />
                <p className="mt-2 font-medium leading-snug line-clamp-2 group-hover:underline underline-offset-2">
                  {a.title}
                </p>
              </Link>
              <p className="text-sm text-muted-foreground line-clamp-1">
                <ArtistLinks artists={a.artists} /> · <span className="tnum">{a.releaseYear}</span>
              </p>
            </li>
          ))}
        </ul>
      )}
      <Pager page={albums.data?.page} onPage={setPage} />

      <AlbumForm open={creating} onOpenChange={setCreating} onSaved={(a) => navigate(`/albuns/${a.id}`)} />
    </>
  )
}

export function AlbumPage() {
  const id = Number(useParams().id)
  const navigate = useNavigate()
  const album = useFetch(() => api<Album>(`/albums/${id}`), [id])
  const tracks = useFetch(() => list<Track>(`/tracks/album/${id}?size=99`), [id])
  const cover = useFetch(
    () =>
      api<Cover>(`/covers/album/${id}`).catch((e) => {
        if (e instanceof ApiError && e.status === 404) return null
        throw e
      }),
    [id],
  )

  const [editing, setEditing] = useState(false)
  const [deleting, setDeleting] = useState(false)
  const [coverOpen, setCoverOpen] = useState(false)
  const [trackForm, setTrackForm] = useState<{ open: boolean; track?: Track }>({ open: false })
  const [trackDel, setTrackDel] = useState<Track>()

  if (album.error) return <ErrorState error={album.error} onRetry={album.reload} />
  if (!album.data)
    return (
      <div className="grid md:grid-cols-[18rem_1fr] gap-8">
        <Skeleton className="aspect-square w-full" />
        <div className="space-y-3">
          <Skeleton className="h-8 w-2/3" />
          <Skeleton className="h-4 w-1/3" />
        </div>
      </div>
    )

  const a = album.data
  const items = tracks.data?.items ?? []
  const total = items.reduce((s, t) => s + t.durationSeconds, 0)

  return (
    <>
      <Link to="/" className="text-sm text-muted-foreground hover:text-foreground">
        ← Álbuns
      </Link>

      <div className="mt-4 grid md:grid-cols-[18rem_1fr] gap-8 items-start">
        <div className="space-y-2">
          {cover.loading ? (
            <Skeleton className="aspect-square w-full" />
          ) : (
            <CoverImg url={cover.data?.imageUrl} title={a.title} className="rounded-sm border" />
          )}
          <div className="flex items-center justify-between text-xs text-muted-foreground">
            <span className="line-clamp-1">{cover.data ? `Capa: ${cover.data.designer}` : ""}</span>
            <WriteButton variant="link" size="sm" className="h-auto p-0 text-xs" onClick={() => setCoverOpen(true)}>
              {cover.data ? "Trocar capa" : "Adicionar capa"}
            </WriteButton>
          </div>
        </div>

        <div className="min-w-0">
          <p className="text-sm text-muted-foreground">
            {ALBUM_TYPES[a.type]} · <span className="tnum">{a.releaseYear}</span>
          </p>
          <h1 className="mt-1 text-3xl font-semibold tracking-tight">{a.title}</h1>
          <p className="mt-1 text-lg">
            <ArtistLinks artists={a.artists} />
          </p>

          <dl className="mt-5 grid grid-cols-[auto_1fr] gap-x-6 gap-y-1.5 text-sm">
            <dt className="text-muted-foreground">Gravadora</dt>
            <dd>{a.label ? `${a.label.name} (${a.label.country})` : "Independente"}</dd>
            <dt className="text-muted-foreground">Gêneros</dt>
            <dd className="flex flex-wrap gap-1">
              {a.genres.length ? a.genres.map((g) => <Badge key={g.id} variant="secondary">{g.name}</Badge>) : "—"}
            </dd>
            <dt className="text-muted-foreground">Duração</dt>
            <dd className="tnum">
              {items.length} faixas{total ? `, ${Math.round(total / 60)} min` : ""}
            </dd>
          </dl>

          <div className="mt-5 flex gap-2">
            <WriteButton variant="outline" size="sm" onClick={() => setEditing(true)}>
              Editar
            </WriteButton>
            <WriteButton variant="ghost" size="sm" className="text-destructive" onClick={() => setDeleting(true)}>
              Excluir
            </WriteButton>
          </div>

          <div className="mt-8 flex items-center justify-between">
            <h2 className="font-semibold">Faixas</h2>
            <WriteButton
              size="sm"
              variant="outline"
              onClick={() => setTrackForm({ open: true })}
            >
              Adicionar faixa
            </WriteButton>
          </div>
          {tracks.error ? (
            <ErrorState error={tracks.error} onRetry={tracks.reload} />
          ) : items.length === 0 && !tracks.loading ? (
            <Empty>Sem faixas ainda.</Empty>
          ) : (
            <Table className="mt-2">
              <TableHeader>
                <TableRow>
                  <TableHead className="w-10 text-right">#</TableHead>
                  <TableHead>Título</TableHead>
                  <TableHead className="w-16 text-right">Tempo</TableHead>
                  <TableHead className="w-28" />
                </TableRow>
              </TableHeader>
              <TableBody>
                {items.map((t) => (
                  <TableRow key={t.id} className="group">
                    <TableCell className="text-right text-muted-foreground tnum">{t.trackNumber}</TableCell>
                    <TableCell>
                      {t.title}
                      {t.featuredArtists.length > 0 && (
                        <span className="text-muted-foreground">
                          {" "}
                          (feat. <ArtistLinks artists={t.featuredArtists} />)
                        </span>
                      )}
                    </TableCell>
                    <TableCell className="text-right tnum">{formatDuration(t.durationSeconds)}</TableCell>
                    <TableCell className="text-right">
                      <span className="opacity-0 group-hover:opacity-100 focus-within:opacity-100 transition-opacity">
                        <WriteButton
                          variant="ghost"
                          size="sm"
                          className="h-7 px-2"
                          onClick={() => setTrackForm({ open: true, track: t })}
                        >
                          Editar
                        </WriteButton>
                        <WriteButton
                          variant="ghost"
                          size="sm"
                          className="h-7 px-2 text-destructive"
                          onClick={() => setTrackDel(t)}
                        >
                          Excluir
                        </WriteButton>
                      </span>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </div>
      </div>

      <AlbumForm open={editing} onOpenChange={setEditing} initial={a} onSaved={album.reload} />
      <ConfirmDelete
        open={deleting}
        onOpenChange={setDeleting}
        what={`"${a.title}"`}
        extra="As faixas e a capa do álbum vão junto."
        onConfirm={async () => {
          await api(`/albums/${a.id}`, { method: "DELETE" })
          navigate("/")
        }}
      />
      <CoverForm
        open={coverOpen}
        onOpenChange={setCoverOpen}
        initial={cover.data ?? undefined}
        albumId={a.id}
        onSaved={cover.reload}
      />
      <TrackForm
        open={trackForm.open}
        onOpenChange={(open) => setTrackForm((s) => ({ ...s, open }))}
        initial={trackForm.track}
        albumId={a.id}
        nextNumber={items.length ? Math.max(...items.map((t) => t.trackNumber)) + 1 : 1}
        onSaved={tracks.reload}
      />
      <ConfirmDelete
        open={!!trackDel}
        onOpenChange={(o) => !o && setTrackDel(undefined)}
        what={`"${trackDel?.title}"`}
        onConfirm={async () => {
          await api(`/tracks/${trackDel!.id}`, { method: "DELETE" })
          tracks.reload()
        }}
      />
    </>
  )
}

// pagina so de leitura com todas as faixas, na versao 2 da API
export function TracksPage() {
  const [q, setQ] = useState("")
  const [page, setPage] = useState(0)
  const query = useDebounced(q)
  const tracks = useFetch(
    () =>
      query
        ? list<Track>(`/tracks/search?title=${encodeURIComponent(query)}&page=${page}&size=25`).then((r) => ({
            ...r,
            items: r.items.map((t) => ({
              id: t.id,
              title: t.title,
              trackNumber: t.trackNumber,
              duration: formatDuration(t.durationSeconds),
              album: { id: t.album.id, title: t.album.title },
              featuredArtists: t.featuredArtists.map((f) => f.name),
            })),
          }))
        : list<import("@/lib/api").TrackV2>(`/tracks?page=${page}&size=25`, "2"),
    [query, page],
  )

  return (
    <>
      <PageHeader title="Faixas" count={tracks.data?.page.totalElements}>
        <Input
          placeholder="Buscar por título"
          value={q}
          onChange={(e) => {
            setQ(e.target.value)
            setPage(0)
          }}
          className="w-56"
        />
      </PageHeader>
      {tracks.error ? (
        <ErrorState error={tracks.error} onRetry={tracks.reload} />
      ) : tracks.data?.items.length === 0 ? (
        <Empty>Nenhuma faixa encontrada.</Empty>
      ) : (
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Título</TableHead>
              <TableHead>Álbum</TableHead>
              <TableHead className="w-10 text-right">#</TableHead>
              <TableHead className="w-16 text-right">Tempo</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {tracks.data?.items.map((t) => (
              <TableRow key={t.id}>
                <TableCell>
                  {t.title}
                  {t.featuredArtists.length > 0 && (
                    <span className="text-muted-foreground"> (feat. {t.featuredArtists.join(", ")})</span>
                  )}
                </TableCell>
                <TableCell>
                  <Link to={`/albuns/${t.album.id}`} className="hover:underline underline-offset-2">
                    {t.album.title}
                  </Link>
                </TableCell>
                <TableCell className="text-right text-muted-foreground tnum">{t.trackNumber}</TableCell>
                <TableCell className="text-right tnum">{t.duration}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}
      <Pager page={tracks.data?.page} onPage={setPage} />
      {tracks.loading && !tracks.data && <Button variant="ghost" disabled>Carregando…</Button>}
    </>
  )
}
