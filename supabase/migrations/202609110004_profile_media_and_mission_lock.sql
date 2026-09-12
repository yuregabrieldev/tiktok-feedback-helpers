alter table public.profiles add column if not exists tiktok_screenshot_path text;

create or replace function public.start_mission(p_campaign_id uuid)
returns table (mission_id uuid, tiktok_profile_url text, eligible_after timestamptz, expires_at timestamptz)
language plpgsql
security definer
set search_path = public
as $$
declare
  v_user uuid := auth.uid();
  v_campaign public.campaigns%rowtype;
  v_mission public.missions%rowtype;
  v_eligible timestamptz := now() + interval '15 seconds';
  v_expires timestamptz := now() + interval '20 minutes';
  v_tiktok_url text;
begin
  if v_user is null then raise exception 'not_authenticated'; end if;
  if exists (select 1 from public.missions where evaluator_id = v_user and status in ('started', 'ready_for_feedback')) then
    raise exception 'mission_in_progress';
  end if;
  select * into v_campaign from public.campaigns where id = p_campaign_id for update;
  if not found or v_campaign.status <> 'active' then raise exception 'campaign_unavailable'; end if;
  if v_campaign.creator_id = v_user then raise exception 'cannot_evaluate_own_campaign'; end if;
  if v_campaign.kind <> 'tutorial' and not exists (select 1 from public.profiles where id = v_user and tutorial_completed_at is not null) then
    raise exception 'tutorial_required';
  end if;
  if exists (select 1 from public.feedbacks where campaign_id = p_campaign_id and reviewer_id = v_user) then raise exception 'already_completed'; end if;
  if v_campaign.feedback_completed >= v_campaign.feedback_target then raise exception 'campaign_full'; end if;
  select p.tiktok_profile_url into v_tiktok_url from public.profiles as p where p.id = v_campaign.creator_id;
  if v_tiktok_url is null then raise exception 'profile_link_unavailable'; end if;
  update public.missions as m set status = 'expired' where m.campaign_id = p_campaign_id and m.evaluator_id = v_user and m.status in ('started', 'ready_for_feedback') and m.expires_at < now();
  insert into public.missions (campaign_id, evaluator_id, eligible_after, expires_at)
  values (p_campaign_id, v_user, v_eligible, v_expires)
  returning * into v_mission;
  return query select v_mission.id, v_tiktok_url, v_mission.eligible_after, v_mission.expires_at;
end;
$$;

revoke all on function public.start_mission(uuid) from public;
grant execute on function public.start_mission(uuid) to authenticated;
