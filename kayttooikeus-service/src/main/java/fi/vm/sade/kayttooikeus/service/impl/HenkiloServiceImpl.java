package fi.vm.sade.kayttooikeus.service.impl;

import com.google.common.collect.Lists;
import fi.vm.sade.kayttooikeus.dto.OrganisaatioOidsSearchDto;
import fi.vm.sade.kayttooikeus.repositories.HenkiloHibernateRepository;
import fi.vm.sade.kayttooikeus.service.HenkiloService;
import fi.vm.sade.kayttooikeus.service.PermissionCheckerService;
import fi.vm.sade.kayttooikeus.service.exception.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class HenkiloServiceImpl extends AbstractService implements HenkiloService {

    private HenkiloHibernateRepository henkiloHibernateRepository;

    private PermissionCheckerService permissionCheckerService;

    @Autowired
    HenkiloServiceImpl(HenkiloHibernateRepository henkiloHibernateRepository,
                       PermissionCheckerService permissionCheckerService) {
        this.henkiloHibernateRepository = henkiloHibernateRepository;
        this.permissionCheckerService = permissionCheckerService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> findHenkilos(OrganisaatioOidsSearchDto organisaatioOidsSearchDto) {
        ArrayList<String> allowedRoles = Lists.newArrayList("READ", "READ_UPDATE", "CRUD");
        Set<String> roles = getCasRoles();

        return henkiloHibernateRepository.findHenkiloOids(organisaatioOidsSearchDto.getHenkiloTyyppi(),
                organisaatioOidsSearchDto.getOrganisaatioOids(), organisaatioOidsSearchDto.getGroupName())
                .stream()
                .filter(henkiloOid -> permissionCheckerService.hasInternalAccess(henkiloOid, allowedRoles, roles))
                .collect(Collectors.toList());
    }

    @Override
    public String getUsernameByOidHenkilo(String oidHenkilo) {
        return this.henkiloHibernateRepository.getUsernameByOidHenkilo(oidHenkilo)
                .orElseThrow(() -> new NotFoundException("Username not found by given henkilo oid"));
    }
}
