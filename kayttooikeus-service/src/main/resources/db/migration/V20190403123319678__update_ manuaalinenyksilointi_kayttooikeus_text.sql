UPDATE text SET text = 'Hetuttoman manuaalinen yksilöinti' WHERE textgroup_id = (
  SELECT textgroup_id FROM kayttooikeus WHERE rooli = 'MANUAALINEN_YKSILOINTI' AND palvelu_id = (SELECT id FROM palvelu WHERE name = 'OPPIJANUMEROREKISTERI')
);
