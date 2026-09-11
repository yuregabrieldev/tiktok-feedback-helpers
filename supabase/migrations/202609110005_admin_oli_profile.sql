-- Configure the owner's account and mandatory TikTok profile.
update public.profiles p
set is_admin = true,
    display_name = 'Olí Indica',
    username = 'olipelomundo',
    tiktok_profile_url = 'https://www.tiktok.com/@olipelomundo',
    niche = 'VIAGENS',
    bio = 'Dicas de viagens, cafés e experiências pelo mundo.',
    tutorial_completed_at = coalesce(p.tutorial_completed_at, now())
from auth.users u
where p.id = u.id and lower(u.email) = 'developer.yuregabriel@gmail.com';

insert into public.campaigns (creator_id, kind, status, title, prompt, niche, feedback_target, reward_per_feedback, reserved_points)
select p.id, 'tutorial', 'active', 'Missão de acesso PULSO', 'A bio deixa claro o que este perfil oferece?', 'VIAGENS', 10, 0, 0
from public.profiles p
where p.is_admin = true and p.username = 'olipelomundo'
  and not exists (select 1 from public.campaigns c where c.kind = 'tutorial' and c.creator_id = p.id and c.status = 'active');

-- Admin-only profile editing path (normal users remain restricted to their own profile).
create or replace function public.admin_update_profile(
  p_profile_id uuid,
  p_display_name text,
  p_username text,
  p_tiktok_profile_url text,
  p_niche text,
  p_bio text
)
returns void
language plpgsql
security definer
set search_path = ''
as $$
begin
  if not exists (select 1 from public.profiles where id = auth.uid() and is_admin = true and is_suspended = false) then
    raise exception 'admin_required';
  end if;
  update public.profiles
  set display_name = left(coalesce(trim(p_display_name), ''), 120),
      username = left(regexp_replace(coalesce(trim(p_username), ''), '^@', ''), 80),
      tiktok_profile_url = nullif(trim(p_tiktok_profile_url), ''),
      niche = nullif(left(trim(coalesce(p_niche, '')), 80), ''),
      bio = left(coalesce(trim(p_bio), ''), 220)
  where id = p_profile_id;
end;
$$;

revoke all on function public.admin_update_profile(uuid, text, text, text, text, text) from public;
grant execute on function public.admin_update_profile(uuid, text, text, text, text, text) to authenticated;

drop policy if exists "users update their own profile" on public.profiles;
create policy "users update their own profile"
  on public.profiles for update to authenticated
  using (id = auth.uid() and not is_suspended)
  with check (id = auth.uid() and not is_suspended);
