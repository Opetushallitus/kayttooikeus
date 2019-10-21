SELECT insertpalvelu('YKSITYISTEN_REKISTEROITYMINEN', 'Yksityisten rekisteröityminen -palvelu');
SELECT insertkayttooikeus('YKSITYISTEN_REKISTEROITYMINEN', 'YKSITYISTEN_REKISTEROITYMINEN_READ', 'Rekisteröitymisten lukuoikeus');
SELECT insertkayttooikeus('YKSITYISTEN_REKISTEROITYMINEN', 'YKSITYISTEN_REKISTEROITYMINEN_UPDATE', 'Rekisteröitymisten muokkausoikeus');
SELECT insertkayttooikeus('YKSITYISTEN_REKISTEROITYMINEN', 'YKSITYISTEN_REKISTEROITYMINEN_DELETE', 'Rekisteröitymisten poisto-oikeus');
