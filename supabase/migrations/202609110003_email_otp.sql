create table if not exists public.email_login_codes (
  email text primary key,
  code_hash text not null,
  expires_at timestamptz not null,
  attempts smallint not null default 0,
  sent_at timestamptz not null default now(),
  consumed_at timestamptz
);

alter table public.email_login_codes enable row level security;
revoke all on table public.email_login_codes from anon, authenticated, public;
