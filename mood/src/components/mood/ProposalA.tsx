import { useEffect, useRef, useState } from "react";
import {
  ArrowUpRight,
  ArrowDown,
  Menu,
  X,
  Image as ImageIcon,
  Play,
  Instagram,
  Check,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import heroA from "@/assets/hero-a.jpg";
import cyclist from "@/assets/athlete-cyclist.jpg";
import { panels, testimonials } from "./content";
import { LibraryA } from "./LibraryA";

const links = [
  { label: "Mood", id: "mood-a" },
  { label: "Fisiologia", id: "fisiologia-a" },
  { label: "Pesquisa", id: "investigacao-a" },
  { label: "Atletas", id: "atletas-a" },
];
function Media({ kind = "produto" }: { kind?: "produto" | "foto" | "video" }) {
  return (
    <div className={`a-media a-media-${kind}`}>
      {kind === "video" ? (
        <Play size={28} aria-hidden="true" />
      ) : (
        <ImageIcon size={28} aria-hidden="true" />
      )}
      <span className="a-label">[Placeholder]</span>
      <p>
        {kind === "produto"
          ? "Fotografia real do produto"
          : kind === "foto"
            ? "Fotografia autorizada do atleta"
            : "Vídeo autorizado do atleta"}
      </p>
      <small>A fornecer {kind === "produto" ? "pela equipe Mood" : "· Não disponível"}</small>
    </div>
  );
}
export function ProposalA() {
  const [open, setOpen] = useState(false);
  const [topic, setTopic] = useState("Todos");
  const menu = useRef<HTMLButtonElement>(null);
  useEffect(() => {
    if (!open) return;
    const close = (e: KeyboardEvent) => {
      if (e.key === "Escape") {
        setOpen(false);
        menu.current?.focus();
      }
    };
    document.addEventListener("keydown", close);
    return () => document.removeEventListener("keydown", close);
  }, [open]);
  return (
    <div className="mood-a bg-ivory text-charcoal">
      <a className="a-skip" href="#mood-a">
        Saltar para o conteúdo
      </a>
      <header className="a-header">
        <div className="a-wrap a-header-row">
          <a href="#inicio-a" className="a-logo">
            mood<span className="text-forest">.</span>
          </a>
          <nav className="hidden md:flex" aria-label="Navegação principal">
            {links.map((n) => (
              <a key={n.id} href={`#${n.id}`}>
                {n.label}
              </a>
            ))}
            <Button asChild className="a-button">
              <a href="#candidatura">
                Quero me candidatar <ArrowUpRight />
              </a>
            </Button>
          </nav>
          <Button
            ref={menu}
            variant="ghost"
            size="icon"
            className="a-menu-button md:hidden"
            aria-label={open ? "Fechar menu" : "Abrir menu"}
            aria-expanded={open}
            aria-controls="a-mobile-nav"
            onClick={() => setOpen(!open)}
          >
            {open ? <X /> : <Menu />}
          </Button>
        </div>
        {open && (
          <nav id="a-mobile-nav" className="a-mobile-nav md:hidden" aria-label="Navegação móvel">
            {links.map((n, i) => (
              <a key={n.id} href={`#${n.id}`} onClick={() => setOpen(false)}>
                <span className="a-label">0{i + 1}</span>
                {n.label}
                <ArrowUpRight size={20} />
              </a>
            ))}
            <Button asChild className="a-button">
              <a href="#candidatura" onClick={() => setOpen(false)}>
                Quero me candidatar <ArrowUpRight />
              </a>
            </Button>
          </nav>
        )}
      </header>
      <main>
        <section id="inicio-a" className="a-hero">
          <div className="a-wrap a-hero-grid">
            <div>
              <p className="a-label text-forest">Programa para atletas de alta performance</p>
              <h1>
                Ciência ao serviço
                <br />
                do <em>rendimento.</em>
              </h1>
            </div>
            <div className="a-hero-copy">
              <p>
                [Placeholder] Apresentação curta do projeto Mood: propósito, público e abordagem
                científica, a aprovar pela equipe.
              </p>
              <div className="a-hero-actions">
                <Button asChild className="a-button">
                  <a href="#candidatura">
                    Quero me candidatar <ArrowUpRight />
                  </a>
                </Button>
                <Button asChild variant="link" className="a-link">
                  <a href="#sistema-a">
                    Conhecer o Mood <ArrowDown />
                  </a>
                </Button>
              </div>
              <small>Candidaturas sujeitas a aprovação manual.</small>
            </div>
          </div>
          <figure className="a-wrap a-hero-image">
            <img
              src={heroA}
              alt="Velocista nos blocos de partida ao amanhecer — fotografia ilustrativa"
              width={1600}
              height={1104}
              fetchPriority="high"
            />
            <figcaption>
              <span>Fotografia ilustrativa · A substituir por imagem real</span>
              <span>N.º 01 — Home</span>
            </figcaption>
          </figure>
        </section>
        <section id="mood-a" className="a-section">
          <div className="a-wrap a-institution">
            <p className="a-label text-forest">01 / O Mood</p>
            <div>
              <h2>
                [Placeholder] Declaração institucional do Mood — missão e visão, escrita pela
                equipe, numa frase forte e editorial.
              </h2>
              <div className="a-institution-paragraphs">
                <p>[Placeholder] Parágrafo sobre a origem do projeto e a equipe responsável.</p>
                <p>
                  [Placeholder] Parágrafo sobre a relação com atletas e com a comunidade científica.
                </p>
              </div>
            </div>
          </div>
        </section>
        <section id="sistema-a" className="a-section a-product">
          <div className="a-wrap">
            <div className="a-section-title">
              <div>
                <p className="a-label text-forest">02 / Produto</p>
                <h2>Sistema Mood</h2>
              </div>
              <p className="a-note">
                [Placeholder] Apresentação do produto e descrições aprovadas a fornecer pela equipe.
              </p>
            </div>
            <div className="a-product-grid">
              <article className="a-product-main">
                <div className="a-panel-head">
                  <span>01</span>
                  <p className="a-label">Produto / Visão geral</p>
                </div>
                <Media />
                <div className="a-product-description">
                  <h3>{panels[0]?.title}</h3>
                  <p>{panels[0]?.body}</p>
                </div>
              </article>
              <div className="a-product-side">
                <article>
                  <div className="a-panel-head">
                    <span>02</span>
                    <p className="a-label">Conteúdo por aprovar</p>
                  </div>
                  <h3>{panels[1]?.title}</h3>
                  <p>{panels[1]?.body}</p>
                  <div className="a-detail-placeholder">
                    <ImageIcon size={20} />
                    <span>[Placeholder] Imagem de detalhe do produto</span>
                  </div>
                </article>
                <article className="a-product-third">
                  <span>03</span>
                  <div>
                    <h3>{panels[2]?.title}</h3>
                    <p>{panels[2]?.body}</p>
                  </div>
                </article>
              </div>
            </div>
          </div>
        </section>
        <section id="fisiologia-a" className="a-section a-education bg-forest text-ivory">
          <div className="a-wrap">
            <div className="a-section-title">
              <div>
                <p className="a-label text-forest-light">03 / Conteúdos</p>
                <h2>Fisiologia</h2>
              </div>
              <p className="a-note">
                [Placeholder] Conteúdos educativos e enquadramento científico a aprovar.
              </p>
            </div>
            <div className="a-topics" role="group" aria-label="Filtrar conteúdos por assunto">
              {["Todos", "Fisiologia", "Recuperação", "Treino"].map((t) => (
                <Button
                  key={t}
                  variant="ghost"
                  className="a-topic"
                  aria-pressed={topic === t}
                  onClick={() => setTopic(t)}
                >
                  {t}
                </Button>
              ))}
            </div>
            <div className="a-education-grid">
              <figure>
                <img
                  src={cyclist}
                  alt="Ciclista em subida ao pôr do sol — fotografia ilustrativa"
                  loading="lazy"
                  width={1280}
                  height={1024}
                />
                <figcaption>
                  <p className="a-label text-forest-light">
                    Destaque / {topic === "Todos" ? "Fisiologia" : topic}
                  </p>
                  <h3>[Placeholder] Título do conteúdo educativo</h3>
                  <small>Imagem ilustrativa · Conteúdo a definir</small>
                </figcaption>
              </figure>
              <div className="a-education-list" aria-live="polite">
                <article>
                  <p className="a-label text-forest-light flex items-center gap-2">
                    <Instagram size={18} />
                    Instagram / Educativo
                  </p>
                  <h3>
                    [Placeholder] Publicação sobre{" "}
                    {topic === "Todos" ? "fisiologia" : topic.toLowerCase()}
                  </h3>
                  <p>Conteúdo e link da publicação original a fornecer.</p>
                  <span className="a-pending">
                    Publicação pendente <ArrowUpRight size={16} />
                  </span>
                </article>
                <article>
                  <p className="a-label text-forest-light">Leitura / Artigo científico</p>
                  <h3>
                    [Placeholder] Artigo sobre{" "}
                    {topic === "Todos" ? "o assunto em destaque" : topic.toLowerCase()}
                  </h3>
                  <p>Título, autores e enquadramento científico por validar.</p>
                  <Button asChild variant="link" className="a-link">
                    <a href="#investigacao-a">
                      Consultar biblioteca <ArrowUpRight />
                    </a>
                  </Button>
                </article>
              </div>
            </div>
          </div>
        </section>
        <LibraryA />
        <section id="atletas-a" className="a-section a-athletes">
          <div className="a-wrap">
            <div className="a-section-title">
              <div>
                <p className="a-label text-forest">05 / Atletas</p>
                <h2>Vozes de quem compete</h2>
              </div>
            </div>
            <div className="a-testimonials">
              <figure className="a-testimonial-main">
                <Media kind="foto" />
                <div>
                  <span className="a-quote" aria-hidden="true">
                    “
                  </span>
                  <blockquote>{testimonials[0]?.quote}</blockquote>
                  <figcaption>
                    <strong>{testimonials[0]?.name}</strong>
                    <small>{testimonials[0]?.sport} · [Placeholder]</small>
                  </figcaption>
                </div>
              </figure>
              <figure className="a-testimonial-video">
                <Media kind="video" />
                <blockquote>{testimonials[1]?.quote}</blockquote>
                <figcaption>
                  <strong>{testimonials[1]?.name}</strong>
                  <small>{testimonials[1]?.sport} · [Placeholder]</small>
                </figcaption>
              </figure>
            </div>
          </div>
        </section>
        <section id="candidatura" className="a-section a-application bg-charcoal text-ivory">
          <div className="a-wrap a-application-grid">
            <div>
              <p className="a-label text-forest-light">06 / Candidaturas</p>
              <h2>
                Faça parte
                <br />
                do Mood.
              </h2>
              <p className="a-note">
                [Placeholder] Critérios de elegibilidade e requisitos a fornecer.
              </p>
            </div>
            <div className="a-approval">
              <p className="a-label text-forest-light flex items-center gap-2">
                <Check size={18} />
                Aprovação manual
              </p>
              <p>
                As candidaturas são analisadas pela equipe Mood. O envio de uma candidatura não
                garante a admissão.
              </p>
              <ol>
                <li>
                  <span>01</span>Envio da candidatura
                </li>
                <li>
                  <span>02</span>Análise manual pela equipe
                </li>
                <li>
                  <span>03</span>Comunicação da decisão
                </li>
              </ol>
              <Button disabled className="a-application-button">
                Iniciar candidatura <ArrowUpRight />
              </Button>
              <small>[Placeholder] Formulário a disponibilizar.</small>
            </div>
          </div>
        </section>
      </main>
      <footer className="a-footer bg-charcoal text-ivory">
        <div className="a-wrap">
          <div className="a-footer-grid">
            <div>
              <a href="#inicio-a" className="a-logo">
                mood<span className="text-forest-light">.</span>
              </a>
              <p>[Placeholder] Assinatura institucional.</p>
            </div>
            <nav aria-label="Navegação do rodapé">
              <p className="a-label">Navegação</p>
              {links.map((n) => (
                <a key={n.id} href={`#${n.id}`}>
                  {n.label}
                </a>
              ))}
            </nav>
            <div>
              <p className="a-label">Contato</p>
              <p>[Placeholder] Contato institucional</p>
              <p>[Placeholder] Morada</p>
            </div>
            <div>
              <p className="a-label">Legal</p>
              <p>Privacidade · Pendente</p>
              <p>Termos · Pendente</p>
            </div>
          </div>
          <div className="a-footer-bottom">
            <p>© 2026 Mood · [Placeholder] Entidade institucional</p>
            <a href="#inicio-a">
              Voltar ao início <ArrowUpRight size={16} />
            </a>
          </div>
        </div>
      </footer>
    </div>
  );
}
