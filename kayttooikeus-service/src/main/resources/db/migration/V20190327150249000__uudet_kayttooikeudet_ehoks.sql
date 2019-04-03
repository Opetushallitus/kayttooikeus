SELECT insertpalvelu('EHOKS', 'EHOKS-palvelu');
SELECT insertkayttooikeus('EHOKS', 'TIEDONSIIRTO', 'Ehoks tiedonsiirto omaan organisaatioon');
SELECT insertkayttooikeus('EHOKS', 'CRUD', 'Luku-, luonti- ja päivitysoikeudet (omat organisaatiot)');
SELECT insertkayttooikeus('EHOKS', 'READ', 'Lukuoikeus (omat organisaatiot)');
SELECT insertkayttooikeus('EHOKS', 'OPHPAAKAYTTAJA', 'Ehoks pääkäyttäjä');
