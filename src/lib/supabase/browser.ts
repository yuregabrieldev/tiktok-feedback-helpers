import { createClient, type SupabaseClient } from '@supabase/supabase-js';

const url = process.env.NEXT_PUBLIC_SUPABASE_URL;
const key = process.env.NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY;

export const hasSupabaseConfig = Boolean(url && key);

// The project does not generate Supabase database types yet; keep the
// singleton untyped so RPC payloads remain validated by the database.
let browserClient: SupabaseClient<any> | null = null;

export function createBrowserSupabaseClient() {
  if (!url || !key) {
    throw new Error('Supabase não está configurado.');
  }

  if (browserClient) return browserClient;
  browserClient = createClient(url, key, {
    auth: {
      persistSession: true,
      autoRefreshToken: true,
      detectSessionInUrl: true,
    },
  });
  return browserClient;
}
