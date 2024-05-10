SELECT insertpalvelu('PALAUTE', 'PALAUTE-palvelu');
SELECT insertkayttooikeus('PALAUTE', 'PALAUTE_READ', 'Palautteen lukuoikeus');
SELECT insertkayttooikeus('PALAUTE', 'PALAUTE_CREATE', 'Palautteen luontioikeus');
