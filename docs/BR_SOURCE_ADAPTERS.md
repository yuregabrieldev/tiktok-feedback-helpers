# Harvester Browser ↔ Vista Stream: Brazilian source adapters

## What's shipped now

- **Harvester 2.7**: observes the video URL already detected by WebView, and infers optional PT-BR audio/dub/sub, quality, source origin, season and episode only from labels already present. It makes **no network request** to infer metadata.
- **Vista 0.8 development**: the separate app parses those fields, lets the user correct them and saves **only after explicit confirmation**, with video source labelled **Browser** on the selected movie/episode.
- Matching movie/episode identities uses the existing Cinemeta/AniList catalog and selected identity, not a fuzzy URL title alone.
- Chromecast is handled by Vista's existing player; never by browser video scanners.

## What these builds do not contain

CloudStream `.cs3`/`.jar` extensions **cannot run** directly in Vista: the current Marketplace lists their metadata as incompatible. Installing remote executable plugins, downloading stream URLs from arbitrary Brazilian sites, or integrating the LietRepo/AnimeFire/PobreFlix extractors are **not implemented** here.

The following are *architecture references, not preinstalled providers*:
- LietRepo: https://github.com/lawlietbr/lietrepo — individual CloudStream Kotlin sources, including `AnimeFire` and `PobreFlix` directories.
- SaimuelRepo: https://github.com/saimuelbr/saimuelrepo — CloudStream plugin manifests.
- Yūzōnō Anime Extensions: https://github.com/yuzono/anime-extensions — AniYomi/Anikku metadata, episode identity, PT-BR provider interfaces.
- GuIndex: https://github.com/GuickerZ/guindex — Stremio-style title index matching; torrents/debrid are not a browser extractor.

Review license **per project and module**, dependencies, and distribution permission before adapting any code. Finding a GitHub repo or a module folder does not establish reliable playback or rights to a particular video.

## Next implementation unit: declarative SourceAdapter v1

Allow users to **add, remove, enable or disable** trusted, reviewed providers in the Vista source-management UI; avoid downloading arbitrary executable JAR/DEX into the app.

Recommended manifest fields:

```json
{
  "schemaVersion": 1,
  "id": "example.authorized.ptbr",
  "name": "Example PT-BR Provider",
  "language": "pt-BR",
  "mediaTypes": ["movie", "series", "anime"],
  "capabilities": ["search", "metadata", "episode-list", "sources"],
  "origin": "https://media.example.org",
  "engine": {
    "type": "declarative-http",
    "apiVersion": 1
  }
}
```

A trusted adapter responds to four typed methods:

1. `search(query, locale)` → candidate title, localized/original names, year, provider ID and stable external identifiers (IMDb/TMDB/AniList).
2. `details(id)` → title metadata plus language/season/episode numbering.
3. `episodes(seriesId)` → ordered season/episode tuples. Anime absolute episode numbering must be mapped explicitly.
4. `sources(contentId, season?, episode?)` → already-authorized HLS/DASH/MP4 stream URLs, declared audio and subtitle tracks, expiry, and provenance. Never assume PT-BR from the domain.

### Network and security constraints

- Opt-in provider registration and per-domain allowlists; do not browse arbitrary redirect targets on behalf of plugins.
- Default to WebView **passive observation**. Allow explicit authorized API calls with a bounded per-host budget, concurrency limits, exponential backoff, and immediate stop on 429/5xx/Cloudflare errors.
- Never export browser session cookies, Authorization headers, private tokens, or personal data to unrelated apps/plugins.
- Don't attempt to defeat DRM, signed URLs, IP binding, paywalls, or access restrictions.
- Store source availability and expiry, mark temporary URLs as temporary.
- Sanitize all incoming manifest URLs and trim response sizes.
- Keep separate app signing and IPC intents; browser playback **does not imply saving**.
- Ensure legitimate authorized media is playable on Google Cast **by the receiver**, not only on the phone.

## Validation

Test adapter logic against a controlled mock HTTP server and licensed/public test streams. Unit-test the naming variants `S01E05`, `T1 EP5`, `1x05`, `pt-BR`, `Dual Áudio`, HLS quality and unknown labels. Use an actual Android phone and Cast receiver for smoke tests; successful compilation does not prove receiver playback.

## Interaction

1. Harvester discovers video → user's Play opens Vista's player without saving.
2. Harvester `+` → Vista import activity opens and suggests a catalog match.
3. User selects/edits title, series/episode, audio, quality, subtitles.
4. User confirms → both independent apps may persist their own library entry; Vista associates to exact catalog identity.
5. Vista displays **Browser** alongside eligible, authorized source adapters for the same movie/episode.
