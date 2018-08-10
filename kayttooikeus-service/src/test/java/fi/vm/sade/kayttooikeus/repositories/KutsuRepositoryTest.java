package fi.vm.sade.kayttooikeus.repositories;

import fi.vm.sade.kayttooikeus.dto.KutsunTila;
import fi.vm.sade.kayttooikeus.dto.enumeration.KutsuView;
import fi.vm.sade.kayttooikeus.enumeration.KutsuOrganisaatioOrder;
import fi.vm.sade.kayttooikeus.model.KayttoOikeus;
import fi.vm.sade.kayttooikeus.model.KayttoOikeusRyhma;
import fi.vm.sade.kayttooikeus.model.Kutsu;
import fi.vm.sade.kayttooikeus.model.KutsuOrganisaatio;
import fi.vm.sade.kayttooikeus.repositories.criteria.KutsuCriteria;
import fi.vm.sade.kayttooikeus.service.PermissionCheckerService;
import org.assertj.core.util.Lists;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDateTime;
import java.util.List;

import static fi.vm.sade.kayttooikeus.controller.KutsuPopulator.kutsu;
import static fi.vm.sade.kayttooikeus.dto.KutsunTila.AVOIN;
import static fi.vm.sade.kayttooikeus.repositories.populate.KayttoOikeusPopulator.oikeus;
import static fi.vm.sade.kayttooikeus.repositories.populate.KayttoOikeusRyhmaPopulator.kayttoOikeusRyhma;
import static fi.vm.sade.kayttooikeus.repositories.populate.KutsuOrganisaatioPopulator.kutsuOrganisaatio;
import static fi.vm.sade.kayttooikeus.repositories.populate.OrganisaatioHenkiloKayttoOikeusPopulator.myonnettyKayttoOikeus;
import static fi.vm.sade.kayttooikeus.repositories.populate.OrganisaatioHenkiloPopulator.organisaatioHenkilo;
import static fi.vm.sade.kayttooikeus.repositories.populate.TextGroupPopulator.text;
import static fi.vm.sade.kayttooikeus.service.impl.PermissionCheckerServiceImpl.PALVELU_HENKILONHALLINTA;
import static fi.vm.sade.kayttooikeus.service.impl.PermissionCheckerServiceImpl.PALVELU_KAYTTOOIKEUS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
public class KutsuRepositoryTest extends AbstractRepositoryTest {
    @Autowired
    private KutsuRepositoryCustom kutsuRepository;

    @MockBean
    private PermissionCheckerService permissionCheckerService;

    @Test
    public void listKutsuListDtosTest() {
        populate(myonnettyKayttoOikeus(organisaatioHenkilo("1.2.3", "1.2.3.4.5"),
                kayttoOikeusRyhma("RYHMA1")));
        Kutsu kutsu = populate(kutsu("Aapo", "Esimerkki", "a@example.com")
            .kutsuja("1.2.3").aikaleima(LocalDateTime.of(2016,1,1,0,0,0, 0))
            .organisaatio(kutsuOrganisaatio("1.2.3.4.5")
                .ryhma(kayttoOikeusRyhma("RYHMA")
                    .withNimi(text("FI", "Kuvaus")))
            )
        );

        List<Kutsu> results = kutsuRepository.listKutsuListDtos(KutsuCriteria.builder().kutsujaOid("1.2.3")
                .tilas(Lists.newArrayList(KutsunTila.AVOIN)).build(), KutsuOrganisaatioOrder.AIKALEIMA.getSortWithDirection(), null, null);
        assertEquals(1, results.size());
        Kutsu dto = results.get(0);
        assertEquals(LocalDateTime.of(2016,1,1,0,0,0, 0), dto.getAikaleima());
        assertEquals("a@example.com", dto.getSahkoposti());
        assertEquals(AVOIN, dto.getTila());
        assertEquals(kutsu.getId(), dto.getId());

        results = kutsuRepository.listKutsuListDtos(KutsuCriteria.builder().kutsujaOid("1.2.3")
                .tilas(Lists.newArrayList(KutsunTila.POISTETTU, KutsunTila.KAYTETTY)).build(), KutsuOrganisaatioOrder.AIKALEIMA.getSortWithDirection(), null, null);
        assertEquals(0, results.size());

        results = kutsuRepository.listKutsuListDtos(new KutsuCriteria(), KutsuOrganisaatioOrder.AIKALEIMA.getSortWithDirection(), null, null);
        assertEquals(1, results.size());
    }

    @Test
    public void listKutsuWithAdminView() {
        populate(myonnettyKayttoOikeus(organisaatioHenkilo("1.2.3", "1.2.3.4.5"),
                kayttoOikeusRyhma("RYHMA1")));
        populate(kutsu("Aapo", "Esimerkki", "a@example.com")
                .kutsuja("1.2.3")
                .aikaleima(LocalDateTime.of(2016, 1, 1, 0, 0, 0, 0))
                .organisaatio(kutsuOrganisaatio("1.2.3.4.5")
                        .ryhma(kayttoOikeusRyhma("RYHMA")
                                .withNimi(text("FI", "Kuvaus"))
                                .withOikeus(oikeus(PALVELU_KAYTTOOIKEUS, "VASTUUKAYTTAJAT")))
                )
        );
        populate(kutsu("Beepo", "Bsimerkki", "b@example.com")
                .kutsuja("1.2.3")
                .aikaleima(LocalDateTime.of(2016, 1, 1, 0, 0, 0, 0))
                .organisaatio(kutsuOrganisaatio("1.2.3.4.5")
                        .ryhma(kayttoOikeusRyhma("RYHMA2")
                                .withNimi(text("FI", "Kuvaus2"))
                                .withOikeus(oikeus(PALVELU_HENKILONHALLINTA, "READ")))
                )
        );
        List<Kutsu> kutsuList = this.kutsuRepository.listKutsuListDtos(KutsuCriteria.builder().view(KutsuView.ADMIN).build(),
                KutsuOrganisaatioOrder.AIKALEIMA.getSortWithDirection(),
                null, null);
        assertThat(kutsuList).flatExtracting(Kutsu::getSahkoposti).containsExactly("a@example.com");
        assertThat(kutsuList)
                .flatExtracting(Kutsu::getOrganisaatiot)
                .flatExtracting(KutsuOrganisaatio::getRyhmat)
                .flatExtracting(KayttoOikeusRyhma::getKayttoOikeus)
                .flatExtracting(KayttoOikeus::getRooli)
                .containsExactly("VASTUUKAYTTAJAT");
    }

}
