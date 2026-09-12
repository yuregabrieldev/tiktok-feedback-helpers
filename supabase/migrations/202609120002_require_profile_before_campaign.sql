-- A regular member must save a complete profile before creating their first campaign.
-- Administrators remain exempt from this community requirement.
create or replace function public.create_normal_campaign(p_prompt text, p_niche text, p_feedback_target smallint)
returns uuid
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_user uuid := auth.uid();
  v_profile public.profiles%rowtype;
  v_is_admin boolean;
  v_points integer;
  v_campaign_id uuid;
begin
  if v_user is null then raise exception 'not_authenticated'; end if;
  select * into v_profile from public.profiles where id = v_user;
  v_is_admin := coalesce(v_profile.is_admin, false) and not coalesce(v_profile.is_suspended, false);
  if not v_is_admin and v_profile.tutorial_completed_at is null then raise exception 'tutorial_required'; end if;
  if not v_is_admin and (
    char_length(trim(coalesce(v_profile.display_name, ''))) = 0 or
    char_length(trim(coalesce(v_profile.username, ''))) = 0 or
    v_profile.tiktok_profile_url is null or
    char_length(trim(coalesce(v_profile.niche, ''))) = 0 or
    v_profile.avatar_path is null or
    v_profile.tiktok_screenshot_path is null
  ) then raise exception 'profile_required'; end if;
  if p_feedback_target not in (1, 3, 5) then raise exception 'invalid_target'; end if;
  if char_length(trim(p_prompt)) < 12 or char_length(trim(p_prompt)) > 220 then raise exception 'invalid_prompt'; end if;
  if exists (select 1 from public.campaigns where creator_id = v_user and kind = 'normal' and status = 'active') then raise exception 'active_campaign_exists'; end if;
  if not v_is_admin then
    select public.current_points() into v_points;
    if v_points < p_feedback_target then raise exception 'insufficient_points'; end if;
  end if;
  insert into public.campaigns (creator_id, kind, status, prompt, niche, feedback_target, reward_per_feedback, reserved_points)
  values (v_user, 'normal', 'active', trim(p_prompt), nullif(trim(p_niche), ''), p_feedback_target, 1, case when v_is_admin then 0 else p_feedback_target end)
  returning id into v_campaign_id;
  if not v_is_admin then
    insert into public.point_ledger (profile_id, campaign_id, entry_type, amount, idempotency_key)
    values (v_user, v_campaign_id, 'campaign_reserve', -p_feedback_target, gen_random_uuid());
  end if;
  return v_campaign_id;
end;
$$;

revoke all on function public.create_normal_campaign(text, text, smallint) from public;
grant execute on function public.create_normal_campaign(text, text, smallint) to authenticated;

-- Defense in depth for callers that bypass the RPC implementation.
create or replace function public.enforce_complete_profile_for_campaign()
returns trigger language plpgsql security definer set search_path = '' as $$
begin
  if not coalesce((select is_admin from public.profiles where id = auth.uid()), false)
     and not exists (
       select 1 from public.profiles
       where id = auth.uid() and is_suspended = false
         and char_length(trim(display_name)) > 0
         and char_length(trim(username)) > 0
         and tiktok_profile_url is not null
         and char_length(trim(coalesce(niche, ''))) > 0
         and avatar_path is not null
         and tiktok_screenshot_path is not null
     ) then raise exception 'profile_required';
  end if;
  return new;
end;
$$;

drop trigger if exists campaigns_require_profile on public.campaigns;
create trigger campaigns_require_profile before insert on public.campaigns
for each row when (new.kind = 'normal') execute function public.enforce_complete_profile_for_campaign();
