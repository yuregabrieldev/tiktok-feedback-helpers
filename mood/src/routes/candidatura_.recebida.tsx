import { createFileRoute } from "@tanstack/react-router";
import { ReceivedPage } from "@/components/site/ApplicationPages";
import { pageHead } from "@/lib/metadata";
export const Route = createFileRoute("/candidatura_/recebida")({
  head: () =>
    pageHead(
      "Estado da candidatura",
      "Confirmação disponível futuramente apenas após gravação real da candidatura.",
      "/candidatura/recebida",
    ),
  component: ReceivedPage,
});
