package fi.vm.sade.kayttooikeus.model;

import lombok.*;

import javax.persistence.*;

import fi.vm.sade.kayttooikeus.dto.MfaProvider;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Class that contains {@link Henkilo}'s password hash and salt. Only on may
 * exist per {@link Henkilo}*
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "kayttajatiedot")
public class Kayttajatiedot extends IdentifiableAndVersionedEntity {

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "henkiloid", nullable = false, unique = true)
    private Henkilo henkilo;

    @OneToOne(mappedBy = "kayttajatiedot", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private LoginCounter loginCounter;

    /**
     * Username for Henkilo
     */
    @Column(name = "username", unique = true, nullable = false)
    private String username;

    /**
     * this is the permanent security token "salted hash", authtokens under
     * identification are used for temporary authentication
     * <p>
     * Null token disables weak login
     */
    @Column(name = "password")
    private String password;

    /**
     * Salt used for securityToken
     */
    @Column(name = "salt")
    private String salt;

    /**
     * Can be used to invalidate password for being too old
     */
    @Column(name = "createdat")
    private LocalDateTime createdAt;

    /**
     * Manually invalidated password
     */
    @Column(name = "invalidated")
    private Boolean invalidated = false;

    @Column(name = "mfaprovider")
    @Enumerated(EnumType.STRING)
    private MfaProvider mfaProvider;

    @Column(name = "mfabypass")
    private LocalDateTime mfaBypass;

    @Column(name = "mfabypasscount")
    private Integer mfaBypassCount;

    public void incrementLoginCount() {
        loginCounter = Optional.ofNullable(loginCounter)
                .orElse(LoginCounter.builder().kayttajatiedot(this).build());
        loginCounter.increment();
    }

    @PrePersist
    @PreUpdate
    public void setPersistDate() {
        createdAt = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "Kayttajatiedot{" +
                "username='" + username + '\'' +
                ", invalidated=" + invalidated +
                ", createdAt=" + createdAt +
                '}';
    }
}
