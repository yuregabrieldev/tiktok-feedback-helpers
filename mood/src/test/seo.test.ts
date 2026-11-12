import { describe, expect, it } from "vitest";
import { canonicalUrl, isIndexable, robotsText, seoSettings, sitemapXml } from "@/lib/seo";
import { pageHead } from "@/lib/metadata";
const approved = seoSettings("https://example.invalid", "true");
describe("SEO publication boundaries", () => {
  it("keeps previews and missing origins out of search", () => {
    expect(seoSettings(undefined, "true").allowIndexing).toBe(false);
    expect(robotsText(seoSettings())).toContain("Disallow: /");
    expect(sitemapXml(seoSettings())).not.toContain("<url>");
  });
  it("never indexes administrative, receipt or demonstrative detail pages", () => {
    for (const path of [
      "/admin",
      "/admin/login",
      "/candidatura",
      "/candidatura/recebida",
      "/proposta-b",
      "/fisiologia/fisiologia-exemplo",
      "/investigacao/tese-exemplo",
      "/not-found",
    ]) {
      expect(isIndexable(path, approved)).toBe(false);
      expect(sitemapXml(approved)).not.toContain(`<loc>https://example.invalid${path}</loc>`);
    }
  });
  it("uses the approved origin consistently and canonicalizes the duplicate home", () => {
    expect(canonicalUrl("/proposta-b", approved)).toBe("https://example.invalid/");
    expect(robotsText(approved)).toContain("Sitemap: https://example.invalid/sitemap.xml");
    expect(sitemapXml(approved)).toContain("<loc>https://example.invalid/mood</loc>");
  });
  it("rejects malformed or unsafe site origins", () => {
    for (const origin of [
      "javascript:alert(1)",
      "https://example.invalid/path",
      "https://user:pass@example.invalid",
      "https://example.invalid/?q=x",
    ])
      expect(() => seoSettings(origin, "true")).toThrow();
  });
  it("does not duplicate the brand or invent publication metadata", () => {
    const head = pageHead("Mood — High Performance", "Pré-visualização institucional.", "/");
    expect(head.meta[0]).toEqual({ title: "Mood — High Performance" });
    expect(head.meta).toContainEqual({ property: "og:type", content: "website" });
    expect(head.links).toHaveLength(1);
  });
});
