# Discoteca (front da Music Catalog API)

React + Vite + Tailwind + shadcn/ui. Lista álbuns com capa, artistas, faixas, gêneros e gravadoras, com cadastro, edição e exclusão (usando a chave de API).

## Rodar local

```bash
cd frontend
npm install
npm run dev        # http://localhost:5173
```

Por padrão chama a API em `http://localhost:8080`. Para usar outra, crie um `.env` (veja o `.env.example`):

```
VITE_API_URL=https://api-spring-boot-emzp.onrender.com
```

## Deploy no Render (Static Site)

1. **New → Static Site**, mesmo repositório.
2. **Root Directory:** `frontend`
3. **Build Command:** `npm ci && npm run build`
4. **Publish Directory:** `dist`
5. **Environment:** `VITE_API_URL` = endereço da API (sem `/` no final).
6. **Redirects/Rewrites:** source `/*`, destination `/index.html`, action **Rewrite** (senão recarregar em `/artistas` dá 404).
7. Na **API**, libere o endereço do front no CORS: variável `API_CORS_ALLOWED_ORIGINS` com todas as origens separadas por vírgula, por exemplo
   `http://localhost:5173,https://api-spring-boot-emzp.onrender.com,https://SEU-FRONT.onrender.com`
