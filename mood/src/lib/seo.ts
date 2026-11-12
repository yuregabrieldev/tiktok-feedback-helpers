/** Public build settings only. No guessed production domain or runtime Host trust. */
export type SeoSettings = { origin: string | null; allowIndexing: boolean };
export function seoSettings(origin?: string, allowIndexing?: string): SeoSettings {
  let validOrigin: string | null = null;
  if (origin?.trim()) {
    const url = new URL(origin);
    if (
      !["http:", "https:"].includes(url.protocol) ||
      url.username ||
      url.password ||
      url.pathname !== "/" ||
      url.search ||
      url.hash
    ) {
      throw new Error(
        "VITE_SITE_URL must be an HTTP(S) origin without path, credentials, query or hash.",
      );
    }
    validOrigin = url.origin;
  }
  return { origin: validOrigin, allowIndexing: allowIndexing === "true" && validOrigin !== null };
}
export const siteSeo = seoSettings(
  import.meta.env["VITE_SITE_URL"],
  import.meta.env["VITE_ALLOW_INDEXING"],
);
/** Enable indexing only after these pages contain approved public material. */
export const publicPaths = [
  "/",
  "/mood",
  "/fisiologia",
  "/investigacao",
  "/atletas",
  "/contactos",
] as const;
export function isIndexable(path: string, settings: SeoSettings = siteSeo) {
  return settings.allowIndexing && (publicPaths as readonly string[]).includes(path);
}
export function canonicalUrl(path: string, settings: SeoSettings = siteSeo) {
  const canonicalPath = path === "/proposta-b" ? "/" : path;
  return settings.origin ? new URL(canonicalPath, settings.origin).href : canonicalPath;
}
export function robotsText(settings: SeoSettings = siteSeo) {
  // Crawling remains allowed so crawlers can see noindex on demonstrative pages.
  return [
    "User-agent: *",
    settings.allowIndexing ? "Allow: /" : "Disallow: /",
    ...(settings.allowIndexing && settings.origin
      ? [`Sitemap: ${settings.origin}/sitemap.xml`]
      : []),
    "",
  ].join("\n");
}
export function sitemapXml(settings: SeoSettings = siteSeo) {
  const urls =
    settings.allowIndexing && settings.origin
      ? publicPaths
          .map(
            (path) =>
              `<url><loc>${canonicalUrl(path, settings).replaceAll("&", "&amp;").replaceAll("<", "&lt;")}</loc></url>`,
          )
          .join("")
      : "";
  return `<?xml version="1.0" encoding="UTF-8"?><urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">${urls}</urlset>`;
}
