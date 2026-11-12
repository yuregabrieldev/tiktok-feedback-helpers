import { useEffect, useRef, useState, type ReactNode } from "react";
import { Link } from "@tanstack/react-router";
import { Menu, X, ArrowUpRight } from "lucide-react";
import { Button } from "@/components/ui/button";
export function SiteNavLinks({ onNavigate }: { onNavigate?: () => void }) {
  return (
    <>
      {[
        ["Mood", "mood-b"],
        ["Fisiologia", "fisiologia-b"],
        ["Pesquisa", "investigacao-b"],
        ["Atletas", "atletas-b"],
        ["Contato", "contactos-b"],
      ].map(([label, id]) => (
        <a key={id} href={`/#${id}`} onClick={onNavigate}>
          {label}
        </a>
      ))}
    </>
  );
}
export function SiteHeader({ overlay = false }: { overlay?: boolean }) {
  const [open, setOpen] = useState(false);
  const [scrolled, setScrolled] = useState(false);
  useEffect(() => {
    const update = () => setScrolled(window.scrollY > 64);
    update();
    window.addEventListener("scroll", update, { passive: true });
    return () => window.removeEventListener("scroll", update);
  }, []);
  const ref = useRef<HTMLButtonElement>(null);
  useEffect(() => {
    if (!open) return;
    const close = (e: KeyboardEvent) => {
      if (e.key === "Escape") {
        setOpen(false);
        ref.current?.focus();
      }
    };
    document.addEventListener("keydown", close);
    return () => document.removeEventListener("keydown", close);
  }, [open]);
  return (
    <header
      className={`b-header ${overlay ? "" : "site-header-solid"} ${scrolled ? "is-scrolled" : ""}`}
    >
      <div className="b-wrap b-nav-row">
        <Link to="/" className="b-wordmark" aria-label="Mood — início">
          <img src="/mood-logo.svg" alt="" className="mood-logo" />
          <span>Mood</span>
        </Link>
        <nav aria-label="Navegação principal" className="hidden items-center gap-6 lg:flex">
          <SiteNavLinks />
          <Button asChild variant="outline" className="b-nav-cta">
            <a href="/#cta-b">
              Quero me candidatar <ArrowUpRight />
            </a>
          </Button>
        </nav>
        <Button
          ref={ref}
          variant="ghost"
          size="icon"
          className="b-menu-button lg:hidden"
          aria-label={open ? "Fechar menu" : "Abrir menu"}
          aria-expanded={open}
          aria-controls="site-mobile-nav"
          onClick={() => setOpen(!open)}
        >
          {open ? <X /> : <Menu />}
        </Button>
      </div>
      {open && (
        <nav
          id="site-mobile-nav"
          aria-label="Navegação móvel"
          className="site-mobile-nav lg:hidden"
        >
          <SiteNavLinks onNavigate={() => setOpen(false)} />
          <Button asChild className="b-primary-button">
            <a href="/#cta-b" onClick={() => setOpen(false)}>
              Quero me candidatar <ArrowUpRight />
            </a>
          </Button>
        </nav>
      )}
    </header>
  );
}
export function SiteFooter() {
  return (
    <footer className="b-wrap b-footer">
      <div className="b-footer-top">
        <Link to="/" className="b-footer-wordmark" aria-label="Mood — início">
          <img src="/mood-logo.svg" alt="" className="mood-logo mood-logo-footer" />
          <span>Mood</span>
        </Link>
        <nav aria-label="Navegação do rodapé">
          <SiteNavLinks />
        </nav>
        <div id="contactos-b" className="b-footer-contact">
          <p>Contato</p>
          <span>ola@mood.example</span>
          <span>São Paulo · Brasil</span>
          <span>Privacidade</span>
          <span>Termos</span>
        </div>
      </div>
      <div className="b-footer-bottom">
        <p>© 2026 Mood · Projeto de demonstração · Pessoas e dados fictícios</p>
        <Link to="/">
          Mood / High Performance <ArrowUpRight size={15} />
        </Link>
      </div>
    </footer>
  );
}
export function PageFrame({
  children,
  label,
  title,
  intro,
}: {
  children: ReactNode;
  label: string;
  title: string;
  intro?: string;
}) {
  return (
    <div className="mood-b bg-charcoal text-ivory">
      <a href="#page-content" className="b-skip-link">
        Saltar para o conteúdo
      </a>
      <SiteHeader />
      <main id="page-content" tabIndex={-1}>
        <section className="site-page-heading b-wrap">
          <p className="b-kicker text-forest-light">{label}</p>
          <h1>{title}</h1>
          {intro && <p>{intro}</p>}
        </section>
        {children}
      </main>
      <SiteFooter />
    </div>
  );
}
