SELECT insertkayttooikeus('KOSKI', 'LUOTTAMUKSELLINEN_KELA_SUPPEA', 'Kelan suppeat arkaluontoisten Koski-tietojen katseluoikeudet');
SELECT insertkayttooikeus('KOSKI', 'LUOTTAMUKSELLINEN_KELA_LAAJA', 'Kelan laajat arkaluontoisten Koski-tietojen katseluoikeudet');

BEGIN;
  UPDATE text SET text = 'Kaikkien arkaluontoisten Koski-tietojen katseluoikeudet' WHERE textgroup_id = (
    SELECT textgroup_id FROM kayttooikeus WHERE rooli = 'LUOTTAMUKSELLINEN' AND palvelu_id = (SELECT id FROM palvelu WHERE name = 'KOSKI')
  );
  UPDATE kayttooikeus SET rooli = 'LUOTTAMUKSELLINEN_KAIKKI_TIEDOT' WHERE rooli = 'LUOTTAMUKSELLINEN' AND palvelu_id = (SELECT id FROM palvelu WHERE name = 'KOSKI');
COMMIT;