# Changelog

Todas as mudanças da Music Catalog API ficam registradas aqui, da mais nova para a mais antiga.

## Como as versões funcionam

A versão principal fica na URL: a v1 responde em `/v1/...` e uma v2 sairia em `/v2/...`, **com a v1 continuando no ar**.
No Swagger, cada versão aparece no seletor "Select a definition", no topo da página.

O número completo segue o [Versionamento Semântico](https://semver.org/lang/pt-BR/) (`MAJOR.MINOR.PATCH`) e aparece no Swagger e na raiz (`/`):

- **MAJOR** (`1.0.0` → `2.0.0`): a mudança quebra quem já usa a API, por exemplo quando um JSON que era aceito passa a ser recusado. Ganha rota nova (`/v2`).
- **MINOR** (`1.0.0` → `1.1.0`): acrescenta algo sem quebrar, como um endpoint novo. Continua na mesma rota (`/v1`).
- **PATCH** (`1.1.0` → `1.1.1`): só corrige um erro, sem mudar o contrato.

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
