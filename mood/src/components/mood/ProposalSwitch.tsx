import { Link } from "@tanstack/react-router";

export function ProposalSwitch({ current }: { current: "a" | "b" }) {
  const base = "whitespace-nowrap px-3 py-1.5 transition-colors";
  return (
    <div className="fixed bottom-4 left-1/2 z-50 -translate-x-1/2 flex items-center gap-1 rounded-full border border-line-dark bg-charcoal/90 p-1 text-ivory backdrop-blur eyebrow shadow-lg">
      <Link
        to="/"
        className={`${base} rounded-full ${current === "a" ? "bg-ivory text-charcoal" : "hover:text-forest-light"}`}
      >
        A · Editorial
      </Link>
      <Link
        to="/proposta-b"
        className={`${base} rounded-full ${current === "b" ? "bg-ivory text-charcoal" : "hover:text-forest-light"}`}
      >
        B · Performance
      </Link>
    </div>
  );
}
