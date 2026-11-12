import { QueryClient } from "@tanstack/react-query";
import { createRouter, rootRouteId } from "@tanstack/react-router";
import { describe, expect, it } from "vitest";

import { routeTree } from "@/routeTree.gen";

// Match routes without running loaders or rendering: loaders may need a server or
// network the test run lacks, and jsdom never loads the stylesheets React waits on.
describe("App routing", () => {
  it.each([
    ["/mood", "/mood"],
    ["/fisiologia", "/fisiologia"],
    ["/fisiologia/recuperacao-exemplo", "/fisiologia_/$slug"],
    ["/investigacao", "/investigacao"],
    ["/investigacao/tese-exemplo", "/investigacao_/$slug"],
    ["/atletas", "/atletas"],
    ["/candidatura", "/candidatura"],
    ["/candidatura/recebida", "/candidatura_/recebida"],
    ["/contactos", "/contactos"],
    ["/admin", "/admin"],
  ])("matches %s to its page", (path, id) => {
    const router = createRouter({ routeTree, context: { queryClient: new QueryClient() } });
    expect(router.matchRoutes(path).at(-1)?.routeId).toBe(id);
  });
  it("matches the High Performance proposal at /proposta-b", () => {
    const router = createRouter({ routeTree, context: { queryClient: new QueryClient() } });
    expect(router.matchRoutes("/proposta-b").at(-1)?.routeId).toBe("/proposta-b");
  });

  it("matches a page for / instead of falling back to not found", () => {
    const router = createRouter({ routeTree, context: { queryClient: new QueryClient() } });

    const matches = router.matchRoutes("/");

    expect(matches.at(-1)?.routeId).not.toBe(rootRouteId);
  });
});
