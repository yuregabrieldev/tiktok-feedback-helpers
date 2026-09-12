-- A feedback completes the evaluator's mission, never the campaign for everyone.
-- Campaign targets fund only the first N rewards; later unique feedback remains valid
-- but does not award points because no balance was reserved for it.
create or replace function public.start_mission(p_campaign_id uuid)
returns table (mission_id uuid, tiktok_profile_url text, eligible_after timestamptz, expires_at timestamptz)
language plpgsql security definer set search_path = public as $$
declare
  v_user uuid := auth.uid();
  v_campaign public.campaigns%rowtype;
  v_mission public.missions%rowtype;
  v_eligible timestamptz := now() + interval '15 seconds';
  v_expires timestamptz := now() + interval '20 minutes';
  v_tiktok_url text;
begin
  if v_user is null then raise exception 'not_authenticated'; end if;

  update public.missions as m
    set status = 'expired'
    where m.evaluator_id = v_user
      and m.status in ('started', 'ready_for_feedback')
      and m.expires_at < now();

  if exists (select 1 from public.missions where evaluator_id = v_user and status in ('started', 'ready_for_feedback')) then
    raise exception 'mission_in_progress';
  end if;

  select * into v_campaign from public.campaigns where id = p_campaign_id for update;
  if not found or v_campaign.status <> 'active' then raise exception 'campaign_unavailable'; end if;
  if v_campaign.creator_id = v_user then raise exception 'cannot_evaluate_own_campaign'; end if;
  if v_campaign.kind <> 'tutorial' and not exists (
    select 1 from public.profiles where id = v_user and tutorial_completed_at is not null
  ) then raise exception 'tutorial_required'; end if;
  if exists (select 1 from public.feedbacks where campaign_id = p_campaign_id and reviewer_id = v_user) then
    raise exception 'already_completed';
  end if;

  select coalesce(v_campaign.campaign_tiktok_profile_url, p.tiktok_profile_url)
    into v_tiktok_url from public.profiles p where p.id = v_campaign.creator_id;
  if v_tiktok_url is null then raise exception 'profile_link_unavailable'; end if;

  insert into public.missions(campaign_id, evaluator_id, eligible_after, expires_at)
    values (p_campaign_id, v_user, v_eligible, v_expires)
    returning * into v_mission;
  return query select v_mission.id, v_tiktok_url, v_mission.eligible_after, v_mission.expires_at;
end;
$$;

create or replace function public.submit_feedback(p_mission_id uuid, p_bio_clarity text, p_suggestion text)
returns table (awarded_points integer, tutorial_completed boolean, campaign_completed boolean)
language plpgsql security definer set search_path = public as $$
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

  select * into v_mission from public.missions
    where id = p_mission_id and evaluator_id = v_user for update;
  if not found then raise exception 'mission_not_found'; end if;
  if v_mission.status <> 'started' or now() < v_mission.eligible_after or now() > v_mission.expires_at then
    raise exception 'mission_not_eligible';
  end if;

  select * into v_campaign from public.campaigns where id = v_mission.campaign_id for update;
  if not found or v_campaign.status <> 'active' or v_campaign.creator_id = v_user then
    raise exception 'campaign_unavailable';
  end if;
  if exists (select 1 from public.feedbacks where campaign_id = v_campaign.id and reviewer_id = v_user) then
    raise exception 'already_completed';
  end if;

  insert into public.feedbacks (campaign_id, mission_id, reviewer_id, bio_clarity, suggestion)
    values (v_campaign.id, v_mission.id, v_user, p_bio_clarity, trim(p_suggestion))
    returning id into v_feedback_id;
  update public.missions set status = 'completed', completed_at = now() where id = v_mission.id;

  if v_campaign.kind = 'tutorial' then
    update public.profiles
      set tutorial_completed_at = coalesce(tutorial_completed_at, now())
      where id = v_user;
  elsif v_campaign.feedback_completed < v_campaign.feedback_target then
    v_award := v_campaign.reward_per_feedback;
    update public.campaigns
      set feedback_completed = feedback_completed + 1,
          reserved_points = greatest(0, reserved_points - v_award),
          updated_at = now()
      where id = v_campaign.id;

    if v_award > 0 then
      insert into public.point_ledger (profile_id, campaign_id, feedback_id, entry_type, amount, idempotency_key)
      values (
        v_user, v_campaign.id, v_feedback_id,
        case v_campaign.kind
          when 'featured' then 'featured_grant'::public.ledger_entry_type
          when 'seed' then 'seed_grant'::public.ledger_entry_type
          else 'feedback_reward'::public.ledger_entry_type
        end,
        v_award, gen_random_uuid()
      );
    end if;
  end if;

  return query select v_award, v_campaign.kind = 'tutorial', false;
end;
$$;
