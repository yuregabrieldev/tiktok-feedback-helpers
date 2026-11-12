import { Link } from "@tanstack/react-router";
import { ArrowUpRight } from "lucide-react";
import { researchers } from "@/data/editorial";
export function ResearchTeam() {
  return (
    <section className="b-section site-team">
      <div className="b-wrap">
        <div className="b-section-heading b-heading-rule">
          <p className="b-kicker text-forest-light">Equipe / Pesquisa</p>
          <h2>Quem investiga.</h2>
        </div>
        {researchers.map((r) => (
          <article className="site-researcher" key={r.name}>
            <div className="site-researcher-photo">
              <img
                src="/fotos/26448314.jpg"
                alt="Retrato ilustrativo de uma mulher em fundo escuro, do Pexels"
                loading="lazy"
              />
            </div>
            <div>
              <p className="b-kicker text-forest-light">Pesquisa / Fisiologia do exercício</p>
              <h3>{r.name}</h3>
              <small>Perfil fictício · fotografia ilustrativa de banco de imagens</small>
              <p>{r.credentials}</p>
              <p>{r.area}</p>
              <p>{r.bio}</p>
              <Link to="/investigacao" className="site-inline-link">
                Biblioteca científica <ArrowUpRight size={16} />
              </Link>
              <small>Uma abordagem que começa com perguntas melhores.</small>
            </div>
          </article>
        ))}
      </div>
    </section>
  );
}
