create or replace function public.insertpalvelu(character varying, character varying) returns integer
    language plpgsql
    as $_$
declare
  role_name alias for $1;
  role_text_fi alias for $2;
  _role_exists bigint;

begin

  select count(*) into _role_exists from palvelu where name = role_name;

  if _role_exists = 0 then
    insert into text_group (id, version) values (nextval('public.hibernate_sequence'), 1);
    insert into text (id, version, lang, text, textgroup_id) values (nextval('public.hibernate_sequence'), 1, 'FI', role_text_fi, (select max(id) from text_group));
    insert into text (id, version, lang, text, textgroup_id) values (nextval('public.hibernate_sequence'), 1, 'SV', role_text_fi, (select max(id) from text_group));
    insert into text (id, version, lang, text, textgroup_id) values (nextval('public.hibernate_sequence'), 1, 'EN', role_text_fi, (select max(id) from text_group));
    insert into palvelu (id, version, name, palvelutyyppi, textgroup_id) values (nextval('public.hibernate_sequence'), 1, role_name, 'YKSITTAINEN', (select max(id) from text_group));
  end if;

  return 1;

end;

$_$;

alter function public.insertpalvelu(character varying, character varying) owner to oph;


create or replace function public.insertkayttooikeus(character varying, character varying, character varying) returns integer
    language plpgsql
    as $_$
declare
  palvelu_name alias for $1;
  kayttooikeus_rooli alias for $2;
  kayttooikeus_text_fi alias for $3;
  _kayttooikeus_exists bigint;

begin

  select count(*) into _kayttooikeus_exists from kayttooikeus k inner join palvelu p on p.id = k.palvelu_id where k.rooli = kayttooikeus_rooli and p.name = palvelu_name;

  IF _kayttooikeus_exists = 0 THEN
    insert into text_group (id, version) values (nextval('public.hibernate_sequence'), 1);
    insert into text (id, version, lang, text, textgroup_id) values (nextval('public.hibernate_sequence'), 1, 'FI', kayttooikeus_text_fi, (select max(id) from text_group));
    insert into text (id, version, lang, text, textgroup_id) values (nextval('public.hibernate_sequence'), 1, 'SV', kayttooikeus_text_fi, (select max(id) from text_group));
    insert into text (id, version, lang, text, textgroup_id) values (nextval('public.hibernate_sequence'), 1, 'EN', kayttooikeus_text_fi, (select max(id) from text_group));
    insert into kayttooikeus (id, version, palvelu_id, rooli, textgroup_id) values (nextval('public.hibernate_sequence'), 1, (select id from palvelu where name = palvelu_name), kayttooikeus_rooli, (select max(id) from text_group));
  end if;

  return 1;

end;

$_$;

alter function public.insertkayttooikeus(character varying, character varying, character varying) owner to oph;
