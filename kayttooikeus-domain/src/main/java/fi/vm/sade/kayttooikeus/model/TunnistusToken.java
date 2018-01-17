package fi.vm.sade.kayttooikeus.model;

import lombok.*;

import javax.persistence.*;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tunnistus_token")
public class TunnistusToken extends IdentifiableAndVersionedEntity {

    @Column(name = "login_token")
    private String loginToken;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "henkilo_id", nullable = false)
    private Henkilo henkilo;

    @Column(name = "aikaleima")
    private LocalDateTime aikaleima;

    @Column(name = "kaytetty")
    private LocalDateTime kaytetty;

    @Column(name = "hetu")
    private String hetu; // vahvasta tunnistautumisesta saatu hetu

}
