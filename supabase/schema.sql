-- NexiRide2 — run in Supabase SQL Editor on a new project (or drop old public tables first).
-- Enables RLS with dev-friendly policies so the Android app (anon + JWT) can operate.

-- ---------------------------------------------------------------------------
-- Core tables
-- ---------------------------------------------------------------------------

create table if not exists public.routes (
    id text primary key,
    origin text not null,
    destination text not null,
    date text not null,
    available_seats int not null,
    route_json text not null
);

create table if not exists public.cities (
    name text primary key
);

create table if not exists public.profiles (
    id uuid primary key references auth.users (id) on delete cascade,
    full_name text,
    phone text,
    avatar_url text,
    updated_at timestamptz default now()
);

create table if not exists public.bookings (
    id text primary key,
    user_id uuid not null references auth.users (id) on delete cascade,
    status text not null,
    route_id text not null references public.routes (id),
    booking_json text not null
);

create table if not exists public.route_seats (
    route_id text not null references public.routes (id) on delete cascade,
    seat_number text not null,
    row_idx int not null,
    col_idx int not null,
    status text not null default 'AVAILABLE',
    booking_id text references public.bookings (id) on delete set null,
    primary key (route_id, seat_number)
);

create table if not exists public.payment_methods (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references auth.users (id) on delete cascade,
    type text not null,
    name text not null,
    details text not null,
    is_default boolean not null default false
);

create table if not exists public.payment_transactions (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references auth.users (id) on delete cascade,
    booking_id text references public.bookings (id) on delete set null,
    amount numeric not null,
    currency text not null default 'GHS',
    method text not null,
    status text not null default 'CAPTURED',
    created_at timestamptz default now()
);

create table if not exists public.notifications (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references auth.users (id) on delete cascade,
    title text not null,
    message text not null,
    type text not null,
    booking_id text,
    is_read boolean not null default false,
    created_at timestamptz default now()
);

-- ---------------------------------------------------------------------------
-- Profile auto-create on sign-up
-- ---------------------------------------------------------------------------

create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
    insert into public.profiles (id, full_name, phone)
    values (
        new.id,
        coalesce(new.raw_user_meta_data->>'full_name', ''),
        coalesce(new.raw_user_meta_data->>'phone', '')
    )
    on conflict (id) do update
        set full_name = excluded.full_name,
            phone = excluded.phone,
            updated_at = now();
    return new;
end;
$$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
    after insert on auth.users
    for each row execute function public.handle_new_user();

-- ---------------------------------------------------------------------------
-- RLS
-- ---------------------------------------------------------------------------

alter table public.routes enable row level security;
alter table public.cities enable row level security;
alter table public.profiles enable row level security;
alter table public.bookings enable row level security;
alter table public.route_seats enable row level security;
alter table public.payment_methods enable row level security;
alter table public.payment_transactions enable row level security;
alter table public.notifications enable row level security;

drop policy if exists "routes_select_all" on public.routes;
drop policy if exists "routes_insert_dev" on public.routes;
drop policy if exists "cities_select_all" on public.cities;
drop policy if exists "cities_insert_dev" on public.cities;
drop policy if exists "profiles_select_own" on public.profiles;
drop policy if exists "profiles_update_own" on public.profiles;
drop policy if exists "profiles_insert_own" on public.profiles;
drop policy if exists "bookings_select_own" on public.bookings;
drop policy if exists "bookings_insert_own" on public.bookings;
drop policy if exists "bookings_update_own" on public.bookings;
drop policy if exists "route_seats_select_all" on public.route_seats;
drop policy if exists "route_seats_insert_dev" on public.route_seats;
drop policy if exists "route_seats_update_auth" on public.route_seats;
drop policy if exists "payment_methods_all_own" on public.payment_methods;
drop policy if exists "payment_tx_all_own" on public.payment_transactions;
drop policy if exists "notifications_all_own" on public.notifications;

-- Public read for catalog (search without login)
create policy "routes_select_all" on public.routes for select using (true);
-- Debug seed from app uses anon key — tighten for production (service role only).
create policy "routes_insert_dev" on public.routes for insert to anon, authenticated with check (true);

create policy "cities_select_all" on public.cities for select using (true);
create policy "cities_insert_dev" on public.cities for insert to anon, authenticated with check (true);

create policy "profiles_select_own" on public.profiles for select using (auth.uid() = id);
create policy "profiles_insert_own" on public.profiles for insert with check (auth.uid() = id);
create policy "profiles_update_own" on public.profiles for update using (auth.uid() = id);

create policy "bookings_select_own" on public.bookings for select using (auth.uid() = user_id);
create policy "bookings_insert_own" on public.bookings for insert with check (auth.uid() = user_id);
create policy "bookings_update_own" on public.bookings for update using (auth.uid() = user_id);

create policy "route_seats_select_all" on public.route_seats for select using (true);
create policy "route_seats_insert_dev" on public.route_seats for insert to anon, authenticated
    with check (exists (select 1 from public.routes r where r.id = route_id));
-- Authenticated users can update seat assignment for booking flow (dev; refine with RPC for prod).
create policy "route_seats_update_auth" on public.route_seats for update to authenticated using (true) with check (true);

create policy "payment_methods_all_own" on public.payment_methods for all using (auth.uid() = user_id) with check (auth.uid() = user_id);
create policy "payment_tx_all_own" on public.payment_transactions for all using (auth.uid() = user_id) with check (auth.uid() = user_id);

create policy "notifications_all_own" on public.notifications for all using (auth.uid() = user_id) with check (auth.uid() = user_id);
