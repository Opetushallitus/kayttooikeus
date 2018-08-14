package fi.vm.sade.kayttooikeus.repositories.populate;

import fi.vm.sade.kayttooikeus.model.Henkilo;
import fi.vm.sade.kayttooikeus.model.Identification;

import javax.persistence.EntityManager;
import java.time.LocalDateTime;

public class IdentificationPopulator implements Populator<Identification> {
    private HenkiloPopulator henkilo;
    private String idpEntityId;
    private String identifier;
    private String authToken;
    LocalDateTime authTokenCreated;

    public IdentificationPopulator(String idpEntityId, String identifier, HenkiloPopulator henkilo) {
        this.idpEntityId = idpEntityId;
        this.identifier = identifier;
        this.henkilo = henkilo;
    }

    public static IdentificationPopulator identification(String idpEntityId, String identifier, HenkiloPopulator henkilo) {
        return new IdentificationPopulator(idpEntityId, identifier, henkilo);
    }

    public IdentificationPopulator withAuthToken(String token) {
        this.authToken = token;
        this.authTokenCreated = LocalDateTime.now();
        return this;
    }

    @Override
    public Identification apply(EntityManager entityManager) {
        Henkilo henkiloo = henkilo.apply(entityManager);
        Identification identification = new Identification();
        identification.setIdentifier(identifier);
        identification.setIdpEntityId(idpEntityId);
        identification.setAuthtoken(authToken);
        identification.setAuthTokenCreated(authTokenCreated);
        identification.setHenkilo(henkiloo);
        entityManager.persist(identification);
        return entityManager.find(Identification.class, identification.getId());
    }
}
