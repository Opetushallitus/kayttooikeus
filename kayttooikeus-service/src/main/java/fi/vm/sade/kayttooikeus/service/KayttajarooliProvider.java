package fi.vm.sade.kayttooikeus.service;

import java.util.Set;

public interface KayttajarooliProvider {

    Set<String> getByKayttajaOid(String kayttajaOid);

}
