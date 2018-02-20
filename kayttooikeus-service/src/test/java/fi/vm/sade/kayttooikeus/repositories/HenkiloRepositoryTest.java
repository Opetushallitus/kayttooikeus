package fi.vm.sade.kayttooikeus.repositories;

import fi.vm.sade.kayttooikeus.enumeration.OrderByHenkilohaku;
import fi.vm.sade.kayttooikeus.repositories.criteria.HenkiloCriteria;
import fi.vm.sade.kayttooikeus.repositories.dto.HenkilohakuResultDto;
import fi.vm.sade.kayttooikeus.repositories.populate.HenkiloPopulator;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

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
}
