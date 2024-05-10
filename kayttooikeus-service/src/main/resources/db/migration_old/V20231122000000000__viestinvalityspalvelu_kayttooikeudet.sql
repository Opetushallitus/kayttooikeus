SELECT insertpalvelu('VIESTINVALITYS', 'Viestinvälityspalvelu');
SELECT insertkayttooikeus('VIESTINVALITYS', 'LAHETYS', 'Saa lähettää viestejä viestinvälityspalvelun kautta');
SELECT insertkayttooikeus('VIESTINVALITYS', 'KATSELU', 'Saa katsella viestinvälityspalvelun kaikkia viestejä');
SELECT insertkayttooikeus('VIESTINVALITYS', 'OPH_PAAKAYTTAJA', 'Saa käyttää viestinvälityspalvelun kaikkea toiminnallisuutta ilman rajoituksia');