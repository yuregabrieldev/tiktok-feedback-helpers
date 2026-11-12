import { useState } from "react";
import { Link } from "@tanstack/react-router";
import { LayoutDashboard, Users, BookOpen, FileText, Settings, ArrowUpRight } from "lucide-react";
import { Button } from "@/components/ui/button";
import { publications, editorialItems } from "@/data/editorial";
const tabs = [
  { id: "overview", label: "Visão geral", icon: LayoutDashboard },
  { id: "applications", label: "Candidaturas", icon: Users },
  { id: "editorial", label: "Gestão editorial", icon: BookOpen },
  { id: "institution", label: "Institucional", icon: Settings },
];
export function AdminPreview() {
  const [tab, setTab] = useState("overview");
  return (
    <div className="mood-b site-admin bg-charcoal text-ivory">
      <header className="site-admin-header">
        <Link to="/" className="b-wordmark">
          Mood
        </Link>
        <p>Proposta administrativa · Demonstração pública, sem autenticação</p>
        <Button asChild variant="outline" className="b-nav-cta">
          <Link to="/">
            Website <ArrowUpRight />
          </Link>
        </Button>
      </header>
      <div className="site-admin-layout">
        <nav aria-label="Secções administrativas">
          {tabs.map((t) => (
            <Button
              key={t.id}
              variant="ghost"
              aria-pressed={tab === t.id}
              onClick={() => setTab(t.id)}
            >
              <t.icon size={18} />
              {t.label}
            </Button>
          ))}
        </nav>
        <main>
          <p className="b-kicker text-forest-light">Administração / Proposta visual</p>
          <h1>{tabs.find((t) => t.id === tab)?.label}</h1>
          <p className="site-demo-note">
            Sem dados reais, permissões, gravação ou ações de gestão. Este endereço não é privado.
          </p>
          {tab === "overview" ? (
            <>
              <div className="site-admin-stats">
                {["Candidaturas", "Conteúdos", "Publicações"].map((l) => (
                  <div key={l}>
                    <p>{l}</p>
                    <strong>—</strong>
                    <small>Dados reais não ligados</small>
                  </div>
                ))}
              </div>
              <h2>Preparação editorial</h2>
              <div className="site-admin-table">
                {[
                  "Posts do Instagram",
                  "Papers e publicações",
                  "Atletas e depoimentos",
                  "Conteúdos institucionais",
                ].map((l) => (
                  <div key={l}>
                    <span>{l}</span>
                    <span>Conteúdo por validar</span>
                    <FileText size={18} />
                  </div>
                ))}
              </div>
            </>
          ) : tab === "applications" ? (
            <>
              <h2>Candidaturas</h2>
              <div className="site-admin-table">
                {["EXEMPLO-01", "EXEMPLO-02", "EXEMPLO-03"].map((id, i) => (
                  <div key={id}>
                    <strong>{id}</strong>
                    <span>[Nome do atleta]</span>
                    <span>
                      {
                        [
                          "Por analisar · Exemplo",
                          "Em análise · Exemplo",
                          "Decisão pendente · Exemplo",
                        ][i]
                      }
                    </span>
                    <Button variant="outline" className="b-nav-cta" disabled>
                      Analisar
                    </Button>
                  </div>
                ))}
              </div>
              <p className="site-demo-note">
                A aprovação futura será manual. Não existem candidaturas guardadas nesta interface.
              </p>
            </>
          ) : tab === "editorial" ? (
            <>
              <h2>Conteúdos de exemplo</h2>
              <div className="site-admin-table">
                {[...editorialItems, ...publications].map((i) => (
                  <div key={i.slug}>
                    <span>{i.title}</span>
                    <span>Por validar</span>
                    <Button variant="outline" className="b-nav-cta" disabled>
                      Editar
                    </Button>
                  </div>
                ))}
              </div>
              <div className="site-admin-table">
                {["Atletas", "Depoimentos", "Vídeos autorizados"].map((l) => (
                  <div key={l}>
                    <span>{l}</span>
                    <span>[Placeholder] Conteúdo pendente</span>
                    <Button variant="outline" className="b-nav-cta" disabled>
                      Gerir
                    </Button>
                  </div>
                ))}
              </div>
            </>
          ) : (
            <>
              <h2>Conteúdos institucionais</h2>
              <div className="site-admin-table">
                {[
                  "Missão e abordagem",
                  "Produto",
                  "Equipe e credenciais",
                  "Contato",
                  "Privacidade e consentimento",
                ].map((l) => (
                  <div key={l}>
                    <span>{l}</span>
                    <span>Por aprovar</span>
                    <Button variant="outline" className="b-nav-cta" disabled>
                      Editar
                    </Button>
                  </div>
                ))}
              </div>
            </>
          )}
        </main>
      </div>
    </div>
  );
}
