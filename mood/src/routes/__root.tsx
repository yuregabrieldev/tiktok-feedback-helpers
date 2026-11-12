import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import {
  Outlet,
  Link,
  createRootRouteWithContext,
  useRouter,
  HeadContent,
  Scripts,
  type ErrorComponentProps,
} from "@tanstack/react-router";
import { type ReactNode } from "react";
import { Button } from "@/components/ui/button";

import appCss from "../styles.css?url";
import "@fontsource/manrope/latin-400.css";
import "@fontsource/manrope/latin-500.css";
import "@fontsource/manrope/latin-600.css";
import "@fontsource/manrope/latin-700.css";
import "@fontsource/manrope/latin-800.css";
import "@fontsource/source-sans-3/latin-400.css";
import "@fontsource/source-sans-3/latin-500.css";
import "@fontsource/source-sans-3/latin-600.css";

function NotFoundComponent() {
  return (
    <div className="mood-b bg-charcoal text-ivory site-error">
      <title>Página não encontrada — Mood</title>
      <meta name="robots" content="noindex, follow" />
      <main>
        <p className="b-kicker">Mood / 404</p>
        <h1>Página não encontrada.</h1>
        <p>O endereço não existe ou já não está disponível.</p>
        <div className="site-error-actions">
          <Button asChild className="b-primary-button">
            <Link to="/">Voltar ao início</Link>
          </Button>
        </div>
      </main>
    </div>
  );
}

function ErrorComponent({ error, reset }: ErrorComponentProps) {
  console.error(error);
  const router = useRouter();
  return (
    <div className="mood-b bg-charcoal text-ivory site-error">
      <title>Não foi possível carregar a página — Mood</title>
      <meta name="robots" content="noindex, follow" />
      <main>
        <p className="b-kicker">Mood / Indisponibilidade</p>
        <h1>Não foi possível carregar a página.</h1>
        <p>Tente novamente ou volte ao início.</p>
        <div className="site-error-actions">
          <Button
            className="b-primary-button"
            onClick={() => {
              void router.invalidate();
              reset();
            }}
          >
            Tentar novamente
          </Button>
          <Button asChild variant="outline" className="b-nav-cta">
            <Link to="/">Voltar ao início</Link>
          </Button>
        </div>
      </main>
    </div>
  );
}

export const Route = createRootRouteWithContext<{ queryClient: QueryClient }>()({
  head: () => ({
    meta: [
      { charSet: "utf-8" },
      { name: "viewport", content: "width=device-width, initial-scale=1" },
      { name: "robots", content: "noindex, follow" },
    ],
    links: [
      {
        rel: "stylesheet",
        href: appCss,
      },
      { rel: "icon", href: "/favicon.ico?v=transparent2", type: "image/x-icon" },
      { rel: "icon", href: "/favicon-mood.svg?v=transparent2", type: "image/svg+xml" },
    ],
  }),
  shellComponent: RootShell,
  component: RootComponent,
  notFoundComponent: NotFoundComponent,
  errorComponent: ErrorComponent,
});

function RootShell({ children }: { children: ReactNode }) {
  return (
    <html lang="pt-BR">
      <head>
        <HeadContent />
      </head>
      <body>
        {children}
        <Scripts />
      </body>
    </html>
  );
}

function RootComponent() {
  const { queryClient } = Route.useRouteContext();

  return (
    <QueryClientProvider client={queryClient}>
      {/* Required: nested routes render here. Removing <Outlet /> breaks all child routes. */}
      <Outlet />
    </QueryClientProvider>
  );
}
