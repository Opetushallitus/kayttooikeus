package fi.vm.sade.kayttooikeus.util;

import com.google.common.collect.Sets;
import fi.vm.sade.kayttooikeus.dto.*;
import fi.vm.sade.kayttooikeus.dto.types.AnomusTyyppi;
import fi.vm.sade.kayttooikeus.model.*;
import fi.vm.sade.kayttooikeus.service.external.OrganisaatioPerustieto;
import fi.vm.sade.oppijanumerorekisteri.dto.YhteystiedotRyhmaDto;
import fi.vm.sade.oppijanumerorekisteri.dto.YhteystietoDto;
import fi.vm.sade.oppijanumerorekisteri.dto.YhteystietoTyyppi;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;

import static com.google.common.collect.Lists.newArrayList;

public class CreateUtil {
    public static HaettuKayttoOikeusRyhma createHaettuKayttooikeusryhma(String email, String korName, String organisaatioOid) {
        Anomus anomus = Anomus.builder()
                .sahkopostiosoite(email)
                .organisaatioOid(organisaatioOid)
                .anomusTyyppi(AnomusTyyppi.UUSI)
                .build();
        KayttoOikeusRyhma kayttoOikeusRyhma = KayttoOikeusRyhma.builder()
                .name(korName)
                .hidden(false)
                .build();
        return new HaettuKayttoOikeusRyhma(anomus, kayttoOikeusRyhma, LocalDateTime.now(), KayttoOikeudenTila.ANOTTU);
    }

    public static UpdateHaettuKayttooikeusryhmaDto createUpdateHaettuKayttooikeusryhmaDto(Long id, String tila, LocalDate loppupvm) {
        return new UpdateHaettuKayttooikeusryhmaDto(id, tila, LocalDate.now(), loppupvm);
    }

    public static GrantKayttooikeusryhmaDto createGrantKayttooikeusryhmaDto(Long id, LocalDate loppupvm) {
        return new GrantKayttooikeusryhmaDto(id, LocalDate.now(), loppupvm);
    }

    public static HaettuKayttooikeusryhmaDto createHaettuKattyooikeusryhmaDto(Long haettuRyhmaId, String organisaatioOid,
                                                                               KayttoOikeudenTila tila) {
        KayttoOikeusRyhmaDto kayttoOikeusRyhmaDto = new KayttoOikeusRyhmaDto(1001L, "Kayttooikeusryhma x",
                "10", newArrayList(), new TextGroupDto(2001L));
        return new HaettuKayttooikeusryhmaDto(haettuRyhmaId, createAnomusDto(organisaatioOid), kayttoOikeusRyhmaDto, LocalDateTime.now(), tila);
    }

    public static AnomusDto createAnomusDto(String organisaatioOid) {
        return new AnomusDto(organisaatioOid, LocalDateTime.now().minusDays(1), new Date(), AnomusTyyppi.UUSI, HenkiloNimiDto.builder().oid("oid1").build());
    }

    public static KayttoOikeusRyhma createKayttoOikeusRyhma(Long id) {
        KayttoOikeusRyhma kayttoOikeusRyhma = new KayttoOikeusRyhma("Kayttooikeusryhma x", Collections.<KayttoOikeus>emptySet(),
                new TextGroup(), Collections.singleton(createOrganisaatioViite()), false, "10");
        kayttoOikeusRyhma.setId(id);
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

    public static MyonnettyKayttoOikeusRyhmaTapahtuma createMyonnettyKayttoOikeusRyhmaTapahtuma(Long id, Long kayttooikeusryhmaId) {
        MyonnettyKayttoOikeusRyhmaTapahtuma myonnettyKayttoOikeusRyhmaTapahtuma = new MyonnettyKayttoOikeusRyhmaTapahtuma();
        myonnettyKayttoOikeusRyhmaTapahtuma.setId(id);
        myonnettyKayttoOikeusRyhmaTapahtuma.setKayttoOikeusRyhma(createKayttoOikeusRyhma(kayttooikeusryhmaId));
        return myonnettyKayttoOikeusRyhmaTapahtuma;
    }

    public static HaettuKayttoOikeusRyhma createHaettuKayttoOikeusRyhma(String anojaOid, String kasittelijaOid,
                                                                         String organisaatioOid, String tehtavanimike,
                                                                         String perustelut, Long kayttooikeusryhmaId) {
        Anomus anomus = createAnomus(anojaOid, kasittelijaOid, organisaatioOid, tehtavanimike, perustelut);
        HaettuKayttoOikeusRyhma haettuKayttoOikeusRyhma = new HaettuKayttoOikeusRyhma(anomus, createKayttoOikeusRyhma(kayttooikeusryhmaId),
                LocalDateTime.now().minusDays(5), KayttoOikeudenTila.ANOTTU);
        haettuKayttoOikeusRyhma.setAnomus(anomus);
        anomus.setHaettuKayttoOikeusRyhmas(Sets.newHashSet(haettuKayttoOikeusRyhma));
        return haettuKayttoOikeusRyhma;
    }

    public static Henkilo createHenkiloWithOrganisaatio(String oidHenkilo, String organisaatioOid, boolean passivoitu) {
        Henkilo henkilo = createHenkilo(oidHenkilo);
        OrganisaatioHenkilo organisaatioHenkilo = new OrganisaatioHenkilo();
        organisaatioHenkilo.setHenkilo(henkilo);
        organisaatioHenkilo.setOrganisaatioOid(organisaatioOid);
        organisaatioHenkilo.setPassivoitu(passivoitu);
        henkilo.setOrganisaatioHenkilos(Sets.newHashSet(organisaatioHenkilo));
        return henkilo;
    }

    public static Anomus createAnomus(String anojaOid, String kasittelijaOid, String organisaatioOid, String tehtavanimike,
                                       String perustelut) {
        return new Anomus(createHenkilo(anojaOid), createHenkilo(kasittelijaOid), organisaatioOid, null, tehtavanimike,
                AnomusTyyppi.UUSI, AnomuksenTila.ANOTTU, LocalDateTime.now().minusDays(5), LocalDateTime.now().minusDays(5),
                perustelut, "", "", "", "",
                Sets.newHashSet(), Sets.newHashSet());
    }

    public static OrganisaatioPerustieto createOrganisaatioPerustietoNoChildren(String organisaatioOid) {
        OrganisaatioPerustieto organisaatioPerustieto = new OrganisaatioPerustieto();
        organisaatioPerustieto.setOid(organisaatioOid);
        return organisaatioPerustieto;
    }

    public static OrganisaatioPerustieto creaetOrganisaatioPerustietoWithNimi(String organisaatioOid, String nimi) {
        OrganisaatioPerustieto organisaatio = createOrganisaatioPerustietoNoChildren(organisaatioOid);
        organisaatio.setNimi(new HashMap<String, String>(){{put("fi", nimi);}});
        return organisaatio;
    }

    public static OrganisaatioPerustieto createOrganisaatioPerustietoWithChild(String organisaatioOid, String childOid,
                                                                               String childOppilaitostyyppi) {
        OrganisaatioPerustieto organisaatioPerustieto = createOrganisaatioPerustietoNoChildren(organisaatioOid);
        OrganisaatioPerustieto child = createOrganisaatioPerustietoNoChildren(childOid);
        child.setOppilaitostyyppi(childOppilaitostyyppi);
        organisaatioPerustieto.setChildren(newArrayList(child));
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
