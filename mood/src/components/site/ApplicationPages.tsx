import { useState } from "react";
import { Link } from "@tanstack/react-router";
import { ArrowUpRight, Check } from "lucide-react";
import { Button } from "@/components/ui/button";
import { PageFrame } from "./SiteChrome";
import { applicationSteps } from "@/data/editorial";
import { applicationMode } from "@/lib/editorial";
export function ApplicationPage() {
  const [checked, setChecked] = useState(false);
  return (
    <PageFrame
      label="Mood / Candidaturas"
      title="Candidate-se ao Mood."
      intro="As candidaturas são analisadas manualmente pela equipe. O envio não garante a admissão."
    >
      <section className="b-section b-wrap site-application-layout">
        <aside>
          <p className="b-kicker text-forest-light">Processo de candidatura</p>
          <ol className="site-steps">
            {applicationSteps.map((step, i) => (
              <li key={step}>
                <span>0{i + 1}</span>
                {step}
              </li>
            ))}
          </ol>
          <p className="site-demo-note">
            Partilha o seu percurso esportivo. A nossa equipe analisará cada candidatura
            individualmente.
          </p>
        </aside>
        <form
          className="site-form"
          onChange={() => setChecked(false)}
          onSubmit={(e) => {
            e.preventDefault();
            setChecked(true);
          }}
        >
          <div className="site-demo-note">
            <strong>Formulário demonstrativo</strong>
            <p>
              Os dados não são guardados nem enviados. A candidatura ainda não pode ser submetida.
            </p>
          </div>
          <fieldset>
            <legend>01 / Identificação</legend>
            <label>
              Nome completo
              <input name="name" autoComplete="name" required placeholder="Nome completo" />
            </label>
            <label>
              Email
              <input
                name="email"
                autoComplete="email"
                type="email"
                required
                placeholder="nome@exemplo.pt"
              />
            </label>
            <label>
              Contato telefônico <span>(opcional)</span>
              <input name="phone" autoComplete="tel" type="tel" placeholder="Contato telefônico" />
            </label>
          </fieldset>
          <fieldset>
            <legend>02 / Percurso esportivo</legend>
            <label>
              Modalidade
              <input name="sport" required placeholder="Modalidade esportiva" />
            </label>
            <label>
              Clube ou equipe <span>(opcional)</span>
              <input name="team" placeholder="Clube ou equipe" />
            </label>
            <label>
              Apresentação
              <textarea
                name="message"
                rows={5}
                required
                placeholder="Conte para nós um pouco sobre o seu percurso"
              />
            </label>
          </fieldset>
          <label className="site-checkbox">
            <input type="checkbox" required />
            Compreendo que este formulário é demonstrativo e não envia uma candidatura.
          </label>
          <Button type="submit" className="b-primary-button">
            Validar exemplo <Check />
          </Button>
          <Button
            type="button"
            className="b-application-button"
            disabled={!applicationMode.persistenceEnabled}
          >
            Enviar candidatura <ArrowUpRight />
          </Button>
          {checked && (
            <p role="status" className="site-demo-note">
              Campos do exemplo validados. Nenhuma candidatura foi enviada ou guardada.
            </p>
          )}
        </form>
      </section>
    </PageFrame>
  );
}
export function ReceivedPage() {
  return (
    <PageFrame
      label="Mood / Candidaturas"
      title="Confirmação indisponível."
      intro="Esta página será disponibilizada apenas após a gravação real de uma candidatura."
    >
      <section className="b-section b-wrap">
        <div className="site-receipt">
          <p className="b-kicker text-forest-light">Demonstração / Sem persistência</p>
          <h2>Nenhuma candidatura foi recebida.</h2>
          <p>
            O formulário atual não envia nem guarda dados. Não existe comprovante ou número de
            candidatura.
          </p>
          <Button asChild className="b-primary-button">
            <Link to="/candidatura">
              Voltar ao formulário <ArrowUpRight />
            </Link>
          </Button>
        </div>
      </section>
    </PageFrame>
  );
}
