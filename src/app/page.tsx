'use client';

import { ChangeEvent, FormEvent, useEffect, useState } from 'react';
import type { User } from '@supabase/supabase-js';
import { createBrowserSupabaseClient, hasSupabaseConfig } from '@/lib/supabase/browser';

type View = 'tutorial' | 'feed' | 'evaluate' | 'publish' | 'profile';
type MissionKind = 'tutorial' | 'standard';
type ProfileData = { display_name: string; username: string; tiktok_profile_url: string | null; niche: string; bio: string; avatar_path?: string | null; tiktok_screenshot_path?: string | null; tutorial_completed_at?: string | null; is_admin?: boolean };
type CampaignData = { id: string; kind: 'normal' | 'seed' | 'featured' | 'tutorial'; status?: string; niche: string | null; prompt: string; title: string; creator_id: string; campaign_display_name?: string | null; campaign_username?: string | null; campaign_tiktok_profile_url?: string | null; campaign_bio?: string | null; campaign_avatar_path?: string | null; campaign_screenshot_path?: string | null; creator: { display_name: string; username: string; tiktok_profile_url: string | null; niche: string | null; bio: string; avatarUrl?: string | null; screenshotUrl?: string | null } | null; feedback_completed: number; feedback_target: number };

const viewPaths: Record<View, string> = { tutorial: '/primeira-missao', feed: '/for-you', evaluate: '/avaliar', publish: '/publicar', profile: '/conta' };
const pathViews: Record<string, View> = Object.fromEntries(Object.entries(viewPaths).map(([view, path]) => [path, view as View]));

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
  const [campaigns, setCampaigns] = useState<CampaignData[]>([]);
  const [selectedCampaign, setSelectedCampaign] = useState<CampaignData | null>(null);
  const [missionId, setMissionId] = useState<string | null>(null);
  const [dataLoading, setDataLoading] = useState(false);
  const [receivedCount, setReceivedCount] = useState(0);
  const [sentCount, setSentCount] = useState(0);
  const [adminCampaigns, setAdminCampaigns] = useState<CampaignData[]>([]);
  const [adminEditingId, setAdminEditingId] = useState<string | null>(null);
  const [adminMissionMedia, setAdminMissionMedia] = useState<{ avatarUrl: string | null; screenshotUrl: string | null }>({ avatarUrl: null, screenshotUrl: null });
  const [adminDraft, setAdminDraft] = useState({ title: '', prompt: '', niche: '', status: 'active', feedback_target: 10, display_name: '', username: '', tiktok_profile_url: '', bio: '' });
  const [avatarUrl, setAvatarUrl] = useState<string | null>(null);
  const [screenshotUrl, setScreenshotUrl] = useState<string | null>(null);

  function navigateTo(nextView: View, replace = false) {
    if (typeof window !== 'undefined') {
      const path = viewPaths[nextView];
      (replace ? window.history.replaceState : window.history.pushState).call(window.history, {}, '', path);
    }
    setView(nextView);
  }

  useEffect(() => {
    const syncRoute = () => {
      const routedView = pathViews[window.location.pathname];
      if (routedView) setView(routedView);
    };
    syncRoute();
    window.addEventListener('popstate', syncRoute);
    return () => window.removeEventListener('popstate', syncRoute);
  }, []);

  useEffect(() => {
    if (!user || !hasSupabaseConfig) return;
    const supabase = createBrowserSupabaseClient();
    const refresh = () => void loadCommunityData(user.id);
    const channel = supabase.channel(`pulso-live-${user.id}`)
      .on('postgres_changes', { event: '*', schema: 'public', table: 'campaigns' }, refresh)
      .on('postgres_changes', { event: '*', schema: 'public', table: 'feedbacks' }, refresh)
      .on('postgres_changes', { event: '*', schema: 'public', table: 'profiles' }, refresh)
      .subscribe();
    return () => { void supabase.removeChannel(channel); };
  }, [user]);

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
          supabase.from('profiles').select('display_name,username,tiktok_profile_url,niche,bio,avatar_path,tiktok_screenshot_path,tutorial_completed_at,is_admin').eq('id', data.session.user.id).maybeSingle(),
          supabase.rpc('current_points'),
        ]);
        if (profileRow) { setProfile(profileRow as ProfileData); setProfileDraft(profileRow as ProfileData); const loadedProfile = profileRow as ProfileData; const authHeaders = data.session.access_token ? { Authorization: `Bearer ${data.session.access_token}` } : undefined; navigateTo(loadedProfile.is_admin || loadedProfile.tutorial_completed_at ? 'feed' : 'tutorial', true); if (loadedProfile.avatar_path) void fetch('/api/profile/avatar?kind=avatar', { headers: authHeaders }).then((response) => response.ok ? response.json() : null).then((result) => result?.url && setAvatarUrl(result.url)); if (loadedProfile.tiktok_screenshot_path) void fetch('/api/profile/avatar?kind=screenshot', { headers: authHeaders }).then((response) => response.ok ? response.json() : null).then((result) => result?.url && setScreenshotUrl(result.url)); }
        const isAdmin = Boolean((profileRow as ProfileData | null)?.is_admin);
        if (typeof pointTotal === 'number' && !isAdmin) setPoints(pointTotal);
        if (isAdmin) setPoints(0);
        await loadCommunityData(data.session.user.id);
      }
      setAuthLoading(false);
    });
    const { data: subscription } = supabase.auth.onAuthStateChange((_event, session) => setUser(session?.user ?? null));
    return () => subscription.subscription.unsubscribe();
  }, []);

  async function loadCommunityData(userId: string) {
    setDataLoading(true);
    const supabase = createBrowserSupabaseClient();
    const { data: sessionState } = await supabase.auth.getSession();
    const authHeaders = sessionState.session?.access_token ? { Authorization: `Bearer ${sessionState.session.access_token}` } : undefined;
    const { data: viewerRow } = await supabase.from('profiles').select('is_admin').eq('id', userId).maybeSingle();
    const viewer = viewerRow as { is_admin?: boolean } | null;
    let campaignQuery = supabase.from('campaigns').select('id,kind,status,niche,prompt,title,creator_id,feedback_completed,feedback_target,campaign_display_name,campaign_username,campaign_tiktok_profile_url,campaign_bio,creator:profiles!campaigns_creator_id_fkey(display_name,username,tiktok_profile_url,niche,bio)').order('created_at', { ascending: false });
    if (!viewer?.is_admin) campaignQuery = campaignQuery.eq('status', 'active');
    const [{ data: rows }, { data: feedbackRows }, { data: receivedRows }, { data: sentRows }] = await Promise.all([
      campaignQuery,
      supabase.from('feedbacks').select('campaign_id').eq('reviewer_id', userId),
      supabase.from('feedbacks').select('id,campaigns!inner(creator_id)').eq('campaigns.creator_id', userId),
      supabase.from('campaigns').select('id').eq('creator_id', userId),
    ]);
    const completed = new Set((feedbackRows ?? []).map((row: { campaign_id: string }) => row.campaign_id));
    const allCampaigns = (rows ?? []) as unknown as CampaignData[];
    const available = viewer?.is_admin ? allCampaigns.filter((row) => row.status !== 'removed') : allCampaigns.filter((row) => row.status === 'active' && (row.kind === 'tutorial' || ((!completed.has(row.id) || row.creator_id === userId) && row.feedback_completed < row.feedback_target)));
    if (viewer?.is_admin) setAdminCampaigns(allCampaigns.filter((row) => row.kind === 'tutorial' || row.kind === 'featured'));
    const withMedia = await Promise.all(available.map(async (item) => {
      if (!item.creator_id) return item;
      const mediaResponse = await fetch(`/api/profile/media?campaignId=${encodeURIComponent(item.id)}&profileId=${encodeURIComponent(item.creator_id)}`, { headers: authHeaders });
      const media = mediaResponse.ok ? await mediaResponse.json() : {};
      return { ...item, creator: item.creator ? { ...item.creator, display_name: item.campaign_display_name || item.creator.display_name, username: item.campaign_username || item.creator.username, tiktok_profile_url: item.campaign_tiktok_profile_url || item.creator.tiktok_profile_url, bio: item.campaign_bio || item.creator.bio, avatarUrl: media.avatarUrl, screenshotUrl: media.screenshotUrl } : item.creator };
    }));
    setCampaigns(withMedia);
    setSelectedCampaign((current) => current && withMedia.some((item) => item.id === current.id) ? current : withMedia.find((item) => item.creator_id !== userId) ?? withMedia[0] ?? null);
    setReceivedCount((receivedRows ?? []).length);
    setSentCount((sentRows ?? []).length);
    const { data: openMissionRow } = await supabase.from('missions').select('id,campaign_id,status,expires_at,campaigns!inner(id,kind,status,niche,prompt,title,creator_id,feedback_completed,feedback_target,creator:profiles!campaigns_creator_id_fkey(display_name,username,tiktok_profile_url,niche,bio))').eq('evaluator_id', userId).in('status', ['started', 'ready_for_feedback']).gt('expires_at', new Date().toISOString()).maybeSingle();
    const openMission = openMissionRow as { id: string; campaigns: CampaignData } | null;
    if (openMission?.id) {
      const missionCampaign = openMission.campaigns as unknown as CampaignData;
      // The open-mission query is intentionally small and does not include
      // private media paths. Resolve the creator media again before restoring
      // the evaluation screen after returning from TikTok.
      if (missionCampaign.creator_id) {
        const mediaResponse = await fetch(`/api/profile/media?profileId=${encodeURIComponent(missionCampaign.creator_id)}`, { headers: authHeaders });
        const media = mediaResponse.ok ? await mediaResponse.json() : {};
        if (missionCampaign.creator) {
          missionCampaign.creator = { ...missionCampaign.creator, avatarUrl: media.avatarUrl ?? null, screenshotUrl: media.screenshotUrl ?? null };
        }
      }
      setMissionId(openMission.id); setActiveMission(true); setSelectedCampaign(missionCampaign); setMissionKind(missionCampaign.kind === 'tutorial' ? 'tutorial' : 'standard'); navigateTo('evaluate');
    }
    setDataLoading(false);
  }

  async function beginMission(kind: MissionKind, item = selectedCampaign) {
    if (activeMission) {
      setNotice('Termine a missão aberta antes de iniciar outra.');
      navigateTo('evaluate');
      const openUrl = item?.creator?.tiktok_profile_url;
      if (openUrl) window.open(openUrl, '_blank', 'noopener,noreferrer');
      return;
    }
    if (!item) { setNotice('Não há campanhas disponíveis neste momento.'); return; }
    // Reserve the tab while this click is still a user gesture. Opening it only
    // after the RPC finishes is commonly blocked by mobile browsers as a popup.
    const tiktokTab = window.open(item.creator?.tiktok_profile_url || 'about:blank', '_blank', 'noopener,noreferrer');
    if (tiktokTab) tiktokTab.opener = null;
    const supabase = createBrowserSupabaseClient();
    const { data, error } = await supabase.rpc('start_mission', { p_campaign_id: item.id });
    if (error || !data?.[0]) { tiktokTab?.close(); const reason = error?.message || ''; setNotice(reason.includes('mission_in_progress') ? 'Você já tem uma missão aberta. Conclua-a antes de começar outra.' : reason.includes('campaign_full') ? 'Esta missão já recebeu todos os feedbacks.' : reason.includes('profile_link_unavailable') ? 'O perfil ainda não tem um link TikTok válido.' : reason.includes('already_completed') ? 'Você já avaliou esta campanha.' : `Não foi possível iniciar esta missão${reason ? `: ${reason}` : '.'}`); return; }
    setMissionId(data[0].mission_id);
    setSelectedCampaign(item);
    setActiveMission(true);
    setMissionKind(kind);
    setNotice('Missão iniciada. Ao voltar do TikTok, envie a sua avaliação.');
    navigateTo('evaluate');
    if (tiktokTab && data[0].tiktok_profile_url !== item.creator?.tiktok_profile_url) tiktokTab.location.replace(data[0].tiktok_profile_url);
    else window.open(data[0].tiktok_profile_url, '_blank', 'noopener,noreferrer');
  }

  async function submitFeedback(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!answer || feedback.trim().length < 20) {
      setNotice('Escolha uma resposta e escreva uma sugestão com pelo menos 20 caracteres.');
      return;
    }

    if (!missionId) { setNotice('Não há uma missão ativa para concluir.'); return; }
    const supabase = createBrowserSupabaseClient();
    const { data: result, error } = await supabase.rpc('submit_feedback', { p_mission_id: missionId, p_bio_clarity: answer === 'Sim' ? 'yes' : answer === 'Mais ou menos' ? 'partly' : 'no', p_suggestion: feedback.trim() });
    if (error) { setNotice(error.message.includes('not_eligible') ? 'Aguarde os 15 segundos de análise antes de enviar.' : 'Não foi possível enviar este feedback.'); return; }
    const awarded = result?.[0]?.awarded_points ?? 0;
    const { data: total } = await supabase.rpc('current_points');
    if (typeof total === 'number' && !profile.is_admin) setPoints(total);
    if (result?.[0]?.tutorial_completed) setProfile((current) => ({ ...current, tutorial_completed_at: new Date().toISOString() }));
    navigateTo('feed');
    setNotice(awarded > 0 ? `Feedback enviado. Ganhou ${awarded} ponto.` : 'Feedback enviado. A sua conta está ativa.');
    setActiveMission(false);
    setMissionId(null);
    setFeedback('');
    setAnswer(null);
    if (user) await loadCommunityData(user.id);
  }

  async function launchCampaign(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (activeMission) { setNotice('Termine a missão aberta antes de publicar uma campanha.'); navigateTo('evaluate'); return; }
    if (!profile.is_admin && points < 1) { setNotice('É preciso ter pelo menos 1 ponto para lançar uma campanha.'); return; }
    if (!profile.is_admin && (!profile.display_name.trim() || !profile.username.trim() || !profile.tiktok_profile_url || !profile.niche.trim() || !profile.avatar_path || !profile.tiktok_screenshot_path)) { setNotice('Atualize o seu perfil antes de publicar a primeira campanha.'); navigateTo('profile'); return; }
    if (campaignPrompt.trim().length < 12) { setNotice('Escreva uma pergunta com pelo menos 12 caracteres.'); return; }
    setPublishing(true);
    const supabase = createBrowserSupabaseClient();
    const { error } = await supabase.rpc(profile.is_admin ? 'create_featured_campaign' : 'create_normal_campaign', { p_prompt: campaignPrompt.trim(), p_niche: profile.niche || 'GERAL', p_feedback_target: 1 });
    setPublishing(false);
    if (error) { setNotice(error.message.includes('insufficient_points') ? 'Você não tem pontos suficientes.' : error.message.includes('profile_required') ? 'Atualize o seu perfil antes de publicar a primeira campanha.' : 'Não foi possível lançar a campanha agora.'); return; }
    if (!profile.is_admin) setPoints((value) => value - 1); setCampaignPrompt(''); setNotice('Campanha lançada. Ela já está disponível no For You.'); navigateTo('feed');
  }

  async function saveProfile(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const supabase = createBrowserSupabaseClient();
    if (!user) return;
    // Only client-editable fields are sent. Internal flags and media paths must
    // never be overwritten by a stale form snapshot from another session.
    const editableProfile = {
      display_name: profileDraft.display_name.trim(),
      username: profileDraft.username.trim().replace(/^@+/, ''),
      tiktok_profile_url: profileDraft.tiktok_profile_url?.trim() || null,
      niche: profileDraft.niche.trim(),
      bio: profileDraft.bio.trim(),
    };
    if (!editableProfile.display_name || !editableProfile.username || !editableProfile.tiktok_profile_url || !editableProfile.niche.trim() || !profileDraft.avatar_path || !profileDraft.tiktok_screenshot_path) {
      setNotice('Preencha todos os campos e adicione a foto e o screenshot do TikTok. A bio é opcional.');
      return;
    }
    const { error } = await supabase.from('profiles').update(editableProfile).eq('id', user.id);
    if (error) { setNotice('Não foi possível guardar o perfil. Verifique o link do TikTok.'); return; }
    setProfile((current) => ({ ...current, ...editableProfile }));
    setProfileDraft((current) => ({ ...current, ...editableProfile }));
    setProfileEditing(false); setNotice('Perfil atualizado.');
  }

  async function saveAdminCampaign(event: FormEvent<HTMLFormElement>, campaignId: string) {
    event.preventDefault();
    const supabase = createBrowserSupabaseClient();
    const { error } = await supabase.rpc('admin_update_campaign', { p_campaign_id: campaignId, p_title: adminDraft.title, p_prompt: adminDraft.prompt, p_niche: adminDraft.niche, p_status: adminDraft.status, p_feedback_target: adminDraft.feedback_target });
    if (error) { setNotice('Não foi possível guardar a campanha.'); return; }
    const { error: profileError } = await supabase.rpc('admin_update_campaign_profile', { p_campaign_id: campaignId, p_display_name: adminDraft.display_name, p_username: adminDraft.username, p_tiktok_profile_url: adminDraft.tiktok_profile_url, p_bio: adminDraft.bio });
    if (profileError) { setNotice('Campanha guardada, mas não foi possível guardar o perfil da missão.'); return; }
    setAdminEditingId(null); setNotice('Campanha atualizada.');
    if (user) await loadCommunityData(user.id);
  }

  async function deleteCampaign(campaignId: string) {
    if (!profile.is_admin) return;
    const supabase = createBrowserSupabaseClient();
    const { error } = await supabase.rpc('admin_delete_campaign', { p_campaign_id: campaignId });
    if (error) { setNotice('Não foi possível excluir a publicação.'); return; }
    setNotice('Publicação excluída.');
    if (user) await loadCommunityData(user.id);
  }

  async function editAdminCampaign(item: CampaignData) {
    setAdminEditingId(item.id);
    setAdminDraft({ title: item.title || '', prompt: item.prompt, niche: item.niche || '', status: item.status || 'active', feedback_target: item.feedback_target, display_name: item.campaign_display_name || item.creator?.display_name || '', username: item.campaign_username || item.creator?.username || '', tiktok_profile_url: item.campaign_tiktok_profile_url || item.creator?.tiktok_profile_url || '', bio: item.campaign_bio || item.creator?.bio || '' });
    const response = await fetch(`/api/profile/media?campaignId=${encodeURIComponent(item.id)}&profileId=${encodeURIComponent(item.creator_id)}`);
    setAdminMissionMedia(response.ok ? await response.json() : { avatarUrl: null, screenshotUrl: null });
  }

  async function uploadProfileImage(event: ChangeEvent<HTMLInputElement>, kind: 'avatar' | 'screenshot', campaignId?: string) {
    const file = event.target.files?.[0]; if (!file) return;
    const body = new FormData(); body.set('file', file); body.set('kind', kind);
    if (campaignId) body.set('campaignId', campaignId);
    const browser = createBrowserSupabaseClient();
    const { data: sessionState } = await browser.auth.getSession();
    const response = await fetch('/api/profile/avatar', { method: 'POST', headers: sessionState.session?.access_token ? { Authorization: `Bearer ${sessionState.session.access_token}` } : undefined, body });
    const result = await response.json().catch(() => ({}));
    if (!response.ok) { setNotice(result.error || 'Não foi possível processar a imagem.'); return; }
    if (campaignId) { setNotice(kind === 'avatar' ? 'Foto da missão atualizada.' : 'Screenshot da missão atualizado.'); return; }
    if (kind === 'avatar') setAvatarUrl(result.url ?? null); else setScreenshotUrl(result.url ?? null);
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
        <button className="brand" onClick={() => navigateTo(view === 'tutorial' ? 'tutorial' : 'feed')} aria-label="Ir para For You">
          <span className="pulse-mark" aria-hidden="true"><i /><i /><i /></span>
          <strong>PULSO</strong>
          <small>TIKTOK FEEDBACK HELPERS</small>
        </button>
        <span className="points">{profile.is_admin ? <b>ADMIN</b> : <><b>{points}</b> PONTO{points === 1 ? '' : 'S'}</>}</span>
      </header>

      <section className="screen" aria-labelledby="screen-title">
        <div className="eyebrow"><span className="live-dot" /> {view === 'tutorial' ? 'ACESSO PENDENTE' : 'COMUNIDADE ATIVA'}</div>
        <h1 id="screen-title">{title}</h1>

        {notice && view !== 'publish' && <button className="notice notice-action" role="status" onClick={() => activeMission && navigateTo('evaluate')}>{notice}</button>}

        {view === 'tutorial' && (
          <>
            <p className="intro">Antes de entrar na comunidade, complete a primeira avaliação.</p>
            {dataLoading ? <p className="empty-state">A carregar a missão de acesso…</p> : <CampaignCard campaign={campaigns.find((item) => item.kind === 'tutorial') ?? null} admin onAction={() => void beginMission('tutorial', campaigns.find((item) => item.kind === 'tutorial') ?? null)} />}
            <p className="rule">Esta missão ativa a sua conta. Não concede pontos.</p>
          </>
        )}

        {view === 'feed' && (
          <>
            <section className="progress-band" aria-label="Estado atual">
              <span>PRONTO PARA PUBLICAR</span>
              <b>{profile.is_admin ? 'Conta administrativa' : `${points} ponto${points === 1 ? '' : 's'} disponível${points === 1 ? '' : 'is'}`}</b>
            </section>
            {dataLoading ? <p className="empty-state">A carregar campanhas…</p> : profile.is_admin ? <div className="campaign-list">{campaigns.map((item) => <CampaignCard key={item.id} campaign={item} admin onAction={() => void beginMission('standard', item)} onDelete={() => void deleteCampaign(item.id)} />)}</div> : <CampaignCard campaign={selectedCampaign} owner={selectedCampaign?.creator_id === user.id} onAction={() => void beginMission('standard')} />}
            <section className="feed-next" aria-label="Próximas campanhas">
              <span>PRÓXIMOS PULSOS</span>
              <p>O feed será preenchido com campanhas que ainda não avaliou.</p>
            </section>
          </>
        )}

        {view === 'evaluate' && (
          <form className="evaluation" onSubmit={submitFeedback}>
            <span className="mission-tag">MISSÃO EM AVALIAÇÃO</span>
            <div className="mini-profile"><Avatar initials={(selectedCampaign?.creator?.display_name || selectedCampaign?.creator?.username || '?').slice(0, 1).toUpperCase()} src={selectedCampaign?.creator?.avatarUrl} /><div><b>@{selectedCampaign?.creator?.username || 'perfil'}</b><span>{selectedCampaign?.niche || 'COMUNIDADE'}</span></div></div>
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
            <span className="mission-tag">{profile.is_admin ? 'DESTAQUE · ADMIN' : 'A SUA CAMPANHA'}</span>
            <h2>Peça feedback sobre algo específico.</h2>
            <label htmlFor="campaign-question">O que quer saber?</label>
            <textarea id="campaign-question" value={campaignPrompt} onChange={(event) => setCampaignPrompt(event.target.value)} placeholder="A minha bio deixa claro o que eu posto?" minLength={12} maxLength={220} required />
            <div className="purchase-row"><span>1 feedback</span><b>1 ponto</b></div>
            <button className="primary-action" type="submit" disabled={publishing || (!profile.is_admin && points < 1)}>{publishing ? 'A lançar…' : profile.is_admin ? 'Publicar destaque' : 'Lançar campanha'} {!profile.is_admin && <span>−1</span>}</button>
          </form>
        )}

        {view === 'profile' && (
          <section className="account-card">
            <div className="account-head"><div className="avatar-edit-wrap"><Avatar initials={(profile.display_name || 'Y').slice(0, 1).toUpperCase()} src={avatarUrl} large />{profileEditing && <label className="avatar-edit" title="Alterar foto de perfil"><span aria-hidden="true">✎</span><input type="file" accept="image/jpeg,image/png,image/webp,image/heic,image/heif" onChange={(event) => void uploadProfileImage(event, 'avatar')} /></label>}</div><div><span>O SEU PERFIL</span><h2>{profile.username ? `@${profile.username.replace(/^@/, '')}` : '@o_seu_tiktok'}</h2><p>{profile.tiktok_profile_url || 'Adicione o seu link do TikTok para começar.'}</p></div></div>
            {profileEditing ? <form className="profile-form" onSubmit={saveProfile}>
              <label htmlFor="profile-name">Nome<input id="profile-name" value={profileDraft.display_name} onChange={(event) => setProfileDraft({ ...profileDraft, display_name: event.target.value })} required /></label>
              <label htmlFor="profile-username">@ do TikTok<input id="profile-username" value={profileDraft.username} onChange={(event) => setProfileDraft({ ...profileDraft, username: event.target.value.replace(/^@/, '') })} placeholder="o_seu_tiktok" required /></label>
              <label htmlFor="profile-url">Link do perfil TikTok<input id="profile-url" type="url" pattern="https://(www\\.)?tiktok\\.com/@[A-Za-z0-9._-]+/?" value={profileDraft.tiktok_profile_url ?? ''} onChange={(event) => setProfileDraft({ ...profileDraft, tiktok_profile_url: event.target.value })} placeholder="https://www.tiktok.com/@o_seu_tiktok" required /></label>
              <label htmlFor="profile-niche">Nicho<input id="profile-niche" value={profileDraft.niche} onChange={(event) => setProfileDraft({ ...profileDraft, niche: event.target.value })} placeholder="Ex.: receitas" required /></label>
              <label htmlFor="profile-bio">Bio<textarea id="profile-bio" value={profileDraft.bio} onChange={(event) => setProfileDraft({ ...profileDraft, bio: event.target.value })} maxLength={220} /></label>
              <div className="screenshot-field"><label className="upload-label">Screenshot do perfil TikTok<input type="file" accept="image/jpeg,image/png,image/webp,image/heic,image/heif" required={!profileDraft.tiktok_screenshot_path} onChange={(event) => void uploadProfileImage(event, 'screenshot')} /></label><button type="button" className="help-dot" title="Envie uma captura onde apareçam o nome e o @ do seu perfil no TikTok." aria-label="O que é o screenshot do perfil?">?</button><small>Mostra que o link pertence a si. O @ deve estar visível.</small></div>
              <button className="primary-action" type="submit">Guardar perfil</button>
              <button className="secondary-action" type="button" onClick={() => setProfileEditing(false)}>Cancelar</button>
            </form> : <button className="secondary-action edit-profile" onClick={() => { setProfileDraft(profile); setProfileEditing(true); }}>Editar perfil</button>}
            <div className="stats"><div><b>{profile.is_admin ? '—' : points}</b><span>{profile.is_admin ? 'ADMIN' : 'PONTOS'}</span></div><div><b>{receivedCount}</b><span>RECEBIDOS</span></div><div><b>{sentCount}</b><span>ENVIADAS</span></div></div>
            {profile.is_admin && <section className="admin-panel" aria-labelledby="admin-title">
              <div className="admin-panel-head"><span>ADMINISTRAÇÃO</span><h2 id="admin-title">PULSO e destaques</h2><p>Edite a missão principal e os destaques publicados a partir do seu perfil.</p></div>
              {adminCampaigns.length === 0 ? <p className="empty-state">Ainda não existem campanhas para administrar.</p> : adminCampaigns.map((item) => adminEditingId === item.id ? <form className="profile-form admin-edit-form" key={item.id} onSubmit={(event) => void saveAdminCampaign(event, item.id)}>
                <strong>{item.kind === 'tutorial' ? 'MISSÃO PRINCIPAL' : 'CAMPANHA'}</strong>
                <label>Título<input value={adminDraft.title} onChange={(event) => setAdminDraft({ ...adminDraft, title: event.target.value })} maxLength={120} required /></label>
                <label>Pergunta<textarea value={adminDraft.prompt} onChange={(event) => setAdminDraft({ ...adminDraft, prompt: event.target.value })} minLength={12} maxLength={220} required /></label>
                <label>Nicho<input value={adminDraft.niche} onChange={(event) => setAdminDraft({ ...adminDraft, niche: event.target.value })} maxLength={80} /></label>
                <label>Nome do perfil<input value={adminDraft.display_name} onChange={(event) => setAdminDraft({ ...adminDraft, display_name: event.target.value })} required /></label>
                <label>@ do TikTok<input value={adminDraft.username} onChange={(event) => setAdminDraft({ ...adminDraft, username: event.target.value.replace(/^@/, '') })} required /></label>
                <label>Link do perfil<input type="url" value={adminDraft.tiktok_profile_url} onChange={(event) => setAdminDraft({ ...adminDraft, tiktok_profile_url: event.target.value })} required /></label>
                <label>Bio<textarea value={adminDraft.bio} onChange={(event) => setAdminDraft({ ...adminDraft, bio: event.target.value })} maxLength={220} /></label>
                <label>Foto do perfil da missão<input type="file" accept="image/jpeg,image/png,image/webp,image/heic,image/heif" onChange={(event) => void uploadProfileImage(event, 'avatar', item.id)} />{adminMissionMedia.avatarUrl ? <small className="saved-media"><img src={adminMissionMedia.avatarUrl} alt="Última foto da missão" />Foto salva atualmente</small> : <small>Nenhuma foto salva</small>}</label>
                <label>Screenshot do perfil TikTok<input type="file" accept="image/jpeg,image/png,image/webp,image/heic,image/heif" onChange={(event) => void uploadProfileImage(event, 'screenshot', item.id)} />{adminMissionMedia.screenshotUrl ? <small className="saved-media"><img src={adminMissionMedia.screenshotUrl} alt="Último screenshot da missão" />Screenshot salvo atualmente</small> : <small>Nenhum screenshot salvo</small>}</label>
                <label>Estado<select value={adminDraft.status} onChange={(event) => setAdminDraft({ ...adminDraft, status: event.target.value })}><option value="active">Ativa</option><option value="paused">Pausada</option><option value="expired">Expirada</option></select></label>
                <div className="admin-actions"><button className="primary-action" type="submit">Guardar</button><button className="secondary-action" type="button" onClick={() => setAdminEditingId(null)}>Cancelar</button></div>
              </form> : <div className="admin-campaign-row" key={item.id}><div><span>{item.kind === 'tutorial' ? 'MISSÃO PRINCIPAL' : item.kind.toUpperCase()} · {item.status}</span><b>{item.title || item.prompt}</b><small>{item.feedback_completed}/{item.feedback_target} feedbacks</small></div><button className="secondary-action" onClick={() => editAdminCampaign(item)}>Editar</button></div>)}
            </section>}
            <button className="secondary-action" onClick={async () => { const supabase = createBrowserSupabaseClient(); await supabase.auth.signOut(); }}>Sair da conta</button>
          </section>
        )}
      </section>

      {view !== 'tutorial' && <nav className="bottom-nav" aria-label="Navegação principal">
        <button className={view === 'feed' || view === 'evaluate' ? 'active' : ''} onClick={() => navigateTo('feed')}><Icon name="home" /><span>For You</span></button>
        <button className={view === 'publish' ? 'active' : ''} onClick={() => navigateTo('publish')}><Icon name="plus" /><span>Publicar</span></button>
        <button className={view === 'profile' ? 'active' : ''} onClick={() => navigateTo('profile')}><Icon name="user" /><span>Conta</span></button>
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

function CampaignCard({ campaign, admin = false, owner = false, onAction, onDelete }: { campaign: CampaignData | null; admin?: boolean; owner?: boolean; onAction: () => void; onDelete?: () => void }) {
  if (!campaign) return <section className="empty-state"><b>{admin ? 'Missão de acesso indisponível' : 'Nenhum pulso disponível'}</b><p>Volte em instantes. As campanhas ativas aparecem aqui automaticamente.</p></section>;
  const creator = campaign.creator;
  return <article className="campaign-card">
    <div className="card-top"><span>{campaign.kind === 'tutorial' ? 'TUTORIAL · ADMIN' : `PULSO · ${campaign.id.slice(0, 4).toUpperCase()}`}</span><span>{campaign.niche || creator?.niche || 'GERAL'}</span></div>
    <div className="creator"><Avatar initials={(creator?.display_name || creator?.username || '?').slice(0, 1).toUpperCase()} src={creator?.avatarUrl} /><div><b>@{creator?.username || 'perfil'}</b><span>{creator?.display_name || 'Criador'}</span></div></div>
    <p className="creator-description">{creator?.bio || 'Peça uma leitura honesta de alguém da comunidade.'}</p>
    {creator?.screenshotUrl && <img className="profile-proof" src={creator.screenshotUrl} alt="Screenshot do perfil TikTok" />}
    <div className="request"><span>PEDIDO DA VEZ</span><p>“{campaign.prompt}”</p></div>
    {(!admin || campaign.kind === 'tutorial') && <button className="primary-action" onClick={onAction} disabled={owner}>{owner ? 'A sua campanha' : <>Conhecer e avaliar {!admin && <span>+1</span>}</>}</button>}
    {admin && onDelete && <button className="secondary-action admin-delete" onClick={onDelete}>Excluir publicação</button>}
  </article>;
}

function Avatar({ initials, src, large = false }: { initials: string; src?: string | null; large?: boolean }) {
  return <span className={large ? 'avatar avatar-large' : 'avatar'} aria-hidden="true">{src ? <img src={src} alt="" /> : initials}</span>;
}

function Icon({ name }: { name: 'home' | 'plus' | 'user' }) {
  if (name === 'plus') return <svg className="nav-icon" viewBox="0 0 24 24" aria-hidden="true"><path d="M12 5v14M5 12h14" /></svg>;
  if (name === 'user') return <svg className="nav-icon" viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="8" r="3.5" /><path d="M4.5 20c.7-3.4 3.1-5 7.5-5s6.8 1.6 7.5 5" /></svg>;
  return <svg className="nav-icon" viewBox="0 0 24 24" aria-hidden="true"><path d="m4 11 8-7 8 7v9H4z" /><path d="M9 20v-6h6v6" /></svg>;
}
