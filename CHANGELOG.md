# Changelog

Todas as mudanças da Music Catalog API ficam registradas aqui, da mais nova para a mais antiga.

## Como as versões funcionam

São dois números diferentes:

- **Versão do produto** (`2.0.0`): muda a cada entrega, segue o [Versionamento Semântico](https://semver.org/lang/pt-BR/) e aparece no Swagger e na raiz (`/`).
  - **MAJOR** (`1.0.0` → `2.0.0`): a mudança quebra quem já usa a API, por exemplo uma rota que muda de endereço.
  - **MINOR** (`2.0.0` → `2.1.0`): acrescenta algo sem quebrar, como um endpoint novo.
  - **PATCH** (`2.1.0` → `2.1.1`): só corrige um erro, sem mudar o contrato.
- **Versão de um endpoint** (header `X-API-Version`, valores `1` e `2`): quando o formato de um endpoint precisa mudar, a versão nova é criada **ao lado** da antiga, no mesmo endereço, e o cliente escolhe qual quer pelo header. Sem o header, a API responde a versão 1. No Swagger, cada versão aparece no seletor "Select a definition", no topo da página.

## [2.0.1] - 2026-11-02

É uma versão PATCH: corrige um erro sem mudar as rotas nem os formatos de resposta.

### Corrigido

- **Chave de API na idempotência:** repetir o `POST /api-keys` com a mesma `X-Idempotency-Key` devolvia a chave completa de novo, porque a resposta guardada incluía o campo `apiKey`. Agora a resposta guardada sai sem esse campo: a repetição continua sem criar outra chave, a chave completa só aparece na primeira resposta e a API volta a guardar apenas o hash dela.

### Adicionado

- **Coleção do Postman** (`postman/`) com todos os endpoints e as demonstrações de paginação, versionamento, idempotência, chave de API, limite de requisições, CORS e erros.
- **Teste automatizado** da repetição do `POST /api-keys` (`AdvancedFeaturesTests`).

## [2.0.0] - 2026-11-02

Entrega da fase 2. É uma versão MAJOR porque as rotas perderam o prefixo `/v1` (a versão passou para o header `X-API-Version`) e porque criar, alterar e excluir passou a exigir chave de API.

### Mudanças incompatíveis

- **Rotas sem `/v1`:** `/v1/artists` voltou a ser `/artists`, e assim por diante. A versão agora vai no header `X-API-Version`.
- **Escrita exige chave:** `POST`, `PUT` e `DELETE` do catálogo respondem 401 sem uma chave válida no header `X-API-Key`. Ler continua livre.

### Adicionado

- **Chaves de API (`X-API-Key`):** `POST /api-keys` gera uma chave de nível USER; `GET` e `DELETE /api-keys/me` mostram e apagam a própria chave; `GET /api-keys`, `GET`, `PATCH` e `DELETE /api-keys/{id}` gerenciam todas as chaves e exigem nível ADMIN. A API guarda só o hash da chave. A chave ADMIN inicial vem da variável de ambiente `API_ADMIN_KEY`.
- **Idempotência (`X-Idempotency-Key`):** em qualquer `POST`, repetir a mesma chave devolve a resposta da primeira vez (header `X-Idempotency-Replayed: true`) sem criar outro registro. A mesma chave com outra requisição responde 422, e uma requisição ainda em andamento responde 409. A chave vale por 24 horas.
- **Limite de requisições:** 60 por minuto sem chave e 120 com chave, por cliente. Toda resposta traz `X-RateLimit-Limit`, `X-RateLimit-Remaining` e `X-RateLimit-Reset`; acima do limite, a resposta é 429 com `Retry-After`.
- **Versão 2 das faixas (`X-API-Version: 2`):** `GET /tracks`, `GET /tracks/{id}` e `GET /tracks/album/{albumId}` trazem a duração formatada (`"duration": "2:39"`), o álbum resumido e os nomes dos convidados. A versão 1 continua igual.
- **Validação de parâmetros:** `from` e `to` de `/albums/released` precisam estar entre 1900 e 2100.
- **Erros novos com o motivo:** 401 (com `WWW-Authenticate`), 403, 405 (com `Allow`), 415, 422, 429, versão inexistente (400) e rota inexistente (404).
- **Testes automatizados** dos recursos da fase 2 (`AdvancedFeaturesTests` e `RateLimitTests`).

### Alterado

- **CORS:** só as origens listadas em `api.cors.allowed-origins` podem chamar a API pelo navegador (antes era qualquer origem). Os headers da API (`X-API-Key`, `X-API-Version`, `X-Idempotency-Key`) foram liberados, e os de resposta (limite, versão, idempotência) ficam visíveis para o site.
- **Camadas:** o código foi separado em pacotes (`controller`, `service`, `repository`, `model`, `dto`, `assembler`, `exception`, `config`, `web`), e as regras de negócio saíram dos controllers para os services.
- **Swagger:** grupos por versão, botão Authorize para a chave, e cada endpoint documenta automaticamente os headers e os erros de chave, idempotência e limite.
- **Termos de serviço:** explicam as chaves de API, o limite de requisições e o versionamento por header.

## [1.0.1] - 2026-11-02

É uma versão PATCH: as rotas da API (`/v1/...`) e as respostas não mudaram.

### Alterado

- Os termos de serviço (`/terms.html`) passaram a citar pelo nome quem mantém a API e o nome completo do curso.

## [1.0.0] - 2026-11-02

Primeira versão publicada. Todas as rotas começam com `/v1`: `/v1/artists`, `/v1/albums`, `/v1/tracks`, `/v1/covers`, `/v1/genres` e `/v1/labels`.

### Recursos

- CRUD paginado com HATEOAS (HAL) de artistas, álbuns, faixas, capas, gêneros e gravadoras.
- Consultas personalizadas: busca por nome ou título, filtro por tipo, álbuns por período, discografia por artista, gênero ou gravadora, faixas de um álbum, participações especiais e capa de um álbum.

### Validação e erros

- Cada campo do JSON exige o tipo certo: texto não aceita número nem `true`/`false`, inteiro não aceita texto nem decimal, e enum só aceita o nome do valor (ex.: `"BAND"`).
- O `id` enviado no corpo do `POST` é ignorado; quem gera o id é o banco.
- Todo erro responde com o motivo: campo ou parâmetro inválido, id que não é número, registro não encontrado, valor único repetido, `sort` com campo inexistente ou período com `from` maior que `to`.

### Documentação

- Swagger com os erros reais de cada endpoint, explicando o motivo e mostrando um exemplo da resposta.
- Os campos informam se o limite é de caracteres (texto) ou de valor (número).
- Termos de serviço em `/terms.html`, licença MIT em `/license.txt` e link para o site do TSI (`https://www.sp.senac.br/`).
