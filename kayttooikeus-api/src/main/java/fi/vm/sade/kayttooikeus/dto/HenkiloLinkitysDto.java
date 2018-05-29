package fi.vm.sade.kayttooikeus.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

/**
 * Sisältää henkilön kaikki linkitystiedot käyttöoikeuspalvelussa
 */
@Getter
@Setter
public class HenkiloLinkitysDto {
    Set<String> henkiloVarmennettavas = new HashSet<>();
    Set<String> henkiloVarmentajas = new HashSet<>();
}
