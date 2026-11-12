import { createFileRoute } from "@tanstack/react-router";
import { ContactsPage } from "@/components/site/ContentPages";
import { pageHead } from "@/lib/metadata";
export const Route = createFileRoute("/contactos")({
  head: () =>
    pageHead(
      "Contato institucionais",
      "Área de contatos Mood em preparação. Contatos oficiais por confirmar e acesso ao formulário demonstrativo.",
      "/contactos",
    ),
  component: ContactsPage,
});
