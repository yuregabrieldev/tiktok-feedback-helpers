import { createFileRoute, notFound } from "@tanstack/react-router";
import { PublicationDetail } from "@/components/site/ContentPages";
import { publications } from "@/data/editorial";
import { pageHead } from "@/lib/metadata";
export const Route = createFileRoute("/investigacao_/$slug")({
  loader: ({ params }) => {
    const item = publications.find((i) => i.slug === params.slug);
    if (!item) throw notFound();
    return item;
  },
  head: ({ loaderData, params }) =>
    pageHead(
      loaderData?.title ?? "Conteúdo indisponível",
      "Publicação científica do Mood. Conteúdo demonstrativo por validar pela equipe.",
      `/investigacao/${encodeURIComponent(params.slug)}`,
    ),
  component: Detail,
});
function Detail() {
  const { slug } = Route.useParams();
  return <PublicationDetail slug={slug} />;
}
