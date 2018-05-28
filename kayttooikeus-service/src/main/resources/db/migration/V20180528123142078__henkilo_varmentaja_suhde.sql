create table henkilo_varmentaja_suhde
(
  id bigint not null,
  version bigint not null,
  varmennettava_henkilo_id bigint not null references henkilo (id),
  varmentava_henkilo_id bigint not null references henkilo (id),
  tila boolean not null,
  aikaleima timestamp without time zone,
  primary key (id)
);
