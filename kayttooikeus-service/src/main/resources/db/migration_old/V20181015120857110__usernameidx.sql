drop index if exists username_idx;
create index username_idx on kayttajatiedot (lower(username));
