import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import { ApiError, api, setApiKey } from "./api"

// o api() roda no navegador; aqui no Node simulamos window e localStorage
function browserStubs() {
  const store = new Map<string, string>()
  vi.stubGlobal("window", new EventTarget())
  vi.stubGlobal("localStorage", {
    getItem: (k: string) => store.get(k) ?? null,
    setItem: (k: string, v: string) => void store.set(k, v),
    removeItem: (k: string) => void store.delete(k),
  })
}

function respond(status: number, body?: unknown, headers: Record<string, string> = {}) {
  const text = body === undefined ? "" : typeof body === "string" ? body : JSON.stringify(body)
  return vi.fn().mockResolvedValue(new Response(status === 204 ? null : text, { status, headers }))
}

async function catchError(p: Promise<unknown>) {
  try {
    await p
  } catch (e) {
    return e as ApiError
  }
  throw new Error("era pra ter dado erro")
}

beforeEach(browserStubs)
afterEach(() => vi.unstubAllGlobals())

describe("api(): erros chegam traduzidos", () => {
  it("400 de validação: mensagem e campos em português", async () => {
    vi.stubGlobal(
      "fetch",
      respond(400, {
        title: "Bad Request",
        status: 400,
        detail: "Validation failed for: name, startYear",
        errors: { name: "must not be blank", startYear: "must be greater than or equal to 1900" },
      }),
    )
    const e = await catchError(api("/artists", { method: "POST", body: {} }))
    expect(e).toBeInstanceOf(ApiError)
    expect(e.status).toBe(400)
    expect(e.title).toBe("Dados inválidos")
    expect(e.message).toBe("Confira: Nome, Ano de início.")
    expect(e.errors).toEqual({ name: "não pode ficar vazio", startYear: "precisa ser no mínimo 1900" })
    expect(e.detail).toBe("Validation failed for: name, startYear") // o original fica guardado
  })

  it("401 sem chave", async () => {
    vi.stubGlobal(
      "fetch",
      respond(401, { status: 401, detail: "Missing API key: send it in the X-API-Key header (create one with POST /api-keys)" }),
    )
    const e = await catchError(api("/genres", { method: "POST", body: { name: "Funk" } }))
    expect(e.title).toBe("Sem chave válida")
    expect(e.message).toMatch(/precisa de uma chave de API/)
  })

  it("404 com o recurso e o id", async () => {
    vi.stubGlobal("fetch", respond(404, { status: 404, detail: "Could not find album with ID: 42" }))
    const e = await catchError(api("/albums/42"))
    expect(e.message).toBe("O álbum #42 não existe. Pode ter sido excluído.")
  })

  it("API fora do ar (fetch falha): status 0 e mensagem de conexão", async () => {
    vi.stubGlobal("fetch", vi.fn().mockRejectedValue(new TypeError("Failed to fetch")))
    const e = await catchError(api("/albums"))
    expect(e.status).toBe(0)
    expect(e.title).toBe("Sem conexão")
  })

  it("502 com página HTML do Render não quebra o JSON.parse", async () => {
    vi.stubGlobal("fetch", respond(502, "<html><body>Bad Gateway</body></html>", { "Content-Type": "text/html" }))
    const e = await catchError(api("/albums"))
    expect(e.status).toBe(502)
    expect(e.title).toBe("API indisponível")
  })

  it("500 sem corpo usa o texto genérico", async () => {
    vi.stubGlobal("fetch", respond(500))
    const e = await catchError(api("/albums"))
    expect(e.message).toMatch(/problema inesperado/)
  })

  it("429 traz o tempo de espera", async () => {
    vi.stubGlobal(
      "fetch",
      respond(429, { status: 429, detail: "Too many requests: the limit is 60 requests per minute. Try again in 12 seconds" }),
    )
    const e = await catchError(api("/albums"))
    expect(e.message).toBe("O limite é de 60 requisições por minuto. Tente de novo em 12 segundos.")
  })

  it("ApiError.local mantém a mensagem do front", () => {
    const e = ApiError.local("A duração precisa estar no formato m:ss.", { durationSeconds: "use m:ss" })
    expect(e.message).toBe("A duração precisa estar no formato m:ss.")
    expect(e.errors).toEqual({ durationSeconds: "use m:ss" })
  })
})

describe("api(): o que vai na requisição", () => {
  it("POST manda chave, X-Idempotency-Key e JSON", async () => {
    const fetch = respond(201, { id: 1 })
    vi.stubGlobal("fetch", fetch)
    setApiKey("mca_teste")
    await api("/genres", { method: "POST", body: { name: "Funk" } })
    const [, init] = fetch.mock.calls[0]
    expect(init.headers["X-API-Key"]).toBe("mca_teste")
    expect(init.headers["X-Idempotency-Key"]).toMatch(/^[0-9a-f-]{36}$/)
    expect(init.headers["Content-Type"]).toBe("application/json")
    expect(init.body).toBe('{"name":"Funk"}')
  })

  it("GET não manda X-Idempotency-Key e 204 devolve undefined", async () => {
    const fetch = respond(204)
    vi.stubGlobal("fetch", fetch)
    await expect(api("/genres/1", { method: "DELETE" })).resolves.toBeUndefined()
    await api("/genres")
    expect(fetch.mock.calls[1][1].headers["X-Idempotency-Key"]).toBeUndefined()
  })
})
