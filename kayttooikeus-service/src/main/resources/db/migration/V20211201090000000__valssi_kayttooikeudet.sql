--
-- KJHH-2068
--
select insertpalvelu('VALSSI', 'Valssi-palvelu');
select insertkayttooikeus('VALSSI', 'YLLAPITAJA', 'Kutsuu/hallinnoi kaikkia valssi käyttäjiä: Ylläpitäjä, Pääkäyttäjä');
select insertkayttooikeus('VALSSI', 'PAAKAYTTAJA', 'Kutsuu/hallinnoi valssi käyttäjiä oman organisaatio sisällä: Pääkäyttäjä, Rinnakkaispääkäyttäjä, Toteuttaja');
select insertkayttooikeus('VALSSI', 'TOTEUTTAJA', 'Toteuttaja');
