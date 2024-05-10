-- Poistetaan vanha KOOSTEROOLIENHALLINTA palvelu ja tämän käyttöoikeudet. Oletetaan, että tätä ennen
-- vanhat ei passivoidut oikeudet on vaihdettu uusiin.

-- Poistetaan vanhojen käyttöoikeuksien myönnetyt oikeudet
delete from myonnetty_kayttooikeusryhma_tapahtuma
where id in (
  select distinct mkt.id
  from myonnetty_kayttooikeusryhma_tapahtuma mkt
  join kayttooikeusryhma_kayttooikeus kk on mkt.kayttooikeusryhma_id = kk.kayttooikeusryhma_id
  join kayttooikeus k on kk.kayttooikeus_id = k.id
  join palvelu p on k.palvelu_id = p.id
  where p.name = 'KOOSTEROOLIENHALLINTA'
);

-- Poistetaan vanhat käyttöoikeudet käyttöoikeusryhmistä
delete from kayttooikeusryhma_kayttooikeus
where kayttooikeus_id in (
  select distinct kk.kayttooikeus_id
  from kayttooikeusryhma_kayttooikeus kk
  join kayttooikeus k on kk.kayttooikeus_id = k.id
  join palvelu p on k.palvelu_id = p.id
  where p.name = 'KOOSTEROOLIENHALLINTA'
);

-- Poistetaan vanhat käyttöoikeudet
delete from kayttooikeus
where id in (
  select distinct k.id
  from kayttooikeus k
  join palvelu p on k.palvelu_id = p.id
  where p.name = 'KOOSTEROOLIENHALLINTA'
);

-- Poistetaan vanhojen palveluiden tekstit
delete from text
where textgroup_id in (
  select distinct p.textgroup_id
  from palvelu p
  where p.name = 'KOOSTEROOLIENHALLINTA'
);

-- Poistetaan vanhat palvelut
delete from palvelu where name = 'KOOSTEROOLIENHALLINTA';

-- Siivoa viittauksettomat textgroupit. Tätä ei voi tehdä ennen palvelun poistamista koska palvelu viittaa textgrouppiin
-- joten palvelu on poistettava ensin.
delete from text_group
where id in (select t.id
from text_group t
left join kayttooikeusryhma k on k.textgroup_id = t.id
left join text tt on tt.textgroup_id = t.id
left join rooli r on r.textgroup_id = t.id
left join kayttooikeusryhma kr on kr.kuvaus_id = t.id
where k.textgroup_id is null
  and tt.textgroup_id is null
  and r.textgroup_id is null
  and kr.kuvaus_id is null);