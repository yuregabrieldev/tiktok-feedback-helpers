'use client';

import { FormEvent, useEffect, useState } from 'react';
import type { User } from '@supabase/supabase-js';
import { createBrowserSupabaseClient, hasSupabaseConfig } from '@/lib/supabase/browser';

type View = 'tutorial' | 'feed' | 'evaluate' | 'publish' | 'profile';
type MissionKind = 'tutorial' | 'standard';

const campaign = {
  pulse: 'PULSO #041',
  niche: 'RECEITAS',
  handle: '@marianaem15',
  name: 'Mariana',
  description: 'Receitas práticas para quem não tem tempo.',
  request: 'A minha bio explica claramente o que eu posto?',
  initials: 'M',
};

export default function HomePage() {
  const [authLoading, setAuthLoading] = useState(true);
  const [user, setUser] = useState<User | null>(null);
  const [view, setView] = useState<View>('tutorial');
  const [points, setPoints] = useState(0);
  const [feedback, setFeedback] = useState('');
  const [answer, setAnswer] = useState<string | null>(null);
  const [notice, setNotice] = useState('');
  const [missionKind, setMissionKind] = useState<MissionKind>('tutorial');

  useEffect(() => {
    if (!hasSupabaseConfig) {
      setAuthLoading(false);
      return;
    }
    const supabase = createBrowserSupabaseClient();
    supabase.auth.getSession().then(({ data }) => {
      setUser(data.session?.user ?? null);
      setAuthLoading(false);
    });
    const { data: subscription } = supabase.auth.onAuthStateChange((_event, session) => setUser(session?.user ?? null));
    return () => subscription.subscription.unsubscribe();
  }, []);

  function beginMission(kind: MissionKind) {
    setMissionKind(kind);
    setNotice('Missão iniciada. Ao voltar do TikTok, envie a sua avaliação.');
    window.setTimeout(() => setView('evaluate'), 500);
  }

  function submitFeedback(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!answer || feedback.trim().length < 20) {
      setNotice('Escolha uma resposta e escreva uma sugestão com pelo menos 20 caracteres.');
      return;
    }

    if (missionKind === 'standard') {
      setPoints(1);
      setView('feed');
      setNotice('Feedback enviado. Ganhou o seu primeiro ponto.');
    } else {
      setView('feed');
      setNotice('Tutorial concluído. A sua conta está ativa.');
    }
    setFeedback('');
    setAnswer(null);
  }

  const title = view === 'tutorial'
    ? 'Primeira missão'
    : view === 'evaluate'
      ? 'Avaliar perfil'
      : view === 'publish'
        ? 'Publicar campanha'
        : view === 'profile'
          ? 'Minha conta'
          : 'For You';

  if (authLoading) return <main className="auth-shell"><div className="auth-card"><Brand /><p>A preparar o PULSO…</p></div></main>;
  if (!user) return <AuthScreen />;

  return (
    <main className="app-shell">
      <header className="topbar">
        <button className="brand" onClick={() => setView(view === 'tutorial' ? 'tutorial' : 'feed')} aria-label="Ir para For You">
          <span className="pulse-mark" aria-hidden="true"><i /><i /><i /></span>
          <strong>PULSO</strong>
          <small>TIKTOK FEEDBACK HELPERS</small>
        </button>
        <span className="points"><b>{points}</b> PONTO{points === 1 ? '' : 'S'}</span>
      </header>

      <section className="screen" aria-labelledby="screen-title">
        <div className="eyebrow"><span className="live-dot" /> {view === 'tutorial' ? 'ACESSO PENDENTE' : 'COMUNIDADE ATIVA'}</div>
        <h1 id="screen-title">{title}</h1>

        {notice && <p className="notice" role="status">{notice}</p>}

        {view === 'tutorial' && (
          <>
            <p className="intro">Antes de entrar na comunidade, complete a primeira avaliação.</p>
            <CampaignCard admin onAction={() => beginMission('tutorial')} />
            <p className="rule">Esta missão ativa a sua conta. Não concede pontos.</p>
          </>
        )}

        {view === 'feed' && (
          <>
            <section className="progress-band" aria-label="Estado atual">
              <span>PRONTO PARA PUBLICAR</span>
              <b>{points} ponto disponível</b>
            </section>
            <CampaignCard onAction={() => beginMission('standard')} />
            <section className="feed-next" aria-label="Próximas campanhas">
              <span>PRÓXIMOS PULSOS</span>
              <p>O feed será preenchido com campanhas que ainda não avaliou.</p>
            </section>
          </>
        )}

        {view === 'evaluate' && (
          <form className="evaluation" onSubmit={submitFeedback}>
            <span className="mission-tag">MISSÃO EM AVALIAÇÃO</span>
            <div className="mini-profile"><Avatar initials={campaign.initials} /><div><b>{campaign.handle}</b><span>{campaign.niche}</span></div></div>
            <h2>Como foi conhecer este perfil?</h2>
            <fieldset>
              <legend>A bio deixa claro o nicho?</legend>
              <div className="choice-row">
                {['Sim', 'Mais ou menos', 'Não'].map((option) => (
                  <button type="button" className={answer === option ? 'choice selected' : 'choice'} onClick={() => setAnswer(option)} key={option}>{option}</button>
                ))}
              </div>
            </fieldset>
            <label htmlFor="feedback">Uma sugestão útil</label>
            <textarea id="feedback" value={feedback} onChange={(event) => setFeedback(event.target.value)} placeholder="Ex.: A bio ficaria ainda mais clara se dissesse…" minLength={20} maxLength={400} />
            <button className="primary-action" type="submit">Enviar feedback {missionKind === 'standard' && <span>+1</span>}</button>
          </form>
        )}

        {view === 'publish' && (
          <section className="publish-card">
            <span className="mission-tag">A SUA CAMPANHA</span>
            <h2>Peça feedback sobre algo específico.</h2>
            <label htmlFor="campaign-question">O que quer saber?</label>
            <textarea id="campaign-question" placeholder="A minha bio deixa claro o que eu posto?" maxLength={220} />
            <div className="purchase-row"><span>1 feedback</span><b>1 ponto</b></div>
            <button className="primary-action" disabled={points < 1}>Lançar campanha <span>−1</span></button>
          </section>
        )}

        {view === 'profile' && (
          <section className="account-card">
            <div className="account-head"><Avatar initials="Y" large /><div><span>O SEU PERFIL</span><h2>@o_seu_tiktok</h2><p>Adicione a sua foto e link TikTok ao concluir o cadastro.</p></div></div>
            <div className="stats"><div><b>{points}</b><span>PONTOS</span></div><div><b>0</b><span>RECEBIDOS</span></div><div><b>1</b><span>ENVIADO</span></div></div>
            <button className="secondary-action" onClick={async () => { const supabase = createBrowserSupabaseClient(); await supabase.auth.signOut(); }}>Sair da conta</button>
          </section>
        )}
      </section>

      {view !== 'tutorial' && <nav className="bottom-nav" aria-label="Navegação principal">
        <button className={view === 'feed' || view === 'evaluate' ? 'active' : ''} onClick={() => setView('feed')}><span>01</span>For You</button>
        <button className={view === 'publish' ? 'active' : ''} onClick={() => setView('publish')}><span>02</span>Publicar</button>
        <button className={view === 'profile' ? 'active' : ''} onClick={() => setView('profile')}><span>03</span>Conta</button>
      </nav>}
    </main>
  );
}

function AuthScreen() {
  const [email, setEmail] = useState('');
  const [code, setCode] = useState('');
  const [step, setStep] = useState<'email' | 'code'>('email');
  const [status, setStatus] = useState('');
  const [sending, setSending] = useState(false);

  async function requestAccess(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!hasSupabaseConfig) return;
    setSending(true);
    setStatus('');
    const response = await fetch('/api/auth/otp/send', { method: 'POST', headers: { 'content-type': 'application/json' }, body: JSON.stringify({ email: email.trim() }) });
    const result = await response.json().catch(() => ({}));
    setSending(false);
    if (!response.ok) {
      setStatus(result.error === 'cooldown' ? 'Aguarde 60 segundos antes de pedir outro código.' : 'Não foi possível enviar o código. Tente novamente dentro de instantes.');
      return;
    }
    setStep('code');
    setStatus(`Enviámos um código de 6 dígitos para ${email.trim()}.`);
  }

  async function verifyCode(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!hasSupabaseConfig || !/^\d{6}$/.test(code)) {
      setStatus('Insira os 6 números recebidos no e-mail.');
      return;
    }
    setSending(true);
    setStatus('');
    const response = await fetch('/api/auth/otp/verify', { method: 'POST', headers: { 'content-type': 'application/json' }, body: JSON.stringify({ email: email.trim(), code }) });
    setSending(false);
    if (!response.ok) setStatus('Código inválido ou expirado. Peça um novo código.');
    else window.location.reload();
  }

  return <main className="auth-shell">
    <section className="auth-card" aria-labelledby="access-title">
      <Brand />
      <div className="eyebrow"><span className="live-dot" /> ENTRADA DA COMUNIDADE</div>
      <h1 id="access-title">Entre no pulso.</h1>
      <p>Use o seu e-mail. Não precisa criar nem memorizar uma palavra-passe.</p>
      {step === 'email' ? <form onSubmit={requestAccess}>
        <label htmlFor="email">O seu e-mail</label>
        <input id="email" type="email" value={email} onChange={(event) => setEmail(event.target.value)} placeholder="voce@email.com" autoComplete="email" required />
        <button className="primary-action" type="submit" disabled={sending}>{sending ? 'A enviar…' : 'Receber código de acesso'}</button>
      </form> : <form onSubmit={verifyCode}>
        <label htmlFor="access-code">Código de 6 dígitos</label>
        <input id="access-code" type="text" value={code} onChange={(event) => setCode(event.target.value.replace(/\D/g, '').slice(0, 6))} inputMode="numeric" autoComplete="one-time-code" pattern="[0-9]{6}" maxLength={6} placeholder="000000" required />
        <button className="primary-action" type="submit" disabled={sending}>{sending ? 'A verificar…' : 'Entrar no PULSO'}</button>
        <button className="secondary-action" type="button" onClick={() => { setCode(''); setStep('email'); setStatus(''); }} disabled={sending}>Alterar e-mail</button>
      </form>}
      {status && <p className="notice" role="status">{status}</p>}
    </section>
  </main>;
}

function Brand() {
  return <div className="brand brand-static"><span className="pulse-mark" aria-hidden="true"><i /><i /><i /></span><strong>PULSO</strong><small>TIKTOK FEEDBACK HELPERS</small></div>;
}

function CampaignCard({ admin = false, onAction }: { admin?: boolean; onAction: () => void }) {
  const current = admin ? {
    ...campaign,
    pulse: 'TUTORIAL · ADMIN',
    niche: 'BOAS-VINDAS',
    handle: '@perfil_do_admin',
    name: 'Perfil de boas-vindas',
    description: 'A primeira avaliação ativa a sua conta no PULSO.',
    request: 'A bio deixa claro o que este perfil oferece?',
    initials: 'P',
  } : campaign;

  return <article className="campaign-card">
    <div className="card-top"><span>{current.pulse}</span><span>{current.niche}</span></div>
    <div className="creator"><Avatar initials={current.initials} /><div><b>{current.handle}</b><span>{current.name}</span></div></div>
    <p className="creator-description">{current.description}</p>
    <div className="request"><span>PEDIDO DA VEZ</span><p>“{current.request}”</p></div>
    <button className="primary-action" onClick={onAction}>Conhecer e avaliar {!admin && <span>+1</span>}</button>
  </article>;
}

function Avatar({ initials, large = false }: { initials: string; large?: boolean }) {
  return <span className={large ? 'avatar avatar-large' : 'avatar'} aria-hidden="true">{initials}</span>;
}
