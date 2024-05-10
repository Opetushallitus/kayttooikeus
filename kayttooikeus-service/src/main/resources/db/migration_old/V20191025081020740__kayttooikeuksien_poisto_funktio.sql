create or replace function public.deletekayttooikeus(character varying, character varying) returns integer
    language plpgsql
    as $_$
declare
  palvelu_name alias for $1;
  kayttooikeus_rooli alias for $2;
  _kayttooikeus_id bigint;
  _textgroup_id bigint;

begin

  select k.id into strict _kayttooikeus_id from kayttooikeus k inner join palvelu p on p.id = k.palvelu_id where k.rooli = kayttooikeus_rooli and p.name = palvelu_name;
  select textgroup_id into strict _textgroup_id from kayttooikeus where id = _kayttooikeus_id;
  delete from kayttooikeusryhma_kayttooikeus where kayttooikeus_id = _kayttooikeus_id;
  delete from kayttooikeus where id = _kayttooikeus_id;
  delete from text where textgroup_id = _textgroup_id;
  delete from text_group where id = _textgroup_id;
  return 1;

end;

$_$;

