-- Administrators can publish campaigns without consuming community points.
create or replace function public.create_normal_campaign(p_prompt text, p_niche text, p_feedback_target smallint)
returns uuid
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_user uuid := auth.uid();
  v_is_admin boolean;
  v_points integer;
  v_campaign_id uuid;
begin
  if v_user is null then raise exception 'not_authenticated'; end if;
  select coalesce(is_admin, false) and not is_suspended into v_is_admin from public.profiles where id = v_user;
  if not coalesce(v_is_admin, false) and not exists (select 1 from public.profiles where id = v_user and tutorial_completed_at is not null and is_suspended = false) then raise exception 'tutorial_required'; end if;
  if p_feedback_target not in (1, 3, 5) then raise exception 'invalid_target'; end if;
  if char_length(trim(p_prompt)) < 12 or char_length(trim(p_prompt)) > 220 then raise exception 'invalid_prompt'; end if;
  if exists (select 1 from public.campaigns where creator_id = v_user and kind = 'normal' and status = 'active') then raise exception 'active_campaign_exists'; end if;

  if not coalesce(v_is_admin, false) then
    select public.current_points() into v_points;
    if v_points < p_feedback_target then raise exception 'insufficient_points'; end if;
  end if;

  insert into public.campaigns (creator_id, kind, status, prompt, niche, feedback_target, reward_per_feedback, reserved_points)
  values (v_user, 'normal', 'active', trim(p_prompt), nullif(trim(p_niche), ''), p_feedback_target, 1, case when v_is_admin then 0 else p_feedback_target end)
  returning id into v_campaign_id;

  if not coalesce(v_is_admin, false) then
    insert into public.point_ledger (profile_id, campaign_id, entry_type, amount, idempotency_key)
    values (v_user, v_campaign_id, 'campaign_reserve', -p_feedback_target, gen_random_uuid());
  end if;
  return v_campaign_id;
end;
$$;

revoke all on function public.create_normal_campaign(text, text, smallint) from public;
grant execute on function public.create_normal_campaign(text, text, smallint) to authenticated;
