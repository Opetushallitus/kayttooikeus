update kayttooikeus
set rooli = 'AMMATILLINEN_' || rooli
where palvelu_id in (select id from palvelu where name = 'OIVA_APP')
  and rooli in ('KATSELIJA', 'NIMENKIRJOITTAJA', 'ESITTELIJA');

update kayttooikeus
set rooli = 'AMMATILLINEN_MUOKKAAJA'
where palvelu_id in (select id from palvelu where name = 'OIVA_APP')
  and rooli = 'KAYTTAJA';
