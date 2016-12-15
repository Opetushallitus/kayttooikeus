package fi.vm.sade.kayttooikeus.repositories;

import fi.vm.sade.kayttooikeus.dto.HenkiloTyyppi;
import fi.vm.sade.kayttooikeus.model.Henkilo;
import fi.vm.sade.kayttooikeus.model.Kayttajatiedot;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@DataJpaTest
@Transactional(readOnly = true)
public class HenkiloRepositoryTest {
    @Autowired
    HenkiloRepository henkiloRepository;

    @Autowired
    HenkiloHibernateRepository henkiloHibernateRepository;

    @Autowired
    TestEntityManager testEntityManager;

    @Test
    public void findByOidHenkilo() {
        Henkilo henkilo = new Henkilo();
        henkilo.setHenkiloTyyppi(HenkiloTyyppi.OPPIJA);
        henkilo.setOidHenkilo("1.2.3.4.5");
        this.testEntityManager.persist(henkilo);

        Optional<Henkilo> returnHenkilo = this.henkiloRepository.findByOidHenkilo("1.2.3.4.5");
        assertThat(returnHenkilo).hasValueSatisfying(h -> assertThat(h.getOidHenkilo()).isEqualTo("1.2.3.4.5"));
    }

    @Test
    public void findByOidHenkiloNotFound() {
        Optional<Henkilo> returnHenkilo = this.henkiloRepository.findByOidHenkilo("1.2.3.4.5");
        assertThat(returnHenkilo).isEmpty();
    }

    @Test
    public void getUsernameByOidHenkilo() {
        Henkilo henkilo = new Henkilo();
        henkilo.setHenkiloTyyppi(HenkiloTyyppi.OPPIJA);
        henkilo.setOidHenkilo("1.2.3.4.5");

        Kayttajatiedot kayttajatiedot = new Kayttajatiedot();
        kayttajatiedot.setUsername("username");
        kayttajatiedot.setHenkilo(henkilo);
        henkilo.setKayttajatiedot(kayttajatiedot);
        this.testEntityManager.persist(henkilo);
        this.testEntityManager.persistAndFlush(kayttajatiedot);

        Optional<String> username = this.henkiloHibernateRepository.getUsernameByOidHenkilo("1.2.3.4.5");
        assertThat(username).hasValue("username");
    }

    @Test
    public void getUsernameByOidHenkiloUsernameNull() {
        Henkilo henkilo = new Henkilo();
        henkilo.setHenkiloTyyppi(HenkiloTyyppi.OPPIJA);
        henkilo.setOidHenkilo("1.2.3.4.5");
        this.testEntityManager.persist(henkilo);

        Optional<String> username = this.henkiloHibernateRepository.getUsernameByOidHenkilo("1.2.3.4.5");
        assertThat(username).isEmpty();
    }


}
