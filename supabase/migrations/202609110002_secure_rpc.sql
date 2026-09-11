-- Security-definer functions are the only supported mutation path for the
-- point economy. They are callable by authenticated users but derive identity
-- exclusively from auth.uid().

alter table public.missions drop constraint if exists one_open_mission_per_campaign_evaluator;
create unique index one_live_mission_per_campaign_evaluator
  on public.missions (campaign_id, evaluator_id)
  where status in ('started', 'ready_for_feedback');

create or replace function public.current_points()
returns integer
language sql
stable
security invoker
set search_path = ''
as $$
  select coalesce(sum(amount), 0)::integer
  from public.point_ledger
  where profile_id = auth.uid()
$$;

create or replace function public.start_mission(p_campaign_id uuid)
returns table (mission_id uuid, tiktok_profile_url text, eligible_after timestamptz, expires_at timestamptz)
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_user uuid := auth.uid();
  v_campaign public.campaigns%rowtype;
  v_tiktok_url text;
  v_mission uuid;
  v_eligible timestamptz := now() + interval '15 seconds';
  v_expires timestamptz := now() + interval '20 minutes';
begin
  if v_user is null then raise exception 'not_authenticated'; end if;

  select * into v_campaign from public.campaigns where id = p_campaign_id for update;
  if not found or v_campaign.status <> 'active' then raise exception 'campaign_unavailable'; end if;
  if v_campaign.creator_id = v_user then raise exception 'cannot_evaluate_own_campaign'; end if;
  if v_campaign.kind <> 'tutorial' and not exists (select 1 from public.profiles where id = v_user and tutorial_completed_at is not null) then
    raise exception 'tutorial_required';
  end if;
  if exists (select 1 from public.feedbacks where campaign_id = p_campaign_id and reviewer_id = v_user) then raise exception 'already_completed'; end if;
  if v_campaign.feedback_completed >= v_campaign.feedback_target then raise exception 'campaign_full'; end if;

  select tiktok_profile_url into v_tiktok_url from public.profiles where id = v_campaign.creator_id;
  if v_tiktok_url is null then raise exception 'profile_link_unavailable'; end if;

  update public.missions set status = 'expired' where campaign_id = p_campaign_id and evaluator_id = v_user and status in ('started', 'ready_for_feedback') and expires_at < now();
  insert into public.missions (campaign_id, evaluator_id, eligible_after, expires_at)
  values (p_campaign_id, v_user, v_eligible, v_expires)
  returning id into v_mission;

  return query select v_mission, v_tiktok_url, v_eligible, v_expires;
end;
$$;

create or replace function public.submit_feedback(p_mission_id uuid, p_bio_clarity text, p_suggestion text)
returns table (awarded_points integer, tutorial_completed boolean, campaign_completed boolean)
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_user uuid := auth.uid();
  v_mission public.missions%rowtype;
  v_campaign public.campaigns%rowtype;
  v_award integer := 0;
  v_feedback_id uuid;
begin
  if v_user is null then raise exception 'not_authenticated'; end if;
  if p_bio_clarity not in ('yes', 'partly', 'no') then raise exception 'invalid_answer'; end if;
  if char_length(trim(p_suggestion)) < 20 or char_length(trim(p_suggestion)) > 400 then raise exception 'invalid_suggestion'; end if;

  select * into v_mission from public.missions where id = p_mission_id and evaluator_id = v_user for update;
  if not found then raise exception 'mission_not_found'; end if;
  if v_mission.status <> 'started' or now() < v_mission.eligible_after or now() > v_mission.expires_at then raise exception 'mission_not_eligible'; end if;

  select * into v_campaign from public.campaigns where id = v_mission.campaign_id for update;
  if not found or v_campaign.status <> 'active' or v_campaign.creator_id = v_user then raise exception 'campaign_unavailable'; end if;
  if v_campaign.feedback_completed >= v_campaign.feedback_target then raise exception 'campaign_full'; end if;
  if exists (select 1 from public.feedbacks where campaign_id = v_campaign.id and reviewer_id = v_user) then raise exception 'already_completed'; end if;

  insert into public.feedbacks (campaign_id, mission_id, reviewer_id, bio_clarity, suggestion)
  values (v_campaign.id, v_mission.id, v_user, p_bio_clarity, trim(p_suggestion))
  returning id into v_feedback_id;

  update public.missions set status = 'completed', completed_at = now() where id = v_mission.id;
  update public.campaigns
    set feedback_completed = feedback_completed + 1,
        reserved_points = greatest(0, reserved_points - reward_per_feedback),
        status = case when feedback_completed + 1 >= feedback_target then 'completed' else status end
    where id = v_campaign.id;

  if v_campaign.kind = 'tutorial' then
    update public.profiles set tutorial_completed_at = coalesce(tutorial_completed_at, now()) where id = v_user;
  else
    v_award := v_campaign.reward_per_feedback;
    if v_award > 0 then
      insert into public.point_ledger (profile_id, campaign_id, feedback_id, entry_type, amount, idempotency_key)
      values (
        v_user,
        v_campaign.id,
        v_feedback_id,
        case v_campaign.kind when 'featured' then 'featured_grant'::public.ledger_entry_type when 'seed' then 'seed_grant'::public.ledger_entry_type else 'feedback_reward'::public.ledger_entry_type end,
        v_award,
        gen_random_uuid()
      );
    end if;
  end if;

  return query select v_award, v_campaign.kind = 'tutorial', v_campaign.feedback_completed + 1 >= v_campaign.feedback_target;
end;
$$;

create or replace function public.create_normal_campaign(p_prompt text, p_niche text, p_feedback_target smallint)
returns uuid
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_user uuid := auth.uid();
  v_points integer;
  v_campaign_id uuid;
begin
  if v_user is null then raise exception 'not_authenticated'; end if;
  if not exists (select 1 from public.profiles where id = v_user and tutorial_completed_at is not null and is_suspended = false) then raise exception 'tutorial_required'; end if;
  if p_feedback_target not in (1, 3, 5) then raise exception 'invalid_target'; end if;
  if char_length(trim(p_prompt)) < 12 or char_length(trim(p_prompt)) > 220 then raise exception 'invalid_prompt'; end if;
  if exists (select 1 from public.campaigns where creator_id = v_user and kind = 'normal' and status = 'active') then raise exception 'active_campaign_exists'; end if;

  select public.current_points() into v_points;
  if v_points < p_feedback_target then raise exception 'insufficient_points'; end if;

  insert into public.campaigns (creator_id, kind, status, prompt, niche, feedback_target, reward_per_feedback, reserved_points)
  values (v_user, 'normal', 'active', trim(p_prompt), nullif(trim(p_niche), ''), p_feedback_target, 1, p_feedback_target)
  returning id into v_campaign_id;

  insert into public.point_ledger (profile_id, campaign_id, entry_type, amount, idempotency_key)
  values (v_user, v_campaign_id, 'campaign_reserve', -p_feedback_target, gen_random_uuid());

  return v_campaign_id;
end;
$$;

revoke all on function public.start_mission(uuid) from public;
revoke all on function public.submit_feedback(uuid, text, text) from public;
revoke all on function public.create_normal_campaign(text, text, smallint) from public;
grant execute on function public.current_points() to authenticated;
grant execute on function public.start_mission(uuid) to authenticated;
grant execute on function public.submit_feedback(uuid, text, text) to authenticated;
grant execute on function public.create_normal_campaign(text, text, smallint) to authenticated;
