import { createFileRoute } from "@tanstack/react-router";
import { robotsText } from "@/lib/seo";
export const Route = createFileRoute("/robots.txt")({
  server: {
    handlers: {
      GET: () =>
        new Response(robotsText(), { headers: { "Content-Type": "text/plain; charset=utf-8" } }),
    },
  },
});
