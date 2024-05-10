UPDATE kayttooikeus SET rooli = 'MUOKKAAJA' WHERE rooli = 'KAYTTAJA' AND palvelu_id in (
  SELECT id FROM palvelu WHERE name = 'OIVA_APP'
);
