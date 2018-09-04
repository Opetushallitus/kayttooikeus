package fi.vm.sade.kayttooikeus.service.impl;

import fi.vm.sade.kayttooikeus.config.OrikaBeanMapper;
import fi.vm.sade.kayttooikeus.dto.KayttajaCriteriaDto;
import fi.vm.sade.kayttooikeus.dto.KayttajaReadDto;
import fi.vm.sade.kayttooikeus.repositories.HenkiloHibernateRepository;
import fi.vm.sade.kayttooikeus.repositories.criteria.OrganisaatioHenkiloCriteria;
import fi.vm.sade.kayttooikeus.service.KayttajaService;
import fi.vm.sade.kayttooikeus.service.PermissionCheckerService;
import fi.vm.sade.kayttooikeus.service.external.OppijanumerorekisteriClient;
import fi.vm.sade.oppijanumerorekisteri.dto.HenkiloHakuCriteria;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

@Service
@Transactional
@RequiredArgsConstructor
public class KayttajaServiceImpl implements KayttajaService {

    private static final Logger LOGGER = LoggerFactory.getLogger(KayttajaServiceImpl.class);

    private final PermissionCheckerService permissionCheckerService;
    private final OppijanumerorekisteriClient oppijanumerorekisteriClient;
    private final HenkiloHibernateRepository henkiloHibernateRepository;
    private final OrikaBeanMapper mapper;

    @Override
    public Iterable<KayttajaReadDto> list(KayttajaCriteriaDto criteria) {
        LOGGER.info("Haetaan käyttäjät {}", criteria);

        if (criteria.getOrganisaatioOids() == null && criteria.getKayttooikeudet() == null) {
            throw new IllegalArgumentException("Pakollinen hakuehto 'organisaatioOids' tai 'kayttooikeudet' puuttuu");
        }

        Set<String> henkiloOids = getHenkiloOids(criteria);
        if (henkiloOids.isEmpty()) {
            return emptyList();
        }

        HenkiloHakuCriteria oppijanumerorekisteriCriteria = mapper.map(criteria, HenkiloHakuCriteria.class);
        oppijanumerorekisteriCriteria.setHenkiloOids(henkiloOids);
        return oppijanumerorekisteriClient.listYhteystiedot(oppijanumerorekisteriCriteria).stream()
                .map(henkilo -> mapper.map(henkilo, KayttajaReadDto.class))
                .collect(toList());
    }

    private Set<String> getHenkiloOids(KayttajaCriteriaDto kayttajaCriteria) {
        OrganisaatioHenkiloCriteria organisaatioHenkiloCriteria = mapper.map(kayttajaCriteria, OrganisaatioHenkiloCriteria.class);
        if (kayttajaCriteria.getKayttooikeudet() != null) {
            organisaatioHenkiloCriteria.setKayttooikeudet(kayttajaCriteria.getKayttooikeudet().entrySet().stream()
                    .flatMap(entry -> entry.getValue().stream().map(value -> entry.getKey() + "_" + value))
                    .collect(toSet()));
        }

        // juuriorganisaatioon kuuluvalla henkilöllä on oikeus kaikkiin alla oleviin organisaatioihin
        if (this.permissionCheckerService.isCurrentUserMiniAdmin()) {
            return henkiloHibernateRepository.findOidsBy(organisaatioHenkiloCriteria);
        }

        // perustapauksena henkilöllä on oikeus omien organisaatioiden henkilötietoihin
        String kayttajaOid = permissionCheckerService.getCurrentUserOid();
        return henkiloHibernateRepository.findOidsBySamaOrganisaatio(kayttajaOid, organisaatioHenkiloCriteria);
    }

}
