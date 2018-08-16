package fi.vm.sade.kayttooikeus.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Date;

@Entity
@Getter
@Setter
@Table(name = "identification", uniqueConstraints = {
        @UniqueConstraint(name = "identification_uk1", columnNames = {"idpentityid", "identifier"}),
})
public class Identification extends IdentifiableAndVersionedEntity {

    public static final String WEAK_AUTHENTICATION_IDP = "email";
    public static final String STRONG_AUTHENTICATION_IDP = "vetuma";
    public static final String HAKA_AUTHENTICATION_IDP = "haka";

    public Identification() {
    }

    public Identification(Henkilo henkilo, String idpEntityId, String identifier) {
        this.henkilo = henkilo;
        this.idpEntityId = idpEntityId;
        this.identifier = identifier;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "henkilo_id", nullable = false)
    private Henkilo henkilo;

    @Column(name = "idpentityid", nullable = false)
    private String idpEntityId;

    @Column(name = "identifier", nullable = false)
    private String identifier;

    @Column(name = "authtoken")
    private String authtoken;

    private LocalDateTime authTokenCreated;

    /**
     * Voimassaoloaika.
     *
     * @deprecated Sarake löytyy henkilöpalvelun tietokannasta, mutta sille ei
     * ole tällä hetkellä käyttöä käyttöoikeuspalvelun puolella
     */
    @Column(name = "expiration_date")
    @Temporal(TemporalType.TIMESTAMP)
    @Deprecated
    private Date expirationDate;

    @Column(name = "email")
    private String email;

    public boolean isVahvaTunniste() {
        return !idpEntityId.equals(WEAK_AUTHENTICATION_IDP);
    }
}
