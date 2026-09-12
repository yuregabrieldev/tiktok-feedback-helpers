create or replace function public.admin_delete_campaign(p_campaign_id uuid)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  if not exists (select 1 from public.profiles where id = auth.uid() and is_admin = true and is_suspended = false) then
    raise exception 'admin_required';
  end if;
  update public.campaigns set status = 'removed', updated_at = now() where id = p_campaign_id;
end;
$$;

revoke all on function public.admin_delete_campaign(uuid) from public;
grant execute on function public.admin_delete_campaign(uuid) to authenticated;
