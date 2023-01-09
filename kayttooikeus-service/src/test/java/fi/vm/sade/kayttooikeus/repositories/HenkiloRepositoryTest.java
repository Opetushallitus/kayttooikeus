package fi.vm.sade.kayttooikeus.repositories;

import fi.vm.sade.kayttooikeus.enumeration.OrderByHenkilohaku;
import fi.vm.sade.kayttooikeus.repositories.criteria.HenkiloCriteria;
import fi.vm.sade.kayttooikeus.repositories.criteria.OrganisaatioHenkiloCriteria;
import fi.vm.sade.kayttooikeus.repositories.dto.HenkilohakuResultDto;
import fi.vm.sade.kayttooikeus.repositories.populate.HenkiloPopulator;
import fi.vm.sade.kayttooikeus.repositories.populate.OrganisaatioHenkiloPopulator;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
public class HenkiloRepositoryTest extends AbstractRepositoryTest {
    @Autowired
    private HenkiloHibernateRepository henkiloHibernateRepository;

    @Test
    public void findByCriteriaNameQuery() {
        populate(HenkiloPopulator.henkilo("1.2.3.4.5").withNimet("etunimi1", "sukunimi1").withUsername("arpa1"));
        populate(HenkiloPopulator.henkilo("1.2.3.4.6").withNimet("etunimi2", "sukunimi2").withUsername("arpa2"));
        populate(HenkiloPopulator.henkilo("1.2.3.4.7").withNimet("etunimi3", "sukunimi3").withUsername("arpa3"));
        populate(HenkiloPopulator.henkilo("1.2.3.4.8").withNimet("etunimi4", "sukunimi4"));
        populate(HenkiloPopulator.henkilo("1.2.3.4.9").withNimet("etunimi5", "sukunimi5"));

        List<HenkilohakuResultDto> henkilohakuResultDtoList = this.henkiloHibernateRepository.findByCriteria(
                HenkiloCriteria.builder()
                        .nameQuery("etunimi")
                        .noOrganisation(true)
                        .build(),
                1L,
                100L,
                OrderByHenkilohaku.HENKILO_NIMI_DESC.getValue());
        assertThat(henkilohakuResultDtoList).extracting(HenkilohakuResultDto::getNimi)
                .containsExactly("sukunimi4, etunimi4",
                        "sukunimi3, etunimi3",
                        "sukunimi2, etunimi2",
                        "sukunimi1, etunimi1");
        assertThat(henkilohakuResultDtoList).extracting(HenkilohakuResultDto::getOidHenkilo)
                .containsExactly("1.2.3.4.8", "1.2.3.4.7", "1.2.3.4.6", "1.2.3.4.5");
        assertThat(henkilohakuResultDtoList).extracting(HenkilohakuResultDto::getKayttajatunnus)
                .containsExactly(null, "arpa3", "arpa2", "arpa1");
    }

    private void populateVoimassaHenkilos() {
        var now = LocalDate.now();
        populate(
                OrganisaatioHenkiloPopulator.organisaatioHenkilo(
                        HenkiloPopulator.henkilo("1.2.0.0.1").withUsername("ville_voimassa"), 
                        "2.1.0.1"
                ).voimassaAlkaen(now.minusDays(3)).voimassaAsti(now.plusDays(3))
        );
        populate(
                OrganisaatioHenkiloPopulator.organisaatioHenkilo(
                        HenkiloPopulator.henkilo("1.2.0.0.2").withUsername("jussi_justnyt"), 
                        "2.1.0.1"
                ).voimassaAlkaen(now).voimassaAsti(now)
        );
        populate(
                OrganisaatioHenkiloPopulator.organisaatioHenkilo(
                        HenkiloPopulator.henkilo("1.2.0.0.3").withUsername("teppo_tulevaisuus"), 
                        "2.1.0.1"
                ).voimassaAlkaen(now.plusDays(3)).voimassaAsti(now.plusDays(6))
        );
        populate(
                OrganisaatioHenkiloPopulator.organisaatioHenkilo(
                        HenkiloPopulator.henkilo("1.2.0.0.4").withUsername("matti_menneisyys"), 
                        "2.1.0.1"
                ).voimassaAlkaen(now.minusDays(6)).voimassaAsti(now.minusDays(3))
        );
    }

    @Test
    public void findOidsByFindsHenkilosWithVoimassa() {
        populateVoimassaHenkilos();

        OrganisaatioHenkiloCriteria criteria = new OrganisaatioHenkiloCriteria();
        criteria.setOrganisaatioOids(Set.of("2.1.0.1"));
        criteria.setOrganisaatioVoimassa(true);

        Set<String> oids = this.henkiloHibernateRepository.findOidsBy(criteria);
        assertThat(oids).containsExactly("1.2.0.0.1", "1.2.0.0.2");
    }

    @Test
    public void findOidsByFindsHenkilosWithoutVoimassa() {
        populateVoimassaHenkilos();

        OrganisaatioHenkiloCriteria criteria = new OrganisaatioHenkiloCriteria();
        criteria.setOrganisaatioOids(Set.of("2.1.0.1"));
        criteria.setOrganisaatioVoimassa(false);

        Set<String> oids = this.henkiloHibernateRepository.findOidsBy(criteria);
        assertThat(oids).containsExactly("1.2.0.0.3", "1.2.0.0.4");
    }
}
