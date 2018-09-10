package fi.vm.sade.kayttooikeus.service.impl;

import fi.vm.sade.kayttooikeus.dto.KayttajaTyyppi;
import fi.vm.sade.kayttooikeus.dto.VahvaTunnistusRequestDto;
import fi.vm.sade.kayttooikeus.dto.VahvaTunnistusResponseDto;
import fi.vm.sade.kayttooikeus.dto.YhteystietojenTyypit;
import fi.vm.sade.kayttooikeus.model.Henkilo;
import fi.vm.sade.kayttooikeus.model.TunnistusToken;
import fi.vm.sade.kayttooikeus.repositories.HenkiloDataRepository;
import fi.vm.sade.kayttooikeus.repositories.TunnistusTokenDataRepository;
import fi.vm.sade.kayttooikeus.service.IdentificationService;
import fi.vm.sade.kayttooikeus.service.KayttajatiedotService;
import fi.vm.sade.kayttooikeus.service.LdapSynchronizationService;
import fi.vm.sade.kayttooikeus.service.VahvaTunnistusService;
import fi.vm.sade.kayttooikeus.service.dto.HenkiloVahvaTunnistusDto;
import fi.vm.sade.kayttooikeus.service.exception.LoginTokenNotFoundException;
import fi.vm.sade.kayttooikeus.service.exception.NotFoundException;
import fi.vm.sade.kayttooikeus.service.external.OppijanumerorekisteriClient;
import fi.vm.sade.kayttooikeus.util.HenkiloUtils;
import fi.vm.sade.oppijanumerorekisteri.dto.HenkiloDto;
import fi.vm.sade.oppijanumerorekisteri.dto.YhteystietoTyyppi;
import fi.vm.sade.properties.OphProperties;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class VahvaTunnistusServiceImpl implements VahvaTunnistusService {

    private final IdentificationService identificationService;
    private final KayttajatiedotService kayttajatiedotService;

    private final HenkiloDataRepository henkiloDataRepository;

    private final OppijanumerorekisteriClient oppijanumerorekisteriClient;
    private final OphProperties ophProperties;

    @Override
    public VahvaTunnistusResponseDto tunnistaudu(String loginToken, VahvaTunnistusRequestDto lisatiedotDto) {
        TunnistusToken tunnistusToken = identificationService.getByValidLoginToken(loginToken);
        return tunnistaudu(tunnistusToken, lisatiedotDto);
    }

    private VahvaTunnistusResponseDto tunnistaudu(TunnistusToken tunnistusToken, VahvaTunnistusRequestDto lisatiedotDto) {
        Henkilo henkiloByLoginToken = tunnistusToken.getHenkilo();
        String henkiloOid = henkiloByLoginToken.getOidHenkilo();

        HenkiloVahvaTunnistusDto vahvaTunnistusDto = new HenkiloVahvaTunnistusDto(tunnistusToken.getHetu());
        Optional.ofNullable(lisatiedotDto.getTyosahkopostiosoite())
                .filter(StringUtils::hasLength)
                .ifPresent(vahvaTunnistusDto::setTyosahkopostiosoite);
        oppijanumerorekisteriClient.setStrongIdentifiedHetu(henkiloOid, vahvaTunnistusDto);

        Optional.ofNullable(lisatiedotDto.getSalasana())
                .filter(StringUtils::hasLength)
                .ifPresent(salasana -> kayttajatiedotService.changePasswordAsAdmin(henkiloOid, salasana, LdapSynchronizationService.LdapSynchronizationType.NOW));

        String authToken = identificationService.consumeLoginToken(tunnistusToken.getLoginToken());

        return VahvaTunnistusResponseDto.builder()
                .authToken(authToken)
                .service(ophProperties.url("virkailijan-tyopoyta"))
                .build();
    }

    @Override
    public String kasitteleKutsunTunnistus(String kutsuToken, String kielisyys, String hetu, String etunimet, String sukunimi) {
        Map<String, String> queryParams;
        try {
            // Dekoodataan etunimet ja sukunimi manuaalisesti, koska shibboleth välittää ASCII-enkoodatut request headerit UTF-8 -merkistössä
            Charset windows1252 = Charset.forName("Windows-1252");
            Charset utf8 = Charset.forName("UTF-8");
            etunimet = new String(etunimet.getBytes(windows1252), utf8);
            sukunimi = new String(sukunimi.getBytes(windows1252), utf8);

            String temporaryKutsuToken = this.identificationService
                    .updateKutsuAndGenerateTemporaryKutsuToken(kutsuToken, hetu, etunimet, sukunimi);
            queryParams = new HashMap<String, String>() {{
                put("temporaryKutsuToken", temporaryKutsuToken);
            }};
            return this.ophProperties.url("henkilo-ui.rekisteroidy", queryParams);
        } catch (NotFoundException e) {
            return this.ophProperties.url("henkilo-ui.vahvatunnistus.virhe", kielisyys, "vanhakutsu");
        }
    }

    @Override
    public String kirjaaVahvaTunnistus(String loginToken, String kielisyys, String hetu) {
        try {
            // otetaan hetu talteen jotta se on vielä tiedossa seuraavassa vaiheessa
            TunnistusToken tunnistusToken = identificationService.updateLoginToken(loginToken, hetu);
            HenkiloDto henkiloByLoginToken = oppijanumerorekisteriClient.getHenkiloByOid(tunnistusToken.getHenkilo().getOidHenkilo());
            if (KayttajaTyyppi.PALVELU.equals(tunnistusToken.getHenkilo().getKayttajaTyyppi())) {
                log.error("Palvelukäyttäjänä kirjautuminen on estetty");
                return this.ophProperties.url("henkilo-ui.vahvatunnistus.virhe", kielisyys, "palvelukayttaja");
            }

            // tarkistetaan että virkailijalla on tämä hetu käytössä
            if (StringUtils.hasLength(henkiloByLoginToken.getHetu()) && !henkiloByLoginToken.getHetu().equals(hetu)) {
                log.error(String.format("Vahvan tunnistuksen henkilötunnus %s on eri kuin virkailijan henkilötunnus %s", hetu, henkiloByLoginToken.getHetu()));
                return this.ophProperties.url("henkilo-ui.vahvatunnistus.virhe", kielisyys, "vaara");
            }

            return getRedirectUrl(loginToken, kielisyys, tunnistusToken.getSalasananVaihto(), henkiloByLoginToken);
        } catch (LoginTokenNotFoundException e) {
            return this.ophProperties.url("henkilo-ui.vahvatunnistus.virhe", kielisyys, "vanha");
        } catch (Exception e) {
            log.error("User failed strong identification", e);
            return this.ophProperties.url("henkilo-ui.vahvatunnistus.virhe", kielisyys, loginToken);
        }
    }

    private String getRedirectUrl(String loginToken, String kielisyys, Boolean salasananVaihto, HenkiloDto henkiloByLoginToken) {
        boolean sahkopostinAsetus = !HenkiloUtils
                .getYhteystieto(henkiloByLoginToken, YhteystietojenTyypit.TYOOSOITE, YhteystietoTyyppi.YHTEYSTIETO_SAHKOPOSTI)
                .isPresent();
        boolean salasananVaihtoBool = Boolean.TRUE.equals(salasananVaihto);
        // pyydetään käyttäjää täydentämään tietoja ("uudelleenrekisteröinti")
        if (sahkopostinAsetus || salasananVaihtoBool) {
            return ophProperties.url("henkilo-ui.uudelleenrekisterointi", kielisyys, loginToken, sahkopostinAsetus, salasananVaihtoBool);
        }
        // jos mitään tietoja ei tarvitse täyttää, suoritetaan tunnistautuminen ilman rekisteröintisivua
        VahvaTunnistusRequestDto vahvaTunnistusRequestDto = new VahvaTunnistusRequestDto();
        VahvaTunnistusResponseDto vahvaTunnistusResponseDto = this.tunnistaudu(loginToken, vahvaTunnistusRequestDto);
        return ophProperties.url("cas.login", vahvaTunnistusResponseDto.asMap());
    }


    @Override
    public String kirjaaKayttajaVahvallaTunnistuksella(String hetu, String kielisyys) {
        Optional<HenkiloDto> henkiloDto = this.oppijanumerorekisteriClient.getHenkiloByHetu(hetu);
        // Validointi
        if (!henkiloDto.isPresent()) {
            log.error(String.format("Henkilöä ei löytynyt hetulla %s", hetu));
            return this.ophProperties.url("henkilo-ui.vahvatunnistus.virhe", kielisyys, "eiloydy");
        }
        if (henkiloDto.get().isPassivoitu()) {
            return ophProperties.url("henkilo-ui.vahvatunnistus.virhe", kielisyys, "passivoitu");
        }
        Optional<Henkilo> henkilo = this.henkiloDataRepository.findByOidHenkilo(henkiloDto.get().getOidHenkilo());
        if (!henkilo.isPresent()) {
            log.error(String.format("Virkailijaa ei löytynyt oidilla %s", henkiloDto.get().getOidHenkilo()));
            return this.ophProperties.url("henkilo-ui.vahvatunnistus.virhe", kielisyys, "eivirkailija");
        }
        if (henkilo.get().getOrganisaatioHenkilos().size() == 0) {
            // Ei ole koskaan ollut virkailija
            log.error("Henkilö ei ole koskaan ollut virkailija.");
            return this.ophProperties.url("henkilo-ui.vahvatunnistus.virhe", kielisyys, "eivirkailija");
        }

        // ei haka-käyttäjille salasananvaihtoa = jos käyttäjällä on salasana on CAS-käyttäjä
        Boolean salasananVaihto = !Boolean.TRUE.equals(henkilo.get().getVahvastiTunnistettu())
                && StringUtils.hasLength(henkilo.get().getKayttajatiedot().getPassword());
        String loginToken = this.identificationService.createLoginToken(henkilo.get().getOidHenkilo(), salasananVaihto, hetu);
        return getRedirectUrl(loginToken, kielisyys, salasananVaihto, henkiloDto.get());
    }

}
