import { createHash, randomInt } from 'node:crypto';
import { NextResponse } from 'next/server';
import { createAdminSupabaseClient } from '@/lib/supabase/admin';

const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const normalize = (value: string) => value.trim().toLowerCase();
const hashCode = (email: string, code: string) => createHash('sha256').update(`${email}:${code}:${process.env.OTP_PEPPER ?? process.env.SUPABASE_SERVICE_ROLE_KEY ?? ''}`).digest('hex');

export async function POST(request: Request) {
  try {
    const body = await request.json() as { email?: unknown };
    const email = typeof body.email === 'string' ? normalize(body.email) : '';
    if (!emailPattern.test(email) || email.length > 254) return NextResponse.json({ error: 'invalid_email' }, { status: 400 });
    const admin = createAdminSupabaseClient();
    const { data: prior } = await admin.from('email_login_codes').select('sent_at').eq('email', email).maybeSingle();
    if (prior && Date.now() - new Date(prior.sent_at).getTime() < 60_000) return NextResponse.json({ error: 'cooldown' }, { status: 429 });
    const code = String(randomInt(0, 1_000_000)).padStart(6, '0');
    const { error: upsertError } = await admin.from('email_login_codes').upsert({ email, code_hash: hashCode(email, code), expires_at: new Date(Date.now() + 10 * 60_000).toISOString(), attempts: 0, sent_at: new Date().toISOString(), consumed_at: null });
    if (upsertError) throw new Error(`otp_upsert_${upsertError.code ?? 'unknown'}:${upsertError.message}`);
    const apiKey = process.env.BREVO_API_KEY;
    const senderEmail = process.env.BREVO_SENDER_EMAIL ?? 'noreply@auth.tkoi.online';
    const senderName = process.env.BREVO_SENDER_NAME ?? 'PULSO';
    if (!apiKey) throw new Error('brevo_not_configured');
    const delivery = await fetch('https://api.brevo.com/v3/smtp/email', { method: 'POST', headers: { accept: 'application/json', 'api-key': apiKey, 'content-type': 'application/json' }, body: JSON.stringify({ templateId: 3, sender: { email: senderEmail, name: senderName }, to: [{ email }], params: { code } }) });
    if (!delivery.ok) throw new Error(`brevo_${delivery.status}`);
    return NextResponse.json({ ok: true });
  } catch (error) {
    console.error('[otp-send]', error instanceof Error ? error.message : JSON.stringify(error));
    return NextResponse.json({ error: 'delivery_failed' }, { status: 500 });
  }
}
