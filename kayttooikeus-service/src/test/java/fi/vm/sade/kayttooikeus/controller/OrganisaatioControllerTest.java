package fi.vm.sade.kayttooikeus.controller;

import fi.vm.sade.kayttooikeus.dto.OrganisaatioCriteriaDto;
import fi.vm.sade.kayttooikeus.dto.enumeration.OrganisaatioStatus;
import fi.vm.sade.kayttooikeus.dto.enumeration.OrganisaatioTyyppi;
import fi.vm.sade.kayttooikeus.service.OrganisaatioService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
public class OrganisaatioControllerTest extends AbstractControllerTest {

    @MockBean
    private OrganisaatioService organisaatioService;

    @Test
    public void getOrganisaatioRequiresAuthentication() throws Exception {
        this.mvc.perform(get("/organisaatio")).andExpect(status().isFound());
    }

    @Test
    @WithMockUser
    public void listByWorksWithoutParameters() throws Exception {
        this.mvc.perform(get("/organisaatio")).andExpect(status().isOk());

        ArgumentCaptor<OrganisaatioCriteriaDto> criteriaCaptor = ArgumentCaptor.forClass(OrganisaatioCriteriaDto.class);
        verify(organisaatioService).listBy(criteriaCaptor.capture());
        OrganisaatioCriteriaDto criteria = criteriaCaptor.getValue();

        assertThat(criteria).returns(null, OrganisaatioCriteriaDto::getTyyppi);
    }

    @Test
    @WithMockUser
    public void listByWorksWithTyyppiParameter() throws Exception {
        this.mvc.perform(get("/organisaatio?tyyppi=RYHMA")).andExpect(status().isOk());

        ArgumentCaptor<OrganisaatioCriteriaDto> criteriaCaptor = ArgumentCaptor.forClass(OrganisaatioCriteriaDto.class);
        verify(organisaatioService).listBy(criteriaCaptor.capture());
        OrganisaatioCriteriaDto criteria = criteriaCaptor.getValue();

        assertThat(criteria).returns(OrganisaatioTyyppi.RYHMA, OrganisaatioCriteriaDto::getTyyppi);
    }

    @Test
    @WithMockUser
    public void listByWorksWithTilaParameter() throws Exception {
        this.mvc.perform(get("/organisaatio?tila=AKTIIVINEN")).andExpect(status().isOk());

        ArgumentCaptor<OrganisaatioCriteriaDto> criteriaCaptor = ArgumentCaptor.forClass(OrganisaatioCriteriaDto.class);
        verify(organisaatioService).listBy(criteriaCaptor.capture());
        OrganisaatioCriteriaDto criteria = criteriaCaptor.getValue();

        assertThat(criteria).returns(singleton(OrganisaatioStatus.AKTIIVINEN), OrganisaatioCriteriaDto::getTila);
    }

    @Test
    @WithMockUser
    public void getRootWithChildrenByWorksWithoutParameters() throws Exception {
        this.mvc.perform(get("/organisaatio/root")).andExpect(status().isOk());

        verify(organisaatioService).getRootWithChildrenBy(any(OrganisaatioCriteriaDto.class));
    }

    @Test
    @WithMockUser
    public void getByOidWorks() throws Exception {
        this.mvc.perform(get("/organisaatio/oid123")).andExpect(status().isOk());

        verify(organisaatioService).getByOid(eq("oid123"));
    }

}
