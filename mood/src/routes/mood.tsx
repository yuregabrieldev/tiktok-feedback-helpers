import { createFileRoute } from "@tanstack/react-router";
import { MoodPage } from "@/components/site/ContentPages";
import { pageHead } from "@/lib/metadata";
export const Route = createFileRoute("/mood")({
  head: () =>
    pageHead(
      "Mood — projeto e produto",
      "Apresentação institucional Mood em preparação. Textos, imagens do produto e perfis da equipe por aprovar.",
      "/mood",
    ),
  component: MoodPage,
});
