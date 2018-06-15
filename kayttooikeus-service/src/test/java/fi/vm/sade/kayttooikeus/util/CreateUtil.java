package fi.vm.sade.kayttooikeus.util;

import com.google.common.collect.Sets;
import fi.vm.sade.kayttooikeus.dto.*;
import fi.vm.sade.kayttooikeus.dto.types.AnomusTyyppi;
import fi.vm.sade.kayttooikeus.model.*;
import fi.vm.sade.kayttooikeus.service.external.OrganisaatioPerustieto;
import fi.vm.sade.oppijanumerorekisteri.dto.YhteystiedotRyhmaDto;
import fi.vm.sade.oppijanumerorekisteri.dto.YhteystietoDto;
import fi.vm.sade.oppijanumerorekisteri.dto.YhteystietoTyyppi;
import fi.vm.sade.kayttooikeus.dto.enumeration.OrganisaatioStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;

import static com.google.common.collect.Lists.newArrayList;

public class CreateUtil {
    public static HaettuKayttoOikeusRyhma createHaettuKayttooikeusryhma(String email, String tunniste, String organisaatioOid) {
        Anomus anomus = Anomus.builder()
                .sahkopostiosoite(email)
                .organisaatioOid(organisaatioOid)
                .anomusTyyppi(AnomusTyyppi.UUSI)
                .build();
        KayttoOikeusRyhma kayttoOikeusRyhma = KayttoOikeusRyhma.builder()
                .tunniste(tunniste)
                .passivoitu(false)
                .build();
        return new HaettuKayttoOikeusRyhma(anomus, kayttoOikeusRyhma, LocalDateTime.now(), KayttoOikeudenTila.ANOTTU);
    }

    public static UpdateHaettuKayttooikeusryhmaDto createUpdateHaettuKayttooikeusryhmaDto(Long id, String tila, LocalDate loppupvm, String hylkaysperuste) {
        return new UpdateHaettuKayttooikeusryhmaDto(id, tila, LocalDate.now(), loppupvm, hylkaysperuste);
    }

    public static UpdateHaettuKayttooikeusryhmaDto createUpdateHaettuKayttooikeusryhmaDto(Long id, String tila, LocalDate loppupvm) {
        return CreateUtil.createUpdateHaettuKayttooikeusryhmaDto(id, tila, loppupvm, "hylkaysperuste");
    }

    public static GrantKayttooikeusryhmaDto createGrantKayttooikeusryhmaDto(Long id, LocalDate loppupvm) {
        return new GrantKayttooikeusryhmaDto(id, LocalDate.now(), loppupvm);
    }

    public static HaettuKayttooikeusryhmaDto createHaettuKattyooikeusryhmaDto(Long haettuRyhmaId, String organisaatioOid,
                                                                               KayttoOikeudenTila tila) {
        KayttoOikeusRyhmaDto kayttoOikeusRyhmaDto = new KayttoOikeusRyhmaDto(1001L, "Kayttooikeusryhma x",
                "10", newArrayList(), new TextGroupDto(2001L), new TextGroupDto(2002L), false, false);
        return new HaettuKayttooikeusryhmaDto(haettuRyhmaId, createAnomusDto(organisaatioOid), kayttoOikeusRyhmaDto, LocalDateTime.now(), tila);
    }

    public static AnomusDto createAnomusDto(String organisaatioOid) {
        return new AnomusDto(organisaatioOid, LocalDateTime.now().minusDays(1), new Date(), AnomusTyyppi.UUSI, HenkiloNimiDto.builder().oid("oid1").build(), "Perusteluteksti");
    }

    public static KayttoOikeusRyhma createKayttooikeusryhma(Long id) {
        KayttoOikeusRyhma kayttoOikeusRyhma = new KayttoOikeusRyhma("Kayttooikeusryhma x", Collections.<KayttoOikeus>emptySet(),
                new TextGroup(), new TextGroup(), Sets.newHashSet(), false, "10", false);
        kayttoOikeusRyhma.setId(id);
        return kayttoOikeusRyhma;
    }

    public static KayttoOikeusRyhma createKayttoOikeusRyhmaWithViite(Long id) {
        KayttoOikeusRyhma kayttoOikeusRyhma = createKayttooikeusryhma(id);
        kayttoOikeusRyhma.setOrganisaatioViite(Sets.newHashSet(createOrganisaatioViite()));
        return kayttoOikeusRyhma;

    }

    public static OrganisaatioViite createOrganisaatioViite() {
        return new OrganisaatioViite();
    }

    public static Henkilo createHenkilo(String oidHenkilo) {
        Henkilo henkilo = new Henkilo();
        henkilo.setOidHenkilo(oidHenkilo);
        return henkilo;
    }

    public static KayttoOikeusRyhmaTapahtumaHistoria createKayttooikeusryhmaTapahtumaHistoria(Long kayttooikeusryhmaId, String organisaatioOid, KayttoOikeudenTila tila) {
        return new KayttoOikeusRyhmaTapahtumaHistoria(createKayttoOikeusRyhmaWithViite(kayttooikeusryhmaId),
                createOrganisaatioHenkilo(organisaatioOid, false), "syy", tila, null, LocalDateTime.now());
    }

    public static MyonnettyKayttoOikeusRyhmaTapahtuma createMyonnettyKayttoOikeusRyhmaTapahtumaWithOrganisation(
            Long id, Long kayttooikeusryhmaId, String organisaatioOid) {
        MyonnettyKayttoOikeusRyhmaTapahtuma myonnettyKayttoOikeusRyhmaTapahtuma = createMyonnettyKayttoOikeusRyhmaTapahtuma(id, kayttooikeusryhmaId);
        myonnettyKayttoOikeusRyhmaTapahtuma.setOrganisaatioHenkilo(createOrganisaatioHenkilo(organisaatioOid, false));
        return myonnettyKayttoOikeusRyhmaTapahtuma;
    }

    public static MyonnettyKayttoOikeusRyhmaTapahtuma createMyonnettyKayttoOikeusRyhmaTapahtuma(Long id, Long kayttooikeusryhmaId) {
        MyonnettyKayttoOikeusRyhmaTapahtuma myonnettyKayttoOikeusRyhmaTapahtuma = new MyonnettyKayttoOikeusRyhmaTapahtuma();
        myonnettyKayttoOikeusRyhmaTapahtuma.setId(id);
        myonnettyKayttoOikeusRyhmaTapahtuma.setKayttoOikeusRyhma(createKayttoOikeusRyhmaWithViite(kayttooikeusryhmaId));
        return myonnettyKayttoOikeusRyhmaTapahtuma;
    }

    public static HaettuKayttoOikeusRyhma createHaettuKayttoOikeusRyhma(String anojaOid, String kasittelijaOid,
                                                                         String organisaatioOid, String tehtavanimike,
                                                                         String perustelut, Long kayttooikeusryhmaId) {
        Anomus anomus = createAnomus(anojaOid, kasittelijaOid, organisaatioOid, tehtavanimike, perustelut);
        HaettuKayttoOikeusRyhma haettuKayttoOikeusRyhma = new HaettuKayttoOikeusRyhma(anomus, createKayttoOikeusRyhmaWithViite(kayttooikeusryhmaId),
                LocalDateTime.now().minusDays(5), KayttoOikeudenTila.ANOTTU);
        haettuKayttoOikeusRyhma.setAnomus(anomus);
        anomus.setHaettuKayttoOikeusRyhmas(Sets.newHashSet(haettuKayttoOikeusRyhma));
        return haettuKayttoOikeusRyhma;
    }

    public static Henkilo createHenkiloWithOrganisaatio(String oidHenkilo, String organisaatioOid, boolean passivoitu) {
        Henkilo henkilo = createHenkilo(oidHenkilo);
        OrganisaatioHenkilo organisaatioHenkilo = createOrganisaatioHenkilo(organisaatioOid, passivoitu);
        organisaatioHenkilo.setHenkilo(henkilo);
        henkilo.setOrganisaatioHenkilos(Sets.newHashSet(organisaatioHenkilo));
        return henkilo;
    }

    public static OrganisaatioHenkilo createOrganisaatioHenkilo(String organisaatioOid, boolean passivoitu) {
        OrganisaatioHenkilo organisaatioHenkilo = new OrganisaatioHenkilo();
        organisaatioHenkilo.setOrganisaatioOid(organisaatioOid);
        organisaatioHenkilo.setPassivoitu(passivoitu);
        organisaatioHenkilo.setMyonnettyKayttoOikeusRyhmas(new HashSet<>());
        return organisaatioHenkilo;
    }

    public static Anomus createAnomusWithHaettuKayttooikeusryhma(String anojaOid, String kasittelijaOid,
                                                                 String organisaatioOid, String tehtavanimike,
                                                                 String perustelut, Long kayttooikeusryhmaId) {
        Anomus anomus = createAnomus(anojaOid, kasittelijaOid, organisaatioOid, tehtavanimike, perustelut);
        anomus.setHaettuKayttoOikeusRyhmas(Sets.newHashSet(createHaettuKayttoOikeusRyhma(anojaOid, kasittelijaOid, organisaatioOid, tehtavanimike,
                perustelut, kayttooikeusryhmaId)));
        return anomus;
    }

    public static Anomus createAnomus(String anojaOid, String kasittelijaOid, String organisaatioOid, String tehtavanimike,
                                       String perustelut) {
        return new Anomus(createHenkilo(anojaOid), createHenkilo(kasittelijaOid), organisaatioOid, tehtavanimike,
                AnomusTyyppi.UUSI, AnomuksenTila.ANOTTU, LocalDateTime.now().minusDays(5), LocalDateTime.now().minusDays(5),
                perustelut, "", "", "", "",
                Sets.newHashSet(), Sets.newHashSet());
    }

    public static OrganisaatioPerustieto createOrganisaatioPerustietoNoChildren(String organisaatioOid) {
        OrganisaatioPerustieto organisaatioPerustieto = new OrganisaatioPerustieto();
        organisaatioPerustieto.setOid(organisaatioOid);
        organisaatioPerustieto.setStatus(OrganisaatioStatus.AKTIIVINEN);
        return organisaatioPerustieto;
    }

    public static OrganisaatioPerustieto creaetOrganisaatioPerustietoWithNimi(String organisaatioOid, String nimi) {
        OrganisaatioPerustieto organisaatio = createOrganisaatioPerustietoNoChildren(organisaatioOid);
        organisaatio.setNimi(new HashMap<String, String>(){{put("fi", nimi);}});
        organisaatio.setStatus(OrganisaatioStatus.AKTIIVINEN);
        return organisaatio;
    }

    public static OrganisaatioPerustieto createOrganisaatioPerustietoWithChild(String organisaatioOid, String childOid,
                                                                               String childOppilaitostyyppi) {
        OrganisaatioPerustieto organisaatioPerustieto = createOrganisaatioPerustietoNoChildren(organisaatioOid);
        OrganisaatioPerustieto child = createOrganisaatioPerustietoNoChildren(childOid);
        child.setOppilaitostyyppi(childOppilaitostyyppi);
        organisaatioPerustieto.setChildren(newArrayList(child));
        child.setStatus(OrganisaatioStatus.AKTIIVINEN);
        return organisaatioPerustieto;
    }

    public static YhteystiedotRyhmaDto createYhteystietoSahkoposti(String email, String tyyppi) {
        return YhteystiedotRyhmaDto.builder()
                .yhteystieto(YhteystietoDto.builder()
                        .yhteystietoArvo(email)
                        .yhteystietoTyyppi(YhteystietoTyyppi.YHTEYSTIETO_SAHKOPOSTI).build())
                .ryhmaKuvaus(tyyppi)
                .build();
    }

}
