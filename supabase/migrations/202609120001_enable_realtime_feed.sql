-- Keep the community feed synchronized for every connected client.
-- The DO blocks make this migration safe when a table is already present
-- in Supabase's realtime publication.
do $$
begin
  alter publication supabase_realtime add table public.campaigns;
exception
  when duplicate_object then null;
end $$;

do $$
begin
  alter publication supabase_realtime add table public.feedbacks;
exception
  when duplicate_object then null;
end $$;

do $$
begin
  alter publication supabase_realtime add table public.profiles;
exception
  when duplicate_object then null;
end $$;
