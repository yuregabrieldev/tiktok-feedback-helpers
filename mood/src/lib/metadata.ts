import { canonicalUrl, isIndexable, siteSeo } from "./seo";

export function pageHead(title: string, description: string, path: string) {
  const fullTitle = title.startsWith("Mood") ? title : `${title} — Mood`;
  const canonical = canonicalUrl(path);
  const image = siteSeo.origin ? `${siteSeo.origin}/og-mood.jpg` : "/og-mood.jpg";
  return {
    meta: [
      { title: fullTitle },
      { name: "description", content: description },
      { name: "robots", content: isIndexable(path) ? "index, follow" : "noindex, follow" },
      { property: "og:title", content: fullTitle },
      { property: "og:description", content: description },
      { property: "og:type", content: "website" },
      { property: "og:site_name", content: "Mood" },
      { property: "og:locale", content: "pt_BR" },
      { property: "og:url", content: canonical },
      { property: "og:image", content: image },
      {
        property: "og:image:alt",
        content: "Remador em treino — imagem ilustrativa da Proposta B Mood",
      },
      { name: "twitter:card", content: "summary_large_image" },
      { name: "twitter:title", content: fullTitle },
      { name: "twitter:description", content: description },
      { name: "twitter:image", content: image },
      { name: "twitter:image:alt", content: "Remador em treino — imagem ilustrativa" },
    ],
    links: [{ rel: "canonical", href: canonical }],
  };
}
