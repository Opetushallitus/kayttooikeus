FROM maven:3.9.8-amazoncorretto-21 AS build
WORKDIR /app

COPY pom.xml .
COPY kayttooikeus-service ./kayttooikeus-service
COPY kayttooikeus-api ./kayttooikeus-api
COPY kayttooikeus-domain ./kayttooikeus-domain
RUN mvn clean package -DskipTests

FROM amazoncorretto:21.0.4

WORKDIR /app
COPY --from=build /app/kayttooikeus-service/target/kayttooikeus-service-1.1.4-SNAPSHOT.jar .


CMD [ "java", "-Dspring.config.additional-location=classpath:/config/${ENV}.yml", "--add-opens", "java.base/java.lang=ALL-UNNAMED", "--add-opens", "java.base/java.util=ALL-UNNAMED", "-jar", "kayttooikeus-service-1.1.4-SNAPSHOT.jar" ]
