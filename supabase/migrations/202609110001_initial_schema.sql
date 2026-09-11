-- PULSO MVP schema. Run through the Supabase CLI or SQL editor before enabling
-- server routes that create missions, campaigns or point movements.

create extension if not exists pgcrypto;

insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('avatars-clean', 'avatars-clean', false, 1048576, array['image/webp'])
on conflict (id) do nothing;

create type public.campaign_kind as enum ('normal', 'seed', 'featured', 'tutorial');
create type public.campaign_status as enum ('draft', 'active', 'paused', 'completed', 'expired', 'removed');
create type public.mission_status as enum ('started', 'ready_for_feedback', 'completed', 'expired', 'cancelled');
create type public.ledger_entry_type as enum ('campaign_reserve', 'feedback_reward', 'campaign_refund', 'seed_grant', 'featured_grant', 'admin_adjustment');

create table public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  display_name text not null default '',
  username text not null default '',
  tiktok_profile_url text,
  avatar_path text,
  niche text,
  bio text not null default '',
  tutorial_completed_at timestamptz,
  is_admin boolean not null default false,
  is_suspended boolean not null default false,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint profiles_username_length check (char_length(username) <= 80),
  constraint profiles_bio_length check (char_length(bio) <= 220),
  constraint profiles_tiktok_url check (
    tiktok_profile_url is null or tiktok_profile_url ~* '^https://(www\\.)?tiktok\\.com/@[A-Za-z0-9._-]+/?$'
  )
);

create table public.campaigns (
  id uuid primary key default gen_random_uuid(),
  creator_id uuid not null references public.profiles(id) on delete cascade,
  kind public.campaign_kind not null default 'normal',
  status public.campaign_status not null default 'draft',
  title text not null default '',
  prompt text not null,
  niche text,
  feedback_target smallint not null default 1 check (feedback_target between 1 and 10),
  feedback_completed smallint not null default 0 check (feedback_completed >= 0),
  reward_per_feedback smallint not null default 1 check (reward_per_feedback between 0 and 2),
  reserved_points integer not null default 0 check (reserved_points >= 0),
  featured_version integer,
  expires_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint campaigns_prompt_length check (char_length(prompt) between 12 and 220),
  constraint campaigns_completed_within_target check (feedback_completed <= feedback_target),
  constraint featured_versions_only_for_featured check (
    (kind = 'featured' and featured_version is not null) or (kind <> 'featured')
  )
);

create unique index one_active_normal_campaign_per_creator
  on public.campaigns (creator_id)
  where kind = 'normal' and status = 'active';

create unique index featured_version_once
  on public.campaigns (featured_version)
  where kind = 'featured';

create index campaigns_feed_index
  on public.campaigns (status, kind, created_at desc)
  where status = 'active';

create table public.missions (
  id uuid primary key default gen_random_uuid(),
  campaign_id uuid not null references public.campaigns(id) on delete cascade,
  evaluator_id uuid not null references public.profiles(id) on delete cascade,
  status public.mission_status not null default 'started',
  started_at timestamptz not null default now(),
  eligible_after timestamptz not null,
  expires_at timestamptz not null,
  completed_at timestamptz,
  created_at timestamptz not null default now(),
  constraint one_open_mission_per_campaign_evaluator unique (campaign_id, evaluator_id)
);

create table public.feedbacks (
  id uuid primary key default gen_random_uuid(),
  campaign_id uuid not null references public.campaigns(id) on delete cascade,
  mission_id uuid not null unique references public.missions(id) on delete cascade,
  reviewer_id uuid not null references public.profiles(id) on delete cascade,
  bio_clarity text not null check (bio_clarity in ('yes', 'partly', 'no')),
  suggestion text not null,
  status text not null default 'accepted' check (status in ('accepted', 'flagged', 'removed')),
  created_at timestamptz not null default now(),
  constraint one_feedback_per_campaign_reviewer unique (campaign_id, reviewer_id),
  constraint feedback_suggestion_length check (char_length(trim(suggestion)) between 20 and 400)
);

create index feedbacks_campaign_index on public.feedbacks (campaign_id, created_at desc);

create table public.point_ledger (
  id uuid primary key default gen_random_uuid(),
  profile_id uuid not null references public.profiles(id) on delete cascade,
  campaign_id uuid references public.campaigns(id) on delete set null,
  feedback_id uuid references public.feedbacks(id) on delete set null,
  entry_type public.ledger_entry_type not null,
  amount integer not null check (amount <> 0),
  idempotency_key uuid not null unique,
  created_at timestamptz not null default now()
);

create index point_ledger_profile_index on public.point_ledger (profile_id, created_at desc);

create table public.reports (
  id uuid primary key default gen_random_uuid(),
  reporter_id uuid not null references public.profiles(id) on delete cascade,
  target_profile_id uuid references public.profiles(id) on delete cascade,
  target_campaign_id uuid references public.campaigns(id) on delete cascade,
  reason text not null check (char_length(trim(reason)) between 3 and 500),
  status text not null default 'open' check (status in ('open', 'reviewing', 'closed')),
  created_at timestamptz not null default now(),
  constraint report_has_target check (target_profile_id is not null or target_campaign_id is not null)
);

create or replace function public.set_updated_at()
returns trigger
language plpgsql
security invoker
set search_path = ''
as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

create trigger profiles_set_updated_at before update on public.profiles
for each row execute function public.set_updated_at();

create trigger campaigns_set_updated_at before update on public.campaigns
for each row execute function public.set_updated_at();

create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  insert into public.profiles (id)
  values (new.id);
  return new;
end;
$$;

create trigger auth_user_created
  after insert on auth.users
  for each row execute procedure public.handle_new_user();

alter table public.profiles enable row level security;
alter table public.campaigns enable row level security;
alter table public.missions enable row level security;
alter table public.feedbacks enable row level security;
alter table public.point_ledger enable row level security;
alter table public.reports enable row level security;

create policy "profiles readable by authenticated users"
  on public.profiles for select to authenticated using (true);
create policy "users update their own profile"
  on public.profiles for update to authenticated using (id = auth.uid()) with check (id = auth.uid() and not is_admin and not is_suspended);

create policy "active campaigns visible to authenticated users"
  on public.campaigns for select to authenticated using (status = 'active' or creator_id = auth.uid());
create policy "users see their own missions"
  on public.missions for select to authenticated using (evaluator_id = auth.uid());
create policy "feedback visible to creator or reviewer"
  on public.feedbacks for select to authenticated using (
    reviewer_id = auth.uid() or exists (
      select 1 from public.campaigns c where c.id = campaign_id and c.creator_id = auth.uid()
    )
  );
create policy "users see their own point ledger"
  on public.point_ledger for select to authenticated using (profile_id = auth.uid());
create policy "users create their own reports"
  on public.reports for insert to authenticated with check (reporter_id = auth.uid());
create policy "users read their own reports"
  on public.reports for select to authenticated using (reporter_id = auth.uid());

-- No client insert/update/delete policies exist for campaigns, missions,
-- feedbacks or the point ledger. Those changes must use server-side, validated
-- transactional functions with the service role.
