package fi.vm.sade.kayttooikeus.service.impl;

import com.querydsl.core.Tuple;
import fi.vm.sade.kayttooikeus.config.OrikaBeanMapper;
import fi.vm.sade.kayttooikeus.dto.KayttoOikeusHistoriaDto;
import fi.vm.sade.kayttooikeus.dto.KayttoOikeusRyhmaDto;
import fi.vm.sade.kayttooikeus.dto.PalveluKayttoOikeusDto;
import fi.vm.sade.kayttooikeus.model.QMyonnettyKayttoOikeusRyhmaTapahtuma;
import fi.vm.sade.kayttooikeus.model.QOrganisaatioHenkilo;
import fi.vm.sade.kayttooikeus.repositories.KayttoOikeusRepository;
import fi.vm.sade.kayttooikeus.repositories.KayttoOikeusRyhmaRepository;
import fi.vm.sade.kayttooikeus.repositories.dto.ExpiringKayttoOikeusDto;
import fi.vm.sade.kayttooikeus.service.KayttoOikeusService;
import fi.vm.sade.kayttooikeus.service.LocalizationService;
import org.joda.time.LocalDate;
import org.joda.time.Period;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class KayttoOikeusServiceImpl extends AbstractService implements KayttoOikeusService {
    private KayttoOikeusRyhmaRepository kayttoOikeusRyhmaRepository;
    private KayttoOikeusRepository kayttoOikeusRepository;
    private LocalizationService localizationService;
    private OrikaBeanMapper mapper;

    @Autowired
    public KayttoOikeusServiceImpl(KayttoOikeusRyhmaRepository kayttoOikeusRyhmaRepository,
                                   KayttoOikeusRepository kayttoOikeusRepository,
                                   LocalizationService localizationService,
                                   OrikaBeanMapper mapper) {
        this.kayttoOikeusRyhmaRepository = kayttoOikeusRyhmaRepository;
        this.kayttoOikeusRepository = kayttoOikeusRepository;
        this.localizationService = localizationService;
        this.mapper = mapper;
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<KayttoOikeusRyhmaDto> listAllKayttoOikeusRyhmas() {
        return mapper.mapAsList(kayttoOikeusRyhmaRepository.listAll(), KayttoOikeusRyhmaDto.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PalveluKayttoOikeusDto> listKayttoOikeusByPalvelu(String palveluName) {
        return localizationService.localize(kayttoOikeusRepository.listKayttoOikeusByPalvelu(palveluName));
    }

    @Override
    @Transactional(readOnly = true)
    public List<KayttoOikeusHistoriaDto> listMyonnettyKayttoOikeusHistoriaForCurrentUser() {
        return localizationService.localize(kayttoOikeusRepository.listMyonnettyKayttoOikeusHistoriaForHenkilo(getCurrentUserOid()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExpiringKayttoOikeusDto> findToBeExpiringMyonnettyKayttoOikeus(LocalDate at, Period... expirationPeriods) {
        return localizationService.localize(kayttoOikeusRepository.findSoonToBeExpiredTapahtumas(at, expirationPeriods));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, List<Integer>> findKayttooikeusryhmatAndOrganisaatioByHenkiloOid(String henkiloOid)  {
        List<Tuple> results = this.kayttoOikeusRepository.findOrganisaatioOidAndRyhmaIdByHenkiloOid(henkiloOid);

        HashMap<String, List<Integer>> kayttooikeusRyhmasByOrganisation = new HashMap<>();
        QOrganisaatioHenkilo organisaatioHenkilo = QOrganisaatioHenkilo.organisaatioHenkilo;
        QMyonnettyKayttoOikeusRyhmaTapahtuma myonnettyKayttoOikeusRyhmaTapahtuma
                = QMyonnettyKayttoOikeusRyhmaTapahtuma.myonnettyKayttoOikeusRyhmaTapahtuma;
        for(Tuple result : results) {
            String organisaatioOid = result.get(organisaatioHenkilo.organisaatioOid);
            Integer ryhmaId = Optional.ofNullable(result.get(myonnettyKayttoOikeusRyhmaTapahtuma.kayttoOikeusRyhma.id))
                    .orElseThrow(() -> new NullPointerException("null_ryhma_id")).intValue();

            List<Integer> ryhmasInOrganisaatio = kayttooikeusRyhmasByOrganisation.get(organisaatioOid);
            if(ryhmasInOrganisaatio == null) {
                ryhmasInOrganisaatio = new ArrayList<>();
                kayttooikeusRyhmasByOrganisation.put(organisaatioOid, ryhmasInOrganisaatio);
            }
            ryhmasInOrganisaatio.add(ryhmaId);
        }
        return kayttooikeusRyhmasByOrganisation;
    }
}
