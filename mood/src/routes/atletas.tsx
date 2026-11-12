import { createFileRoute } from "@tanstack/react-router";
import { AthletesPage } from "@/components/site/ContentPages";
import { pageHead } from "@/lib/metadata";
export const Route = createFileRoute("/atletas")({
  head: () =>
    pageHead(
      "Atletas e experiências",
      "Pré-visualização da área de atletas Mood, preparada para experiências e imagens reais após autorização e revisão.",
      "/atletas",
    ),
  component: AthletesPage,
});
