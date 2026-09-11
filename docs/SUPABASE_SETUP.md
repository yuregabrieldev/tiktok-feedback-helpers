# Ativação do Supabase — PULSO

## 1. Aplicar as migrações

No painel do projeto Supabase, abra **SQL Editor** e execute, nesta ordem:

1. `supabase/migrations/202609110001_initial_schema.sql`
2. `supabase/migrations/202609110002_secure_rpc.sql`

As migrações criam as tabelas, RLS, bucket privado de avatares e as funções que protegem pontos, campanhas e feedbacks.

## 2. Configurar autenticação por e-mail

Em **Authentication → URL Configuration**, adicione:

- URL local: `http://localhost:3000/auth/callback`
- URL de produção: `https://SEU-DOMINIO.vercel.app/auth/callback`

Em **Authentication → Providers → Email**, deixe Magic Link ativo. O PULSO não usa palavra-passe no MVP.

## 3. Primeira conta administrativa

Depois de entrar uma vez no PULSO com o seu e-mail, localize o seu utilizador em `profiles` e execute:

```sql
update public.profiles
set is_admin = true
where id = 'UUID_DO_SEU_UTILIZADOR';
```

## 4. Campanhas iniciais

Cadastre os seus perfis TikTok como perfis válidos e crie:

- uma campanha `tutorial`, sem recompensa;
- uma campanha `seed`, recompensa de 1 ponto;
- quando desejar, uma campanha `featured`, recompensa de 2 pontos.

Use URLs de perfil no formato `https://www.tiktok.com/@username`. Não cadastre links de vídeo, páginas encurtadas ou outros domínios.

## 5. Avatar e moderação

O bucket `avatars-clean` é privado e o endpoint recria toda foto enviada como WebP, sem EXIF. Antes de liberar upload em produção, defina `IMAGE_MODERATION_ENDPOINT` na Vercel com um serviço de moderação de imagens que responda `{"allowed": true}` ou `{"allowed": false}`.

## 6. Variáveis de produção na Vercel

Cadastre exatamente estas variáveis no projeto Vercel:

```text
NEXT_PUBLIC_SUPABASE_URL
NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY
SUPABASE_SERVICE_ROLE_KEY
IMAGE_MODERATION_ENDPOINT
```

`SUPABASE_SERVICE_ROLE_KEY` nunca deve ser exposta como variável `NEXT_PUBLIC_*`.
