import { createFileRoute } from "@tanstack/react-router";
import { ProposalB } from "@/components/mood/ProposalB";
import { pageHead } from "@/lib/metadata";
export const Route = createFileRoute("/")({
  head: () =>
    pageHead(
      "Mood — High Performance",
      "Mood — apresentação High Performance. Conteúdos institucionais em preparação e formulário demonstrativo, sem envio de candidaturas.",
      "/",
    ),
  component: ProposalB,
});
