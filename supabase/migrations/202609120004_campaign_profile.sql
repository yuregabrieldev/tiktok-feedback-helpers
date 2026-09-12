alter table public.campaigns add column if not exists campaign_display_name text;
alter table public.campaigns add column if not exists campaign_username text;
alter table public.campaigns add column if not exists campaign_tiktok_profile_url text;
alter table public.campaigns add column if not exists campaign_bio text;
alter table public.campaigns add column if not exists campaign_avatar_path text;
alter table public.campaigns add column if not exists campaign_screenshot_path text;

create or replace function public.admin_update_campaign_profile(p_campaign_id uuid, p_display_name text, p_username text, p_tiktok_profile_url text, p_bio text)
returns void language plpgsql security definer set search_path = '' as $$
begin
  if not exists (select 1 from public.profiles where id = auth.uid() and is_admin = true and is_suspended = false) then raise exception 'admin_required'; end if;
  update public.campaigns set campaign_display_name = nullif(trim(p_display_name), ''), campaign_username = nullif(trim(p_username), ''), campaign_tiktok_profile_url = nullif(trim(p_tiktok_profile_url), ''), campaign_bio = nullif(trim(p_bio), ''), updated_at = now() where id = p_campaign_id and kind in ('tutorial','featured');
end;
$$;
revoke all on function public.admin_update_campaign_profile(uuid,text,text,text,text) from public;
grant execute on function public.admin_update_campaign_profile(uuid,text,text,text,text) to authenticated;
