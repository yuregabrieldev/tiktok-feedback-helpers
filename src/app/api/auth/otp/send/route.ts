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
    if (upsertError) throw upsertError;
    const apiKey = process.env.BREVO_API_KEY;
    const senderEmail = process.env.BREVO_SENDER_EMAIL ?? 'noreply@auth.tkoi.online';
    const senderName = process.env.BREVO_SENDER_NAME ?? 'PULSO';
    if (!apiKey) throw new Error('brevo_not_configured');
    const delivery = await fetch('https://api.brevo.com/v3/smtp/email', { method: 'POST', headers: { accept: 'application/json', 'api-key': apiKey, 'content-type': 'application/json' }, body: JSON.stringify({ sender: { email: senderEmail, name: senderName }, to: [{ email }], subject: 'O seu código de acesso ao PULSO', htmlContent: `<div style="background:#f4f0e6;padding:32px;font-family:Arial,sans-serif;color:#101010"><div style="max-width:520px;margin:auto;border:1px solid #101010;background:#fbf9f2;padding:28px"><p style="font-size:12px;letter-spacing:2px">PULSO / TIKTOK FEEDBACK HELPERS</p><h1 style="font-size:34px;margin:24px 0 8px">O seu código</h1><p>Use este código para entrar na comunidade:</p><p style="font:700 42px monospace;letter-spacing:10px;margin:28px 0">${code}</p><p style="font-size:13px">Expira em 10 minutos. Se não pediu este acesso, ignore este e-mail.</p></div></div>` }) });
    if (!delivery.ok) throw new Error(`brevo_${delivery.status}`);
    return NextResponse.json({ ok: true });
  } catch (error) {
    console.error('[otp-send]', error instanceof Error ? error.message : 'unknown');
    return NextResponse.json({ error: 'delivery_failed' }, { status: 500 });
  }
}
