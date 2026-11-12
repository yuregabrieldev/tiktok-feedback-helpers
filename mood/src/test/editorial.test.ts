import { describe, expect, it } from "vitest";
import { applicationMode, filterEditorial, filterPublications } from "@/lib/editorial";
import { applicationSteps, editorialItems, publications } from "@/data/editorial";
const empty = { query: "", subject: "", author: "", year: "", kind: "" };
describe("Publication rules", () => {
  it("searches publication titles", () =>
    expect(filterPublications(publications, { ...empty, query: "02" }).map((p) => p.slug)).toEqual([
      "tese-exemplo",
    ]));
  it("filters by subject", () =>
    expect(
      filterPublications(publications, { ...empty, subject: "Fisiologia" }).map((p) => p.slug),
    ).toEqual(["artigo-exemplo", "publicacao-exemplo"]));
  it("filters by author", () =>
    expect(filterPublications(publications, { ...empty, author: "Absent author" })).toEqual([]));
  it("filters by year", () =>
    expect(filterPublications(publications, { ...empty, year: "2026" })).toEqual([]));
  it("identifies theses and preprints", () =>
    expect(
      filterPublications(publications, { ...empty, kind: "Preprint" }).map((p) => p.slug),
    ).toEqual(["preprint-exemplo"]));
  it("keeps unverified sources and DOI unavailable", () =>
    expect(publications.every((p) => p.placeholder && p.sourceUrl === null && p.doi === null)).toBe(
      true,
    ));
});
describe("Educational rules", () => {
  it("combines search and category", () =>
    expect(
      filterEditorial(editorialItems, "recuperação", "Recuperação").map((p) => p.slug),
    ).toEqual(["recuperacao-exemplo"]));
});
describe("Application rules", () => {
  it("never confirms receipt without persistence", () =>
    expect(applicationMode.canConfirmReceipt).toBe(false));
  it("does not enable submission without persistence", () =>
    expect(applicationMode.persistenceEnabled).toBe(false));
  it("requires manual review between sending and decision", () =>
    expect(applicationSteps).toEqual([
      "Envio da candidatura",
      "Análise manual pela equipe",
      "Comunicação da decisão",
    ]));
});
