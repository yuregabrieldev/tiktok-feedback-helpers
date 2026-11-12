import { createFileRoute, notFound } from "@tanstack/react-router";
import { PhysiologyDetail } from "@/components/site/ContentPages";
import { editorialItems } from "@/data/editorial";
import { pageHead } from "@/lib/metadata";
export const Route = createFileRoute("/fisiologia_/$slug")({
  loader: ({ params }) => {
    const item = editorialItems.find((i) => i.slug === params.slug);
    if (!item) throw notFound();
    return item;
  },
  head: ({ loaderData, params }) =>
    pageHead(
      loaderData?.title ?? "Conteúdo indisponível",
      "Conteúdo educativo do Mood. Conteúdo demonstrativo por validar pela equipe.",
      `/fisiologia/${encodeURIComponent(params.slug)}`,
    ),
  component: Detail,
});
function Detail() {
  const { slug } = Route.useParams();
  return <PhysiologyDetail slug={slug} />;
}
