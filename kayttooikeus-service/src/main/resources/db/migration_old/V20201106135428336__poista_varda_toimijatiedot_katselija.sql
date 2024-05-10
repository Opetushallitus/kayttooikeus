-- poistetaan edellisessä migraatiossa lisätty oikeus tarpeettomana
SELECT deletekayttooikeus('VARDA', 'VARDA_TOIMIJATIEDOT_KATSELIJA');
