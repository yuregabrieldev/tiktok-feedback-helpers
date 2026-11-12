import { createFileRoute } from "@tanstack/react-router";
import { ApplicationPage } from "@/components/site/ApplicationPages";
import { pageHead } from "@/lib/metadata";
export const Route = createFileRoute("/candidatura")({
  head: () =>
    pageHead(
      "Candidatura ao Mood",
      "Formulário demonstrativo e processo de análise manual das candidaturas Mood.",
      "/candidatura",
    ),
  component: ApplicationPage,
});
