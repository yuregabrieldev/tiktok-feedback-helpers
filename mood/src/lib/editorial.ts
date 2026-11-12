import type { EditorialItem, Publication } from "@/data/editorial";
export const applicationMode = { persistenceEnabled: false, canConfirmReceipt: false } as const;
export function filterPublications(
  items: Publication[],
  filters: { query: string; subject: string; author: string; year: string; kind: string },
) {
  const query = filters.query.trim().toLocaleLowerCase("pt");
  return items.filter(
    (p) =>
      (!query || p.title.toLocaleLowerCase("pt").includes(query)) &&
      (!filters.subject || p.subject === filters.subject) &&
      (!filters.author || p.authors.includes(filters.author)) &&
      (!filters.year || (p.year === null ? "pending" : String(p.year)) === filters.year) &&
      (!filters.kind || p.kind === filters.kind),
  );
}
export function filterEditorial(items: EditorialItem[], query: string, subject: string) {
  return items.filter(
    (p) =>
      (!query.trim() ||
        p.title.toLocaleLowerCase("pt").includes(query.trim().toLocaleLowerCase("pt"))) &&
      (!subject || p.subject === subject),
  );
}
