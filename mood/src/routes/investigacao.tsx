import { createFileRoute } from "@tanstack/react-router";
import { InvestigationPage } from "@/components/site/ContentPages";
import { pageHead } from "@/lib/metadata";
export const Route = createFileRoute("/investigacao")({
  head: () =>
    pageHead(
      "Biblioteca científica",
      "Pré-visualização da biblioteca científica Mood. Registros demonstrativos; fontes, autores e publicações por validar.",
      "/investigacao",
    ),
  component: InvestigationPage,
});
