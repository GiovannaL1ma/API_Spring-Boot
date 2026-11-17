import { useState, type ReactNode } from "react"
import { Link, useNavigate, useParams } from "react-router-dom"
import { ConfirmDelete, Empty, ErrorState, PageHeader, Pager, WriteButton } from "@/components/common"
import { ArtistForm, GenreForm, LabelForm } from "@/components/forms"
import { Badge } from "@/components/ui/badge"
import { Input } from "@/components/ui/input"
import { Skeleton } from "@/components/ui/skeleton"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import {
  ALBUM_TYPES,
  ARTIST_TYPES,
  api,
  list,
  type Album,
  type Artist,
  type Genre,
  type Label,
  type Paged,
  type Track,
} from "@/lib/api"
import { useDebounced, useFetch } from "@/lib/hooks"

// tabela com busca, paginacao, editar e excluir; usada por artistas, generos e gravadoras
function CrudTable<T extends { id: number; name: string }>({
  title,
  newLabel,
  fetchPage,
  columns,
  renderForm,
  deletePath,
  deleteExtra,
  searchPlaceholder = "Buscar por nome",
}: {
  title: string
  newLabel: string
  fetchPage: (query: string, page: number) => Promise<Paged<T>>
  columns: { head: string; cell: (row: T) => ReactNode; className?: string }[]
  renderForm: (p: { open: boolean; onOpenChange: (o: boolean) => void; initial?: T; onSaved: () => void }) => ReactNode
  deletePath: (row: T) => string
  deleteExtra?: string
  searchPlaceholder?: string
}) {
  const [q, setQ] = useState("")
  const [page, setPage] = useState(0)
  const query = useDebounced(q)
  const data = useFetch(() => fetchPage(query, page), [query, page])
  const [form, setForm] = useState<{ open: boolean; row?: T }>({ open: false })
  const [del, setDel] = useState<T>()

  return (
    <>
      <PageHeader title={title} count={data.data?.page.totalElements}>
        <Input
          placeholder={searchPlaceholder}
          value={q}
          onChange={(e) => {
            setQ(e.target.value)
            setPage(0)
          }}
          className="w-56"
        />
        <WriteButton onClick={() => setForm({ open: true })}>{newLabel}</WriteButton>
      </PageHeader>

      {data.error ? (
        <ErrorState error={data.error} onRetry={data.reload} />
      ) : data.loading && !data.data ? (
        <div className="space-y-2">
          {Array.from({ length: 5 }).map((_, i) => (
            <Skeleton key={i} className="h-9 w-full" />
          ))}
        </div>
      ) : data.data?.items.length === 0 ? (
        <Empty>{query ? `Nada com "${query}".` : "Nada cadastrado ainda."}</Empty>
      ) : (
        <Table>
          <TableHeader>
            <TableRow>
              {columns.map((c) => (
                <TableHead key={c.head} className={c.className}>
                  {c.head}
                </TableHead>
              ))}
              <TableHead className="w-36" />
            </TableRow>
          </TableHeader>
          <TableBody>
            {data.data?.items.map((row) => (
              <TableRow key={row.id} className="group">
                {columns.map((c) => (
                  <TableCell key={c.head} className={c.className}>
                    {c.cell(row)}
                  </TableCell>
                ))}
                <TableCell className="text-right">
                  <span className="opacity-0 group-hover:opacity-100 focus-within:opacity-100 transition-opacity">
                    <WriteButton
                      variant="ghost"
                      size="sm"
                      className="h-7 px-2"
                      onClick={() => setForm({ open: true, row })}
                    >
                      Editar
                    </WriteButton>
                    <WriteButton
                      variant="ghost"
                      size="sm"
                      className="h-7 px-2 text-destructive"
                      onClick={() => setDel(row)}
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
      <Pager page={data.data?.page} onPage={setPage} />

      {renderForm({
        open: form.open,
        onOpenChange: (open) => setForm((s) => ({ ...s, open })),
        initial: form.row,
        onSaved: data.reload,
      })}
      <ConfirmDelete
        open={!!del}
        onOpenChange={(o) => !o && setDel(undefined)}
        what={`"${del?.name}"`}
        extra={deleteExtra}
        onConfirm={async () => {
          await api(deletePath(del!), { method: "DELETE" })
          data.reload()
        }}
      />
    </>
  )
}

const searchOr = (base: string, param: string, query: string, page: number) =>
  query
    ? `${base}/search?${param}=${encodeURIComponent(query)}&page=${page}&size=20&sort=name,asc`
    : `${base}?page=${page}&size=20&sort=name,asc`

export function ArtistsPage() {
  return (
    <CrudTable<Artist>
      title="Artistas"
      newLabel="Novo artista"
      fetchPage={(q, p) => list<Artist>(searchOr("/artists", "name", q, p))}
      columns={[
        {
          head: "Nome",
          cell: (a) => (
            <Link to={`/artistas/${a.id}`} className="font-medium hover:underline underline-offset-2">
              {a.name}
            </Link>
          ),
        },
        { head: "Formação", cell: (a) => ARTIST_TYPES[a.type] },
        { head: "País", cell: (a) => a.country },
        { head: "Desde", cell: (a) => <span className="tnum">{a.startYear}</span>, className: "text-right w-20" },
      ]}
      renderForm={(p) => <ArtistForm {...p} />}
      deletePath={(a) => `/artists/${a.id}`}
      deleteExtra="Só dá pra excluir artista que não tem álbum."
    />
  )
}

export function GenresPage() {
  return (
    <CrudTable<Genre>
      title="Gêneros"
      newLabel="Novo gênero"
      fetchPage={(q, p) => list<Genre>(searchOr("/genres", "name", q, p))}
      columns={[
        { head: "Nome", cell: (g) => <span className="font-medium">{g.name}</span>, className: "w-48" },
        { head: "Descrição", cell: (g) => <span className="text-muted-foreground">{g.description || "—"}</span> },
      ]}
      renderForm={(p) => <GenreForm {...p} />}
      deletePath={(g) => `/genres/${g.id}`}
      deleteExtra="O gênero sai dos álbuns que usam ele; os álbuns continuam."
    />
  )
}

export function LabelsPage() {
  return (
    <CrudTable<Label>
      title="Gravadoras"
      newLabel="Nova gravadora"
      fetchPage={(q, p) => list<Label>(searchOr("/labels", "name", q, p))}
      columns={[
        { head: "Nome", cell: (l) => <span className="font-medium">{l.name}</span> },
        { head: "País", cell: (l) => l.country },
        { head: "Fundada", cell: (l) => <span className="tnum">{l.foundedYear}</span>, className: "text-right w-24" },
      ]}
      renderForm={(p) => <LabelForm {...p} />}
      deletePath={(l) => `/labels/${l.id}`}
      deleteExtra="Só dá pra excluir gravadora sem álbuns."
    />
  )
}

export function ArtistPage() {
  const id = Number(useParams().id)
  const navigate = useNavigate()
  const artist = useFetch(() => api<Artist>(`/artists/${id}`), [id])
  const albums = useFetch(() => list<Album>(`/albums/artist/${id}?size=100&sort=releaseYear,desc`), [id])
  const feats = useFetch(() => list<Track>(`/tracks/featuring/${id}?size=100`), [id])
  const [editing, setEditing] = useState(false)
  const [deleting, setDeleting] = useState(false)

  if (artist.error) return <ErrorState error={artist.error} onRetry={artist.reload} />
  if (!artist.data) return <Skeleton className="h-10 w-1/2" />
  const a = artist.data

  return (
    <>
      <Link to="/artistas" className="text-sm text-muted-foreground hover:text-foreground">
        ← Artistas
      </Link>
      <div className="mt-4 flex flex-wrap items-end justify-between gap-4 border-b pb-6">
        <div>
          <p className="text-sm text-muted-foreground">
            {ARTIST_TYPES[a.type]} · {a.country} · desde <span className="tnum">{a.startYear}</span>
          </p>
          <h1 className="mt-1 text-3xl font-semibold tracking-tight">{a.name}</h1>
        </div>
        <div className="flex gap-2">
          <WriteButton variant="outline" size="sm" onClick={() => setEditing(true)}>
            Editar
          </WriteButton>
          <WriteButton variant="ghost" size="sm" className="text-destructive" onClick={() => setDeleting(true)}>
            Excluir
          </WriteButton>
        </div>
      </div>

      <section className="mt-8">
        <h2 className="font-semibold mb-3">Discografia</h2>
        {albums.data?.items.length === 0 ? (
          <Empty>Nenhum álbum ainda.</Empty>
        ) : (
          <ul className="divide-y border-y">
            {albums.data?.items.map((al) => (
              <li key={al.id} className="flex items-center justify-between gap-4 py-2.5">
                <Link to={`/albuns/${al.id}`} className="font-medium hover:underline underline-offset-2">
                  {al.title}
                </Link>
                <span className="flex items-center gap-3 text-sm text-muted-foreground">
                  <Badge variant="outline">{ALBUM_TYPES[al.type]}</Badge>
                  <span className="tnum">{al.releaseYear}</span>
                </span>
              </li>
            ))}
          </ul>
        )}
      </section>

      {!!feats.data?.items.length && (
        <section className="mt-8">
          <h2 className="font-semibold mb-3">Participações</h2>
          <ul className="divide-y border-y">
            {feats.data.items.map((t) => (
              <li key={t.id} className="flex items-center justify-between gap-4 py-2.5">
                <span>{t.title}</span>
                <Link
                  to={`/albuns/${t.album.id}`}
                  className="text-sm text-muted-foreground hover:text-foreground hover:underline underline-offset-2"
                >
                  {t.album.title}
                </Link>
              </li>
            ))}
          </ul>
        </section>
      )}

      <ArtistForm open={editing} onOpenChange={setEditing} initial={a} onSaved={artist.reload} />
      <ConfirmDelete
        open={deleting}
        onOpenChange={setDeleting}
        what={a.name}
        extra="Só dá pra excluir artista que não tem álbum."
        onConfirm={async () => {
          await api(`/artists/${a.id}`, { method: "DELETE" })
          navigate("/artistas")
        }}
      />
    </>
  )
}
