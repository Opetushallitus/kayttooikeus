select insertpalvelu('EPERUSTEET_MAARAYS', 'ePerusteet määräyskokoelma');
select insertkayttooikeus('EPERUSTEET_MAARAYS', 'READ', 'Lukuoikeus'); 
select insertkayttooikeus('EPERUSTEET_MAARAYS', 'CRUD', 'Luku-, muokkaus- ja poisto-oikeus'); 