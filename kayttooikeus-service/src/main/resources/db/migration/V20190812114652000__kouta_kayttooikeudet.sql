SELECT insertpalvelu('KOUTA', 'KOUTA-palvelu');
SELECT insertkayttooikeus('KOUTA', 'INDEKSOINTI', 'Indeksoinnin oikeudet');
SELECT insertkayttooikeus('KOUTA', 'OPHPAAKAYTTAJA', 'OPH:n pääkäyttäjäoikeudet');
SELECT insertkayttooikeus('KOUTA', 'KOULUTUS_READ', 'Koulutuksen lukuoikeus');
SELECT insertkayttooikeus('KOUTA', 'KOULUTUS_READ_UPDATE', 'Koulutuksen luku- ja muokkausoikeus');
SELECT insertkayttooikeus('KOUTA', 'KOULUTUS_CRUD', 'Koulutuksen luonti-, luku-, muokkaus- ja poisto-oikeus');
SELECT insertkayttooikeus('KOUTA', 'TOTEUTUS_READ', 'Toteutuksen lukuoikeus');
SELECT insertkayttooikeus('KOUTA', 'TOTEUTUS_READ_UPDATE', 'Toteutuksen luku- ja muokkausoikeus');
SELECT insertkayttooikeus('KOUTA', 'TOTEUTUS_CRUD', 'Toteutuksen luonti-, luku-, muokkaus- ja poisto-oikeus');
SELECT insertkayttooikeus('KOUTA', 'HAKU_READ', 'Haun lukuoikeus');
SELECT insertkayttooikeus('KOUTA', 'HAKU_READ_UPDATE', 'Haun luku- ja muokkausoikeus');
SELECT insertkayttooikeus('KOUTA', 'HAKU_CRUD', 'Haun luonti-, luku-, muokkaus- ja poisto-oikeus');
SELECT insertkayttooikeus('KOUTA', 'HAKUKOHDE_READ', 'Hakukohteen lukuoikeus');
SELECT insertkayttooikeus('KOUTA', 'HAKUKOHDE_READ_UPDATE', 'Hakukohteen luku- ja muokkausoikeus');
SELECT insertkayttooikeus('KOUTA', 'HAKUKOHDE_CRUD', 'Hakukohteen luonti-, luku-, muokkaus- ja poisto-oikeus');
SELECT insertkayttooikeus('KOUTA', 'VALINTAPERUSTE_READ', 'Valintaperustekuvauksen lukuoikeus');
SELECT insertkayttooikeus('KOUTA', 'VALINTAPERUSTE_READ_UPDATE', 'Valintaperustekuvauksen luku- ja muokkausoikeus');
SELECT insertkayttooikeus('KOUTA', 'VALINTAPERUSTE_CRUD', 'Valintaperustekuvauksen luonti-, luku-, muokkaus- ja poisto-oikeus');