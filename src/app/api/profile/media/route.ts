import { NextResponse } from 'next/server';
import { createAdminSupabaseClient } from '@/lib/supabase/admin';
import { createServerSupabaseClient } from '@/lib/supabase/server';

export const runtime = 'nodejs';

export async function GET(request: Request) {
  const server = await createServerSupabaseClient();
  const { data: { user } } = await server.auth.getUser();
  if (!user) return NextResponse.json({ error: 'Sessão expirada. Entre novamente.' }, { status: 401 });
  const profileId = new URL(request.url).searchParams.get('profileId');
  if (!profileId) return NextResponse.json({ error: 'Perfil não informado.' }, { status: 400 });
  const admin = createAdminSupabaseClient();
  const { data: profile } = await admin.from('profiles').select('avatar_path,tiktok_screenshot_path').eq('id', profileId).maybeSingle();
  if (!profile) return NextResponse.json({ error: 'Perfil não encontrado.' }, { status: 404 });
  const [avatar, screenshot] = await Promise.all([
    profile.avatar_path ? admin.storage.from('avatars-clean').createSignedUrl(profile.avatar_path, 3600) : Promise.resolve({ data: null }),
    profile.tiktok_screenshot_path ? admin.storage.from('avatars-clean').createSignedUrl(profile.tiktok_screenshot_path, 3600) : Promise.resolve({ data: null }),
  ]);
  return NextResponse.json({ avatarUrl: avatar.data?.signedUrl ?? null, screenshotUrl: screenshot.data?.signedUrl ?? null });
}
