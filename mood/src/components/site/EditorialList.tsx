import { useState } from "react";
import { Link } from "@tanstack/react-router";
import { Instagram, ArrowUpRight, Search } from "lucide-react";
import { Button } from "@/components/ui/button";
const articlePhotos: Record<string, { src: string; alt: string }> = {
  "fisiologia-exemplo": {
    src: "/fotos/3763867.jpg",
    alt: "Atleta se preparando para correr em uma pista, foto ilustrativa",
  },
  "recuperacao-exemplo": {
    src: "/fotos/7690843.jpg",
    alt: "Pessoa alongando em ambiente de treino, foto ilustrativa",
  },
  "treino-exemplo": {
    src: "/fotos/3764553.jpg",
    alt: "Atleta correndo em pista coberta, foto ilustrativa",
  },
};
import { editorialItems, subjects } from "@/data/editorial";
import { filterEditorial } from "@/lib/editorial";
export function EditorialList({ compact = false }: { compact?: boolean }) {
  const [query, setQuery] = useState("");
  const [subject, setSubject] = useState("");
  const Heading = compact ? "h3" : "h2";
  const items = filterEditorial(editorialItems, query, subject);
  return (
    <div className="site-editorial">
      <div className="site-editorial-controls">
        {!compact && (
          <label className="site-search">
            <Search size={18} />
            <input
              aria-label="Pesquisar conteúdos"
              placeholder="Pesquisar conteúdos"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
            />
          </label>
        )}
        <div className="b-topic-row" role="group" aria-label="Filtrar conteúdos por assunto">
          {["", ...subjects].map((s) => (
            <Button
              key={s}
              variant="ghost"
              className={`b-topic ${subject === s ? "b-topic-active" : ""}`}
              aria-pressed={subject === s}
              onClick={() => setSubject(s)}
            >
              {s || "Todos"}
            </Button>
          ))}
        </div>
      </div>
      <div className="site-editorial-grid" aria-live="polite">
        {items.map((item) => (
          <article className="site-editorial-item" key={item.slug}>
            {articlePhotos[item.slug] && (
              <img
                src={articlePhotos[item.slug]?.src}
                alt={articlePhotos[item.slug]?.alt}
                width={1280}
                height={800}
                loading="lazy"
              />
            )}
            <div className="site-item-copy">
              <p className="b-kicker text-forest-light flex items-center gap-2">
                {item.format === "Instagram" && <Instagram size={17} />} {item.format} /{" "}
                {item.subject}
              </p>
              <Heading>
                <Link to="/fisiologia/$slug" params={{ slug: item.slug }}>
                  {item.title}
                </Link>
              </Heading>
              <p>{item.summary}</p>
              <div className="site-item-meta">
                <span>{item.author}</span>
                <span>12 outubro 2024</span>
                <span>{item.origin}</span>
              </div>
              <Button asChild variant="link" className="b-text-link">
                <Link to="/fisiologia/$slug" params={{ slug: item.slug }}>
                  Ler conteúdo <ArrowUpRight />
                </Link>
              </Button>
            </div>
          </article>
        ))}
      </div>
      {items.length === 0 && (
        <div className="site-empty" role="status">
          <Heading>Nenhum conteúdo encontrado</Heading>
          <Button
            variant="link"
            className="b-text-link"
            onClick={() => {
              setQuery("");
              setSubject("");
            }}
          >
            Limpar pesquisa
          </Button>
        </div>
      )}
      {compact && (
        <Button asChild variant="outline" className="b-nav-cta site-explore">
          <Link to="/fisiologia">
            Explorar conteúdos <ArrowUpRight />
          </Link>
        </Button>
      )}
    </div>
  );
}
