# MoniBank Frontend

Operator dashboard built with Vite, React JavaScript, Tailwind CSS and TanStack Query.

## Start locally

```bash
npm install
cp .env.example .env
npm run dev
```

The Spring backend is expected at `http://localhost:8080`. During local
development Vite proxies `/api` calls there, so no frontend CORS workaround is
needed.

## Logo

The current brand block is a temporary text fallback. Put the final transparent logo in `public/monibank-logo.png` and replace `Brand.jsx` when the asset is available.
