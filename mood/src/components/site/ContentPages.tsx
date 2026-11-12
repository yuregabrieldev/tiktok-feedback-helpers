import { Link } from "@tanstack/react-router";
import { ArrowUpRight, Image as ImageIcon } from "lucide-react";
import { Button } from "@/components/ui/button";
import { PageFrame } from "./SiteChrome";
import { EditorialList } from "./EditorialList";
import { ResearchTeam } from "./ResearchTeam";
import { ScientificLibrary } from "@/components/mood/ScientificLibrary";
import { editorialItems, publications } from "@/data/editorial";
import { panels, testimonials } from "@/components/mood/content";
import detail from "@/assets/athlete-detail.jpg";
import cyclist from "@/assets/athlete-cyclist.jpg";
export function MoodPage() {
  return (
    <PageFrame
      label="Mood / Institucional"
      title="O Mood."
      intro="[Placeholder] Apresentação institucional a aprovar pela equipe."
    >
      <section className="b-section b-wrap site-about-grid">
        <figure>
          <img
            src={detail}
            width={1024}
            height={1280}
            alt="Mãos de atleta — imagem ilustrativa"
            loading="lazy"
          />
          <figcaption>Imagem ilustrativa · Foto real a fornecer</figcaption>
        </figure>
        <div className="site-prose">
          <h2>[Placeholder] Declaração do projeto.</h2>
          <h3>Missão</h3>
          <p>[Placeholder] Missão e origem do Mood a fornecer pela equipe.</p>
          <h3>Abordagem</h3>
          <p>
            [Placeholder] Apresentação aprovada da relação com atletas e da abordagem científica.
          </p>
        </div>
      </section>
      <section className="b-section b-wrap">
        <div className="b-heading-rule">
          <p className="b-kicker text-forest-light">Produto</p>
          <h2 className="site-section-title">Sistema Mood</h2>
        </div>
        <div className="site-product-placeholder">
          <ImageIcon size={32} />
          <p>[Placeholder] Fotografias e capturas reais do produto</p>
        </div>
        <div className="site-product-lines">
          {panels.map((p) => (
            <article key={p.n}>
              <span className="b-panel-number">{p.n}</span>
              <div>
                <h3>{p.title}</h3>
                <p>{p.body}</p>
              </div>
            </article>
          ))}
        </div>
      </section>
      <ResearchTeam />
    </PageFrame>
  );
}
export function PhysiologyPage() {
  return (
    <PageFrame
      label="Mood / Fisiologia"
      title="Corpo. Esforço. Conhecimento."
      intro="[Placeholder] Seleção editorial de conteúdos educativos, posts e artigos por validar."
    >
      <section className="b-section b-wrap site-list-section">
        <EditorialList />
      </section>
    </PageFrame>
  );
}
export function PhysiologyDetail({ slug }: { slug: string }) {
  const item = editorialItems.find((i) => i.slug === slug);
  if (!item)
    return (
      <PageFrame label="Mood / Conteúdos" title="Conteúdo indisponível">
        <section className="b-section b-wrap">
          <Link to="/fisiologia">Voltar aos conteúdos</Link>
        </section>
      </PageFrame>
    );
  return (
    <PageFrame label={`${item.format} / ${item.subject}`} title={item.title}>
      <section className="b-section b-wrap site-detail-grid">
        <aside className="site-detail-meta">
          <p>{item.author}</p>
          <p>12 outubro 2024</p>
          <p>{item.origin}</p>
          <p className="site-demo-note">Leitura editorial · Fisiologia e desempenho</p>
          <Link to="/fisiologia">Todos os conteúdos</Link>
        </aside>
        <article className="site-prose">
          <img src={cyclist} alt="Ciclista — imagem ilustrativa" width={1280} height={1024} />
          <p className="site-photo-caption">
            Fotografia ilustrativa · Não representa participante do Mood
          </p>
          <h2>Enquadramento</h2>
          <p>{item.summary}</p>
          <h2>Conteúdo</h2>
          <p>
            O desempenho constrói-se ao longo do tempo. O contexto, a consistência e a atenção às
            necessidades individuais fazem parte de uma abordagem consciente ao treino.
          </p>
          <h2>Autoria e fontes</h2>
          <p>Equipe editorial Mood · Conteúdo de apresentação.</p>
          {item.sourceUrl ? (
            <a href={item.sourceUrl} target="_blank" rel="noopener noreferrer">
              Consultar original
            </a>
          ) : (
            <p>Conteúdo editorial Mood.</p>
          )}
        </article>
      </section>
    </PageFrame>
  );
}
export function InvestigationPage() {
  return (
    <PageFrame
      label="Mood / Pesquisa"
      title="Biblioteca científica."
      intro="[Placeholder] Curadoria e critérios científicos a aprovar."
    >
      <ScientificLibrary standalone />
      <ResearchTeam />
    </PageFrame>
  );
}
export function PublicationDetail({ slug }: { slug: string }) {
  const p = publications.find((i) => i.slug === slug);
  if (!p)
    return (
      <PageFrame label="Mood / Pesquisa" title="Publicação indisponível">
        <section className="b-section b-wrap">
          <Link to="/investigacao">Voltar à biblioteca</Link>
        </section>
      </PageFrame>
    );
  return (
    <PageFrame label={`${p.kind} de exemplo / ${p.subject}`} title={p.title}>
      <section className="b-section b-wrap site-detail-grid">
        <aside className="site-detail-meta">
          <p>{p.authors.join(", ")}</p>
          <p>{p.year ?? "[Ano por confirmar]"}</p>
          <p>{p.journal}</p>
          <p>DOI: {p.doi ?? "[Por fornecer]"}</p>
          <p className="site-demo-note">Biblioteca Mood · Material de apresentação</p>
          <Link to="/investigacao">Voltar à biblioteca</Link>
        </aside>
        <article className="site-prose">
          <h2>Resumo</h2>
          <p>{p.abstract}</p>
          <h2>Referências</h2>
          {p.references.length ? (
            p.references.map((r) => <p key={r}>{r}</p>)
          ) : (
            <p>Referências indisponíveis nesta apresentação.</p>
          )}
          <h2>Acesso ao original</h2>
          {p.sourceUrl ? (
            <Button asChild className="b-primary-button">
              <a href={p.sourceUrl} target="_blank" rel="noopener noreferrer">
                Consultar original <ArrowUpRight />
              </a>
            </Button>
          ) : (
            <p>Consulta do documento indisponível nesta apresentação.</p>
          )}
        </article>
      </section>
    </PageFrame>
  );
}
export function AthletesPage() {
  return (
    <PageFrame
      label="Mood / Atletas"
      title="Vozes de quem compete."
      intro="[Placeholder] Experiências e testemunhos reais a coletar com autorização."
    >
      <section className="b-section b-wrap site-athlete-stories">
        {testimonials.map((t, i) => (
          <article key={t.quote}>
            <div className="site-product-placeholder">
              <ImageIcon size={30} />
              <p>[Placeholder] Fotografia autorizada do atleta</p>
            </div>
            <div>
              <p className="b-kicker text-forest-light">História 0{i + 1} / Conteúdo pendente</p>
              <h2>{t.name}</h2>
              <p>{t.sport} · [Placeholder]</p>
              <blockquote>{t.quote}</blockquote>
              <p className="site-demo-note">
                Fotografia, vídeo opcional e história integral por fornecer. Resultados somente
                serão publicados após verificação.
              </p>
              <Button variant="outline" className="b-nav-cta" disabled>
                História completa · Pendente <ArrowUpRight />
              </Button>
            </div>
          </article>
        ))}
      </section>
    </PageFrame>
  );
}
export function ContactsPage() {
  return (
    <PageFrame
      label="Mood / Contato"
      title="Fale com o Mood."
      intro="[Placeholder] Contato institucionais a confirmar pela equipe."
    >
      <section className="b-section b-wrap site-contact-grid">
        <div className="site-prose">
          <h2>Contato institucional</h2>
          <p>ola@mood.example</p>
          <h2>Localização</h2>
          <p>Lisboa · Portugal</p>
          <h2>Redes sociais</h2>
          <p>Instagram · @mood.performance</p>
        </div>
        <div className="site-contact-callout">
          <p className="b-kicker text-forest-light">Atletas</p>
          <h2>Candidaturas ao Mood.</h2>
          <p>A admissão depende da análise manual pela equipe.</p>
          <Button asChild className="b-primary-button">
            <Link to="/candidatura">
              Quero me candidatar <ArrowUpRight />
            </Link>
          </Button>
        </div>
      </section>
    </PageFrame>
  );
}
