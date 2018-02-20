package fi.vm.sade.kayttooikeus.service.impl;

import fi.vm.sade.kayttooikeus.config.OrikaBeanMapper;
import fi.vm.sade.kayttooikeus.config.properties.CommonProperties;
import fi.vm.sade.kayttooikeus.dto.HenkilohakuCriteriaDto;
import fi.vm.sade.kayttooikeus.repositories.HenkiloDataRepository;
import fi.vm.sade.kayttooikeus.repositories.HenkiloHibernateRepository;
import fi.vm.sade.kayttooikeus.repositories.OrganisaatioHenkiloRepository;
import fi.vm.sade.kayttooikeus.service.PermissionCheckerService;
import fi.vm.sade.kayttooikeus.service.external.OrganisaatioClient;
import fi.vm.sade.kayttooikeus.util.HenkilohakuBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import fi.vm.sade.kayttooikeus.service.HenkilohakuBuilderService;

@Service
@RequiredArgsConstructor
public class HenkilohakuBuilderServiceImpl implements HenkilohakuBuilderService {

    private final HenkiloHibernateRepository henkiloHibernateRepository;
    private final OrikaBeanMapper mapper;
    private final PermissionCheckerService permissionCheckerService;
    private final HenkiloDataRepository henkiloDataRepository;
    private final OrganisaatioClient organisaatioClient;
    private final OrganisaatioHenkiloRepository organisaatioHenkiloRepository;
    private final CommonProperties commonProperties;

    @Override
    public HenkilohakuBuilder getBuilder(HenkilohakuCriteriaDto criteria) {
        return new HenkilohakuBuilder(henkiloHibernateRepository, mapper,
                permissionCheckerService, henkiloDataRepository,
                organisaatioClient, organisaatioHenkiloRepository,
                commonProperties).builder(criteria);
    }

}
