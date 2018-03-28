package fi.vm.sade.kayttooikeus.service.impl;

import fi.vm.sade.kayttooikeus.config.OrikaBeanMapper;
import fi.vm.sade.kayttooikeus.dto.HenkilohakuCriteriaDto;
import fi.vm.sade.kayttooikeus.dto.KayttajaTyyppi;
import fi.vm.sade.kayttooikeus.dto.PalvelukayttajaCreateDto;
import fi.vm.sade.kayttooikeus.dto.PalvelukayttajaCriteriaDto;
import fi.vm.sade.kayttooikeus.dto.PalvelukayttajaReadDto;
import fi.vm.sade.kayttooikeus.enumeration.OrderByHenkilohaku;
import fi.vm.sade.kayttooikeus.model.Henkilo;
import fi.vm.sade.kayttooikeus.repositories.HenkiloDataRepository;
import fi.vm.sade.kayttooikeus.repositories.dto.HenkilohakuResultDto;
import fi.vm.sade.kayttooikeus.service.PalvelukayttajaService;
import fi.vm.sade.kayttooikeus.service.external.OppijanumerorekisteriClient;
import fi.vm.sade.oppijanumerorekisteri.dto.HenkiloCreateDto;

import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import fi.vm.sade.kayttooikeus.service.HenkilohakuBuilderService;
import static java.util.stream.Collectors.toList;

@Service
@Transactional
@RequiredArgsConstructor
public class PalvelukayttajaServiceImpl implements PalvelukayttajaService {

    private final OppijanumerorekisteriClient oppijanumerorekisteriClient;
    private final HenkiloDataRepository henkiloRepository;
    private final HenkilohakuBuilderService henkilohakuBuilderService;
    private final OrikaBeanMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public Iterable<PalvelukayttajaReadDto> list(PalvelukayttajaCriteriaDto palvelukayttajaCriteriaDto) {
        HenkilohakuCriteriaDto henkilohakuCriteriaDto = mapper.map(palvelukayttajaCriteriaDto, HenkilohakuCriteriaDto.class);
        henkilohakuCriteriaDto.setNameQuery(null);
        henkilohakuCriteriaDto.setSukunimi(palvelukayttajaCriteriaDto.getNameQuery());
        henkilohakuCriteriaDto.setKayttajatunnus(palvelukayttajaCriteriaDto.getNameQuery());
        henkilohakuCriteriaDto.setKayttajaTyyppi(KayttajaTyyppi.PALVELU);

        OrderByHenkilohaku orderBy = OrderByHenkilohaku.HENKILO_NIMI_ASC;
        Collection<HenkilohakuResultDto> palvelukayttajat = henkilohakuBuilderService.getBuilder(henkilohakuCriteriaDto)
                .exclusion()
                .search(0L, null, orderBy)
                .build();
        return palvelukayttajat.stream().map(henkilohakuResult -> {
            PalvelukayttajaReadDto palvelukayttaja = new PalvelukayttajaReadDto();
            palvelukayttaja.setOid(henkilohakuResult.getOidHenkilo());
            palvelukayttaja.setNimi(henkilohakuResult.getSukunimi());
            palvelukayttaja.setKayttajatunnus(henkilohakuResult.getKayttajatunnus());
            return palvelukayttaja;
        }).collect(toList());
    }

    @Override
    public PalvelukayttajaReadDto create(PalvelukayttajaCreateDto createDto) {
        HenkiloCreateDto henkiloCreateDto = new HenkiloCreateDto();
        henkiloCreateDto.setSukunimi(createDto.getNimi());
        // oppijanumerorekisteri pakottaa näiden tietojen syöttämisen
        henkiloCreateDto.setEtunimet("_");
        henkiloCreateDto.setKutsumanimi("_");

        String oid = oppijanumerorekisteriClient.createHenkilo(henkiloCreateDto);

        Henkilo henkilo = henkiloRepository.findByOidHenkilo(oid).orElseGet(Henkilo::new);
        henkilo.setOidHenkilo(oid);
        henkilo.setKayttajaTyyppi(KayttajaTyyppi.PALVELU);
        henkiloRepository.save(henkilo);

        PalvelukayttajaReadDto readDto = new PalvelukayttajaReadDto();
        readDto.setOid(oid);
        readDto.setNimi(createDto.getNimi()); // luotetaan että tämä tallentui
        return readDto;
    }

}
