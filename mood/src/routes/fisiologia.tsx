import { createFileRoute } from "@tanstack/react-router";
import { PhysiologyPage } from "@/components/site/ContentPages";
import { pageHead } from "@/lib/metadata";
export const Route = createFileRoute("/fisiologia")({
  head: () =>
    pageHead(
      "Fisiologia e conteúdos",
      "Pré-visualização da área de fisiologia Mood, com organização por assunto e conteúdos de exemplo por validar.",
      "/fisiologia",
    ),
  component: PhysiologyPage,
});
