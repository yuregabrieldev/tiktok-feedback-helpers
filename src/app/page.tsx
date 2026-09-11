'use client';

import { ChangeEvent, FormEvent, useEffect, useState } from 'react';
import type { User } from '@supabase/supabase-js';
import { createBrowserSupabaseClient, hasSupabaseConfig } from '@/lib/supabase/browser';

type View = 'tutorial' | 'feed' | 'evaluate' | 'publish' | 'profile';
type MissionKind = 'tutorial' | 'standard';
type ProfileData = { display_name: string; username: string; tiktok_profile_url: string; niche: string; bio: string; avatar_path?: string | null; tiktok_screenshot_path?: string | null };

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
  const [activeMission, setActiveMission] = useState(false);
  const [profile, setProfile] = useState<ProfileData>({ display_name: '', username: '', tiktok_profile_url: '', niche: '', bio: '' });
  const [profileDraft, setProfileDraft] = useState(profile);
  const [profileEditing, setProfileEditing] = useState(false);
  const [campaignPrompt, setCampaignPrompt] = useState('');
  const [publishing, setPublishing] = useState(false);

  useEffect(() => {
    if (!hasSupabaseConfig) {
      setAuthLoading(false);
      return;
    }
    const supabase = createBrowserSupabaseClient();
    supabase.auth.getSession().then(async ({ data }) => {
      setUser(data.session?.user ?? null);
      if (data.session?.user) {
        const [{ data: profileRow }, { data: pointTotal }] = await Promise.all([
          supabase.from('profiles').select('display_name,username,tiktok_profile_url,niche,bio,avatar_path,tiktok_screenshot_path').eq('id', data.session.user.id).maybeSingle(),
          supabase.rpc('current_points'),
        ]);
        if (profileRow) { setProfile(profileRow as ProfileData); setProfileDraft(profileRow as ProfileData); }
        if (typeof pointTotal === 'number') setPoints(pointTotal);
      }
      setAuthLoading(false);
    });
    const { data: subscription } = supabase.auth.onAuthStateChange((_event, session) => setUser(session?.user ?? null));
    return () => subscription.subscription.unsubscribe();
  }, []);

  function beginMission(kind: MissionKind) {
    if (activeMission) {
      setNotice('Termine a missão aberta antes de iniciar outra.');
      setView('evaluate');
      return;
    }
    setActiveMission(true);
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
    setActiveMission(false);
    setFeedback('');
    setAnswer(null);
  }

  async function launchCampaign(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (activeMission) { setNotice('Termine a missão aberta antes de publicar uma campanha.'); setView('evaluate'); return; }
    if (points < 1) { setNotice('É preciso ter pelo menos 1 ponto para lançar uma campanha.'); return; }
    if (!profile.tiktok_profile_url) { setNotice('Complete o seu perfil com o link do TikTok antes de publicar.'); setView('profile'); return; }
    if (campaignPrompt.trim().length < 12) { setNotice('Escreva uma pergunta com pelo menos 12 caracteres.'); return; }
    setPublishing(true);
    const supabase = createBrowserSupabaseClient();
    const { error } = await supabase.rpc('create_normal_campaign', { p_prompt: campaignPrompt.trim(), p_niche: profile.niche || 'GERAL', p_feedback_target: 1 });
    setPublishing(false);
    if (error) { setNotice(error.message.includes('insufficient_points') ? 'Você não tem pontos suficientes.' : 'Não foi possível lançar a campanha agora.'); return; }
    setPoints((value) => value - 1); setCampaignPrompt(''); setNotice('Campanha lançada. Ela já está disponível no For You.'); setView('feed');
  }

  async function saveProfile(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const supabase = createBrowserSupabaseClient();
    const { error } = await supabase.from('profiles').update(profileDraft).eq('id', user?.id);
    if (error) { setNotice('Não foi possível guardar o perfil. Verifique o link do TikTok.'); return; }
    setProfile(profileDraft); setProfileEditing(false); setNotice('Perfil atualizado.');
  }

  async function uploadProfileImage(event: ChangeEvent<HTMLInputElement>, kind: 'avatar' | 'screenshot') {
    const file = event.target.files?.[0]; if (!file) return;
    const body = new FormData(); body.set('file', file); body.set('kind', kind);
    const response = await fetch('/api/profile/avatar', { method: 'POST', body });
    const result = await response.json().catch(() => ({}));
    if (!response.ok) { setNotice(result.error || 'Não foi possível processar a imagem.'); return; }
    setProfile((current) => ({ ...current, ...(kind === 'avatar' ? { avatar_path: result.path } : { tiktok_screenshot_path: result.path }) }));
    setProfileDraft((current) => ({ ...current, ...(kind === 'avatar' ? { avatar_path: result.path } : { tiktok_screenshot_path: result.path }) }));
    setNotice(kind === 'avatar' ? 'Foto de perfil atualizada.' : 'Screenshot do TikTok atualizada.');
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

        {notice && <button className="notice notice-action" role="status" onClick={() => activeMission && setView('evaluate')}>{notice}</button>}

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
          <form className="publish-card" onSubmit={launchCampaign}>
            <span className="mission-tag">A SUA CAMPANHA</span>
            <h2>Peça feedback sobre algo específico.</h2>
            <label htmlFor="campaign-question">O que quer saber?</label>
            <textarea id="campaign-question" value={campaignPrompt} onChange={(event) => setCampaignPrompt(event.target.value)} placeholder="A minha bio deixa claro o que eu posto?" minLength={12} maxLength={220} required />
            <div className="purchase-row"><span>1 feedback</span><b>1 ponto</b></div>
            <button className="primary-action" type="submit" disabled={publishing || points < 1}>{publishing ? 'A lançar…' : 'Lançar campanha'} <span>−1</span></button>
          </form>
        )}

        {view === 'profile' && (
          <section className="account-card">
            <div className="account-head"><div className="avatar-edit-wrap"><Avatar initials={(profile.display_name || 'Y').slice(0, 1).toUpperCase()} large />{profileEditing && <label className="avatar-edit" title="Alterar foto de perfil"><span aria-hidden="true">✎</span><input type="file" accept="image/jpeg,image/png,image/webp,image/heic,image/heif" onChange={(event) => void uploadProfileImage(event, 'avatar')} /></label>}</div><div><span>O SEU PERFIL</span><h2>{profile.username ? `@${profile.username.replace(/^@/, '')}` : '@o_seu_tiktok'}</h2><p>{profile.tiktok_profile_url || 'Adicione o seu link do TikTok para começar.'}</p></div></div>
            {profileEditing ? <form className="profile-form" onSubmit={saveProfile}>
              <label htmlFor="profile-name">Nome<input id="profile-name" value={profileDraft.display_name} onChange={(event) => setProfileDraft({ ...profileDraft, display_name: event.target.value })} /></label>
              <label htmlFor="profile-username">@ do TikTok<input id="profile-username" value={profileDraft.username} onChange={(event) => setProfileDraft({ ...profileDraft, username: event.target.value.replace(/^@/, '') })} placeholder="o_seu_tiktok" required /></label>
              <label htmlFor="profile-url">Link do perfil TikTok<input id="profile-url" type="url" pattern="https://(www\\.)?tiktok\\.com/@[A-Za-z0-9._-]+/?" value={profileDraft.tiktok_profile_url} onChange={(event) => setProfileDraft({ ...profileDraft, tiktok_profile_url: event.target.value })} placeholder="https://www.tiktok.com/@o_seu_tiktok" required /></label>
              <label htmlFor="profile-niche">Nicho<input id="profile-niche" value={profileDraft.niche} onChange={(event) => setProfileDraft({ ...profileDraft, niche: event.target.value })} placeholder="Ex.: receitas" /></label>
              <label htmlFor="profile-bio">Bio<textarea id="profile-bio" value={profileDraft.bio} onChange={(event) => setProfileDraft({ ...profileDraft, bio: event.target.value })} maxLength={220} /></label>
              <div className="screenshot-field"><label className="upload-label">Screenshot do perfil TikTok<input type="file" accept="image/jpeg,image/png,image/webp,image/heic,image/heif" onChange={(event) => void uploadProfileImage(event, 'screenshot')} /></label><button type="button" className="help-dot" title="Envie uma captura onde apareçam o nome e o @ do seu perfil no TikTok." aria-label="O que é o screenshot do perfil?">?</button><small>Mostra que o link pertence a si. O @ deve estar visível.</small></div>
              <button className="primary-action" type="submit">Guardar perfil</button>
              <button className="secondary-action" type="button" onClick={() => setProfileEditing(false)}>Cancelar</button>
            </form> : <button className="secondary-action edit-profile" onClick={() => { setProfileDraft(profile); setProfileEditing(true); }}>Editar perfil</button>}
            <div className="stats"><div><b>{points}</b><span>PONTOS</span></div><div><b>0</b><span>RECEBIDOS</span></div><div><b>1</b><span>ENVIADO</span></div></div>
            <button className="secondary-action" onClick={async () => { const supabase = createBrowserSupabaseClient(); await supabase.auth.signOut(); }}>Sair da conta</button>
          </section>
        )}
      </section>

      {view !== 'tutorial' && <nav className="bottom-nav" aria-label="Navegação principal">
        <button className={view === 'feed' || view === 'evaluate' ? 'active' : ''} onClick={() => setView('feed')}><Icon name="home" /><span>For You</span></button>
        <button className={view === 'publish' ? 'active' : ''} onClick={() => setView('publish')}><Icon name="plus" /><span>Publicar</span></button>
        <button className={view === 'profile' ? 'active' : ''} onClick={() => setView('profile')}><Icon name="user" /><span>Conta</span></button>
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
  const [resendIn, setResendIn] = useState(0);

  useEffect(() => {
    if (resendIn <= 0) return;
    const timer = window.setInterval(() => setResendIn((value) => Math.max(0, value - 1)), 1000);
    return () => window.clearInterval(timer);
  }, [resendIn]);

  useEffect(() => {
    if (step === 'code' && code.length === 6 && !sending) void verifyCode();
  }, [code, step]);

  async function sendCode() {
    if (!hasSupabaseConfig || !email.trim()) return;
    setSending(true);
    setStatus('');
    const response = await fetch('/api/auth/otp/send', { method: 'POST', headers: { 'content-type': 'application/json' }, body: JSON.stringify({ email: email.trim() }) });
    const result = await response.json().catch(() => ({}));
    setSending(false);
    if (!response.ok) {
      setStatus(result.error === 'cooldown' ? 'Aguarde 60 segundos antes de pedir outro código.' : 'Não foi possível enviar o código. Tente novamente dentro de instantes.');
      return;
    }
    setCode('');
    setStep('code');
    setResendIn(60);
    setStatus(`Enviámos um código de 6 dígitos para ${email.trim()}.`);
  }

  async function requestAccess(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    await sendCode();
  }

  async function verifyCode() {
    if (!hasSupabaseConfig || !/^\d{6}$/.test(code)) {
      setStatus('Insira os 6 números recebidos no e-mail.');
      return;
    }
    setSending(true);
    setStatus('');
    const response = await fetch('/api/auth/otp/verify', { method: 'POST', headers: { 'content-type': 'application/json' }, body: JSON.stringify({ email: email.trim(), code }) });
    const result = await response.json().catch(() => ({}));
    setSending(false);
    if (!response.ok || !result.session) {
      setStatus('Código inválido ou expirado. Peça um novo código.');
      return;
    }
    const supabase = createBrowserSupabaseClient();
    const { error } = await supabase.auth.setSession(result.session);
    if (error) {
      setStatus('Não foi possível iniciar a sessão. Tente novamente.');
      return;
    }
    window.location.reload();
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
      </form> : <form onSubmit={(event) => { event.preventDefault(); void verifyCode(); }}>
        <label htmlFor="access-code">Código de 6 dígitos</label>
        <input id="access-code" type="text" value={code} onChange={(event) => setCode(event.target.value.replace(/\D/g, '').slice(0, 6))} inputMode="numeric" autoComplete="one-time-code" pattern="[0-9]{6}" maxLength={6} placeholder="000000" required />
        <button className="primary-action" type="submit" disabled={sending}>{sending ? 'A verificar…' : 'Entrar no PULSO'}</button>
        <button className="secondary-action" type="button" onClick={() => void sendCode()} disabled={sending || resendIn > 0}>{resendIn > 0 ? `Reenviar código em 00:${String(resendIn).padStart(2, '0')}` : 'Reenviar código'}</button>
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
    handle: '@olipelomundo',
    name: 'Olí Indica',
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

function Icon({ name }: { name: 'home' | 'plus' | 'user' }) {
  if (name === 'plus') return <svg className="nav-icon" viewBox="0 0 24 24" aria-hidden="true"><path d="M12 5v14M5 12h14" /></svg>;
  if (name === 'user') return <svg className="nav-icon" viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="8" r="3.5" /><path d="M4.5 20c.7-3.4 3.1-5 7.5-5s6.8 1.6 7.5 5" /></svg>;
  return <svg className="nav-icon" viewBox="0 0 24 24" aria-hidden="true"><path d="m4 11 8-7 8 7v9H4z" /><path d="M9 20v-6h6v6" /></svg>;
}
