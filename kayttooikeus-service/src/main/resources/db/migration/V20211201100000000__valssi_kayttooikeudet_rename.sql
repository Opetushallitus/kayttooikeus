--
-- KJHH-2068
--
update text set text = 'Ylläpitäjä' where textgroup_id = (select textgroup_id from kayttooikeus where rooli = 'YLLAPITAJA' and palvelu_id = (select id from palvelu where name = 'VALSSI'));
update text set text = 'Pääkäyttäjä' where textgroup_id = (select textgroup_id from kayttooikeus where rooli = 'PAAKAYTTAJA' and palvelu_id = (select id from palvelu where name = 'VALSSI'));
