-- Oiva palvelu ja uusi oikeus oiva-admin sekä tähän uuteen palveluun oikeudet vanhojen koute oikeuksien tilalle
select insertpalvelu('OIVA_APP', 'Oiva-palvelu');

select insertkayttooikeus('OIVA_APP', 'ADMIN', 'Oiva-admin');

select insertkayttooikeus('OIVA_APP', 'ESITTELIJA', 'Oiva-esittelijä');
select insertkayttooikeus('OIVA_APP', 'KATSELIJA', 'Oiva-katselija');
select insertkayttooikeus('OIVA_APP', 'KAYTTAJA', 'Oiva-käyttäjä');
select insertkayttooikeus('OIVA_APP', 'NIMENKIRJOITTAJA', 'Oiva-nimenkirjoittaja');
select insertkayttooikeus('OIVA_APP', 'YLLAPITAJA', 'Oiva-ylläpitäjä');
