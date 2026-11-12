import { useRef, useState } from "react";
import { Link } from "@tanstack/react-router";
import { ArrowDown, Image as ImageIcon, Play, ArrowUpRight, Check } from "lucide-react";
import { Button } from "@/components/ui/button";
import heroB from "@/assets/hero-b.jpg";
import detail from "@/assets/athlete-detail.jpg";
import { panels, testimonials } from "./content";
import { ScientificLibrary } from "./ScientificLibrary";
import { EditorialList } from "@/components/site/EditorialList";
import { ResearchTeam } from "@/components/site/ResearchTeam";
import { SiteHeader, SiteFooter } from "@/components/site/SiteChrome";

function SectionHead({ idx, label, title }: { idx: string; label: string; title: string }) {
  return (
    <div className="b-section-heading b-heading-rule">
      <p className="b-kicker text-forest-light">
        {idx} / {label}
      </p>
      <h2>{title}</h2>
    </div>
  );
}
function TrainingVideo() {
  const ref = useRef<HTMLVideoElement>(null);
  const [playing, setPlaying] = useState(false);
  return (
    <div className="mood-video">
      <div className="mood-video-frame">
        <video
          ref={ref}
          controls
          playsInline
          preload="none"
          poster="/treino-poster.jpg"
          aria-label="Vídeo ilustrativo de um atleta correndo na chuva"
          onPlay={() => setPlaying(true)}
          onPause={() => setPlaying(false)}
        >
          <source src="/treino-ilustrativo.mp4" type="video/mp4" />O seu navegador não suporta
          vídeo.
        </video>
        {!playing && (
          <button
            type="button"
            className="mood-video-play"
            aria-label="Reproduzir vídeo ilustrativo"
            onClick={() => {
              void ref.current?.play().catch(() => {});
            }}
          >
            <Play aria-hidden="true" size={24} fill="currentColor" />
          </button>
        )}
        <span className="mood-video-label">MOOD / MOVIMENTO</span>
      </div>
      <p className="mood-video-credit">
        Vídeo ilustrativo de Eduardo Holanda / Pexels. A pessoa retratada não representa o Mood.
      </p>
    </div>
  );
}
export function ProposalB() {
  return (
    <div className="mood-b bg-charcoal text-ivory">
      <a href="#home-content" className="b-skip-link">
        Saltar para o conteúdo
      </a>
      <SiteHeader overlay />
      <main id="home-content" tabIndex={-1}>
        <section id="top-b" className="b-hero">
          <img
            src={heroB}
            alt="Remador em treino noturno — imagem ilustrativa"
            width={1600}
            height={1008}
            fetchPriority="high"
            className="b-hero-photo"
          />
          <div className="b-hero-shade" />
          <div className="b-wrap b-hero-inner">
            <div className="b-hero-copy">
              <p className="b-kicker text-forest-light">Atletas · Alta performance</p>
              <h1>
                Para lá
                <br />
                do limite
                <br />
                visível.
              </h1>
              <div className="b-hero-description">
                <span className="b-kicker text-forest-light">Mood</span>
                <p>
                  Um projeto ligado a atletas de alta performance, com candidaturas analisadas
                  manualmente.
                </p>
              </div>
              <div className="b-hero-actions">
                <Button asChild className="b-primary-button">
                  <Link to="/candidatura">
                    Quero me candidatar <ArrowUpRight />
                  </Link>
                </Button>
                <Button asChild variant="link" className="b-secondary-button">
                  <a href="#sistema-b">
                    Conhecer o Mood <ArrowDown />
                  </a>
                </Button>
              </div>
              <p className="b-hero-footnote">Candidaturas sujeitas a aprovação manual.</p>
            </div>
          </div>
          <div className="b-hero-bottom b-wrap">
            <span className="b-kicker">Mood / High Performance</span>
            <a href="#mood-b" aria-label="Descobrir o Mood">
              <ArrowDown size={20} />
            </a>
          </div>
        </section>
        <section id="mood-b" className="b-section b-institutional">
          <div className="b-wrap b-institutional-grid">
            <figure className="b-detail-photo">
              <img
                src={detail}
                alt="Mãos de atleta com magnésio — imagem ilustrativa"
                loading="lazy"
                width={1024}
                height={1280}
              />
              <figcaption className="b-kicker">
                Imagem ilustrativa · A substituir por foto real
              </figcaption>
            </figure>
            <div className="b-institutional-copy">
              <p className="b-kicker text-forest-light">01 / O Mood</p>
              <h2>Mood e atletas de alta performance.</h2>
              <div className="b-mission-grid">
                {["Missão", "Abordagem"].map((k) => (
                  <div key={k}>
                    <h3 className="b-kicker text-forest-light">{k}</h3>
                    <p>Informação institucional em preparação.</p>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </section>
        <section id="sistema-b" className="b-section b-product">
          <div className="b-wrap">
            <SectionHead idx="02" label="Produto" title="Sistema Mood" />
            <div className="b-product-grid">
              <article className="b-product-feature">
                <div className="b-panel-heading">
                  <span className="b-panel-number">01</span>
                  <span className="b-kicker">Produto / Visão geral</span>
                </div>
                <figure className="real-stock-feature">
                  <img
                    src="/fotos/3756042.jpg"
                    alt="Atleta se aquecendo em uma pista coberta"
                    loading="lazy"
                  />
                  <figcaption>
                    Fotografia ilustrativa · Andrea Piacquadio / Pexels · Não representa o produto
                    Mood
                  </figcaption>
                </figure>
                <div className="b-panel-copy">
                  <h3>{panels[0]?.title}</h3>
                  <p>{panels[0]?.body}</p>
                </div>
              </article>
              <div className="b-product-aside">
                <article className="b-product-secondary">
                  <div className="b-panel-heading">
                    <span className="b-panel-number">02</span>
                    <span className="b-kicker">Conteúdo por aprovar</span>
                  </div>
                  <h3>{panels[1]?.title}</h3>
                  <p>{panels[1]?.body}</p>
                  <figure className="real-stock-detail">
                    <img
                      src="/fotos/4753983.jpg"
                      alt="Pessoa se preparando para treinar em ambiente esportivo"
                      loading="lazy"
                    />
                    <figcaption>Fotografia ilustrativa · cottonbro studio / Pexels</figcaption>
                  </figure>
                </article>
                <article className="b-product-tertiary">
                  <span className="b-panel-number">03</span>
                  <div>
                    <h3>{panels[2]?.title}</h3>
                    <p>{panels[2]?.body}</p>
                  </div>
                </article>
              </div>
            </div>
          </div>
        </section>
        <section id="fisiologia-b" className="b-section b-education">
          <div className="b-wrap">
            <SectionHead idx="03" label="Fisiologia" title="Compreender o corpo em esforço" />

            <EditorialList compact />
          </div>
        </section>
        <ScientificLibrary />
        <ResearchTeam />
        <section id="atletas-b" className="b-section b-athletes">
          <div className="b-wrap">
            <SectionHead idx="05" label="Atletas" title="Vozes de quem compete" />

            <div className="b-testimonials-grid">
              <figure className="b-testimonial-feature">
                <div className="b-portrait-placeholder">
                  <img
                    src="/fotos/6389862.jpg"
                    alt="Foto ilustrativa de uma atleta em repouso, de Tima Miroshnichenko / Pexels"
                    loading="lazy"
                  />
                </div>
                <div className="b-quote-copy">
                  <span className="b-quote-mark" aria-hidden="true">
                    “
                  </span>
                  <blockquote>{testimonials[0]?.quote}</blockquote>
                  <figcaption>
                    <span>{testimonials[0]?.name}</span>
                    <small>Personagem fictícia · foto ilustrativa de banco de imagens</small>
                    <small>{testimonials[0]?.sport}</small>
                  </figcaption>
                  <Button asChild variant="link" className="b-text-link">
                    <Link to="/atletas">
                      Histórias dos atletas <ArrowUpRight />
                    </Link>
                  </Button>
                </div>
              </figure>
              <figure className="b-testimonial-video">
                <TrainingVideo />
                <blockquote>{testimonials[1]?.quote}</blockquote>
                <figcaption>
                  <span>{testimonials[1]?.name}</span>
                  <small>{testimonials[1]?.sport}</small>
                </figcaption>
              </figure>
            </div>
          </div>
        </section>
        <section id="cta-b" className="b-application bg-forest">
          <div className="b-wrap b-application-grid">
            <div>
              <p className="b-kicker">06 / Candidaturas</p>
              <h2>
                Candidate-se
                <br />
                ao Mood.
              </h2>
              <p className="b-application-placeholder">
                Conte para nós o seu percurso e descubra o que podemos construir em conjunto.
              </p>
            </div>
            <div className="b-application-copy">
              <div className="b-approval-label">
                <Check size={18} />
                <span className="b-kicker">Aprovação manual</span>
              </div>
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
              <Button asChild className="b-application-button">
                <Link to="/candidatura">
                  Iniciar candidatura <ArrowUpRight />
                </Link>
              </Button>
              <small>Formulário demonstrativo · Sem envio de dados.</small>
            </div>
          </div>
        </section>
      </main>
      <SiteFooter />
    </div>
  );
}
