--
-- KJHH-2068
--
update text set text = 'Ylläpitäjä - Kutsuu/hallinnoi kaikkia valssi käyttäjiä' where textgroup_id = (select textgroup_id from kayttooikeus where rooli = 'YLLAPITAJA' and palvelu_id = (select id from palvelu where name = 'VALSSI'));
update text set text = 'Pääkäyttäjä - Kutsuu/hallinnoi valssi käyttäjiä oman organisaatio sisällä' where textgroup_id = (select textgroup_id from kayttooikeus where rooli = 'PAAKAYTTAJA' and palvelu_id = (select id from palvelu where name = 'VALSSI'));
