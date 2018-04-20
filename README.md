# Käyttöoikeuspalvelu

Käyttöoikeuspalvelu on henkilö-palvelusta eriytetty käyttöoikeusryhmien ja -anomusten hallintaan tarkoitettu palvelu.

## Teknologiat

## Vaatimukset
- Java 1.8.0_92 (ei toiminut 1.8.0_60)

### Palvelin
* Tomcat 7
* Java 8
* Spring Boot
* QueryDSL
* PostgreSQL
* JPA / Hibernate 4

### Käännösautomaatio
* Maven 3

## Testien ajaminen

    mvn clean test
    
## Käynnistäminen

### kayttooikeus-service

Devausta varten kopioi oph-configuraation-hakemistoon
* security-context-backend-local-testing.properties.template ja nimeä tiedosto security-context.properties
* kayttooikeus.yml.template, nimeä tiedosto kayttooikeus.yml ja aseta placeholdereiden tilalle kehitysympäristön tiedot

Lisää kayttoikeus.yml:ään host-osioon

    front.lokalisointi.baseUrl: https://<virkailija>

Backend-palvelun käynnistäminen dev-profiililla:

    java -jar -Dspring.profiles.active=dev -Dspring.config.additional-location=/<path>/<to>/oph-configuration/kayttooikeus.yml kayttooikeus-service/target/kayttooikeus-service-1.0.0-SNAPSHOT.jar

## Webpack-dev-server

Service käynnistää käyttöliittymän oletuksena /kayttooikeus-service/virkailija-polkuun.

Jos haluat käyttää palvelua suoraan käyttöliittymän kanssa, lisää palvelimen käynnistykseen

    -Dfront.kayttooikeus-service.virkailija-ui.basePath=""

tai jos tämä on yleisempi vaihtoehto, niin vaihtoehtoisesti kayttooikeus.yml-tiedostoon host-osioon:

    front.kayttooikeus-service.virkailija-ui.basePath:

ja ylikirjoita tämä tarvittaessa.

## Kääntäminen

    mvn clean install

## API-dokumentaatio

Rest API on dokumentoitu swaggerin avulla ja löytyy osoitteesta https://virkailija.opintopolku.fi/kayttooikeus-service/swagger-ui.html

## Virkailijan luonti -käyttöoikeus

```
INSERT INTO text_group (id, version) VALUES (nextval('hibernate_sequence'), 0);
INSERT INTO text (id, version, lang, text, textgroup_id) VALUES (nextval('hibernate_sequence'), 0, 'FI', 'Virkailijan luonti', (SELECT max(id) FROM text_group));
INSERT INTO text (id, version, lang, text, textgroup_id) VALUES (nextval('hibernate_sequence'), 0, 'SV', 'Virkailijan luonti', (SELECT max(id) FROM text_group));
INSERT INTO text (id, version, lang, text, textgroup_id) VALUES (nextval('hibernate_sequence'), 0, 'EN', 'Virkailijan luonti', (SELECT max(id) FROM text_group));
INSERT INTO kayttooikeus (id, version, palvelu_id, rooli, textgroup_id) VALUES (nextval('hibernate_sequence'), 0, (SELECT id FROM palvelu WHERE name = 'KAYTTOOIKEUS'), 'VIRKAILIJANLUONTI', (SELECT max(id) FROM text_group));
```
