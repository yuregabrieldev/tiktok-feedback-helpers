import { useState } from "react";
import { Link } from "@tanstack/react-router";
import { ArrowUpRight, Search, RotateCcw } from "lucide-react";
import { Button } from "@/components/ui/button";
import { publications, subjects } from "@/data/editorial";
import { filterPublications } from "@/lib/editorial";
export function ScientificLibrary({
  standalone = false,
  loading = false,
}: {
  standalone?: boolean;
  loading?: boolean;
}) {
  const EmptyHeading = standalone ? "h2" : "h3";
  const [filters, setFilters] = useState({
    query: "",
    subject: "",
    author: "",
    year: "",
    kind: "",
  });
  const items = filterPublications(publications, filters);
  const reset = () => setFilters({ query: "", subject: "", author: "", year: "", kind: "" });
  return (
    <section id="investigacao-b" className="b-library b-section bg-ivory text-charcoal">
      <div className="b-wrap">
        {!standalone && (
          <div className="b-section-heading">
            <div>
              <p className="b-kicker text-forest">04 / Pesquisa</p>
              <h2>
                Biblioteca
                <br className="hidden md:block" /> científica.
              </h2>
            </div>
            <p className="b-section-note">
              Conhecimento em movimento: uma seleção para explorar as perguntas que orientam a nossa
              visão.
            </p>
          </div>
        )}
        <label className="site-search site-library-search">
          <Search size={18} />
          <input
            aria-label="Pesquisar publicações por título"
            placeholder="Pesquisar por título"
            value={filters.query}
            onChange={(e) => setFilters({ ...filters, query: e.target.value })}
          />
        </label>
        <div className="b-library-filters">
          <span className="b-kicker">Filtrar por</span>
          {(
            [
              { key: "subject", label: "Assunto", options: subjects },
              {
                key: "author",
                label: "Autor",
                options: [...new Set(publications.flatMap((p) => p.authors))],
              },
              { key: "year", label: "Ano", options: ["2024"] },
              { key: "kind", label: "Tipo", options: ["Artigo", "Tese", "Preprint"] },
            ] as const
          ).map(({ key, label, options }) => (
            <label key={key} className="b-filter">
              <span>{label}</span>
              <select
                aria-label={`Filtrar por ${label.toLowerCase()}`}
                value={filters[key]}
                onChange={(e) => setFilters({ ...filters, [key]: e.target.value })}
              >
                <option value="">
                  {key === "subject"
                    ? "Todos os assuntos"
                    : key === "author"
                      ? "Todos os autores"
                      : key === "year"
                        ? "Todos os anos"
                        : "Todos os tipos"}
                </option>
                {options.map((o) => (
                  <option key={o} value={o}>
                    {o}
                  </option>
                ))}
              </select>
            </label>
          ))}
          <Button
            variant="ghost"
            size="icon"
            className="b-filter-reset"
            title="Limpar filtros"
            aria-label="Limpar filtros"
            disabled={!Object.values(filters).some(Boolean)}
            onClick={reset}
          >
            <RotateCcw />
          </Button>
        </div>
        <p className="b-library-caption" role="status" aria-live="polite">
          {items.length} publicações · Explore por assunto, autor ou tipo
        </p>
        <div className="b-table-wrap" aria-busy={loading}>
          {loading ? (
            <div className="site-skeleton" role="status">
              Carregando publicações…
            </div>
          ) : items.length === 0 ? (
            <div className="site-empty" role="status">
              <EmptyHeading>Nenhuma publicação encontrada</EmptyHeading>
              <Button variant="link" onClick={reset}>
                Limpar pesquisa e filtros
              </Button>
            </div>
          ) : (
            <table className="b-paper-table">
              <caption className="sr-only">Biblioteca de publicações para demonstração</caption>
              <thead>
                <tr>
                  <th scope="col">Ano</th>
                  <th scope="col">Trabalho / assunto</th>
                  <th scope="col">Autores</th>
                  <th scope="col">Publicação / tipo</th>
                  <th scope="col">Original</th>
                </tr>
              </thead>
              <tbody>
                {items.map((p) => (
                  <tr key={p.slug}>
                    <td data-label="Ano">{p.year ?? "—"}</td>
                    <td>
                      <strong>
                        <Link to="/investigacao/$slug" params={{ slug: p.slug }}>
                          {p.title}
                        </Link>
                      </strong>
                      <span className="b-paper-subject">{p.subject}</span>
                    </td>
                    <td data-label="Autores">{p.authors.join(", ")}</td>
                    <td data-label="Publicação">
                      <i>{p.journal}</i>
                      <small className="site-paper-kind">{p.kind}</small>
                    </td>
                    <td>
                      {p.sourceUrl ? (
                        <a
                          href={p.sourceUrl}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="b-source-pending"
                        >
                          Original <ArrowUpRight size={16} />
                        </a>
                      ) : (
                        <span className="b-source-pending">Consulta indisponível</span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
        {!standalone && (
          <Button asChild variant="link" className="site-library-link">
            <Link to="/investigacao">
              Biblioteca completa <ArrowUpRight />
            </Link>
          </Button>
        )}
      </div>
    </section>
  );
}
