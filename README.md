# Käyttöoikeuspalvelu

Käyttöoikeuspalvelu on henkilö-palvelusta eriytetty käyttöoikeusryhmien ja -anomusten hallintaan tarkoitettu palvelu.

## Teknologiat

### Avainteknologiat

* Spring Boot
* Spring Security (CAS)
* PostgreSQL
* QueryDSL
* JPA / Hibernate 5
* Flyway
* Orika
* Lombok
* Swagger
* DB-scheduler

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
        -DbaseUrl=https://<testiympäristö_host> \
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
        -DbaseUrl=https://<testiympäristö_host> \
        -Dspring.config.additional-location=<path/to/configfile>/kayttooikeus.yml

Palvelu löytyy käynnistymisen jälkeen osoitteesta <http://localhost:8080/kayttooikeus-service>.

#### Backend-palvelun käynnistäminen dev-profiililla
dev-profiilissa kirjautuminen lokaaliin palveluun tapahtuu basic authilla niin, että käyttäjänimi on haluttu käyttäjän oid ja salasana `password`.

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

## Uuden käyttöoikeuden luonti

Luo uusi migraatio [kayttooikeus-service/src/main/resources/db/migration](kayttooikeus-service/src/main/resources/db/migration)
kansioon jossa kutsutaan `insertkayttooikeus` kantafunktiota.

```
SELECT insertkayttooikeus('PALVELU', 'KAYTTOOIKEUS', 'Kuvaus');
```

Palvelun nimen (esimerkissä `PALVELU`) tulee viitata olemassa olevaan
palveluun taulussa `palvelu`. Uuden palvelun voi luoda kutsumalla `insertpalvelu`  kantafunktiota:

```
SELECT insertpalvelu('PALVELU', 'Kuvaus');
```

Käyttöoikeuden nimi (esimerkissä `KAYTTOOIKEUS`)
tulee osaksi tunnistetta, jolla virkailijalle myönnetty käyttöoikeus esitetään
rajapintavastauksessa. Kuvaustekstiä (esimerkissä `Kuvaus`) käytetään mm. kun
listataan ryhmään kuuluvia käyttöoikeuksia käyttöoikeusryhmien hallinnan
käyttöliittymässä.

## Virkailijan luonti -käyttöoikeus

Testiympäristöihin ajettava virkailijan luonti -käyttöoikeus (Huom! tätä ei saa ajaa tuotantoon):

```
SELECT insertkayttooikeus('KAYTTOOIKEUS', 'VIRKAILIJANLUONTI', 'Virkailijan luonti');
```
