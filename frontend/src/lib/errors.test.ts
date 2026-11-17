import { describe, expect, it } from "vitest"
import { translateError, translateFieldMessage } from "./errors"

// as mensagens abaixo sao as mesmas que o back monta (ApiExceptionAdvice e exceptions)
describe("translateError: cada detail da API vira portugues", () => {
  const cases: [number, string, RegExp][] = [
    // 404
    [404, "Could not find artist with ID: 99", /^O artista #99 não existe/],
    [404, "Could not find album with ID: 7", /^O álbum #7 não existe/],
    [404, "Could not find track with ID: 3", /^A faixa #3 não existe/],
    [404, "Could not find genre with ID: 3", /^O gênero #3 não existe/],
    [404, "Could not find record label with ID: 3", /^A gravadora #3 não existe/],
    [404, "Could not find cover with ID: 3", /^A capa #3 não existe/],
    [404, "Could not find API key with ID: 3", /^A chave de API #3 não existe/],
    [404, "Album with ID: 1 has no cover", /^Esse álbum ainda não tem capa/],
    [404, "No endpoint GET /artistas: check the URL in the Swagger documentation", /não existe na API \(GET \/artistas\)/],
    // 400
    [400, "Validation failed for: name, startYear", /^Confira: Nome, Ano de início\.$/],
    [400, "Invalid value for field 'type'. Accepted values: [SOLO, DUO, BAND, ORCHESTRA]", /Tipo\. Aceitos: SOLO, DUO, BAND, ORCHESTRA\.$/],
    [400, "Invalid value for field 'releaseYear': expected Integer", /^Ano precisa ser um número inteiro\.$/],
    [400, "Invalid value for field 'trackNumber': number out of range", /Número da faixa é grande demais/],
    [400, "Request body is missing: send the resource as JSON", /^Nenhum dado foi enviado/],
    [400, "Malformed JSON: check quotes, commas and braces", /formato inválido/],
    [400, "Invalid value 'abc' for parameter 'id': it must be a whole number", /^"abc" não é um número válido para id\.$/],
    [400, "Invalid value 'ROCK' for parameter 'type'. Accepted values: [SOLO, DUO, BAND, ORCHESTRA]", /^"ROCK" não é válido para Tipo/],
    [400, "Required parameter 'name' is missing", /^Faltou informar Nome\.$/],
    [400, "Invalid sort: Artist has no field 'abc'", /^Não dá pra ordenar por "abc"/],
    [400, "Inform country or name to search record labels", /país ou o nome/],
    [400, "Invalid period: from (2000) must be less than or equal to to (1990)", /ano inicial \(2000\).*final \(1990\)/],
    [400, "Unsupported API version '3' in the X-API-Version header. Supported versions: 1, 2", /versão "3".*Versões: 1, 2/],
    [400, "Unsupported API version '3.0.0' in the X-API-Version header. Supported versions: 1, 2", /^A versão "3" da API/],
    [400, "Invalid X-Idempotency-Key: it must have between 1 and 255 characters", /problema interno do site/],
    // 401 / 403 / 405
    [401, "Missing API key: send it in the X-API-Key header (create one with POST /api-keys)", /precisa de uma chave de API/],
    [401, "Invalid or inactive API key in the X-API-Key header", /não vale mais/],
    [403, "This operation requires an API key with role ADMIN", /^Só uma chave ADMIN pode fazer isso\.$/],
    [405, "Method PATCH is not supported for this endpoint. Supported methods: GET, PUT, DELETE", /operação PATCH não é aceita/],
    // 409
    [409, "Artist with ID: 1 still has albums; remove the artist from them or delete them first", /artista ainda tem álbuns/],
    [409, "Record label with ID: 1 still has albums; move them to another label or delete them first", /gravadora ainda tem álbuns/],
    [409, "This album already has a track with this trackNumber", /já tem uma faixa com esse número/],
    [409, "This album already has a cover: update or delete it instead of creating another", /já tem capa/],
    [409, "An artist with this name already exists", /^Já existe um artista com esse nome\.$/],
    [409, "A genre with this name already exists", /^Já existe um gênero com esse nome\.$/],
    [409, "A record label with this name already exists", /^Já existe uma gravadora com esse nome\.$/],
    [409, "Operation conflicts with existing data: a value that must be unique is already in use", /precisa ser único/],
    [409, "A request with X-Idempotency-Key 'abc' is still being processed; try again in a moment", /ainda está sendo processado/],
    // 415 / 422 / 429 / 500
    [415, "Content-Type 'text/plain' is not supported: send the body as application/json", /formato dos dados/],
    [422, "X-Idempotency-Key 'abc' was already used with a different request; use a new key for a new operation", /conflitou com um anterior/],
    [429, "Too many requests: the limit is 60 requests per minute. Try again in 23 seconds", /^O limite é de 60 requisições por minuto\. Tente de novo em 23 segundos\.$/],
    [500, "Unexpected error: the request could not be completed. Try again later", /problema inesperado/],
  ]

  it.each(cases)("%i %s", (status, detail, expected) => {
    const out = translateError(status, detail)
    expect(out.known).toBe(true)
    expect(out.message).toMatch(expected)
  })

  it("o título vem do status", () => {
    expect(translateError(404, "Could not find artist with ID: 1").title).toBe("Não encontrado")
    expect(translateError(409, "A genre with this name already exists").title).toBe("Conflito")
    expect(translateError(429, "Too many requests: the limit is 60 requests per minute. Try again in 1 seconds").title).toBe(
      "Muitas requisições",
    )
  })

  it("mensagem desconhecida cai no texto genérico do status, sem mostrar inglês", () => {
    const out = translateError(400, "Something new the front does not know")
    expect(out.known).toBe(false)
    expect(out.message).toBe("Algum dado enviado está inválido.")
  })

  it("sem resposta (status 0) e 502 explicam que a API pode estar ligando", () => {
    expect(translateError(0).message).toMatch(/Render.*ligando/)
    expect(translateError(502).message).toMatch(/ligando/)
    expect(translateError(503).title).toBe("API indisponível")
    expect(translateError(599).title).toBe("Erro no servidor")
  })

  it("erros de tipo no JSON marcam o campo no formulário", () => {
    expect(translateError(400, "Invalid value for field 'startYear': expected Integer").fields).toEqual({
      startYear: "precisa ser um número inteiro",
    })
    expect(translateError(400, "Invalid value for field 'artists[0].id': expected Long").fields).toEqual({
      artists: "precisa ser um número inteiro",
    })
    expect(translateError(409, "An artist with this name already exists").fields).toEqual({
      name: "já existe um artista com esse nome",
    })
  })
})

describe("erros de validação por campo", () => {
  it("traduz o errors e usa o nome base do campo", () => {
    const out = translateError(400, "Validation failed for: name, startYear, artists[0]", {
      name: "must not be blank",
      startYear: "must be greater than or equal to 1900",
      "artists[0]": "must not be null",
    })
    expect(out.fields).toEqual({
      name: "não pode ficar vazio",
      startYear: "precisa ser no mínimo 1900",
      artists: "é obrigatório",
    })
    expect(out.message).toBe("Confira: Nome, Ano de início, Artistas.")
  })

  // o servidor manda em ingles ou em portugues conforme o idioma (Accept-Language / locale da JVM)
  it.each([
    ["must not be blank", "não pode ficar vazio"],
    ["não deve estar em branco", "não pode ficar vazio"],
    ["must not be null", "é obrigatório"],
    ["não deve ser nulo", "é obrigatório"],
    ["must not be empty", "escolha pelo menos um"],
    ["não deve estar vazio", "escolha pelo menos um"],
    ["size must be between 1 and 100", "precisa ter entre 1 e 100 caracteres"],
    ["tamanho deve ser entre 2 e 60", "precisa ter entre 2 e 60 caracteres"],
    ["size must be between 0 and 255", "pode ter no máximo 255 caracteres"],
    ["must be greater than or equal to 1900", "precisa ser no mínimo 1900"],
    ["deve ser maior que ou igual à 1900", "precisa ser no mínimo 1900"],
    ["must be less than or equal to 2100", "precisa ser no máximo 2100"],
    ["deve ser menor que ou igual à 2100", "precisa ser no máximo 2100"],
    ["must be a valid URL", "não é uma URL válida (comece com https://)"],
    ["must be a well-formed email address", "não é um e-mail válido"],
    ["deve ser um endereço de e-mail bem formado", "não é um e-mail válido"],
    ["must be greater than or equal to 1900; must be less than or equal to 2100", "precisa ser no mínimo 1900; precisa ser no máximo 2100"],
  ])("%s -> %s", (input, expected) => {
    expect(translateFieldMessage(input)).toBe(expected)
  })
})
