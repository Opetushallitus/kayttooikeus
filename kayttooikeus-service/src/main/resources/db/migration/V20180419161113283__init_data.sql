--
-- Name: insertpalvelu(character varying, character varying); Type: FUNCTION; Schema: public; Owner: oph
--

CREATE FUNCTION public.insertpalvelu(character varying, character varying) RETURNS integer
    LANGUAGE plpgsql
    AS $_$
DECLARE
  role_name ALIAS FOR $1;
  role_text_fi ALIAS FOR $2;
  _role_exists bigint;

BEGIN

  SELECT count(*) INTO _role_exists FROM palvelu WHERE name = role_name;

  IF _role_exists = 0 THEN
    INSERT INTO text_group (id, version) VALUES (nextval('public.hibernate_sequence'), 1);
    INSERT INTO text (id, version, lang, text, textgroup_id) VALUES (nextval('public.hibernate_sequence'), 1, 'FI', role_text_fi, (SELECT max(id) FROM text_group));
    INSERT INTO text (id, version, lang, text, textgroup_id) VALUES (nextval('public.hibernate_sequence'), 1, 'SV', role_text_fi, (SELECT max(id) FROM text_group));
    INSERT INTO text (id, version, lang, text, textgroup_id) VALUES (nextval('public.hibernate_sequence'), 1, 'EN', role_text_fi, (SELECT max(id) FROM text_group));
    INSERT INTO palvelu (id, version, name, palvelutyyppi, textgroup_id) VALUES (nextval('public.hibernate_sequence'), 1, role_name, 'YKSITTAINEN', (SELECT max(id) FROM text_group));
  END IF;

  RETURN 1;

END;

$_$;

ALTER FUNCTION public.insertpalvelu(character varying, character varying) OWNER TO oph;


-- Schedule timestamp for henkilo cache
INSERT INTO schedule_timestamps (id, version, modified, identifier) SELECT nextval('public.hibernate_sequence'), 1, LOCALTIMESTAMP, 'henkilocache';

-- Palvelu
SELECT insertpalvelu('KOODISTO', 'Koodistopalvelu');
SELECT insertpalvelu('ORGANISAATIOHALLINTA', 'Organisaatioidenhallinta');
SELECT insertpalvelu('OID', 'OID-palvelu');
SELECT insertpalvelu('OMATTIEDOT', 'omattiedotpalvelu');
SELECT insertpalvelu('HENKILONHALLINTA', 'Henkilonhallintapalvelu');
SELECT insertpalvelu('ANOMUSTENHALLINTA', 'Anomustenhallintapalvelu');
SELECT insertpalvelu('KOOSTEROOLIENHALLINTA', 'Koosteroolien hallintapalvelu');
SELECT insertpalvelu('TARJONTA', 'Tarjontatiedonhallinta');
SELECT insertpalvelu('HAKUJENHALLINTA', 'Hakujen hallinta');
SELECT insertpalvelu('YHTEYSTIETOTYYPPIENHALLINTA', 'Yhteystietotyyppien hallinta');
SELECT insertpalvelu('VALINTAPERUSTEKUVAUSTENHALLINTA', 'Valintaperustekuvausten hallinta');
SELECT insertpalvelu('VALINTAPERUSTEET', 'Valintaperusteet');
SELECT insertpalvelu('VALINTOJENTOTEUTTAMINEN', 'Valintojen toteuttaminen');
SELECT insertpalvelu('SIJOITTELU', 'Sijoittelu');
SELECT insertpalvelu('SISALLONHALLINTA', 'Sisällönhallinta');
SELECT insertpalvelu('TIEDONSIIRTO', 'Tiedonsiirto');
SELECT insertpalvelu('SUORITUSREKISTERI', 'Suoritusrekisteri');
SELECT insertpalvelu('AITU', 'Aitu');
SELECT insertpalvelu('LOKALISOINTI', 'Käännösten hallinta');
SELECT insertpalvelu('YTLTULOSLUETTELO', 'YTL tulosluettelo');
SELECT insertpalvelu('HAKEMUS', 'Hakemuspalvelu');
SELECT insertpalvelu('RAPORTOINTI', 'Raportointi');
SELECT insertpalvelu('TARJONTA_KK', 'Tarjonta kk');
SELECT insertpalvelu('OSOITE', 'Osoitepalvelu');
SELECT insertpalvelu('VALINTAPERUSTEKUVAUSTENHALLINTA_KK', 'Valintaperustekuvausten hallinta kk');
SELECT insertpalvelu('YTLMATERIAALITILAUS', 'YTL materiaalitilaus');
SELECT insertpalvelu('OHJAUSPARAMETRIT', 'Ohjausparametrit');
SELECT insertpalvelu('HAKULOMAKKEENHALLINTA', 'Hakulomakkeen hallinta');
SELECT insertpalvelu('ASIAKIRJAPALVELU', 'Asiakirjapalvelu');
SELECT insertpalvelu('RYHMASAHKOPOSTI', 'Ryhmäsähköposti');
SELECT insertpalvelu('IPOSTI', 'Iposti');
SELECT insertpalvelu('KKHAKUVIRKAILIJA', 'Kk-haku virkailija');
SELECT insertpalvelu('VALINTAPERUSTEETKK', 'Valintaperusteet kk');
SELECT insertpalvelu('VALINTOJENTOTEUTTAMINENKK', 'Valintalaskentojen toteuttaminen kk');
SELECT insertpalvelu('VALINTATULOSSERVICE', 'Valintatulosservice');
SELECT insertpalvelu('KOUTE', 'Koute');
SELECT insertpalvelu('EPERUSTEET', 'E-perusteet');
SELECT insertpalvelu('AIPAL', 'Aipal');
SELECT insertpalvelu('EPERUSTEET_YLOPS', 'E-perusteet ylops');
SELECT insertpalvelu('VALTIONAVUSTUS', 'Valtionavustus');
SELECT insertpalvelu('HAKUPERUSTEETADMIN', 'Hakuperusteet admin');
SELECT insertpalvelu('AMKPAL', 'AMKPAL');
SELECT insertpalvelu('EPERUSTEET_AMOSAA', 'E-perusteet amosaa');
SELECT insertpalvelu('OIKEUSTULKKIREKISTERI', 'Oikeustulkkirekisteri');
SELECT insertpalvelu('OTI', 'OTI');
SELECT insertpalvelu('ATARU_EDITORI', 'Ataru editori');
SELECT insertpalvelu('ATARU_HAKEMUS', 'Ataru hakemus');
SELECT insertpalvelu('KAYTTOOIKEUS', 'Käyttöoikeus');
SELECT insertpalvelu('OPPIJANUMEROREKISTERI', 'Oppijanumerorekisteri');
SELECT insertpalvelu('KOSKI', 'Koski');
SELECT insertpalvelu('VIRKAILIJANTYOPOYTA', 'Virkailijan työpöytä');
SELECT insertpalvelu('VARDA', 'Varda');

--
-- Name: insertkayttooikeus(character varying, character varying, character varying); Type: FUNCTION; Schema: public; Owner: oph
--

CREATE FUNCTION public.insertkayttooikeus(character varying, character varying, character varying) RETURNS integer
    LANGUAGE plpgsql
    AS $_$
DECLARE
  palvelu_name ALIAS FOR $1;
  kayttooikeus_rooli ALIAS FOR $2;
  kayttooikeus_text_fi ALIAS FOR $3;
  _kayttooikeus_exists bigint;

BEGIN

  SELECT count(*) INTO _kayttooikeus_exists FROM kayttooikeus k INNER JOIN palvelu p ON p.id = k.palvelu_id WHERE k.rooli = kayttooikeus_rooli AND p.name = palvelu_name;

  IF _kayttooikeus_exists = 0 THEN
    INSERT INTO text_group (id, version) VALUES (nextval('public.hibernate_sequence'), 1);
    INSERT INTO text (id, version, lang, text, textgroup_id) VALUES (nextval('public.hibernate_sequence'), 1, 'FI', kayttooikeus_text_fi, (SELECT max(id) FROM text_group));
    INSERT INTO text (id, version, lang, text, textgroup_id) VALUES (nextval('public.hibernate_sequence'), 1, 'SV', kayttooikeus_text_fi, (SELECT max(id) FROM text_group));
    INSERT INTO text (id, version, lang, text, textgroup_id) VALUES (nextval('public.hibernate_sequence'), 1, 'EN', kayttooikeus_text_fi, (SELECT max(id) FROM text_group));
    INSERT INTO kayttooikeus (id, version, palvelu_id, rooli, textgroup_id) VALUES (nextval('public.hibernate_sequence'), 1, (SELECT id FROM palvelu WHERE name = palvelu_name), kayttooikeus_rooli, (SELECT max(id) FROM text_group));
  END IF;

  RETURN 1;

END;

$_$;

ALTER FUNCTION public.insertkayttooikeus(character varying, character varying, character varying) OWNER TO oph;

SELECT insertkayttooikeus('VIRKAILIJANTYOPOYTA', '2ASTE', '2. aste');
SELECT insertkayttooikeus('HENKILONHALLINTA', '2ASTEENVASTUU', '2. asteen vastuukäyttäjä');
SELECT insertkayttooikeus('EPERUSTEET_AMOSAA', 'ADMIN', 'Pääkäyttäjä');
SELECT insertkayttooikeus('EPERUSTEET_YLOPS', 'ADMIN', 'Pääkäyttäjä');
SELECT insertkayttooikeus('VALTIONAVUSTUS', 'ADMIN', 'Pääkäyttäjä');
SELECT insertkayttooikeus('AITU', 'AIPALREAD', 'Aipal lukuoikeus');
SELECT insertkayttooikeus('AITU', 'AITU-OPH-KATSELIJA', 'AITU-OPH-katselija');
SELECT insertkayttooikeus('AIPAL', 'AITUREAD', 'Aitu lukuoikeus');
SELECT insertkayttooikeus('AITU', 'AITU-TMK-JASEN', 'AITU-Tmk-jäsen');
SELECT insertkayttooikeus('AITU', 'AITU-TMK-KATSELIJA', 'AITU-Tmk-katselija');
SELECT insertkayttooikeus('AMKPAL', 'AMKKATSELIJA', 'Arvo-AMK-katselija');
SELECT insertkayttooikeus('AMKPAL', 'AMKKAYTTAJA', 'Arvo-AMK-käyttäjä');
SELECT insertkayttooikeus('AMKPAL', 'AMKVASTUUKAYTTAJA', 'Arvo-AMK-vastuukäyttäjä');
SELECT insertkayttooikeus('AMKPAL', 'ARVO-KT-KATSELIJA', 'Arvo-koulutustoimijan katselija');
SELECT insertkayttooikeus('AMKPAL', 'ARVO-KT-KAYTTAJA', 'Arvo-koulutustoimijan käyttäjä');
SELECT insertkayttooikeus('AMKPAL', 'ARVO-KT-VASTUUKAYTTAJA', 'Arvo-koulutustoimijan vastuukäyttäjä');
SELECT insertkayttooikeus('AMKPAL', 'ARVO-YO-KATSELIJA', 'Arvo-YO-katselija');
SELECT insertkayttooikeus('AMKPAL', 'ARVO-YO-KAYTTAJA', 'Arvo-YO-käyttäjä');
SELECT insertkayttooikeus('AMKPAL', 'ARVO-YO-PAAKAYTTAJA', 'Arvo-YO-pääkäyttäjä');
SELECT insertkayttooikeus('AMKPAL', 'ARVO-YO-VASTUUKAYTTAJA', 'Arvo-YO-vastuukäyttäjä');
SELECT insertkayttooikeus('ASIAKIRJAPALVELU', 'ASIOINTITILICRUD', 'Asiointitilipalvelun luku- ja lähetysoikeudet');
SELECT insertkayttooikeus('ASIAKIRJAPALVELU', 'CREATE_LETTER', 'Kirjeen luonti');
SELECT insertkayttooikeus('ASIAKIRJAPALVELU', 'CREATE_TEMPLATE', 'Kirjepohjan luonti');
SELECT insertkayttooikeus('AIPAL', 'CRUD', 'Luku-, muokkaus- ja poisto-oikeus');
SELECT insertkayttooikeus('VALINTOJENTOTEUTTAMINENKK', 'CRUD', 'Luku-, muokkaus- ja poisto-oikeus');
SELECT insertkayttooikeus('VALINTATULOSSERVICE', 'CRUD', 'Luku-, muokkaus- ja poisto-oikeus');
SELECT insertkayttooikeus('KOUTE', 'CRUD', 'Luku-, muokkaus- ja poisto-oikeus');
SELECT insertkayttooikeus('AMKPAL', 'CRUD', 'Luku-, muokkaus- ja poisto-oikeus');
SELECT insertkayttooikeus('EPERUSTEET', 'CRUD', 'Luku-, muokkaus- ja poisto-oikeus');
SELECT insertkayttooikeus('HAKUPERUSTEETADMIN', 'CRUD', 'Luku-, muokkaus-, ja poisto-oikeus');
SELECT insertkayttooikeus('EPERUSTEET_YLOPS', 'CRUD', 'Luku-, muokkaus- ja poisto-oikeus');
SELECT insertkayttooikeus('VALTIONAVUSTUS', 'CRUD', 'Luku-, muokkaus-, ja poisto-oikeus');
SELECT insertkayttooikeus('VIRKAILIJANTYOPOYTA', 'CRUD', 'Luku-, muokkaus-, ja poisto-oikeus');
SELECT insertkayttooikeus('KOODISTO', 'CRUD', 'Luku-, muokkaus- ja poisto-oikeus');
SELECT insertkayttooikeus('ORGANISAATIOHALLINTA', 'CRUD', 'Luku-, muokkaus- ja poisto-oikeus');
SELECT insertkayttooikeus('OID', 'CRUD', 'Luku-, muokkaus- ja poisto-oikeus');
SELECT insertkayttooikeus('OMATTIEDOT', 'CRUD', 'Luku-, muokkaus- ja poisto-oikeus');
SELECT insertkayttooikeus('HENKILONHALLINTA', 'CRUD', 'Luku-, muokkaus- ja poisto-oikeus');
SELECT insertkayttooikeus('ANOMUSTENHALLINTA', 'CRUD', 'Luku-, muokkaus- ja poisto-oikeus');
SELECT insertkayttooikeus('KOOSTEROOLIENHALLINTA', 'CRUD', 'Luku-, muokkaus- ja poisto-oikeus');
SELECT insertkayttooikeus('TARJONTA', 'CRUD', 'Luku-, muokkaus- ja poisto-oikeus');
SELECT insertkayttooikeus('HAKEMUS', 'CRUD', 'Luku-, muokkaus- ja poisto-oikeus');
SELECT insertkayttooikeus('HAKUJENHALLINTA', 'CRUD', 'Luku-, muokkaus- ja poisto-oikeus');
SELECT insertkayttooikeus('YHTEYSTIETOTYYPPIENHALLINTA', 'CRUD', 'Luku-, muokkaus- ja poisto-oikeus');
SELECT insertkayttooikeus('VALINTAPERUSTEKUVAUSTENHALLINTA', 'CRUD', 'Luku-, muokkaus- ja poisto-oikeus');
SELECT insertkayttooikeus('VALINTAPERUSTEET', 'CRUD', 'Luku-, muokkaus- ja poisto-oikeus');
SELECT insertkayttooikeus('VALINTOJENTOTEUTTAMINEN', 'CRUD', 'Luku-, muokkaus- ja poisto-oikeus');
SELECT insertkayttooikeus('SIJOITTELU', 'CRUD', 'Luku-, muokkaus- ja poisto-oikeus');
SELECT insertkayttooikeus('SISALLONHALLINTA', 'CRUD', 'Luku-, muokkaus- ja poisto-oikeus');
SELECT insertkayttooikeus('TIEDONSIIRTO', 'CRUD', 'Luku-, muokkaus- ja poisto-oikeus');
SELECT insertkayttooikeus('SUORITUSREKISTERI', 'CRUD', 'Luku-, muokkaus- ja poisto-oikeus');
SELECT insertkayttooikeus('AITU', 'CRUD', 'Luku-, muokkaus- ja poisto-oikeus');
SELECT insertkayttooikeus('LOKALISOINTI', 'CRUD', 'Luku-, muokkaus- ja poisto-oikeus');
SELECT insertkayttooikeus('YTLTULOSLUETTELO', 'CRUD', 'Luku-, muokkaus- ja poisto-oikeus');
SELECT insertkayttooikeus('RAPORTOINTI', 'CRUD', 'Luku-, muokkaus- ja poisto-oikeus');
SELECT insertkayttooikeus('OSOITE', 'CRUD', 'Osoitepalvelun ylläpitäjät');
SELECT insertkayttooikeus('VALINTAPERUSTEKUVAUSTENHALLINTA_KK', 'CRUD', 'Luku-, muokkaus- ja poisto-oikeus');
SELECT insertkayttooikeus('TARJONTA_KK', 'CRUD', 'Luku-, muokkaus- ja poisto-oikeus');
SELECT insertkayttooikeus('ATARU_HAKEMUS', 'CRUD', 'Luku-, muokkaus-, ja poisto-oikeus');
SELECT insertkayttooikeus('YTLMATERIAALITILAUS', 'CRUD', 'Luku-, muokkaus- ja poisto-oikeus');
SELECT insertkayttooikeus('ATARU_EDITORI', 'CRUD', 'Luku-, muokkaus-, ja poisto-oikeus');
SELECT insertkayttooikeus('OHJAUSPARAMETRIT', 'CRUD', 'Luku-, muokkaus- ja poisto-oikeus');
SELECT insertkayttooikeus('OTI', 'CRUD', 'Tutkintorekisterin ylläpito');
SELECT insertkayttooikeus('HAKULOMAKKEENHALLINTA', 'CRUD', 'Luku-, muokkaus- ja poisto-oikeus');
SELECT insertkayttooikeus('KKHAKUVIRKAILIJA', 'CRUD', 'Luku-, muokkaus- ja poisto-oikeus');
SELECT insertkayttooikeus('EPERUSTEET_AMOSAA', 'CRUD', 'Luku-, muokkaus- ja poisto-oikeus');
SELECT insertkayttooikeus('KAYTTOOIKEUS', 'CRUD', 'Luku-, muokkaus- ja poisto-oikeus');
SELECT insertkayttooikeus('VALINTAPERUSTEETKK', 'CRUD', 'Luku-, muokkaus- ja poisto-oikeus');
SELECT insertkayttooikeus('VARDA', 'CRUD', 'Luku-, muokkaus- ja poisto-oikeus');
SELECT insertkayttooikeus('OPPIJANUMEROREKISTERI', 'DUPLIKAATTINAKYMA', 'Duplikaattinäkymä');
SELECT insertkayttooikeus('KOUTE', 'ESITTELIJA', 'Oiva-esittelijä');
SELECT insertkayttooikeus('KOSKI', 'GLOBAALI_LUKU_KORKEAKOULU', 'Lukuoikeus (kaikki organisaatiot, korkeakoulutus)');
SELECT insertkayttooikeus('KOSKI', 'GLOBAALI_LUKU_PERUSOPETUS', 'Lukuoikeus (kaikki organisaatiot, perusopetus)');
SELECT insertkayttooikeus('KOSKI', 'GLOBAALI_LUKU_TOINEN_ASTE', 'Lukuoikeus (kaikki organisaatiot, toisen asteen koulutus)');
SELECT insertkayttooikeus('OPPIJANUMEROREKISTERI', 'HENKILON_RU', 'Henkilön luku- ja muokkausoikeus');
SELECT insertkayttooikeus('HENKILONHALLINTA', 'HENKILOVIITE_READ', 'Henkilöviitetiedon lukuoikeus');
SELECT insertkayttooikeus('HAKEMUS', 'HETUTTOMIENKASITTELY', 'Hetuttomien käsittely');
SELECT insertkayttooikeus('KOUTE', 'KATSELIJA', 'Oiva-katselija');
SELECT insertkayttooikeus('AMKPAL', 'KATSELIJA', 'Arvo-katselija');
SELECT insertkayttooikeus('KOUTE', 'KAYTTAJA', 'Oiva-käyttäjä');
SELECT insertkayttooikeus('KAYTTOOIKEUS', 'KAYTTOOIKEUSRYHMIEN_LUKU', 'Käyttöoikeusryhmien luku');
SELECT insertkayttooikeus('VALINTATULOSSERVICE', 'KELA_READ', 'Kela API:n lukuoikeus');
SELECT insertkayttooikeus('VIRKAILIJANTYOPOYTA', 'KK', 'Korkeakoulu');
SELECT insertkayttooikeus('RAPORTOINTI', 'KK', 'KK-Raportointi');
SELECT insertkayttooikeus('HENKILONHALLINTA', 'KKVASTUU', 'Korkeakoulun vastuukäyttäjä');
SELECT insertkayttooikeus('AIPAL', 'KTKATSELIJA', 'Koulutustoimijan katselija');
SELECT insertkayttooikeus('AIPAL', 'KTKAYTTAJA', 'Koulutustoimijan käyttäjä');
SELECT insertkayttooikeus('AIPAL', 'KTPAAKAYTTAJA', 'Koulutustoimijan pääkäyttäjä');
SELECT insertkayttooikeus('AIPAL', 'KTVASTUUKAYTTAJA', 'Koulutustoimijan vastuukäyttäjä');
SELECT insertkayttooikeus('HAKEMUS', 'LISATIETOCRUD', 'Hakemuksen lisätiedot ylläpitäjä');
SELECT insertkayttooikeus('HAKEMUS', 'LISATIETOREAD', 'Hakemuksen lisätiedot lukuoikeus');
SELECT insertkayttooikeus('HAKEMUS', 'LISATIETORU', 'Hakemuksen lisätiedot vastuukäyttäjä');
SELECT insertkayttooikeus('HAKULOMAKKEENHALLINTA', 'LOMAKEPOHJANVAIHTO', 'Lomakepohjan vaihto');
SELECT insertkayttooikeus('KOSKI', 'LUOTTAMUKSELLINEN', 'Arkaluontoisen datan lukuoikeus');
SELECT insertkayttooikeus('OPPIJANUMEROREKISTERI', 'MANUAALINEN_YKSILOINTI', 'Manuaalinen yksilöinti');
SELECT insertkayttooikeus('VIRKAILIJANTYOPOYTA', 'MUUT', 'Muut');
SELECT insertkayttooikeus('HENKILONHALLINTA', 'MUUTOSTIETOPALVELU', 'Muutostietopalvelu');
SELECT insertkayttooikeus('OPPIJANUMEROREKISTERI', 'MUUTOSTIETOPALVELU', 'Muutostieto palvelukäyttäjä');
SELECT insertkayttooikeus('KOUTE', 'NIMENKIRJOITTAJA', 'Oiva-nimenkirjoittaja');
SELECT insertkayttooikeus('AIPAL', 'NTMVASTUUKAYTTAJA', 'NTM-vastuukäyttäjä');
SELECT insertkayttooikeus('OIKEUSTULKKIREKISTERI', 'OIKEUSTULKKI_CRUD', 'Oikeustulkkirekisterin ylläpito');
SELECT insertkayttooikeus('AIPAL', 'OPHKATSELIJA', 'OPH:n katselija');
SELECT insertkayttooikeus('KOSKI', 'OPHKATSELIJA', 'OPH:n katselija');
SELECT insertkayttooikeus('AIPAL', 'OPHPAAKAYTTAJA', 'OPH:n pääkäyttäjä');
SELECT insertkayttooikeus('KOSKI', 'OPHPAAKAYTTAJA', 'OPH:n pääkäyttäjä');
SELECT insertkayttooikeus('HENKILONHALLINTA', 'OPHREKISTERI', 'OPH rekisterinpitäjä');
SELECT insertkayttooikeus('RAPORTOINTI', 'OPO', 'Hakemuspalvelun opinto-ohjaajat');
SELECT insertkayttooikeus('HAKEMUS', 'OPO', 'Hakemuspalvelun opinto-ohjaajat');
SELECT insertkayttooikeus('HENKILONHALLINTA', 'OPOTVIRKAILIJAT', 'Opinto-ohjaajat ja -virkailijat');
SELECT insertkayttooikeus('OPPIJANUMEROREKISTERI', 'OPPIJOIDENTUONTI', 'Oppijoiden tuonti');
SELECT insertkayttooikeus('AITU', 'OSOITEPALVELU', 'Osoitepalvelu');
SELECT insertkayttooikeus('KAYTTOOIKEUS', 'PALVELUKAYTTAJA_CRUD', 'Palvelukäyttäjän muokkausoikeus');
SELECT insertkayttooikeus('VIRKAILIJANTYOPOYTA', 'PERUS', 'Perusopetus');
SELECT insertkayttooikeus('SIJOITTELU', 'PERUUNTUNEIDEN_HYVAKSYNTA', 'Peruuntuneiden hyväksyntä');
SELECT insertkayttooikeus('YTLMATERIAALITILAUS', 'READ', 'Lukuoikeus');
SELECT insertkayttooikeus('ORGANISAATIOHALLINTA', 'READ', 'Lukuoikeus');
SELECT insertkayttooikeus('OID', 'READ', 'Lukuoikeus');
SELECT insertkayttooikeus('OMATTIEDOT', 'READ', 'Lukuoikeus');
SELECT insertkayttooikeus('HENKILONHALLINTA', 'READ', 'Lukuoikeus');
SELECT insertkayttooikeus('ANOMUSTENHALLINTA', 'READ', 'Lukuoikeus');
SELECT insertkayttooikeus('KOOSTEROOLIENHALLINTA', 'READ', 'Lukuoikeus');
SELECT insertkayttooikeus('TARJONTA', 'READ', 'Lukuoikeus');
SELECT insertkayttooikeus('HAKEMUS', 'READ', 'Lukuoikeus');
SELECT insertkayttooikeus('HAKUJENHALLINTA', 'READ', 'Lukuoikeus');
SELECT insertkayttooikeus('YHTEYSTIETOTYYPPIENHALLINTA', 'READ', 'Lukuoikeus');
SELECT insertkayttooikeus('VALINTAPERUSTEKUVAUSTENHALLINTA', 'READ', 'Lukuoikeus');
SELECT insertkayttooikeus('VALINTAPERUSTEET', 'READ', 'Lukuoikeus');
SELECT insertkayttooikeus('VALINTOJENTOTEUTTAMINEN', 'READ', 'Lukuoikeus');
SELECT insertkayttooikeus('SIJOITTELU', 'READ', 'Lukuoikeus');
SELECT insertkayttooikeus('SISALLONHALLINTA', 'READ', 'Lukuoikeus');
SELECT insertkayttooikeus('TIEDONSIIRTO', 'READ', 'Lukuoikeus');
SELECT insertkayttooikeus('SUORITUSREKISTERI', 'READ', 'Lukuoikeus');
SELECT insertkayttooikeus('AITU', 'READ', 'Lukuoikeus');
SELECT insertkayttooikeus('LOKALISOINTI', 'READ', 'Lukuoikeus');
SELECT insertkayttooikeus('YTLTULOSLUETTELO', 'READ', 'Lukuoikeus');
SELECT insertkayttooikeus('RAPORTOINTI', 'READ', 'Lukuoikeus');
SELECT insertkayttooikeus('TARJONTA_KK', 'READ', 'Lukuoikeus');
SELECT insertkayttooikeus('VALINTAPERUSTEKUVAUSTENHALLINTA_KK', 'READ', 'Lukuoikeus');
SELECT insertkayttooikeus('KOODISTO', 'READ', 'Lukuoikeus');
SELECT insertkayttooikeus('HAKULOMAKKEENHALLINTA', 'READ', 'Lukuoikeus');
SELECT insertkayttooikeus('ASIAKIRJAPALVELU', 'READ', 'Kirjeiden luku');
SELECT insertkayttooikeus('IPOSTI', 'READ', 'Lukuoikeus');
SELECT insertkayttooikeus('KKHAKUVIRKAILIJA', 'READ', 'Lukuoikeus');
SELECT insertkayttooikeus('VALINTAPERUSTEETKK', 'READ', 'Lukuoikeus');
SELECT insertkayttooikeus('VALINTOJENTOTEUTTAMINENKK', 'READ', 'Lukuoikeus');
SELECT insertkayttooikeus('VALINTATULOSSERVICE', 'READ', 'Lukuoikeus');
SELECT insertkayttooikeus('KOUTE', 'READ', 'Lukuoikeus');
SELECT insertkayttooikeus('EPERUSTEET', 'READ', 'Lukuoikeus');
SELECT insertkayttooikeus('AIPAL', 'READ', 'Lukuoikeus');
SELECT insertkayttooikeus('EPERUSTEET_YLOPS', 'READ', 'Lukuoikeus');
SELECT insertkayttooikeus('AMKPAL', 'READ', 'Lukuoikeus');
SELECT insertkayttooikeus('EPERUSTEET_AMOSAA', 'READ', 'Lukuoikeus');
SELECT insertkayttooikeus('ATARU_HAKEMUS', 'READ', 'Lukuoikeus');
SELECT insertkayttooikeus('OPPIJANUMEROREKISTERI', 'READ', 'Lukuoikeus');
SELECT insertkayttooikeus('KAYTTOOIKEUS', 'READ', 'Lukuoikeus');
SELECT insertkayttooikeus('KOSKI', 'READ', 'Lukuoikeus (omat organisaatiot)');
SELECT insertkayttooikeus('TARJONTA', 'READ_UPDATE', 'Luku- ja muokkausoikeus');
SELECT insertkayttooikeus('EPERUSTEET_AMOSAA', 'READ_UPDATE', 'Luku- ja muokkausoikeus');
SELECT insertkayttooikeus('KKHAKUVIRKAILIJA', 'READ_UPDATE', 'Luku- ja muokkausoikeus');
SELECT insertkayttooikeus('HAKULOMAKKEENHALLINTA', 'READ_UPDATE', 'Luku- ja muokkausoikeus');
SELECT insertkayttooikeus('YTLMATERIAALITILAUS', 'READ_UPDATE', 'Luku- ja muokkausoikeus');
SELECT insertkayttooikeus('ORGANISAATIOHALLINTA', 'READ_UPDATE', 'Luku- ja muokkausoikeus');
SELECT insertkayttooikeus('TARJONTA_KK', 'READ_UPDATE', 'Luku- ja muokkausoikeus');
SELECT insertkayttooikeus('VALINTAPERUSTEKUVAUSTENHALLINTA_KK', 'READ_UPDATE', 'Luku- ja muokkausoikeus');
SELECT insertkayttooikeus('RAPORTOINTI', 'READ_UPDATE', 'Luku- ja muokkausoikeus');
SELECT insertkayttooikeus('YTLTULOSLUETTELO', 'READ_UPDATE', 'Luku- ja muokkausoikeus');
SELECT insertkayttooikeus('LOKALISOINTI', 'READ_UPDATE', 'Luku- ja muokkausoikeus');
SELECT insertkayttooikeus('AITU', 'READ_UPDATE', 'Luku- ja muokkausoikeus');
SELECT insertkayttooikeus('SUORITUSREKISTERI', 'READ_UPDATE', 'Luku- ja muokkausoikeus');
SELECT insertkayttooikeus('TIEDONSIIRTO', 'READ_UPDATE', 'Luku- ja muokkausoikeus');
SELECT insertkayttooikeus('SISALLONHALLINTA', 'READ_UPDATE', 'Luku- ja muokkausoikeus');
SELECT insertkayttooikeus('SIJOITTELU', 'READ_UPDATE', 'Luku- ja muokkausoikeus');
SELECT insertkayttooikeus('VALINTOJENTOTEUTTAMINEN', 'READ_UPDATE', 'Luku- ja muokkausoikeus');
SELECT insertkayttooikeus('VALINTAPERUSTEET', 'READ_UPDATE', 'Luku- ja muokkausoikeus');
SELECT insertkayttooikeus('VALINTAPERUSTEKUVAUSTENHALLINTA', 'READ_UPDATE', 'Luku- ja muokkausoikeus');
SELECT insertkayttooikeus('YHTEYSTIETOTYYPPIENHALLINTA', 'READ_UPDATE', 'Luku- ja muokkausoikeus');
SELECT insertkayttooikeus('HAKUJENHALLINTA', 'READ_UPDATE', 'Luku- ja muokkausoikeus');
SELECT insertkayttooikeus('HAKEMUS', 'READ_UPDATE', 'Luku- ja muokkausoikeus');
SELECT insertkayttooikeus('AIPAL', 'READ_UPDATE', 'Luku- ja muokkausoikeus');
SELECT insertkayttooikeus('KOODISTO', 'READ_UPDATE', 'Luku- ja muokkausoikeus');
SELECT insertkayttooikeus('KOSKI', 'READ_UPDATE', 'Luku- ja muokkausoikeus (omat organisaatiot)');
SELECT insertkayttooikeus('KOOSTEROOLIENHALLINTA', 'READ_UPDATE', 'Luku- ja muokkausoikeus');
SELECT insertkayttooikeus('ANOMUSTENHALLINTA', 'READ_UPDATE', 'Luku- ja muokkausoikeus');
SELECT insertkayttooikeus('HENKILONHALLINTA', 'READ_UPDATE', 'Luku- ja muokkausoikeus');
SELECT insertkayttooikeus('EPERUSTEET_YLOPS', 'READ_UPDATE', 'Luku- ja muokkausoikeus');
SELECT insertkayttooikeus('OMATTIEDOT', 'READ_UPDATE', 'Luku- ja muokkausoikeus');
SELECT insertkayttooikeus('AMKPAL', 'READ_UPDATE', 'Luku- ja muokkausoikeus');
SELECT insertkayttooikeus('EPERUSTEET', 'READ_UPDATE', 'Luku- ja muokkausoikeus');
SELECT insertkayttooikeus('KOUTE', 'READ_UPDATE', 'Luku- ja muokkausoikeus');
SELECT insertkayttooikeus('VALINTATULOSSERVICE', 'READ_UPDATE', 'Luku- ja muokkausoikeus');
SELECT insertkayttooikeus('VALINTOJENTOTEUTTAMINENKK', 'READ_UPDATE', 'Luku- ja muokkausoikeus');
SELECT insertkayttooikeus('VALINTAPERUSTEETKK', 'READ_UPDATE', 'Luku- ja muokkausoikeus');
SELECT insertkayttooikeus('OID', 'READ_UPDATE', 'Luku- ja muokkausoikeus');
SELECT insertkayttooikeus('HAKUPERUSTEETADMIN', 'REKISTERINPITAJA', 'Rekisterinpitäjä');
SELECT insertkayttooikeus('OPPIJANUMEROREKISTERI', 'REKISTERINPITAJA', 'Rekisterinpitäjä');
SELECT insertkayttooikeus('KAYTTOOIKEUS', 'REKISTERINPITAJA', 'Rekisterinpitäjä');
SELECT insertkayttooikeus('OPPIJANUMEROREKISTERI', 'REKISTERINPITAJA_READ', 'rekisterinpitäjä read');
SELECT insertkayttooikeus('ORGANISAATIOHALLINTA', 'RYHMA', 'Ryhmän käyttöoikeus');
SELECT insertkayttooikeus('KAYTTOOIKEUS', 'SCHEDULE', 'Ajastusoikeus');
SELECT insertkayttooikeus('OPPIJANUMEROREKISTERI', 'SCHEDULE', 'Ajastusoikeus');
SELECT insertkayttooikeus('RYHMASAHKOPOSTI', 'SEND', 'Ryhmäsähköpostin lähetys');
SELECT insertkayttooikeus('IPOSTI', 'SEND', 'Luku- ja muokkausoikeus');
SELECT insertkayttooikeus('ASIAKIRJAPALVELU', 'SEND_LETTER_EMAIL', 'Sähköpostin lähetys kirjeestä');
SELECT insertkayttooikeus('ASIAKIRJAPALVELU', 'SYSTEM_ATTACHMENT_DOWNLOAD', 'Kirjelähetyksen sähköpostiliitteiden lataus (järjestelmien välinen)');
SELECT insertkayttooikeus('KOSKI', 'TIEDONSIIRRON_MITATOINTI', 'Tiedonsiirron mitätöinti');
SELECT insertkayttooikeus('KOSKI', 'TIEDONSIIRTO', 'Tiedonsiirto');
SELECT insertkayttooikeus('AIPAL', 'TKTKATSELIJA', 'Tutkintotoimikunnan katselija');
SELECT insertkayttooikeus('VALINTOJENTOTEUTTAMINEN', 'TOISEN_ASTEEN_MUSIIKKIALAN_VALINTAKAYTTAJA', '2. asteen musiikkialan valintakäyttäjä');
SELECT insertkayttooikeus('VALINTOJENTOTEUTTAMINEN', 'TULOSTENTUONTI', 'Tulosten tuonti');
SELECT insertkayttooikeus('AITU', 'TYOELAMAJARJESTO', 'Työelämäjärjestön käyttäjä');
SELECT insertkayttooikeus('VALTIONAVUSTUS', 'USER', 'Käyttäjä');
SELECT insertkayttooikeus('TIEDONSIIRTO', 'VALINTA', 'Hakeneet ja valitut');
SELECT insertkayttooikeus('SUORITUSREKISTERI', 'VALINTA', 'Hakeneet ja valitut');
SELECT insertkayttooikeus('RAPORTOINTI', 'VALINTAKAYTTAJA', 'Valintakäyttäjä');
SELECT insertkayttooikeus('HENKILONHALLINTA', 'VASTUUKAYTTAJAT', 'Vastuukäyttäjät');
SELECT insertkayttooikeus('RYHMASAHKOPOSTI', 'VIEW', 'Ryhmäsähköpostin raportointi');
SELECT insertkayttooikeus('OPPIJANUMEROREKISTERI', 'VTJ_VERTAILUNAKYMA', 'VTJ vertailunäkymä');
SELECT insertkayttooikeus('AIPAL', 'YLKATSELIJA', 'Yleinen katselija');
SELECT insertkayttooikeus('KOSKI', 'YLLAPITAJA', 'Oiva-ylläpitäjä');
SELECT insertkayttooikeus('AMKPAL', 'YLLAPITAJA', 'Arvo-ylläpitäjä');
SELECT insertkayttooikeus('KOUTE', 'YLLAPITAJA', 'Oiva-ylläpitäjä');


-- Initial admin user
INSERT INTO henkilo (id, oidhenkilo, henkilotyyppi, etunimet_cached, sukunimi_cached, kutsumanimi_cached, hetu_cached, passivoitu_cached, duplicate_cached, vahvasti_tunnistettu)
VALUES (nextval('public.hibernate_sequence'), '1.2.246.562.24.00000000001', 'VIRKAILIJA', 'ROOT', 'USER', 'ROOT', '111111-985K', false, false, true);

INSERT INTO kayttajatiedot (id, version, username, password, salt, henkiloid)
VALUES (nextval('public.hibernate_sequence'), 1, 'ophadmin', 'ucIoGYqQ0yMF4K1K/5KdQw==', '6mh0kd3n0e8ihac2lu6o7q2dc5', (SELECt max(id) from henkilo));

INSERT INTO organisaatiohenkilo (id, version, organisaatio_oid, sahkopostiosoite, henkilo_id, passivoitu)
VALUES (nextval('public.hibernate_sequence'), 1, '1.2.246.562.10.00000000001', 'admin@oph.fi', (SELECT max(id) from henkilo), false);

INSERT INTO kayttooikeusryhma (id, version, name)
VALUES (nextval('public.hibernate_sequence'), 1, 'initial admin group');

CREATE FUNCTION addAllKayttooikeusToAdminGroup() RETURNS integer AS
$$
DECLARE
    ko kayttooikeus%ROWTYPE;

BEGIN
    FOR ko IN
       SELECT * FROM kayttooikeus
    LOOP
       INSERT INTO kayttooikeusryhma_kayttooikeus (kayttooikeusryhma_id, kayttooikeus_id) VALUES ((SELECT max(id) from kayttooikeusryhma), ko.id);
    END LOOP;
    RETURN 1;
END;

$$ LANGUAGE plpgsql;

SELECT addAllKayttooikeusToAdminGroup();

DROP FUNCTION addAllKayttooikeusToAdminGroup();

INSERT INTO myonnetty_kayttooikeusryhma_tapahtuma (id, version, aikaleima, tila, kayttooikeusryhma_id, organisaatiohenkilo_id, voimassaalkupvm, voimassaloppupvm)
VALUES (nextval('public.hibernate_sequence'), 1, current_timestamp, 'MYONNETTY', (SELECT max(id) from kayttooikeusryhma), (SELECT max(id) from organisaatiohenkilo), (current_timestamp - interval '1 year'), (current_timestamp + interval '100 years'));
