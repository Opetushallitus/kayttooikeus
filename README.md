# Käyttöoikeuspalvelu

Käyttöoikeuspalvelu on henkilö-palvelusta eriytetty käyttöoikeusryhmien ja -anomusten hallintaan tarkoitettu palvelu.

## Teknologiat

## Vaatimukset
- Java 1.8.0_92 (ei toiminut 1.8.0_60)

### Palvelin
* Tomcat 8
* Java 8
* Spring Boot
* QueryDSL
* PostgreSQL
* JPA / Hibernate 5

### Käännösautomaatio
* Maven 3

## Testien ajaminen

    mvn clean test
    
## Kääntäminen

    mvn clean install

## Käynnistäminen

### kayttooikeus-service

Devausta varten kopioi oph-configuraation-hakemistoon
* security-context-backend-local-testing.properties.template ja nimeä tiedosto security-context.properties
* kayttooikeus.yml.template, nimeä tiedosto kayttooikeus.yml ja aseta placeholdereiden tilalle kehitysympäristön tiedot


#### Backend-palvelun käynnistäminen CAS kirjautumisella

    java -jar -Dspring.config.additional-location=/<path>/<to>/oph-configuration/kayttooikeus.yml kayttooikeus-service/target/kayttooikeus-service-1.0.0-SNAPSHOT.jar

Tämä vaatii CAS-filtterin konfiguroinnin käyttämään localhostia (ei https!)

    cas:
      service: http://localhost:8180/kayttooikeus-service

Nyt voit käyttää ympäristön CAS-tunnuksiasia kirjautumiseen lokaaliin palveluun.
    
**Ensimmäinen kirjautuminen palvelun käynnistyttyä täytyy tehdä suoralla GET kutsulla rajapintaan, ei swaggeristä**

#### Backend-palvelun käynnistäminen dev-profiililla
Tämä ei ole kehityksessä tällä hetkellä yleisesti käytetty tapa. Tällöin kirjautuminen lokaaliin palveluun tapahtuu basic authilla koodissa määritellyillä tunnuksilla.

    java -jar -Dspring.profiles.active=dev -Dspring.config.additional-location=/<path>/<to>/oph-configuration/kayttooikeus.yml kayttooikeus-service/target/kayttooikeus-service-1.0.0-SNAPSHOT.jar


#### Kantamigraatiot

Kantamigraatiot tapahtuvat db.migrations-kansiosta löytyvillä flyway skripteillä.

Jos toimimassasi ympäristössä on ajettu skriptejä joita ei ole master haarassa voit olla välittämättä näistä seuraavalla konfiguraatiolla

    spring:
        flyway:
            # TODO REMOVE
            ignore-missing-migrations: true

Kommentti muistutuksena, ettei tätä muutosta commitata.

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
