package fi.vm.sade.kayttooikeus.service.impl;

import fi.vm.sade.kayttooikeus.config.OrikaBeanMapper;
import fi.vm.sade.kayttooikeus.config.properties.CommonProperties;
import fi.vm.sade.kayttooikeus.dto.KayttajaCriteriaDto;
import fi.vm.sade.kayttooikeus.dto.KayttajaReadDto;
import fi.vm.sade.kayttooikeus.dto.PalveluRooliGroup;
import fi.vm.sade.kayttooikeus.repositories.HenkiloHibernateRepository;
import fi.vm.sade.kayttooikeus.repositories.OrganisaatioHenkiloRepository;
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

import java.util.List;
import java.util.Set;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

@Service
@Transactional
@RequiredArgsConstructor
public class KayttajaServiceImpl implements KayttajaService {

    private static final Logger LOGGER = LoggerFactory.getLogger(KayttajaServiceImpl.class);

    private final PermissionCheckerService permissionCheckerService;
    private final OppijanumerorekisteriClient oppijanumerorekisteriClient;
    private final HenkiloHibernateRepository henkiloHibernateRepository;
    private final OrganisaatioHenkiloRepository organisaatioHenkiloRepository;
    private final OrikaBeanMapper mapper;
    private final CommonProperties commonProperties;

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

        String kayttajaOid = permissionCheckerService.getCurrentUserOid();
        List<String> organisaatioOids = organisaatioHenkiloRepository.findUsersOrganisaatioHenkilosByPalveluRoolis(
                kayttajaOid, PalveluRooliGroup.KAYTTAJAHAKU);
        if (!organisaatioOids.contains(commonProperties.getRootOrganizationOid())) {
            organisaatioHenkiloCriteria.setOrRetainOrganisaatioOids(organisaatioOids);
        }

        return henkiloHibernateRepository.findOidsBy(organisaatioHenkiloCriteria);
    }

}
