-- Admin publishing creates a featured mission based on the admin profile.
create or replace function public.create_featured_campaign(p_prompt text, p_niche text, p_feedback_target smallint)
returns uuid language plpgsql security definer set search_path = '' as $$
declare
  v_user uuid := auth.uid();
  v_version integer;
  v_campaign_id uuid;
begin
  if v_user is null then raise exception 'not_authenticated'; end if;
  if not exists (select 1 from public.profiles where id = v_user and is_admin = true and is_suspended = false) then raise exception 'admin_required'; end if;
  if p_feedback_target not in (1, 3, 5) then raise exception 'invalid_target'; end if;
  if char_length(trim(p_prompt)) < 12 or char_length(trim(p_prompt)) > 220 then raise exception 'invalid_prompt'; end if;
  select coalesce(max(featured_version), 0) + 1 into v_version from public.campaigns;
  insert into public.campaigns (creator_id, kind, status, prompt, niche, feedback_target, reward_per_feedback, reserved_points, featured_version)
  values (v_user, 'featured', 'active', trim(p_prompt), nullif(trim(p_niche), ''), p_feedback_target, 1, 0, v_version)
  returning id into v_campaign_id;
  return v_campaign_id;
end;
$$;

revoke all on function public.create_featured_campaign(text, text, smallint) from public;
grant execute on function public.create_featured_campaign(text, text, smallint) to authenticated;

-- Permit administrators to edit their own profile fields without exposing role flags.
drop policy if exists "admins update their own profile" on public.profiles;
create policy "admins update their own profile"
  on public.profiles for update to authenticated
  using (id = auth.uid() and is_admin = true)
  with check (id = auth.uid() and is_admin = true and is_suspended = false);
