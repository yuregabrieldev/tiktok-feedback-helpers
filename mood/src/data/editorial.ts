export type EditorialItem = {
  slug: string;
  title: string;
  subject: string;
  author: string;
  date: string | null;
  origin: string;
  format: "Instagram" | "Artigo educativo";
  summary: string;
  sourceUrl: string | null;
  placeholder: true;
};
export type Publication = {
  slug: string;
  title: string;
  subject: string;
  authors: string[];
  year: number | null;
  kind: "Artigo" | "Tese" | "Preprint";
  journal: string;
  abstract: string;
  references: string[];
  doi: string | null;
  sourceUrl: string | null;
  placeholder: true;
};
export const editorialItems: EditorialItem[] = [
  {
    slug: "fisiologia-exemplo",
    title: "A ciência por trás do desempenho",
    subject: "Fisiologia",
    author: "Equipe editorial Mood",
    date: "2024-10-12",
    origin: "Editorial Mood",
    format: "Instagram",
    summary:
      "O corpo adapta-se ao desafio. Conhecer os seus sinais ajuda-nos a compreender o caminho de cada atleta.",
    sourceUrl: null,
    placeholder: true,
  },
  {
    slug: "recuperacao-exemplo",
    title: "Recuperação: a parte invisível do treino",
    subject: "Recuperação",
    author: "Equipe editorial Mood",
    date: "2024-10-12",
    origin: "Editorial Mood",
    format: "Artigo educativo",
    summary:
      "Descanso, rotina e atenção aos sinais do corpo fazem parte da conversa sobre desempenho.",
    sourceUrl: null,
    placeholder: true,
  },
  {
    slug: "treino-exemplo",
    title: "Treinar com intenção, evoluir com consistência",
    subject: "Treino",
    author: "Equipe editorial Mood",
    date: "2024-10-12",
    origin: "Editorial Mood",
    format: "Artigo educativo",
    summary: "Uma perspectiva sobre como a consistência e o contexto moldam uma rotina de treino.",
    sourceUrl: null,
    placeholder: true,
  },
];
export const publications: Publication[] = [
  {
    slug: "artigo-exemplo",
    title: "Fisiologia do esforço e adaptação ao treino",
    subject: "Fisiologia",
    authors: ["M. Duarte, L. Costa"],
    year: 2024,
    kind: "Artigo",
    journal: "Journal of Performance Studies",
    abstract:
      "Resumo demonstrativo sobre fisiologia do exercício e interpretação individual do esforço.",
    references: [],
    doi: null,
    sourceUrl: null,
    placeholder: true,
  },
  {
    slug: "tese-exemplo",
    title: "Estratégias de recuperação no esporte de resistência 02",
    subject: "Recuperação",
    authors: ["M. Duarte, L. Costa"],
    year: 2024,
    kind: "Tese",
    journal: "Instituto de Ciências do Esporte",
    abstract:
      "Resumo demonstrativo sobre práticas de recuperação no contexto do esporte de resistência.",
    references: [],
    doi: null,
    sourceUrl: null,
    placeholder: true,
  },
  {
    slug: "preprint-exemplo",
    title: "Carga de treino e resposta individual",
    subject: "Treino",
    authors: ["M. Duarte, L. Costa"],
    year: 2024,
    kind: "Preprint",
    journal: "Performance Research Review",
    abstract: "Resumo demonstrativo sobre planeamento do treino e resposta individual.",
    references: [],
    doi: null,
    sourceUrl: null,
    placeholder: true,
  },
  {
    slug: "publicacao-exemplo",
    title: "Sono, rotina e preparação para o desempenho",
    subject: "Fisiologia",
    authors: ["M. Duarte, L. Costa"],
    year: 2024,
    kind: "Artigo",
    journal: "Journal of Performance Studies",
    abstract: "Resumo demonstrativo sobre sono, rotina e preparação esportiva.",
    references: [],
    doi: null,
    sourceUrl: null,
    placeholder: true,
  },
];
export const subjects = ["Fisiologia", "Recuperação", "Treino"];
export const researchers = [
  {
    name: "Marina Duarte",
    credentials: "PhD · Fisiologia do exercício",
    area: "Pesquisa e desempenho humano",
    bio: "Investiga a relação entre treino, recuperação e adaptação, aproximando a ciência das perguntas que surgem no dia a dia dos atletas.",
  },
];
export const applicationSteps = [
  "Envio da candidatura",
  "Análise manual pela equipe",
  "Comunicação da decisão",
];
