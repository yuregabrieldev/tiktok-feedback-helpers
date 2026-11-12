import { createFileRoute } from "@tanstack/react-router";
import { ProposalB } from "@/components/mood/ProposalB";
import { pageHead } from "@/lib/metadata";
export const Route = createFileRoute("/proposta-b")({
  head: () =>
    pageHead(
      "High Performance — Proposta B",
      "Direção visual cinematográfica do Mood, preservada como referência da Home definitiva.",
      "/proposta-b",
    ),
  component: ProposalB,
});
