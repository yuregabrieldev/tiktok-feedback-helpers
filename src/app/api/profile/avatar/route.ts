import { NextResponse } from 'next/server';
import sharp from 'sharp';
import { createAdminSupabaseClient } from '@/lib/supabase/admin';
import { createServerSupabaseClient } from '@/lib/supabase/server';

export const runtime = 'nodejs';

const allowedFormats = new Set(['jpeg', 'png', 'webp', 'heif']);
const maxSourceBytes = 8 * 1024 * 1024;
const minDimension = 160;
const maxDimension = 4096;

export async function POST(request: Request) {
  const supabase = await createServerSupabaseClient();
  const { data: { user } } = await supabase.auth.getUser();
  if (!user) return NextResponse.json({ error: 'Não autenticado.' }, { status: 401 });

  const formData = await request.formData();
  const upload = formData.get('file');
  const kind = formData.get('kind') === 'screenshot' ? 'screenshot' : 'avatar';
  if (!(upload instanceof File)) return NextResponse.json({ error: 'Envie uma imagem.' }, { status: 400 });
  if (upload.size === 0 || upload.size > maxSourceBytes) return NextResponse.json({ error: 'A imagem deve ter até 8 MB.' }, { status: 400 });

  try {
    const source = Buffer.from(await upload.arrayBuffer());
    const image = sharp(source, { limitInputPixels: maxDimension * maxDimension });
    const metadata = await image.metadata();

    if (!metadata.format || !allowedFormats.has(metadata.format) || !metadata.width || !metadata.height) {
      return NextResponse.json({ error: 'Formato de imagem não suportado.' }, { status: 400 });
    }
    if (metadata.width < minDimension || metadata.height < minDimension || metadata.width > maxDimension || metadata.height > maxDimension) {
      return NextResponse.json({ error: 'Use uma imagem entre 160 e 4096 pixels.' }, { status: 400 });
    }

    // Decode and recreate the image. This strips EXIF (including location),
    // rejects non-image payloads and ensures the original is never public.
    const safeImage = await image.rotate().resize(768, 768, { fit: 'cover', withoutEnlargement: true }).webp({ quality: 84 }).toBuffer();
    // If configured, run an external nudity/content classifier after the
    // image has been decoded and rebuilt. Without one, Sharp still strips
    // metadata, rejects non-images and stores only the normalized WebP.
    const moderationEndpoint = process.env.IMAGE_MODERATION_ENDPOINT;
    if (moderationEndpoint) {
      const moderation = await fetch(moderationEndpoint, {
        method: 'POST',
        headers: { 'content-type': 'application/json' },
        body: JSON.stringify({ imageBase64: safeImage.toString('base64'), mimeType: 'image/webp' }),
        signal: AbortSignal.timeout(8_000),
      });
      const result = moderation.ok ? await moderation.json() as { allowed?: boolean } : null;
      if (!result?.allowed) return NextResponse.json({ error: 'Não foi possível usar esta imagem.' }, { status: 400 });
    }
    const admin = createAdminSupabaseClient();
    const filePath = `${user.id}/${kind}-${crypto.randomUUID()}.webp`;
    const { error: uploadError } = await admin.storage.from('avatars-clean').upload(filePath, safeImage, {
      contentType: 'image/webp',
      upsert: false,
      cacheControl: '31536000',
    });
    if (uploadError) throw uploadError;

    const { error: profileError } = await admin.from('profiles').update(kind === 'screenshot' ? { tiktok_screenshot_path: filePath } : { avatar_path: filePath }).eq('id', user.id);
    if (profileError) throw profileError;

    return NextResponse.json({ path: filePath });
  } catch {
    return NextResponse.json({ error: 'Não foi possível processar esta imagem.' }, { status: 400 });
  }
}
