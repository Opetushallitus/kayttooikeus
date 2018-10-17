package fi.vm.sade.kayttooikeus.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.vm.sade.kayttooikeus.config.OrikaBeanMapper;
import fi.vm.sade.kayttooikeus.config.properties.CommonProperties;
import fi.vm.sade.kayttooikeus.dto.HenkilohakuCriteriaDto;
import fi.vm.sade.kayttooikeus.repositories.*;
import fi.vm.sade.kayttooikeus.repositories.criteria.HenkiloCriteria;
import fi.vm.sade.kayttooikeus.service.KayttoOikeusService;
import fi.vm.sade.kayttooikeus.service.LdapSynchronizationService;
import fi.vm.sade.kayttooikeus.service.PermissionCheckerService;
import fi.vm.sade.kayttooikeus.service.external.OppijanumerorekisteriClient;
import fi.vm.sade.kayttooikeus.service.external.OrganisaatioClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = OrikaBeanMapper.class)
public class HenkiloServiceImplTest {

    private HenkiloServiceImpl henkiloServiceImpl;

    @Mock
    private HenkiloHibernateRepository henkiloHibernateRepositoryMock;
    @Mock
    private PermissionCheckerService permissionCheckerServiceMock;
    @Mock
    private KayttoOikeusRyhmaTapahtumaHistoriaDataRepository kayttoOikeusRyhmaTapahtumaHistoriaDataRepositoryMock;
    @Mock
    private OrganisaatioHenkiloRepository organisaatioHenkiloRepositoryMock;
    @Mock
    private MyonnettyKayttoOikeusRyhmaTapahtumaRepository myonnettyKayttoOikeusRyhmaTapahtumaRepositoryMock;
    @Mock
    private LdapSynchronizationService ldapSynchronizationServiceMock;
    @Mock
    private HenkiloDataRepository henkiloDataRepositoryMock;
    @Mock
    private KayttajatiedotRepository kayttajatiedotRepositoryMock;
    @Mock
    private CommonProperties commonPropertiesMock;
    @Autowired
    private OrikaBeanMapper mapper;
    @Mock
    private OrganisaatioClient organisaatioClientMock;
    @Mock
    private KayttoOikeusService kayttoOikeusService;
    @Mock
    private OppijanumerorekisteriClient oppijanumerorekisteriClient;
    @Mock
    private ObjectMapper objectMapper;

    @Before
    public void setup() {
        henkiloServiceImpl = new HenkiloServiceImpl(
                henkiloHibernateRepositoryMock,
                permissionCheckerServiceMock,
                kayttoOikeusService,
                kayttoOikeusRyhmaTapahtumaHistoriaDataRepositoryMock,
                organisaatioHenkiloRepositoryMock,
                myonnettyKayttoOikeusRyhmaTapahtumaRepositoryMock,
                ldapSynchronizationServiceMock,
                henkiloDataRepositoryMock,
                kayttajatiedotRepositoryMock,
                commonPropertiesMock,
                mapper,
                organisaatioClientMock,
                oppijanumerorekisteriClient,
                objectMapper
        );
    }

    @Test
    public void henkilohakuHakeeIlmanAliorganisaatioita() {
        when(organisaatioHenkiloRepositoryMock.findUsersOrganisaatioHenkilosByPalveluRoolis(any(),any())).thenReturn(asList("oid1", "oid2"));
        when(commonPropertiesMock.getRootOrganizationOid()).thenReturn("rootOid");
        HenkilohakuCriteriaDto henkilohakuCriteriaDto = new HenkilohakuCriteriaDto();
        henkilohakuCriteriaDto.setOrganisaatioOids(null);
        henkilohakuCriteriaDto.setSubOrganisation(false);

        henkiloServiceImpl.henkilohaku(henkilohakuCriteriaDto, null, null);

        ArgumentCaptor<HenkiloCriteria> henkiloCriteriaCaptor = ArgumentCaptor.forClass(HenkiloCriteria.class);
        verify(henkiloHibernateRepositoryMock).findByCriteria(henkiloCriteriaCaptor.capture(), any(), any(), any());
        HenkiloCriteria henkiloCriteria = henkiloCriteriaCaptor.getValue();
        assertThat(henkiloCriteria.getOrganisaatioOids()).containsExactlyInAnyOrder("oid1", "oid2");
    }

    @Test
    public void henkilohakuHakeeAliorganisaatioilla() {
        when(organisaatioHenkiloRepositoryMock.findUsersOrganisaatioHenkilosByPalveluRoolis(any(), any())).thenReturn(asList("oid1", "oid2"));
        when(commonPropertiesMock.getRootOrganizationOid()).thenReturn("rootOid");
        when(organisaatioClientMock.getChildOids(eq("oid1"))).thenReturn(asList("childOid1"));
        when(organisaatioClientMock.getChildOids(eq("oid2"))).thenReturn(asList("childOid2"));
        HenkilohakuCriteriaDto henkilohakuCriteriaDto = new HenkilohakuCriteriaDto();
        henkilohakuCriteriaDto.setOrganisaatioOids(null);
        henkilohakuCriteriaDto.setSubOrganisation(true);

        henkiloServiceImpl.henkilohaku(henkilohakuCriteriaDto, null, null);

        ArgumentCaptor<HenkiloCriteria> henkiloCriteriaCaptor = ArgumentCaptor.forClass(HenkiloCriteria.class);
        verify(henkiloHibernateRepositoryMock).findByCriteria(henkiloCriteriaCaptor.capture(), any(), any(), any());
        HenkiloCriteria henkiloCriteria = henkiloCriteriaCaptor.getValue();
        assertThat(henkiloCriteria.getOrganisaatioOids()).containsExactlyInAnyOrder("oid1", "oid2", "childOid1", "childOid2");
    }

    @Test
    public void henkilohakuHakeeAnnetuillaOrganisaatioilla() {
        when(organisaatioHenkiloRepositoryMock.findUsersOrganisaatioHenkilosByPalveluRoolis(any(),any())).thenReturn(asList("oid1", "oid3", "oid5"));
        when(commonPropertiesMock.getRootOrganizationOid()).thenReturn("rootOid");
        when(organisaatioClientMock.getChildOids(eq("oid1"))).thenReturn(asList("childOid1"));
        when(organisaatioClientMock.getChildOids(eq("oid3"))).thenReturn(asList("childOid2"));
        when(organisaatioClientMock.getChildOids(eq("oid5"))).thenReturn(asList("childOid3"));
        HenkilohakuCriteriaDto henkilohakuCriteriaDto = new HenkilohakuCriteriaDto();
        henkilohakuCriteriaDto.setOrganisaatioOids(Stream.of("oid1", "oid3", "childOid1").collect(toSet()));
        henkilohakuCriteriaDto.setSubOrganisation(false);

        henkiloServiceImpl.henkilohaku(henkilohakuCriteriaDto, null, null);

        ArgumentCaptor<HenkiloCriteria> henkiloCriteriaCaptor = ArgumentCaptor.forClass(HenkiloCriteria.class);
        verify(henkiloHibernateRepositoryMock).findByCriteria(henkiloCriteriaCaptor.capture(), any(), any(), any());
        HenkiloCriteria henkiloCriteria = henkiloCriteriaCaptor.getValue();
        assertThat(henkiloCriteria.getOrganisaatioOids()).containsExactlyInAnyOrder("oid1", "oid3", "childOid1");
    }

    @Test
    public void henkilohakuHakeeAnnetuillaOrganisaatioillaAliorganisaatiot() {
        when(organisaatioHenkiloRepositoryMock.findUsersOrganisaatioHenkilosByPalveluRoolis(any(), any())).thenReturn(asList("oid1", "oid3", "oid5"));
        when(commonPropertiesMock.getRootOrganizationOid()).thenReturn("rootOid");
        when(organisaatioClientMock.getChildOids(eq("oid1"))).thenReturn(asList("childOid1"));
        when(organisaatioClientMock.getChildOids(eq("oid3"))).thenReturn(asList("childOid2"));
        when(organisaatioClientMock.getChildOids(eq("oid5"))).thenReturn(asList("childOid3"));
        HenkilohakuCriteriaDto henkilohakuCriteriaDto = new HenkilohakuCriteriaDto();
        henkilohakuCriteriaDto.setOrganisaatioOids(Stream.of("oid1", "oid3", "childOid1").collect(toSet()));
        henkilohakuCriteriaDto.setSubOrganisation(true);

        henkiloServiceImpl.henkilohaku(henkilohakuCriteriaDto, null, null);

        ArgumentCaptor<HenkiloCriteria> henkiloCriteriaCaptor = ArgumentCaptor.forClass(HenkiloCriteria.class);
        verify(henkiloHibernateRepositoryMock).findByCriteria(henkiloCriteriaCaptor.capture(), any(), any(), any());
        HenkiloCriteria henkiloCriteria = henkiloCriteriaCaptor.getValue();
        assertThat(henkiloCriteria.getOrganisaatioOids()).containsExactlyInAnyOrder("oid1", "oid3", "childOid1", "childOid2");
    }

    @Test
    public void henkilohakuHakeeRootVirkailijanOrganisaatioilla() {
        when(organisaatioHenkiloRepositoryMock.findUsersOrganisaatioHenkilosByPalveluRoolis(any(), any())).thenReturn(asList("rootOid", "oid1"));
        when(commonPropertiesMock.getRootOrganizationOid()).thenReturn("rootOid");
        HenkilohakuCriteriaDto henkilohakuCriteriaDto = new HenkilohakuCriteriaDto();
        henkilohakuCriteriaDto.setOrganisaatioOids(null);
        henkilohakuCriteriaDto.setSubOrganisation(false);

        henkiloServiceImpl.henkilohaku(henkilohakuCriteriaDto, null, null);

        ArgumentCaptor<HenkiloCriteria> henkiloCriteriaCaptor = ArgumentCaptor.forClass(HenkiloCriteria.class);
        verify(henkiloHibernateRepositoryMock).findByCriteria(henkiloCriteriaCaptor.capture(), any(), any(), any());
        HenkiloCriteria henkiloCriteria = henkiloCriteriaCaptor.getValue();
        assertThat(henkiloCriteria.getOrganisaatioOids()).containsExactlyInAnyOrder("rootOid", "oid1");
    }

    @Test
    public void henkilohakuHakeeRootVirkailijanAliorganisaatioilla() {
        when(organisaatioHenkiloRepositoryMock.findUsersOrganisaatioHenkilosByPalveluRoolis(any(),any())).thenReturn(asList("rootOid"));
        when(this.permissionCheckerServiceMock.isCurrentUserMiniAdmin()).thenReturn(true);
        when(commonPropertiesMock.getRootOrganizationOid()).thenReturn("rootOid");
        HenkilohakuCriteriaDto henkilohakuCriteriaDto = new HenkilohakuCriteriaDto();
        henkilohakuCriteriaDto.setOrganisaatioOids(null);
        henkilohakuCriteriaDto.setSubOrganisation(true);

        henkiloServiceImpl.henkilohaku(henkilohakuCriteriaDto, null, null);

        ArgumentCaptor<HenkiloCriteria> henkiloCriteriaCaptor = ArgumentCaptor.forClass(HenkiloCriteria.class);
        verify(henkiloHibernateRepositoryMock).findByCriteria(henkiloCriteriaCaptor.capture(), any(), any(), any());
        HenkiloCriteria henkiloCriteria = henkiloCriteriaCaptor.getValue();
        assertThat(henkiloCriteria.getOrganisaatioOids()).isNull();
    }

    @Test
    public void henkilohakuHakeeRootVirkailijanAntamallaOrganisaatiolla1() {
        when(organisaatioHenkiloRepositoryMock.findUsersOrganisaatioHenkilosByPalveluRoolis(any(), any())).thenReturn(asList("rootOid"));
        when(this.permissionCheckerServiceMock.isCurrentUserMiniAdmin()).thenReturn(true);
        when(commonPropertiesMock.getRootOrganizationOid()).thenReturn("rootOid");
        HenkilohakuCriteriaDto henkilohakuCriteriaDto = new HenkilohakuCriteriaDto();
        henkilohakuCriteriaDto.setOrganisaatioOids(Stream.of("oid1").collect(toSet()));
        henkilohakuCriteriaDto.setSubOrganisation(false);

        henkiloServiceImpl.henkilohaku(henkilohakuCriteriaDto, null, null);

        ArgumentCaptor<HenkiloCriteria> henkiloCriteriaCaptor = ArgumentCaptor.forClass(HenkiloCriteria.class);
        verify(henkiloHibernateRepositoryMock).findByCriteria(henkiloCriteriaCaptor.capture(), any(), any(), any());
        HenkiloCriteria henkiloCriteria = henkiloCriteriaCaptor.getValue();
        assertThat(henkiloCriteria.getOrganisaatioOids()).containsExactly("oid1");
    }

    @Test
    public void henkilohakuHakeeRootVirkailijanAntamallaOrganisaatiolla2() {
        when(organisaatioHenkiloRepositoryMock.findUsersOrganisaatioHenkilosByPalveluRoolis(any(), any())).thenReturn(asList("rootOid"));
        when(this.permissionCheckerServiceMock.isCurrentUserMiniAdmin()).thenReturn(true);
        when(commonPropertiesMock.getRootOrganizationOid()).thenReturn("rootOid");
        when(organisaatioClientMock.getChildOids(any())).thenReturn(asList("childOid1", "childOid2"));
        HenkilohakuCriteriaDto henkilohakuCriteriaDto = new HenkilohakuCriteriaDto();
        henkilohakuCriteriaDto.setOrganisaatioOids(Stream.of("oid1").collect(toSet()));
        henkilohakuCriteriaDto.setSubOrganisation(true);

        henkiloServiceImpl.henkilohaku(henkilohakuCriteriaDto, null, null);

        ArgumentCaptor<HenkiloCriteria> henkiloCriteriaCaptor = ArgumentCaptor.forClass(HenkiloCriteria.class);
        verify(henkiloHibernateRepositoryMock).findByCriteria(henkiloCriteriaCaptor.capture(), any(), any(), any());
        HenkiloCriteria henkiloCriteria = henkiloCriteriaCaptor.getValue();
        assertThat(henkiloCriteria.getOrganisaatioOids()).containsExactlyInAnyOrder("oid1", "childOid1", "childOid2");
    }

    @Test
    public void henkilohakuHakeeHenkilotJoillaOnOrganisaatioKunRootVirkailija() {
        when(permissionCheckerServiceMock.isCurrentUserAdmin()).thenReturn(false);
        when(this.permissionCheckerServiceMock.isCurrentUserMiniAdmin()).thenReturn(true);
        HenkilohakuCriteriaDto henkilohakuCriteriaDto = new HenkilohakuCriteriaDto();
        henkilohakuCriteriaDto.setNoOrganisation(true);

        henkiloServiceImpl.henkilohaku(henkilohakuCriteriaDto, null, null);

        ArgumentCaptor<HenkiloCriteria> henkiloCriteriaCaptor = ArgumentCaptor.forClass(HenkiloCriteria.class);
        verify(henkiloHibernateRepositoryMock).findByCriteria(henkiloCriteriaCaptor.capture(), any(), any(), any());
        HenkiloCriteria henkiloCriteria = henkiloCriteriaCaptor.getValue();
        assertThat(henkiloCriteria.getNoOrganisation()).isNull();
    }

    @Test
    public void henkilohakuHakeeHenkilotJoillaEiOrganisaatiotaKunRekisterinpitaja() {
        when(permissionCheckerServiceMock.isCurrentUserAdmin()).thenReturn(true);
        HenkilohakuCriteriaDto henkilohakuCriteriaDto = new HenkilohakuCriteriaDto();
        henkilohakuCriteriaDto.setNoOrganisation(true);

        henkiloServiceImpl.henkilohaku(henkilohakuCriteriaDto, null, null);

        ArgumentCaptor<HenkiloCriteria> henkiloCriteriaCaptor = ArgumentCaptor.forClass(HenkiloCriteria.class);
        verify(henkiloHibernateRepositoryMock).findByCriteria(henkiloCriteriaCaptor.capture(), any(), any(), any());
        HenkiloCriteria henkiloCriteria = henkiloCriteriaCaptor.getValue();
        assertThat(henkiloCriteria.getNoOrganisation()).isTrue();
    }

}
