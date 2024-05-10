create table google_auth_token (
  id serial primary key,
  henkilo_id bigint not null references henkilo (id),
  name text not null,
  registration_date timestamp without time zone default current_timestamp,
  secret_key text not null,
  validation_code bigint not null,
  scratch_codes integer[]
);
