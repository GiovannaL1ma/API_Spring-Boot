// teste contra a API de verdade: provoca cada erro e confere que o front reconhece a mensagem
// (se o back mudar um texto, este teste acusa). Roda com a API no ar:  npm run test:api
// por padrao usa http://localhost:8080; para outra, crie .env.api com VITE_API_URL=...
import { beforeAll, describe, expect, it, vi } from "vitest"
import { API_URL, ApiError, api, setApiKey } from "./api"

const enabled = import.meta.env.MODE === "api"

async function fail(p: Promise<unknown>) {
  try {
    await p
  } catch (e) {
    if (e instanceof ApiError) return e
    throw e
  }
  throw new Error("a API era pra ter recusado")
}

// requisicao crua, para casos que o api() nunca gera (JSON quebrado, outro Content-Type, chave repetida)
async function raw(path: string, init: RequestInit) {
  const res = await fetch(API_URL + path, init)
  const body = await res.json()
  return new ApiError(res.status, body.detail, body.errors)
}

// o front reconheceu: nao caiu no texto generico e nao sobrou ingles na tela
function expectTranslated(e: ApiError, status: number, message: RegExp) {
  expect(e.status).toBe(status)
  expect(e.detail, "a API mandou um detail").toBeTruthy()
  expect(e.message, `detail original: ${e.detail}`).toMatch(message)
  for (const v of Object.values(e.errors)) expect(v).not.toMatch(/\bmust\b|\bsize\b/)
}

describe.skipIf(!enabled)(`erros reais da API (${API_URL})`, () => {
  let userKey = ""

  beforeAll(async () => {
    const store = new Map<string, string>()
    vi.stubGlobal("window", new EventTarget())
    vi.stubGlobal("localStorage", {
      getItem: (k: string) => store.get(k) ?? null,
      setItem: (k: string, v: string) => void store.set(k, v),
      removeItem: (k: string) => void store.delete(k),
    })
    const created = await api<{ apiKey: string }>("/api-keys", {
      method: "POST",
      body: { ownerName: "Teste do front", ownerEmail: "teste@example.com" },
    })
    userKey = created.apiKey
    setApiKey(userKey)
  }, 90_000) // no Render a primeira requisicao pode demorar enquanto a API liga

  it("404 recurso inexistente", async () => {
    expectTranslated(await fail(api("/artists/999999")), 404, /^O artista #999999 não existe/)
  })

  it("404 rota inexistente", async () => {
    expectTranslated(await fail(api("/rota-que-nao-existe")), 404, /não existe na API/)
  })

  it("400 validação de campos", async () => {
    const e = await fail(api("/artists", { method: "POST", body: { name: "", country: "B", startYear: 1800 } }))
    expectTranslated(e, 400, /^Confira: /)
    expect(e.errors.name).toMatch(/vazio|caracteres/)
    expect(e.errors.country).toBe("precisa ter entre 2 e 60 caracteres")
    expect(e.errors.startYear).toBe("precisa ser no mínimo 1900")
    expect(e.errors.type).toBe("é obrigatório")
  })

  it("400 texto num campo numérico marca o campo", async () => {
    const e = await fail(
      api("/artists", { method: "POST", body: { name: "X", country: "Brazil", startYear: "abc", type: "SOLO" } }),
    )
    expectTranslated(e, 400, /^Ano de início precisa ser um número inteiro/)
    expect(e.errors.startYear).toBe("precisa ser um número inteiro")
  })

  it("400 valor fora do enum no JSON", async () => {
    const e = await fail(
      api("/artists", { method: "POST", body: { name: "X", country: "Brazil", startYear: 2000, type: "ROCK" } }),
    )
    expectTranslated(e, 400, /Aceitos: SOLO, DUO, BAND, ORCHESTRA/)
  })

  it("400 id que não é número", async () => {
    expectTranslated(await fail(api("/artists/abc")), 400, /"abc" não é um número válido/)
  })

  it("400 valor fora do enum na URL", async () => {
    expectTranslated(await fail(api("/artists/type/ROCK")), 400, /"ROCK" não é válido/)
  })

  it("400 parâmetro obrigatório faltando", async () => {
    expectTranslated(await fail(api("/artists/search")), 400, /^Faltou informar Nome/)
  })

  it("400 ordenação por campo inexistente", async () => {
    expectTranslated(await fail(api("/artists?sort=abc,asc")), 400, /ordenar por "abc"/)
  })

  it("400 período invertido", async () => {
    expectTranslated(await fail(api("/albums/released?from=2000&to=1990")), 400, /Período inválido/)
  })

  it("400 parâmetro fora do limite", async () => {
    const e = await fail(api("/albums/released?from=1800&to=1990"))
    expectTranslated(e, 400, /^Confira: /)
    expect(e.errors.from).toBe("precisa ser no mínimo 1900")
  })

  it("400 busca de gravadora sem filtro", async () => {
    expectTranslated(await fail(api("/labels/search")), 400, /país ou o nome/)
  })

  it("400 versão da API inexistente", async () => {
    expectTranslated(await fail(api("/tracks/1", { version: "3" as "2" })), 400, /versão "3"/)
  })

  it("400 corpo vazio e JSON quebrado", async () => {
    const headers = { "Content-Type": "application/json", "X-API-Key": userKey }
    expectTranslated(await raw("/genres", { method: "POST", headers }), 400, /Nenhum dado/)
    expectTranslated(await raw("/genres", { method: "POST", headers, body: "{nome" }), 400, /formato inválido/)
  })

  it("401 sem chave", async () => {
    setApiKey("")
    try {
      expectTranslated(await fail(api("/genres", { method: "POST", body: { name: "Funk" } })), 401, /precisa de uma chave/)
    } finally {
      setApiKey(userKey)
    }
  })

  it("401 chave inválida", async () => {
    setApiKey("mca_chave_que_nao_existe")
    try {
      expectTranslated(await fail(api("/artists")), 401, /não vale mais/)
    } finally {
      setApiKey(userKey)
    }
  })

  it("403 chave USER em rota de ADMIN", async () => {
    expectTranslated(await fail(api("/api-keys")), 403, /^Só uma chave ADMIN/)
  })

  it("405 método não aceito", async () => {
    expectTranslated(await fail(api("/artists/1", { method: "PATCH", body: {} })), 405, /PATCH não é aceita/)
  })

  it("409 nome repetido", async () => {
    const e = await fail(api("/genres", { method: "POST", body: { name: "Rap" } }))
    expectTranslated(e, 409, /^Já existe um gênero com esse nome/)
    expect(e.errors.name).toBe("já existe um gênero com esse nome")
  })

  it("409 excluir artista que tem álbum", async () => {
    expectTranslated(await fail(api("/artists/1", { method: "DELETE" })), 409, /artista ainda tem álbuns/)
  })

  it("409 número de faixa repetido no álbum", async () => {
    const e = await fail(
      api("/tracks", { method: "POST", body: { title: "Teste", trackNumber: 1, durationSeconds: 100, album: { id: 1 } } }),
    )
    expectTranslated(e, 409, /já tem uma faixa com esse número/)
  })

  it("409 segunda capa no mesmo álbum", async () => {
    const e = await fail(
      api("/covers", {
        method: "POST",
        body: { designer: "Teste", imageUrl: "https://example.com/capa.jpg", album: { id: 1 } },
      }),
    )
    expectTranslated(e, 409, /já tem capa/)
  })

  it("415 corpo em texto puro", async () => {
    const e = await raw("/genres", {
      method: "POST",
      headers: { "Content-Type": "text/plain", "X-API-Key": userKey },
      body: "Funk",
    })
    expectTranslated(e, 415, /formato dos dados/)
  })

  it("422 mesma X-Idempotency-Key com outro corpo", async () => {
    // so resposta 2xx prende a X-Idempotency-Key (erro libera); por isso o primeiro envio precisa dar certo:
    // gerar uma chave de API nao mexe no catalogo
    const headers = { "Content-Type": "application/json", "X-Idempotency-Key": crypto.randomUUID() }
    const first = await fetch(API_URL + "/api-keys", {
      method: "POST",
      headers,
      body: JSON.stringify({ ownerName: "Teste 422", ownerEmail: "a@example.com" }),
    })
    expect(first.status).toBe(201)
    const e = await raw("/api-keys", {
      method: "POST",
      headers,
      body: JSON.stringify({ ownerName: "Teste 422", ownerEmail: "b@example.com" }),
    })
    expectTranslated(e, 422, /conflitou com um anterior/)
  })
})
