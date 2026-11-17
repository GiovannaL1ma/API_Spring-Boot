// a API responde os erros em ingles (Problem Details: status, detail e, na validacao, errors por campo);
// aqui cada mensagem vira um texto em portugues para mostrar na tela

export type FriendlyError = {
  title: string
  message: string
  // erros por campo, com a chave do campo no formato do formulario (ex.: "artists[0].id" -> "artists")
  fields: Record<string, string>
  // false quando nenhuma regra reconheceu a mensagem e caiu no texto generico do status
  known: boolean
}

const FIELD_LABELS: Record<string, string> = {
  name: "Nome",
  title: "Título",
  country: "País",
  startYear: "Ano de início",
  foundedYear: "Ano de fundação",
  releaseYear: "Ano",
  type: "Tipo",
  label: "Gravadora",
  artists: "Artistas",
  genres: "Gêneros",
  trackNumber: "Número da faixa",
  durationSeconds: "Duração",
  album: "Álbum",
  featuredArtists: "Participações",
  designer: "Designer",
  description: "Descrição",
  imageUrl: "URL da imagem",
  ownerName: "Nome",
  ownerEmail: "E-mail",
  role: "Nível",
  active: "Ativa",
  id: "id",
  from: "ano inicial",
  to: "ano final",
  page: "página",
  size: "tamanho da página",
  sort: "ordenação",
}

const RESOURCES: Record<string, string> = {
  album: "O álbum",
  artist: "O artista",
  track: "A faixa",
  genre: "O gênero",
  "record label": "A gravadora",
  cover: "A capa",
  "API key": "A chave de API",
}

const JAVA_TYPES: Record<string, string> = {
  Integer: "um número inteiro",
  int: "um número inteiro",
  Long: "um número inteiro",
  long: "um número inteiro",
  String: "um texto",
  Boolean: "verdadeiro ou falso",
  boolean: "verdadeiro ou falso",
}

const STATUS: Record<number, [string, string]> = {
  0: ["Sem conexão", "Não deu pra falar com a API. Se ela estiver no Render, pode estar ligando: espere uns segundos e tente de novo."],
  400: ["Dados inválidos", "Algum dado enviado está inválido."],
  401: ["Sem chave válida", "Essa ação precisa de uma chave de API válida."],
  403: ["Sem permissão", "Sua chave não tem permissão para isso."],
  404: ["Não encontrado", "Isso não existe (ou foi excluído)."],
  405: ["Operação não permitida", "Essa operação não é aceita aqui."],
  409: ["Conflito", "Isso conflita com dados que já existem."],
  415: ["Formato não aceito", "A API não aceitou o formato dos dados enviados."],
  422: ["Envio repetido", "Esse envio conflitou com um anterior. Tente de novo."],
  429: ["Muitas requisições", "Você passou do limite de requisições. Espere um pouco e tente de novo."],
  500: ["Erro no servidor", "A API teve um problema inesperado. Tente de novo daqui a pouco."],
  502: ["API indisponível", "A API não respondeu. Se estiver no Render, pode estar ligando: espere uns segundos."],
  503: ["API indisponível", "A API não respondeu. Se estiver no Render, pode estar ligando: espere uns segundos."],
  504: ["API indisponível", "A API demorou demais pra responder. Tente de novo."],
}

function statusText(status: number): [string, string] {
  if (STATUS[status]) return STATUS[status]
  if (status >= 500) return STATUS[500]
  return ["Erro", `A API respondeu com erro ${status}.`]
}

// "artists[0].id" -> "artists"
export function baseField(path: string) {
  return path.split(/[.[]/)[0]
}

export function fieldLabel(path: string) {
  return FIELD_LABELS[baseField(path)] ?? path
}

const list = (values: string) =>
  values
    .split(",")
    .map((v) => v.trim())
    .filter(Boolean)
    .join(", ")

// mensagens do Bean Validation (Hibernate Validator), em ingles ou em portugues conforme o idioma do servidor
const FIELD_RULES: [RegExp, (m: RegExpMatchArray) => string][] = [
  [/must not be blank|n[ãa]o (deve|pode) estar em branco/i, () => "não pode ficar vazio"],
  [/must not be empty|n[ãa]o (deve|pode) estar vazi[oa]/i, () => "escolha pelo menos um"],
  [/must not be null|n[ãa]o (deve|pode) ser nul[oa]/i, () => "é obrigatório"],
  [
    /size must be between (\d+) and (\d+)|tamanho deve ser entre (\d+) e (\d+)/i,
    (m) => {
      const min = Number(m[1] ?? m[3])
      const max = Number(m[2] ?? m[4])
      return min <= 0 ? `pode ter no máximo ${max} caracteres` : `precisa ter entre ${min} e ${max} caracteres`
    },
  ],
  [/must be greater than or equal to (\d+)|deve ser maior que ou igual [àa] (\d+)/i, (m) => `precisa ser no mínimo ${m[1] ?? m[2]}`],
  [/must be less than or equal to (\d+)|deve ser menor que ou igual [àa] (\d+)/i, (m) => `precisa ser no máximo ${m[1] ?? m[2]}`],
  [/must be a valid URL|deve ser uma URL v[áa]lida/i, () => "não é uma URL válida (comece com https://)"],
  [/well-formed email|e-mail bem formado|email bem formado/i, () => "não é um e-mail válido"],
]

export function translateFieldMessage(message: string): string {
  // varias regras no mesmo parametro vem separadas por "; "
  return message
    .split("; ")
    .map((part) => {
      for (const [re, fn] of FIELD_RULES) {
        const m = part.match(re)
        if (m) return fn(m)
      }
      return part
    })
    .join("; ")
}

type Rule = [RegExp, (m: RegExpMatchArray) => { message: string; fields?: Record<string, string> }]

// cada "detail" que a API manda (ver ApiExceptionAdvice e as exceptions do back)
const DETAIL_RULES: Rule[] = [
  // 404
  [
    /^Could not find (album|artist|track|genre|record label|cover|API key) with ID: (-?\d+)/,
    (m) => ({ message: `${RESOURCES[m[1]]} #${m[2]} não existe. Pode ter sido excluído.` }),
  ],
  [/^Album with ID: (\d+) has no cover/, () => ({ message: "Esse álbum ainda não tem capa." })],
  [/^No endpoint (\w+) \/(\S*?):/, (m) => ({ message: `Esse endereço não existe na API (${m[1]} /${m[2]}).` })],

  // 400 - validacao (os campos vem no "errors")
  [/^Validation failed for: (.*)$/, (m) => ({ message: `Confira: ${m[1].split(", ").map(fieldLabel).join(", ")}.` })],
  // 400 - JSON com tipo errado
  [
    /^Invalid value for field '(.+?)'\. Accepted values: \[(.*)\]/,
    (m) => ({
      message: `Valor inválido em ${fieldLabel(m[1])}. Aceitos: ${list(m[2])}.`,
      fields: { [baseField(m[1])]: "escolha uma das opções" },
    }),
  ],
  [
    /^Invalid value for field '(.+?)': number out of range/,
    (m) => ({
      message: `O número em ${fieldLabel(m[1])} é grande demais.`,
      fields: { [baseField(m[1])]: "número grande demais" },
    }),
  ],
  [
    /^Invalid value for field '(.+?)': expected (\w+)/,
    (m) => {
      const expected = JAVA_TYPES[m[2]] ?? "outro tipo de valor"
      return {
        message: `${fieldLabel(m[1])} precisa ser ${expected}.`,
        fields: { [baseField(m[1])]: `precisa ser ${expected}` },
      }
    },
  ],
  [/^Request body is missing/, () => ({ message: "Nenhum dado foi enviado." })],
  [/^Malformed JSON/, () => ({ message: "Os dados foram enviados num formato inválido." })],
  // 400 - parametros da URL
  [
    /^Invalid value '(.*)' for parameter '(.+?)'\. Accepted values: \[(.*)\]/,
    (m) => ({ message: `"${m[1]}" não é válido para ${fieldLabel(m[2])}. Aceitos: ${list(m[3])}.` }),
  ],
  [
    /^Invalid value '(.*)' for parameter '(.+?)': it must be a whole number/,
    (m) => ({ message: `"${m[1]}" não é um número válido para ${fieldLabel(m[2])}.` }),
  ],
  [/^Required parameter '(.+?)' is missing/, (m) => ({ message: `Faltou informar ${fieldLabel(m[1])}.` })],
  [/^Invalid sort: \w+ has no field '(.+?)'/, (m) => ({ message: `Não dá pra ordenar por "${m[1]}".` })],
  [/^Inform country or name to search record labels/, () => ({ message: "Informe o país ou o nome para buscar gravadoras." })],
  [
    /^Invalid period: from \((-?\d+)\) must be less than or equal to to \((-?\d+)\)/,
    (m) => ({ message: `Período inválido: o ano inicial (${m[1]}) precisa ser menor ou igual ao final (${m[2]}).` }),
  ],
  // o Spring normaliza a versao pedida ("3" vira "3.0.0"); mostramos como o usuario pediu
  [
    /^Unsupported API version '(.*?)'.*Supported versions: (.*)$/,
    (m) => ({ message: `A versão "${m[1].replace(/(\.0)+$/, "")}" da API não existe. Versões: ${m[2]}.` }),
  ],
  [/^Invalid X-Idempotency-Key/, () => ({ message: "O envio foi recusado por um problema interno do site. Tente de novo." })],

  // 401 / 403
  [/^Missing API key/, () => ({ message: "Pra cadastrar, editar ou excluir você precisa de uma chave de API. Gere uma no botão Chave de API." })],
  [
    /^Invalid or inactive API key/,
    () => ({ message: "Sua chave de API não vale mais (foi desativada ou a API reiniciou). Gere uma nova." }),
  ],
  [/^This operation requires an API key with role (\w+)/, (m) => ({ message: `Só uma chave ${m[1]} pode fazer isso.` })],

  // 405
  [/^Method (\w+) is not supported for this endpoint/, (m) => ({ message: `A operação ${m[1]} não é aceita nesse endereço.` })],

  // 409
  [
    /^Artist with ID: (\d+) still has albums/,
    () => ({ message: "Esse artista ainda tem álbuns. Tire ele dos álbuns ou exclua os álbuns antes." }),
  ],
  [
    /^Record label with ID: (\d+) still has albums/,
    () => ({ message: "Essa gravadora ainda tem álbuns. Troque a gravadora deles ou exclua os álbuns antes." }),
  ],
  [
    /^This album already has a track with this trackNumber/,
    () => ({ message: "Esse álbum já tem uma faixa com esse número.", fields: { trackNumber: "já existe uma faixa com esse número" } }),
  ],
  [
    /^This album already has a cover/,
    () => ({ message: "Esse álbum já tem capa. Troque a capa atual em vez de criar outra." }),
  ],
  [
    /^An artist with this name already exists/,
    () => ({ message: "Já existe um artista com esse nome.", fields: { name: "já existe um artista com esse nome" } }),
  ],
  [
    /^A genre with this name already exists/,
    () => ({ message: "Já existe um gênero com esse nome.", fields: { name: "já existe um gênero com esse nome" } }),
  ],
  [
    /^A record label with this name already exists/,
    () => ({ message: "Já existe uma gravadora com esse nome.", fields: { name: "já existe uma gravadora com esse nome" } }),
  ],
  [/^Operation conflicts with existing data/, () => ({ message: "Algum valor que precisa ser único já está em uso." })],
  [/^A request with X-Idempotency-Key '.*' is still being processed/, () => ({ message: "Esse envio ainda está sendo processado. Espere um pouco e confira." })],

  // 415 / 422 / 429 / 500
  [/^Content-Type '.*' is not supported/, () => ({ message: "A API não aceitou o formato dos dados enviados." })],
  [/^X-Idempotency-Key '.*' was already used with a different request/, () => ({ message: "Esse envio conflitou com um anterior. Tente de novo." })],
  [
    /^Too many requests: the limit is (\d+) requests per minute\. Try again in (\d+) seconds/,
    (m) => ({ message: `O limite é de ${m[1]} requisições por minuto. Tente de novo em ${m[2]} segundos.` }),
  ],
  [/^Unexpected error/, () => ({ message: STATUS[500][1] })],
]

export function translateError(
  status: number,
  detail?: string | null,
  errors?: Record<string, string> | null,
): FriendlyError {
  const [title, fallback] = statusText(status)

  const fields: Record<string, string> = {}
  for (const [path, msg] of Object.entries(errors ?? {})) {
    const key = baseField(path)
    fields[key] ??= translateFieldMessage(msg)
  }

  let message = fallback
  let known = false
  if (detail) {
    for (const [re, fn] of DETAIL_RULES) {
      const m = detail.match(re)
      if (!m) continue
      const out = fn(m)
      message = out.message
      for (const [k, v] of Object.entries(out.fields ?? {})) fields[k] ??= v
      known = true
      break
    }
  }
  return { title, message, fields, known }
}
