package fi.vm.sade.kayttooikeus.repositories;

import fi.vm.sade.kayttooikeus.dto.HenkiloLinkitysDto;
import fi.vm.sade.kayttooikeus.model.Henkilo;
import fi.vm.sade.kayttooikeus.model.HenkiloVarmentaja;
import fi.vm.sade.kayttooikeus.service.PermissionCheckerService;
import fi.vm.sade.kayttooikeus.service.external.OrganisaatioClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@DataJpaTest
@Transactional(readOnly = true)
public class HenkiloDataRepositoryTest {
    @Autowired
    private HenkiloDataRepository henkiloDataRepository;

    @Autowired
    private TestEntityManager testEntityManager;

    @MockBean
    private PermissionCheckerService permissionCheckerService;

    @MockBean
    private OrganisaatioClient organisaatioClient;

    @Test
    public void findByOidHenkilo() {
        Henkilo henkilo = new Henkilo();
        henkilo.setOidHenkilo("1.2.3.4.5");
        this.testEntityManager.persistAndFlush(henkilo);

        Optional<Henkilo> returnHenkilo = this.henkiloDataRepository.findByOidHenkilo("1.2.3.4.5");
        assertThat(returnHenkilo).hasValueSatisfying(h -> assertThat(h.getOidHenkilo()).isEqualTo("1.2.3.4.5"));
    }

    @Test
    public void findByOidHenkiloNotFound() {
        Optional<Henkilo> returnHenkilo = this.henkiloDataRepository.findByOidHenkilo("1.2.3.4.5");
        assertThat(returnHenkilo).isEmpty();
    }

    @Test
    public void linkedHenkilosAreFound() {
        Henkilo varmennettava = new Henkilo();
        varmennettava.setOidHenkilo("1.2.3.4.5");
        this.testEntityManager.persistAndFlush(varmennettava);

        Henkilo varmentaja = new Henkilo();
        varmentaja.setOidHenkilo("5.4.3.2.1");
        this.testEntityManager.persistAndFlush(varmentaja);

        HenkiloVarmentaja henkiloVarmentaja = new HenkiloVarmentaja();
        henkiloVarmentaja.setVarmennettavaHenkilo(varmennettava);
        henkiloVarmentaja.setVarmentavaHenkilo(varmentaja);
        henkiloVarmentaja.setTila(true);
        henkiloVarmentaja.setAikaleima(LocalDateTime.now());
        this.testEntityManager.persistAndFlush(henkiloVarmentaja);

        HenkiloLinkitysDto varmennettavaHenkiloLinkitys = this.henkiloDataRepository.findLinkityksetByOid("1.2.3.4.5");
        assertThat(varmennettavaHenkiloLinkitys.getHenkiloVarmennettavas()).isEmpty();
        assertThat(varmennettavaHenkiloLinkitys.getHenkiloVarmentajas()).containsExactly("5.4.3.2.1");

        HenkiloLinkitysDto varmantajaHenkiloLinkitys = this.henkiloDataRepository.findLinkityksetByOid("5.4.3.2.1");
        assertThat(varmantajaHenkiloLinkitys.getHenkiloVarmennettavas()).containsExactly("1.2.3.4.5");
        assertThat(varmantajaHenkiloLinkitys.getHenkiloVarmentajas()).isEmpty();
    }

    @Test
    public void notLinkedHenkilosWork() {
        HenkiloLinkitysDto tyhjaHenkiloLinkitys = this.henkiloDataRepository.findLinkityksetByOid("ei löydy kannasta");
        assertThat(tyhjaHenkiloLinkitys.getHenkiloVarmennettavas()).isEmpty();
        assertThat(tyhjaHenkiloLinkitys.getHenkiloVarmentajas()).isEmpty();
    }
}
