UPDATE kayttooikeus SET rooli = 'KATSELIJA' WHERE rooli = 'AMMATILLINEN_KATSELIJA' AND palvelu_id in (
  SELECT id FROM palvelu WHERE name = 'OIVA_APP'
);
UPDATE kayttooikeus SET rooli = 'KAYTTAJA' WHERE rooli = 'AMMATILLINEN_MUOKKAAJA' AND palvelu_id in (
  SELECT id FROM palvelu WHERE name = 'OIVA_APP'
);
UPDATE kayttooikeus SET rooli = 'NIMENKIRJOITTAJA' WHERE rooli = 'AMMATILLINEN_NIMENKIRJOITTAJA' AND palvelu_id in (
  SELECT id FROM palvelu WHERE name = 'OIVA_APP'
);
UPDATE kayttooikeus SET rooli = 'ESITTELIJA' WHERE rooli = 'AMMATILLINEN_ESITTELIJA' AND palvelu_id in (
  SELECT id FROM palvelu WHERE name = 'OIVA_APP'
);
