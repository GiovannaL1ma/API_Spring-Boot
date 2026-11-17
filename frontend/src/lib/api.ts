import { translateError } from "./errors"

export const API_URL =(import.meta.env.VITE_API_URL ?? "http://localhost:8080").replace(/\/$/, "")

const KEY_STORAGE = "music-catalog:api-key"

export function getApiKey(): string {
  try {
    return localStorage.getItem(KEY_STORAGE) ?? ""
  } catch {
    return ""
  }
}

export function setApiKey(key: string) {
  try {
    if (key) localStorage.setItem(KEY_STORAGE, key)
    else localStorage.removeItem(KEY_STORAGE)
  } catch {
    /* sem localStorage, a chave vale so ate recarregar */
  }
  window.dispatchEvent(new Event("api-key-change"))
}

// erro da API (Problem Details, RFC 9457) ja traduzido para portugues
// message/title/errors: o que mostrar na tela | detail: o texto original da API, em ingles
export class ApiError extends Error {
  status: number
  title: string
  errors: Record<string, string>
  detail?: string

  constructor(status: number, detail?: string | null, errors?: Record<string, string> | null) {
    const friendly = translateError(status, detail, errors)
    super(friendly.message)
    this.name = "ApiError"
    this.status = status
    this.title = friendly.title
    this.errors = friendly.fields
    this.detail = detail ?? undefined
  }

  // erro detectado no proprio front, antes de chamar a API (mensagens ja em portugues)
  static local(message: string, errors: Record<string, string> = {}) {
    const e = new ApiError(400)
    e.message = message
    e.errors = errors
    return e
  }
}

type Options = {
  method?: "GET" | "POST" | "PUT" | "PATCH" | "DELETE"
  body?: unknown
  version?: "1" | "2"
}

export async function api<T>(path: string, { method = "GET", body, version }: Options = {}): Promise<T> {
  const headers: Record<string, string> = { Accept: "application/hal+json, application/json" }
  const key = getApiKey()
  if (key) headers["X-API-Key"] = key
  if (version) headers["X-API-Version"] = version
  if (body !== undefined) headers["Content-Type"] = "application/json"
  // repetir um POST (clique duplo, rede caindo) nao cria registro duplicado
  if (method === "POST") headers["X-Idempotency-Key"] = crypto.randomUUID()

  let res: Response
  try {
    res = await fetch(API_URL + path, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
    })
  } catch {
    // sem resposta: API fora do ar, ligando no Render, sem internet ou bloqueio de CORS
    throw new ApiError(0)
  }

  const remaining = res.headers.get("X-RateLimit-Remaining")
  const limit = res.headers.get("X-RateLimit-Limit")
  if (remaining && limit) {
    window.dispatchEvent(new CustomEvent("rate-limit", { detail: { remaining: +remaining, limit: +limit } }))
  }

  if (res.status === 204) return undefined as T

  const text = await res.text()
  let data: unknown
  try {
    data = text ? JSON.parse(text) : undefined
  } catch {
    // ex.: pagina HTML de erro do Render (502) enquanto a API liga
    data = undefined
  }

  if (!res.ok) {
    const problem = data as { detail?: string; errors?: Record<string, string> } | undefined
    throw new ApiError(res.status, problem?.detail, problem?.errors)
  }
  return data as T
}

// ---------- tipos ----------

export type Page = { number: number; size: number; totalElements: number; totalPages: number }

export type Paged<T> = { items: T[]; page: Page }

// as listagens HAL vem em _embedded.<nome>List; quando a lista esta vazia, nao vem _embedded
export async function list<T>(path: string, version?: "1" | "2"): Promise<Paged<T>> {
  const data = await api<{ _embedded?: Record<string, T[]>; page: Page }>(path, { version })
  const items = data._embedded ? Object.values(data._embedded)[0] : []
  return { items, page: data.page }
}

export type ArtistType = "SOLO" | "DUO" | "BAND" | "ORCHESTRA"
export type AlbumType = "STUDIO" | "LIVE" | "COMPILATION" | "EP" | "SOUNDTRACK"

export type Artist = { id: number; name: string; country: string; startYear: number; type: ArtistType }
export type Genre = { id: number; name: string; description?: string | null }
export type Label = { id: number; name: string; country: string; foundedYear: number }
export type Album = {
  id: number
  title: string
  releaseYear: number
  type: AlbumType
  label?: Label | null
  artists: Artist[]
  genres: Genre[]
}
export type Cover = { id: number; designer: string; description?: string | null; imageUrl: string; album: Album }
export type Track = {
  id: number
  title: string
  trackNumber: number
  durationSeconds: number
  album: Album
  featuredArtists: Artist[]
}
export type TrackV2 = {
  id: number
  title: string
  trackNumber: number
  duration: string
  album: { id: number; title: string }
  featuredArtists: string[]
}
export type ApiKeyInfo = {
  id: number
  ownerName: string
  ownerEmail: string
  keyPrefix: string
  role: "USER" | "ADMIN"
  active: boolean
  createdAt: string
  apiKey?: string
}

export const ARTIST_TYPES: Record<ArtistType, string> = {
  SOLO: "Solo",
  DUO: "Dupla",
  BAND: "Banda",
  ORCHESTRA: "Orquestra",
}

export const ALBUM_TYPES: Record<AlbumType, string> = {
  STUDIO: "Estúdio",
  LIVE: "Ao vivo",
  COMPILATION: "Coletânea",
  EP: "EP",
  SOUNDTRACK: "Trilha sonora",
}

export function formatDuration(total: number) {
  const m = Math.floor(total / 60)
  const s = total % 60
  return `${m}:${String(s).padStart(2, "0")}`
}

// "2:39" -> 159 ; "159" -> 159
export function parseDuration(value: string): number | null {
  const v = value.trim()
  if (/^\d+$/.test(v)) return Number(v)
  const m = v.match(/^(\d+):([0-5]\d)$/)
  return m ? Number(m[1]) * 60 + Number(m[2]) : null
}
