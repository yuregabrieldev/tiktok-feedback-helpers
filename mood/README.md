# Mood — High Performance

TanStack Start, React 19, TypeScript, Tailwind CSS v4 and reusable components. Proposal B is the definitive visual identity. This source is independent of Lovable: no editor account, editor package or hosted service is required.

## Local development

Use Node 24.22.1 (see `.node-version`; minimum 22.12) and Bun 1.4.4.

```sh
bun install --frozen-lockfile
bun run dev --host 0.0.0.0 --port 3000
```

Checks: `bun run typecheck`, `bun run lint`, `bun run test`, `bun run build` (or `bun run check` for all four). After building, `bun run start` serves the complete Node SSR output on port 3000. Set `PORT` to select another port. `bun run preview` is also available. This is an SSR application; do not deploy only `.output/public` to a static host.

`vite.config.ts` directly configures TanStack Start, React, Tailwind and the Nitro Node adapter. There is no separate managed-editor configuration. Keep the Bun lockfile; do not introduce a second package-manager lockfile.

## SEO and content approval

Copy `.env.example` to `.env.local` when configuring a future approved origin. `VITE_SITE_URL` must be the approved HTTP(S) origin without path, credentials, query or hash. No official Mood domain is assumed. Without an origin, local canonical/social references are relative; absolute sharing URLs require that setting. All values are public build settings and require a restart/rebuild.

`VITE_ALLOW_INDEXING=false` is the default. Preview pages have noindex metadata and an HTTP X-Robots-Tag; robots.txt disallows crawling and sitemap.xml is valid but empty. Only enable indexing after public content has been approved. With indexing enabled and an origin configured, sitemap includes only the six institutional/listing paths in `src/lib/seo.ts`. Admin, candidature/receipt, duplicate `/proposta-b`, dynamic example details and unknown paths remain noindex and excluded. Crawling is allowed in that mode so crawlers can read noindex; robots is not access control.

Open Graph uses the existing illustrative hero photo, explicitly described as illustrative. No scientific Article schema, credentials, results or claims are invented. Replace example content only with verified, approved material. Review image rights before production.

## Presentation-only boundaries

Routes: `/`, `/proposta-b`, `/mood`, `/fisiologia`, `/fisiologia/:slug`, `/investigacao`, `/investigacao/:slug`, `/atletas`, `/candidatura`, `/candidatura/recebida`, `/contactos`, `/admin`, `/robots.txt`, `/sitemap.xml`.

The form only validates example input, never sends or saves it. Receipt is unavailable. Admin is a public visual demonstration with disabled write actions and no real candidate data. Login and legal pages are not yet implemented. No Supabase, database, authentication or production deployment was added in phases 1–2. See `PROXIMA_FASE.md` before activating data collection.

## Browser regression checks

With the local production server running, `bun run test:browser` verifies existing routes, noindex/canonical/OG, menu/keyboard/form/filter behavior, 320/390/768/1440 layouts and axe WCAG 2.2 AA rules. Use `CHROMIUM_EXECUTABLE=/path/to/chromium` when Chromium is not at `/usr/bin/chromium`; `BASE_URL` defaults to `http://localhost:3000`. This does not install a browser or contact external services. Screenshots/results go to `artifacts/browser/` (ignored by Git).

Automated checks are not WCAG certification. Assistive-technology testing, Safari/iPhone, Firefox and real-device performance remain part of homologation. Retained Proposal A source/styles are reference components, not an alternative to the definitive Home; avoid deleting shared component dependencies merely because they are not currently rendered.
