package fi.vm.sade.kayttooikeus.repositories;

import com.google.common.collect.Sets;
import fi.vm.sade.kayttooikeus.dto.KayttoOikeudenTila;
import fi.vm.sade.kayttooikeus.enumeration.KayttooikeusRooli;
import fi.vm.sade.kayttooikeus.model.AnomuksenTila;
import fi.vm.sade.kayttooikeus.model.HaettuKayttoOikeusRyhma;
import fi.vm.sade.kayttooikeus.repositories.criteria.AnomusCriteria;
import fi.vm.sade.kayttooikeus.service.external.OrganisaatioClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

import static fi.vm.sade.kayttooikeus.repositories.populate.AnomusPopulator.anomus;
import static fi.vm.sade.kayttooikeus.repositories.populate.HaettuKayttoOikeusRyhmaPopulator.haettuKayttooikeusryhma;
import static fi.vm.sade.kayttooikeus.repositories.populate.KayttoOikeusPopulator.oikeus;
import static fi.vm.sade.kayttooikeus.repositories.populate.KayttoOikeusRyhmaPopulator.kayttoOikeusRyhma;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
public class HaettuKayttooikeusRyhmaRepositoryCustomTest extends AbstractRepositoryTest {
    @Autowired
    private HaettuKayttooikeusRyhmaRepository haettuKayttooikeusRyhmaRepository;

    @MockBean
    private OrganisaatioClient organisaatioClient;

    @Before
    public void setup() {
        populate(anomus("arpa@kuutio.fi").tila(AnomuksenTila.ANOTTU)
                .withHaettuRyhma(haettuKayttooikeusryhma(null)
                        .withRyhma(kayttoOikeusRyhma("Rekisterinpitäjä (vain OPHn käytössä)")
                                .withOikeus(oikeus("KAYTTOOIKEUS", KayttooikeusRooli.VASTUUKAYTTAJAT.getName())))));
        populate(anomus("arpa@kuutio.fi").tila(AnomuksenTila.ANOTTU)
                .withHaettuRyhma(haettuKayttooikeusryhma(KayttoOikeudenTila.ANOTTU)
                .withRyhma(kayttoOikeusRyhma("Pääkäyttäjä (kk)")
                        .withOikeus(oikeus("KAYTTOOIKEUS", KayttooikeusRooli.VASTUUKAYTTAJAT.getName())))));
        // Hidden kayttooikeusryhmas are not fetched
        populate(anomus("arpa@kuutio.fi").tila(AnomuksenTila.ANOTTU)
                .withHaettuRyhma(haettuKayttooikeusryhma(null)
                        .withRyhma(kayttoOikeusRyhma("Koodiston ylläpitäjä")
                                .withOikeus(oikeus("KAYTTOOIKEUS", KayttooikeusRooli.VASTUUKAYTTAJAT.getName()))
                                .asPassivoitu())));
        populate(anomus("arpa@kuutio.fi").tila(AnomuksenTila.ANOTTU)
                .withHaettuRyhma(haettuKayttooikeusryhma(KayttoOikeudenTila.MYONNETTY)
                        .withRyhma(kayttoOikeusRyhma("Granted ryhmä"))));
        populate(anomus("arpa@kuutio.fi").tila(AnomuksenTila.ANOTTU)
                .withHaettuRyhma(haettuKayttooikeusryhma(null)
                        .withRyhma(kayttoOikeusRyhma("Some random ryhmä"))));
        populate(haettuKayttooikeusryhma(KayttoOikeudenTila.ANOTTU)
                .withRyhma(kayttoOikeusRyhma("Ryhmä without anomus")));
    }

    @Test
    public void findByBasic() throws Exception {
        AnomusCriteria anomusCriteria = AnomusCriteria.builder()
                .onlyActive(true)
                .anomuksenTilat(Sets.newHashSet(AnomuksenTila.ANOTTU))
                .build();
        List<HaettuKayttoOikeusRyhma> haetutResult = this.haettuKayttooikeusRyhmaRepository
                .findBy(anomusCriteria.createAnomusSearchCondition(this.organisaatioClient), null, null, null);
        assertThat(haetutResult)
                .extracting("kayttoOikeusRyhma")
                .extracting("tunniste")
                .containsExactlyInAnyOrder("Rekisterinpitäjä (vain OPHn käytössä)", "Pääkäyttäjä (kk)",
                        "Some random ryhmä");
    }

    @Test
    public void findByBasicWithLimitOffset() throws Exception {
        AnomusCriteria anomusCriteria = AnomusCriteria.builder()
                .onlyActive(true)
                .build();
        List<HaettuKayttoOikeusRyhma> haetutResult = this.haettuKayttooikeusRyhmaRepository
                .findBy(anomusCriteria.createAnomusSearchCondition(this.organisaatioClient), 2L, 1L, null);
        assertThat(haetutResult)
                .extracting("kayttoOikeusRyhma")
                .extracting("tunniste")
                .containsExactlyInAnyOrder("Pääkäyttäjä (kk)", "Some random ryhmä");
    }

    @Test
    public void findByAdminView() throws Exception {
        AnomusCriteria anomusCriteria = AnomusCriteria.builder()
                .onlyActive(true)
                .adminView(true)
                .build();
        List<HaettuKayttoOikeusRyhma> haetutResult = this.haettuKayttooikeusRyhmaRepository
                .findBy(anomusCriteria.createAnomusSearchCondition(this.organisaatioClient), null, null, null);
        assertThat(haetutResult)
                .extracting("kayttoOikeusRyhma")
                .extracting("tunniste")
                .containsExactlyInAnyOrder("Rekisterinpitäjä (vain OPHn käytössä)", "Pääkäyttäjä (kk)");
    }

}
