package fi.vm.sade.kayttooikeus.dto;

import lombok.Getter;

@Getter
public enum PalveluRooliGroup {
    HENKILOHAKU("HENKILOHAKU");

    private String palveluRooliGroup;

    PalveluRooliGroup(String palveluRooliGroup) {
        this.palveluRooliGroup = palveluRooliGroup;
    }
}
