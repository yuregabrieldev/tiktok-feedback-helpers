import type { Metadata } from 'next';
import './globals.css';

export const metadata: Metadata = {
  title: 'PULSO — TikTok Feedback Helpers',
  description: 'Descubra criadores e troque feedbacks que ajudam perfis a evoluir.',
  icons: { icon: '/favicon.svg' },
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="pt-PT">
      <body>{children}</body>
    </html>
  );
}
