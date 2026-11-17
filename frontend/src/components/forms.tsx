import { useEffect, useState } from "react"
import { toast } from "sonner"
import { Field, FormDialog, MultiPick, TextField } from "@/components/common"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Textarea } from "@/components/ui/textarea"
import {
  ALBUM_TYPES,
  ARTIST_TYPES,
  ApiError,
  api,
  formatDuration,
  list,
  parseDuration,
  type Album,
  type AlbumType,
  type Artist,
  type ArtistType,
  type Cover,
  type Genre,
  type Label,
  type Track,
} from "@/lib/api"

type Errors = Record<string, string>

type FormProps<T> = {
  open: boolean
  onOpenChange: (o: boolean) => void
  initial?: T
  onSaved: (saved: T) => void
}

// roda o envio e, se a API recusar campos, marca cada um no formulario
async function submit<T>(setErrors: (e: Errors) => void, fn: () => Promise<T>) {
  setErrors({})
  try {
    return await fn()
  } catch (e) {
    if (e instanceof ApiError && e.errors) setErrors(e.errors)
    throw e
  }
}

// campo numerico: vazio vira null; texto que nao e numero vai como esta, e a API responde qual campo esta errado
const num = (v: string) => {
  const t = v.trim()
  if (t === "") return null
  return /^-?\d+$/.test(t) ? Number(t) : t
}

function TypeSelect<K extends string>({
  value,
  onChange,
  options,
  error,
}: {
  value: K | ""
  onChange: (v: K) => void
  options: Record<K, string>
  error?: string
}) {
  return (
    <Select value={value} onValueChange={(v) => onChange(v as K)}>
      <SelectTrigger className="w-full" aria-invalid={!!error}>
        <SelectValue placeholder="Escolha" />
      </SelectTrigger>
      <SelectContent>
        {(Object.keys(options) as K[]).map((k) => (
          <SelectItem key={k} value={k}>
            {options[k]}
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  )
}

// ---------- artista ----------

export function ArtistForm({ open, onOpenChange, initial, onSaved }: FormProps<Artist>) {
  const [name, setName] = useState("")
  const [country, setCountry] = useState("")
  const [startYear, setStartYear] = useState("")
  const [type, setType] = useState<ArtistType | "">("")
  const [errors, setErrors] = useState<Errors>({})

  useEffect(() => {
    if (!open) return
    setName(initial?.name ?? "")
    setCountry(initial?.country ?? "")
    setStartYear(initial ? String(initial.startYear) : "")
    setType(initial?.type ?? "")
    setErrors({})
  }, [open, initial])

  return (
    <FormDialog
      open={open}
      onOpenChange={onOpenChange}
      title={initial ? `Editar ${initial.name}` : "Novo artista"}
      onSubmit={() =>
        submit(setErrors, async () => {
          const body = { name, country, startYear: num(startYear), type: type || null }
          const saved = initial
            ? await api<Artist>(`/artists/${initial.id}`, { method: "PUT", body })
            : await api<Artist>("/artists", { method: "POST", body })
          toast.success(initial ? "Artista atualizado" : "Artista cadastrado")
          onSaved(saved)
        })
      }
    >
      <TextField label="Nome" value={name} onChange={setName} error={errors.name} autoFocus />
      <div className="grid grid-cols-2 gap-3">
        <TextField label="País" value={country} onChange={setCountry} error={errors.country} placeholder="Brazil" />
        <TextField
          label="Começou em"
          value={startYear}
          onChange={setStartYear}
          error={errors.startYear}
          inputMode="numeric"
          placeholder="2020"
        />
      </div>
      <Field label="Formação" error={errors.type}>
        <TypeSelect value={type} onChange={setType} options={ARTIST_TYPES} error={errors.type} />
      </Field>
    </FormDialog>
  )
}

// ---------- genero ----------

export function GenreForm({ open, onOpenChange, initial, onSaved }: FormProps<Genre>) {
  const [name, setName] = useState("")
  const [description, setDescription] = useState("")
  const [errors, setErrors] = useState<Errors>({})

  useEffect(() => {
    if (!open) return
    setName(initial?.name ?? "")
    setDescription(initial?.description ?? "")
    setErrors({})
  }, [open, initial])

  return (
    <FormDialog
      open={open}
      onOpenChange={onOpenChange}
      title={initial ? `Editar ${initial.name}` : "Novo gênero"}
      onSubmit={() =>
        submit(setErrors, async () => {
          const body = { name, description: description || null }
          const saved = initial
            ? await api<Genre>(`/genres/${initial.id}`, { method: "PUT", body })
            : await api<Genre>("/genres", { method: "POST", body })
          toast.success(initial ? "Gênero atualizado" : "Gênero cadastrado")
          onSaved(saved)
        })
      }
    >
      <TextField label="Nome" value={name} onChange={setName} error={errors.name} autoFocus />
      <Field label="Descrição" error={errors.description} hint="Opcional">
        <Textarea value={description} onChange={(e) => setDescription(e.target.value)} rows={3} />
      </Field>
    </FormDialog>
  )
}

// ---------- gravadora ----------

export function LabelForm({ open, onOpenChange, initial, onSaved }: FormProps<Label>) {
  const [name, setName] = useState("")
  const [country, setCountry] = useState("")
  const [foundedYear, setFoundedYear] = useState("")
  const [errors, setErrors] = useState<Errors>({})

  useEffect(() => {
    if (!open) return
    setName(initial?.name ?? "")
    setCountry(initial?.country ?? "")
    setFoundedYear(initial ? String(initial.foundedYear) : "")
    setErrors({})
  }, [open, initial])

  return (
    <FormDialog
      open={open}
      onOpenChange={onOpenChange}
      title={initial ? `Editar ${initial.name}` : "Nova gravadora"}
      onSubmit={() =>
        submit(setErrors, async () => {
          const body = { name, country, foundedYear: num(foundedYear) }
          const saved = initial
            ? await api<Label>(`/labels/${initial.id}`, { method: "PUT", body })
            : await api<Label>("/labels", { method: "POST", body })
          toast.success(initial ? "Gravadora atualizada" : "Gravadora cadastrada")
          onSaved(saved)
        })
      }
    >
      <TextField label="Nome" value={name} onChange={setName} error={errors.name} autoFocus />
      <div className="grid grid-cols-2 gap-3">
        <TextField label="País" value={country} onChange={setCountry} error={errors.country} />
        <TextField
          label="Fundada em"
          value={foundedYear}
          onChange={setFoundedYear}
          error={errors.foundedYear}
          inputMode="numeric"
        />
      </div>
    </FormDialog>
  )
}

// ---------- album ----------

const NO_LABEL = "none"

export function AlbumForm({ open, onOpenChange, initial, onSaved }: FormProps<Album>) {
  const [title, setTitle] = useState("")
  const [releaseYear, setReleaseYear] = useState("")
  const [type, setType] = useState<AlbumType | "">("")
  const [labelId, setLabelId] = useState(NO_LABEL)
  const [artistIds, setArtistIds] = useState<number[]>([])
  const [genreIds, setGenreIds] = useState<number[]>([])
  const [errors, setErrors] = useState<Errors>({})
  const [opts, setOpts] = useState<{ artists: Artist[]; genres: Genre[]; labels: Label[] }>({
    artists: [],
    genres: [],
    labels: [],
  })

  useEffect(() => {
    if (!open) return
    setTitle(initial?.title ?? "")
    setReleaseYear(initial ? String(initial.releaseYear) : "")
    setType(initial?.type ?? "")
    setLabelId(initial?.label ? String(initial.label.id) : NO_LABEL)
    setArtistIds(initial?.artists.map((a) => a.id) ?? [])
    setGenreIds(initial?.genres.map((g) => g.id) ?? [])
    setErrors({})
    Promise.all([
      list<Artist>("/artists?size=200&sort=name,asc"),
      list<Genre>("/genres?size=200&sort=name,asc"),
      list<Label>("/labels?size=200&sort=name,asc"),
    ])
      .then(([a, g, l]) => setOpts({ artists: a.items, genres: g.items, labels: l.items }))
      .catch(() => toast.error("Não carregou artistas, gêneros e gravadoras"))
  }, [open, initial])

  return (
    <FormDialog
      open={open}
      onOpenChange={onOpenChange}
      title={initial ? `Editar ${initial.title}` : "Novo álbum"}
      onSubmit={() =>
        submit(setErrors, async () => {
          const body = {
            title,
            releaseYear: num(releaseYear),
            type: type || null,
            label: labelId === NO_LABEL ? null : { id: Number(labelId) },
            artists: artistIds.map((id) => ({ id })),
            genres: genreIds.map((id) => ({ id })),
          }
          const saved = initial
            ? await api<Album>(`/albums/${initial.id}`, { method: "PUT", body })
            : await api<Album>("/albums", { method: "POST", body })
          toast.success(initial ? "Álbum atualizado" : "Álbum cadastrado")
          onSaved(saved)
        })
      }
    >
      <TextField label="Título" value={title} onChange={setTitle} error={errors.title} autoFocus />
      <div className="grid grid-cols-2 gap-3">
        <TextField
          label="Ano"
          value={releaseYear}
          onChange={setReleaseYear}
          error={errors.releaseYear}
          inputMode="numeric"
        />
        <Field label="Tipo" error={errors.type}>
          <TypeSelect value={type} onChange={setType} options={ALBUM_TYPES} error={errors.type} />
        </Field>
      </div>
      <Field label="Gravadora" error={errors.label}>
        <Select value={labelId} onValueChange={setLabelId}>
          <SelectTrigger className="w-full">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value={NO_LABEL}>Independente / sem gravadora</SelectItem>
            {opts.labels.map((l) => (
              <SelectItem key={l.id} value={String(l.id)}>
                {l.name}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </Field>
      <Field label="Artistas" error={errors.artists} hint="Pelo menos um">
        <MultiPick options={opts.artists} value={artistIds} onChange={setArtistIds} />
      </Field>
      <Field label="Gêneros" error={errors.genres}>
        <MultiPick options={opts.genres} value={genreIds} onChange={setGenreIds} />
      </Field>
    </FormDialog>
  )
}

// ---------- faixa (sempre dentro de um album) ----------

export function TrackForm({
  open,
  onOpenChange,
  initial,
  onSaved,
  albumId,
  nextNumber,
}: FormProps<Track> & { albumId: number; nextNumber: number }) {
  const [title, setTitle] = useState("")
  const [trackNumber, setTrackNumber] = useState("")
  const [duration, setDuration] = useState("")
  const [featured, setFeatured] = useState<number[]>([])
  const [artists, setArtists] = useState<Artist[]>([])
  const [errors, setErrors] = useState<Errors>({})

  useEffect(() => {
    if (!open) return
    setTitle(initial?.title ?? "")
    setTrackNumber(String(initial?.trackNumber ?? nextNumber))
    setDuration(initial ? formatDuration(initial.durationSeconds) : "")
    setFeatured(initial?.featuredArtists.map((a) => a.id) ?? [])
    setErrors({})
    list<Artist>("/artists?size=200&sort=name,asc")
      .then((r) => setArtists(r.items))
      .catch(() => toast.error("Não carregou os artistas"))
  }, [open, initial, nextNumber])

  return (
    <FormDialog
      open={open}
      onOpenChange={onOpenChange}
      title={initial ? `Editar ${initial.title}` : "Nova faixa"}
      onSubmit={() =>
        submit(setErrors, async () => {
          const seconds = parseDuration(duration)
          if (duration.trim() && seconds === null) {
            throw ApiError.local("A duração precisa estar no formato m:ss, ex.: 3:25.", {
              durationSeconds: "use o formato m:ss, ex.: 3:25",
            })
          }
          const body = {
            title,
            trackNumber: num(trackNumber),
            durationSeconds: seconds,
            album: { id: albumId },
            featuredArtists: featured.map((id) => ({ id })),
          }
          const saved = initial
            ? await api<Track>(`/tracks/${initial.id}`, { method: "PUT", body })
            : await api<Track>("/tracks", { method: "POST", body })
          toast.success(initial ? "Faixa atualizada" : "Faixa adicionada")
          onSaved(saved)
        })
      }
    >
      <div className="grid grid-cols-[4.5rem_1fr] gap-3">
        <TextField
          label="Nº"
          value={trackNumber}
          onChange={setTrackNumber}
          error={errors.trackNumber}
          inputMode="numeric"
        />
        <TextField label="Título" value={title} onChange={setTitle} error={errors.title} autoFocus />
      </div>
      <TextField
        label="Duração"
        value={duration}
        onChange={setDuration}
        error={errors.durationSeconds}
        placeholder="3:25"
        className="w-28 tnum"
      />
      <Field label="Participações (feat.)" error={errors.featuredArtists} hint="Opcional">
        <MultiPick options={artists} value={featured} onChange={setFeatured} />
      </Field>
    </FormDialog>
  )
}

// ---------- capa (uma por album) ----------

export function CoverForm({
  open,
  onOpenChange,
  initial,
  onSaved,
  albumId,
}: FormProps<Cover> & { albumId: number }) {
  const [imageUrl, setImageUrl] = useState("")
  const [designer, setDesigner] = useState("")
  const [description, setDescription] = useState("")
  const [errors, setErrors] = useState<Errors>({})

  useEffect(() => {
    if (!open) return
    setImageUrl(initial?.imageUrl ?? "")
    setDesigner(initial?.designer ?? "")
    setDescription(initial?.description ?? "")
    setErrors({})
  }, [open, initial])

  return (
    <FormDialog
      open={open}
      onOpenChange={onOpenChange}
      title={initial ? "Trocar capa" : "Adicionar capa"}
      onSubmit={() =>
        submit(setErrors, async () => {
          const body = { imageUrl, designer, description: description || null, album: { id: albumId } }
          const saved = initial
            ? await api<Cover>(`/covers/${initial.id}`, { method: "PUT", body })
            : await api<Cover>("/covers", { method: "POST", body })
          toast.success("Capa salva")
          onSaved(saved)
        })
      }
    >
      <TextField
        label="URL da imagem"
        value={imageUrl}
        onChange={setImageUrl}
        error={errors.imageUrl}
        placeholder="https://…"
        autoFocus
      />
      {imageUrl.startsWith("http") && (
        <img src={imageUrl} alt="" className="size-28 rounded-sm border object-cover" />
      )}
      <TextField label="Designer" value={designer} onChange={setDesigner} error={errors.designer} />
      <Field label="Descrição" error={errors.description} hint="Opcional">
        <Textarea value={description} onChange={(e) => setDescription(e.target.value)} rows={2} />
      </Field>
    </FormDialog>
  )
}
