import { useState } from "react";
import { ArrowUpRight, RotateCcw } from "lucide-react";
import { Button } from "@/components/ui/button";
import { papers } from "./content";

export function LibraryA() {
  const [filters, setFilters] = useState({ subject: "", author: "", year: "" });
  return (
    <section id="investigacao-a" className="a-section">
      <div className="a-wrap">
        <div className="a-section-title">
          <div>
            <p className="a-label text-forest">04 / Pesquisa</p>
            <h2>Biblioteca científica</h2>
          </div>
          <p className="a-note">
            [Placeholder] Critérios de seleção das publicações a aprovar pela equipe científica.
          </p>
        </div>
        <div className="a-filters">
          <p className="a-label">Filtrar por</p>
          {(
            [
              { key: "subject", label: "Assunto" },
              { key: "author", label: "Autor" },
              { key: "year", label: "Ano" },
            ] as const
          ).map(({ key, label }) => (
            <label key={key}>
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
                      : "Todos os anos"}
                </option>
                <option value="pending">Por definir</option>
              </select>
            </label>
          ))}
          <Button
            variant="ghost"
            size="icon"
            aria-label="Limpar filtros"
            title="Limpar filtros"
            disabled={!Object.values(filters).some(Boolean)}
            onClick={() => setFilters({ subject: "", author: "", year: "" })}
          >
            <RotateCcw />
          </Button>
        </div>
        <p className="a-table-note">
          [Placeholder] Publicações, metadados e links originais por validar.
        </p>
        <table className="a-table">
          <thead>
            <tr>
              <th>Ano</th>
              <th>Trabalho / assunto</th>
              <th>Autores</th>
              <th>Publicação</th>
              <th>Original</th>
            </tr>
          </thead>
          <tbody>
            {papers.map((p) => (
              <tr key={p.title}>
                <td data-label="Ano">{p.year}</td>
                <td>
                  <strong>{p.title}</strong>
                  <small>[Placeholder] Assunto por definir</small>
                </td>
                <td data-label="Autores">{p.authors}</td>
                <td data-label="Publicação">
                  <em>{p.journal}</em>
                </td>
                <td>
                  <span className="a-pending">
                    Fonte pendente <ArrowUpRight size={15} />
                  </span>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}
