import { createFileRoute } from "@tanstack/react-router";
import { AdminPreview } from "@/components/site/AdminPreview";
import { pageHead } from "@/lib/metadata";
export const Route = createFileRoute("/admin")({
  head: () =>
    pageHead(
      "Administração demonstrativa",
      "Proposta visual administrativa sem dados reais, autenticação ou gestão funcional.",
      "/admin",
    ),
  component: AdminPreview,
});
