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

## Tietokanta

    docker run --name kayttooikeus-db -p 5432:5432 -e POSTGRES_USER=oph -e POSTGRES_PASSWORD=oph -e POSTGRES_DB=kayttooikeus -d postgres:10.6

## Käynnistäminen

    java -jar kayttooikeus-service/target/kayttooikeus-service-1.0.1-SNAPSHOT.jar

Ilman parametreja sovellus käyttää [application.yml](kayttooikeus-service/src/main/resources/application.yml)
-tiedoston mukaisia oletuskonfiguraatioita.

Konfiguraatioiden muuttaminen komentoriviparametreilla (baseUrl-parametrilla määritellään missä osoitteessa muut
sovelluksen käyttämät palvelut sijaitsevat):

    java -jar kayttooikeus-service/target/kayttooikeus-service-0.1.2-SNAPSHOT.jar \
        -DbaseUrl=http://localhost:8081 \
        -Dspring.datasource.username=<tietokannan_tunnus> \
        -Dspring.datasource.password=<tietokannan_salasana> \
        -Dservice-users.default.username=<oma_virkailija_tunnus> \
        -Dservice-users.default.password=<oma_virkailija_salasana>

Kaikki paitsi baseUrl-konfiguraatio on myös mahdollista laittaa erilliseen tiedostoon:

```yaml
spring.datasource.username: <tietokannan_tunnus>
spring.datasource.password: <tietokannan_salasana>
service-users.default.username: <oma_virkailija_tunnus>
service-users.default.password: <oma_virkailija_salasana>
```

...jolloin ajaminen:

    java -jar kayttooikeus-service/target/kayttooikeus-service-0.1.2-SNAPSHOT.jar \
        -DbaseUrl=http://localhost:8081 \
        -Dspring.config.additional-location=<path/to/configfile>/kayttooikeus.yml

Palvelu löytyy käynnistymisen jälkeen osoitteesta <http://localhost:8080/kayttooikeus-service>.

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
