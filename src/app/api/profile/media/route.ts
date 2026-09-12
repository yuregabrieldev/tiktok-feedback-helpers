import { NextResponse } from 'next/server';
import { createClient } from '@supabase/supabase-js';
import { createAdminSupabaseClient } from '@/lib/supabase/admin';
import { createServerSupabaseClient } from '@/lib/supabase/server';

export const runtime = 'nodejs';

export async function GET(request: Request) {
  const server = await createServerSupabaseClient();
  let { data: { user } } = await server.auth.getUser();
  if (!user) {
    const token = request.headers.get('authorization')?.replace(/^Bearer\s+/i, '');
    if (token) {
      const direct = createClient(process.env.NEXT_PUBLIC_SUPABASE_URL!, process.env.NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY!, { auth: { persistSession: false } });
      const result = await direct.auth.getUser(token);
      user = result.data.user;
    }
  }
  if (!user) return NextResponse.json({ error: 'Sessão expirada. Entre novamente.' }, { status: 401 });
  const profileId = new URL(request.url).searchParams.get('profileId');
  const campaignId = new URL(request.url).searchParams.get('campaignId');
  if (!profileId && !campaignId) return NextResponse.json({ error: 'Perfil não informado.' }, { status: 400 });
  const admin = createAdminSupabaseClient();
  const { data: profile } = campaignId
    ? await admin.from('campaigns').select('campaign_avatar_path,campaign_screenshot_path,creator:profiles!campaigns_creator_id_fkey(avatar_path,tiktok_screenshot_path)').eq('id', campaignId).maybeSingle()
    : await admin.from('profiles').select('avatar_path,tiktok_screenshot_path').eq('id', profileId).maybeSingle();
  if (!profile) return NextResponse.json({ error: 'Perfil não encontrado.' }, { status: 404 });
  const creator = (profile as { creator?: { avatar_path?: string | null; tiktok_screenshot_path?: string | null } | null }).creator;
  const avatarPath = (profile as { campaign_avatar_path?: string | null; avatar_path?: string | null }).campaign_avatar_path || creator?.avatar_path || (profile as { avatar_path?: string | null }).avatar_path;
  const screenshotPath = (profile as { campaign_screenshot_path?: string | null; tiktok_screenshot_path?: string | null }).campaign_screenshot_path || creator?.tiktok_screenshot_path || (profile as { tiktok_screenshot_path?: string | null }).tiktok_screenshot_path;
  const [avatar, screenshot] = await Promise.all([
    avatarPath ? admin.storage.from('avatars-clean').createSignedUrl(avatarPath, 3600) : Promise.resolve({ data: null }),
    screenshotPath ? admin.storage.from('avatars-clean').createSignedUrl(screenshotPath, 3600) : Promise.resolve({ data: null }),
  ]);
  return NextResponse.json({ avatarUrl: avatar.data?.signedUrl ?? null, screenshotUrl: screenshot.data?.signedUrl ?? null });
}
