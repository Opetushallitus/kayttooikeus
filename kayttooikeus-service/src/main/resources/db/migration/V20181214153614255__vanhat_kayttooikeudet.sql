-- Poistetaan vanhat HENKILONHALLINTA ja ANOMUSTENHALLINTA palvelut ja näiden käyttöoikeudet. Oletetaan, että tätä ennen
-- vanhat ei passivoidut oikeudet on vaihdettu uusiin.

-- Poistetaan vanhoihin käyttöoikeuksien myönnetyt oikeudet
delete from myonnetty_kayttooikeusryhma_tapahtuma
where id in (
  select distinct mkt.id
  from myonnetty_kayttooikeusryhma_tapahtuma mkt
  join kayttooikeusryhma_kayttooikeus kk on mkt.kayttooikeusryhma_id = kk.kayttooikeusryhma_id
  join kayttooikeus k on kk.kayttooikeus_id = k.id
  join palvelu p on k.palvelu_id = p.id
  where p.name in ('HENKILONHALLINTA', 'ANOMUSTENHALLINTA')
);

-- Poistetaan vanhojen käyttöoikeuksien tekstit
delete from text
where textgroup_id in (
  select distinct k.textgroup_id
  from kayttooikeus k
  join palvelu p on k.palvelu_id = p.id
  where p.name in ('HENKILONHALLINTA', 'ANOMUSTENHALLINTA')
);

-- Poistetaan vanhojen käyttöoikeuksien tekstirymien roolit
delete from rooli
where textgroup_id in (
  select distinct k.textgroup_id
  from kayttooikeus k
  join palvelu p on k.palvelu_id = p.id
  where p.name in ('HENKILONHALLINTA', 'ANOMUSTENHALLINTA')
);

-- Poistetaan vanhojen käyttöoikeuksien tekstiryhmät
delete from text_group
where id in (
  select distinct k.textgroup_id
  from kayttooikeus k
  join palvelu p on k.palvelu_id = p.id
  where p.name in ('HENKILONHALLINTA', 'ANOMUSTENHALLINTA')
);

-- Poistetaan vanhat käyttöoikeudet käyttöoikeusryhmistä
delete from kayttooikeusryhma_kayttooikeus
where kayttooikeus_id in (
  select distinct kk.kayttooikeus_id
  from kayttooikeusryhma_kayttooikeus kk
  join kayttooikeus k on kk.kayttooikeus_id = k.id
  join palvelu p on k.palvelu_id = p.id
  where p.name in ('HENKILONHALLINTA', 'ANOMUSTENHALLINTA')
);

-- Poistetaan vanhat käyttöoikeudet
delete from kayttooikeus
where id in (
  select distinct k.id
  from kayttooikeus k
  join palvelu p on k.palvelu_id = p.id
  where p.name in ('HENKILONHALLINTA', 'ANOMUSTENHALLINTA')
);

-- Poistetaan vanhat palvelut
delete from palvelu where name in ('HENKILONHALLINTA', 'ANOMUSTENHALLINTA');

