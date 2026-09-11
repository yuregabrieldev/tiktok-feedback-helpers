import { createHash } from 'node:crypto';
import { NextResponse } from 'next/server';
import { createClient } from '@supabase/supabase-js';
import { createServerClient } from '@supabase/ssr';
import { cookies } from 'next/headers';
import { createAdminSupabaseClient } from '@/lib/supabase/admin';

const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const normalize = (value: string) => value.trim().toLowerCase();
const hashCode = (email: string, code: string) => createHash('sha256').update(`${email}:${code}:${process.env.OTP_PEPPER ?? process.env.SUPABASE_SERVICE_ROLE_KEY ?? ''}`).digest('hex');

export async function POST(request: Request) {
  try {
    const body = await request.json() as { email?: unknown; code?: unknown };
    const email = typeof body.email === 'string' ? normalize(body.email) : '';
    const code = typeof body.code === 'string' ? body.code : '';
    if (!emailPattern.test(email) || !/^\d{6}$/.test(code)) return NextResponse.json({ error: 'invalid_code' }, { status: 400 });
    const admin = createAdminSupabaseClient();
    const { data: record } = await admin.from('email_login_codes').select('*').eq('email', email).maybeSingle();
    if (!record || record.consumed_at || new Date(record.expires_at).getTime() < Date.now() || record.attempts >= 5) return NextResponse.json({ error: 'invalid_code' }, { status: 400 });
    if (record.code_hash !== hashCode(email, code)) {
      await admin.from('email_login_codes').update({ attempts: record.attempts + 1 }).eq('email', email);
      return NextResponse.json({ error: 'invalid_code' }, { status: 400 });
    }
    await admin.from('email_login_codes').update({ consumed_at: new Date().toISOString() }).eq('email', email);
    const { data: link, error: linkError } = await admin.auth.admin.generateLink({ type: 'magiclink', email });
    const tokenHash = link?.properties?.hashed_token;
    if (linkError || !tokenHash) throw linkError ?? new Error('session_link_failed');
    const cookieStore = await cookies();
    const response = NextResponse.json({ ok: true });
    const supabase = createServerClient(process.env.NEXT_PUBLIC_SUPABASE_URL!, process.env.NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY!, { cookies: { getAll: () => cookieStore.getAll(), setAll: values => values.forEach(({ name, value, options }) => response.cookies.set(name, value, options)) } });
    const { data: authData, error: verifyError } = await supabase.auth.verifyOtp({ token_hash: tokenHash, type: 'email' });
    if (verifyError) throw verifyError;
    const finalResponse = NextResponse.json({ ok: true, session: authData.session }, { headers: { 'Cache-Control': 'no-store' } });
    response.cookies.getAll().forEach((cookie) => finalResponse.cookies.set(cookie));
    return finalResponse;
  } catch (error) {
    console.error('[otp-verify]', error instanceof Error ? error.message : 'unknown');
    return NextResponse.json({ error: 'verification_failed' }, { status: 500 });
  }
}
