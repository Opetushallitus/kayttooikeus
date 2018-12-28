package fi.vm.sade.kayttooikeus.service.external.impl;

public final class HttpClientUtil {

    private HttpClientUtil() {
    }

    public static RuntimeException noContentOrNotFoundException(String url) {
        return new RuntimeException(String.format("Osoite %s palautti 204 tai 404", url));
    }

}
