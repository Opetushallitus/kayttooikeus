package fi.vm.sade.kayttooikeus.service.impl;

import com.google.common.collect.Lists;
import fi.vm.sade.kayttooikeus.config.OrikaBeanMapper;
import fi.vm.sade.kayttooikeus.dto.AnomusKasiteltyRecipientDto;
import fi.vm.sade.kayttooikeus.dto.KayttoOikeudenTila;
import fi.vm.sade.kayttooikeus.dto.TextGroupMapDto;
import fi.vm.sade.kayttooikeus.dto.UpdateHaettuKayttooikeusryhmaDto;
import fi.vm.sade.kayttooikeus.model.Anomus;
import fi.vm.sade.kayttooikeus.model.Henkilo;
import fi.vm.sade.kayttooikeus.model.KayttoOikeusRyhma;
import fi.vm.sade.kayttooikeus.model.Kutsu;
import fi.vm.sade.kayttooikeus.repositories.KayttoOikeusRyhmaRepository;
import fi.vm.sade.kayttooikeus.repositories.dto.ExpiringKayttoOikeusDto;
import fi.vm.sade.kayttooikeus.service.EmailService;
import fi.vm.sade.kayttooikeus.service.exception.NotFoundException;
import fi.vm.sade.kayttooikeus.service.external.OppijanumerorekisteriClient;
import fi.vm.sade.kayttooikeus.service.external.OrganisaatioClient;
import fi.vm.sade.kayttooikeus.service.external.RyhmasahkopostiClient;
import fi.vm.sade.kayttooikeus.service.impl.email.SahkopostiHenkiloDto;
import fi.vm.sade.kayttooikeus.util.LocalisationUtils;
import fi.vm.sade.kayttooikeus.util.UserDetailsUtil;
import fi.vm.sade.kayttooikeus.util.YhteystietoUtil;
import fi.vm.sade.oppijanumerorekisteri.dto.HenkiloDto;
import fi.vm.sade.oppijanumerorekisteri.dto.HenkiloPerustietoDto;
import fi.vm.sade.properties.OphProperties;
import fi.vm.sade.ryhmasahkoposti.api.dto.EmailData;
import fi.vm.sade.ryhmasahkoposti.api.dto.EmailMessage;
import fi.vm.sade.ryhmasahkoposti.api.dto.EmailRecipient;
import fi.vm.sade.ryhmasahkoposti.api.dto.ReportedRecipientReplacementDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.text.DateFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Comparator.comparing;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.*;

@Service
public class EmailServiceImpl implements EmailService {
    public static final String DISCARDED_INVITATION_EMAIL_TEMPLATE = "kayttooikeus_kutsu_postoilmoitus";
    public static final String DISCARDED_APPLICATION_EMAIL_TEMPLATE = "kayttooikeus_anomus_postoilmoitus";
    private static final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);
    private static final String DEFAULT_LANGUAGE_CODE = "fi";
    private static final Locale DEFAULT_LOCALE = new Locale(DEFAULT_LANGUAGE_CODE);
    private static final String KAYTTOOIKEUSMUISTUTUS_EMAIL_TEMPLATE_NAME = "kayttooikeusmuistutus_email_v2";
    private static final String KAYTTOOIKEUSANOMUSILMOITUS_EMAIL_TEMPLATE_NAME = "kayttooikeushakemusilmoitus_email_v2";
    private static final String ANOMUS_KASITELTY_EMAIL_TEMPLATE_NAME = "kayttooikeusanomuskasitelty_email_v3";
    private static final String KUTSUTTU_EMAIL_TEMPLATE_NAME = "kayttooikeus_kutsu_v2";
    private static final String ANOMUS_KASITELTY_EMAIL_REPLACEMENT_VASTAANOTTAJA = "vastaanottaja";
    private static final String ANOMUS_KASITELTY_EMAIL_REPLACEMENT_ROOLI = "rooli";
    private static final String ANOMUS_KASITELTY_EMAIL_REPLACEMENT_LINKKI = "linkki";
    private static final String KAYTTOOIKEUSANOMUSILMOITUS_EMAIL_REPLACEMENT_LINKKI = "linkki";
    private static final String CALLING_PROCESS = "kayttooikeus";

    private final String expirationReminderPersonUrl;

    private final OppijanumerorekisteriClient oppijanumerorekisteriClient;
    private final RyhmasahkopostiClient ryhmasahkopostiClient;
    private final OrganisaatioClient organisaatioClient;

    private final KayttoOikeusRyhmaRepository kayttoOikeusRyhmaRepository;

    private final OrikaBeanMapper mapper;
    private final OphProperties urlProperties;

    @Autowired
    public EmailServiceImpl(OppijanumerorekisteriClient oppijanumerorekisteriClient,
                            RyhmasahkopostiClient ryhmasahkopostiClient,
                            OphProperties ophProperties,
                            KayttoOikeusRyhmaRepository kayttoOikeusRyhmaRepository,
                            OrikaBeanMapper mapper,
                            OphProperties urlProperties,
                            OrganisaatioClient organisaatioClient) {
        this.oppijanumerorekisteriClient = oppijanumerorekisteriClient;
        this.ryhmasahkopostiClient = ryhmasahkopostiClient;
        this.expirationReminderPersonUrl = ophProperties.url("henkilo-ui.omattiedot");
        this.kayttoOikeusRyhmaRepository = kayttoOikeusRyhmaRepository;
        this.mapper = mapper;
        this.urlProperties = urlProperties;
        this.organisaatioClient = organisaatioClient;
    }

    @Override
    public void sendEmailAnomusKasitelty(Anomus anomus, UpdateHaettuKayttooikeusryhmaDto updateHaettuKayttooikeusryhmaDto, Long kayttooikeusryhmaId) {
        // add all handled with accepted status to email
        this.sendEmailAnomusHandled(anomus, updateHaettuKayttooikeusryhmaDto, kayttooikeusryhmaId);
    }

    private void sendEmailAnomusHandled(Anomus anomus, UpdateHaettuKayttooikeusryhmaDto updateHaettuKayttooikeusryhmaDto, Long kayttooikeusryhmaId) {
        /* If this fails, the whole requisition handling process MUST NOT fail!!
         * Having an email sent from handled requisitions is a nice-to-have feature
         * but it's not a reason to cancel the whole transaction
         */

        try {
            HenkiloDto henkiloDto = this.oppijanumerorekisteriClient.getHenkiloByOid(anomus.getHenkilo().getOidHenkilo());
            EmailRecipient recipient = new EmailRecipient();
            recipient.setOid(henkiloDto.getOidHenkilo());
            recipient.setOidType("henkilo");
            recipient.setName(UserDetailsUtil.getName(henkiloDto));
            recipient.setLanguageCode(UserDetailsUtil.getLanguageCode(henkiloDto, "fi", "sv"));
            recipient.setEmail(anomus.getSahkopostiosoite());
            String languageCode = recipient.getLanguageCode();
            List<ReportedRecipientReplacementDTO> replacements = new ArrayList<>();
            replacements.add(new ReportedRecipientReplacementDTO(ANOMUS_KASITELTY_EMAIL_REPLACEMENT_VASTAANOTTAJA, mapper.map(henkiloDto, SahkopostiHenkiloDto.class)));
            AnomusKasiteltyRecipientDto kasiteltyAnomus = createAnomusKasiteltyDto(anomus, updateHaettuKayttooikeusryhmaDto, languageCode, kayttooikeusryhmaId);
            replacements.add(new ReportedRecipientReplacementDTO(ANOMUS_KASITELTY_EMAIL_REPLACEMENT_ROOLI, kasiteltyAnomus));
            replacements.add(new ReportedRecipientReplacementDTO(ANOMUS_KASITELTY_EMAIL_REPLACEMENT_LINKKI, urlProperties.url("cas.login")));
            recipient.setRecipientReplacements(replacements);
            EmailMessage message = this.generateEmailMessage(ANOMUS_KASITELTY_EMAIL_TEMPLATE_NAME, languageCode);
            this.ryhmasahkopostiClient.sendRyhmasahkoposti(new EmailData(Lists.newArrayList(recipient), message));
        } catch (Exception e) {
            logger.error("Error sending requisition handled email", e);
        }
    }

    private AnomusKasiteltyRecipientDto createAnomusKasiteltyDto(Anomus anomus, UpdateHaettuKayttooikeusryhmaDto updateHaettuKayttooikeusDto, String languageCode, Long kayttooikeusryhmaId) {
        KayttoOikeusRyhma kayttooikeusryhma = this.kayttoOikeusRyhmaRepository.findById(kayttooikeusryhmaId).orElseThrow(() -> new NotFoundException("Käyttöoikeusryhmää ei löytynyt id:llä: " + kayttooikeusryhmaId));
        String kayttooikeusryhmaNimi = LocalisationUtils.getText(languageCode, kayttooikeusryhma.getNimi(), kayttooikeusryhma::getTunniste);
        if (KayttoOikeudenTila.valueOf(updateHaettuKayttooikeusDto.getKayttoOikeudenTila()) == KayttoOikeudenTila.MYONNETTY) {
            return new AnomusKasiteltyRecipientDto(kayttooikeusryhmaNimi, KayttoOikeudenTila.MYONNETTY);
        }
        return new AnomusKasiteltyRecipientDto(kayttooikeusryhmaNimi, KayttoOikeudenTila.HYLATTY, updateHaettuKayttooikeusDto.getHylkaysperuste());
    }


    private EmailMessage generateEmailMessage(String templateName, String languageCode) {
        EmailMessage message = new EmailMessage();
        message.setCallingProcess(CALLING_PROCESS);
        message.setTemplateName(templateName);
        message.setHtml(true);
        message.setLanguageCode(languageCode);
        return message;
    }

    @Override
    @Transactional
    public void sendExpirationReminder(String henkiloOid, List<ExpiringKayttoOikeusDto> tapahtumas) {
        // Not grouped by language code since might change to one TX / receiver in the future.

        HenkiloPerustietoDto henkilonPerustiedot = oppijanumerorekisteriClient.getHenkilonPerustiedot(henkiloOid)
                .orElseThrow(() -> new NotFoundException("Henkilö not found by henkiloOid=" + henkiloOid));
        String languageCode = UserDetailsUtil.getLanguageCode(henkilonPerustiedot, "fi", "sv");

        EmailData email = new EmailData();
        email.setEmail(this.generateEmailMessage(KAYTTOOIKEUSMUISTUTUS_EMAIL_TEMPLATE_NAME, languageCode));

        getEmailRecipient(henkiloOid, emailAddress -> henkilo -> getEmailRecipient(henkiloOid, languageCode, tapahtumas, emailAddress, henkilo)).ifPresent(recipient -> {
            email.setRecipient(singletonList(recipient));
            ryhmasahkopostiClient.sendRyhmasahkoposti(email);
        });
    }

    private Optional<EmailRecipient> getEmailRecipient(String henkiloOid, Function<String, Function<HenkiloDto, EmailRecipient>> emailAddressMapper) {
        HenkiloDto henkilo = oppijanumerorekisteriClient.getHenkiloByOid(henkiloOid);
        Optional<String> emailAddress = YhteystietoUtil.getWorkEmail(henkilo.getYhteystiedotRyhma());
        return emailAddress
                .map(emailAddressMapper)
                .map(recipientMapper -> recipientMapper.apply(henkilo));
    }


    private EmailRecipient getEmailRecipient(String henkiloOid, String langugeCode, List<ExpiringKayttoOikeusDto> kayttoOikeudet, String email, HenkiloDto henkilo) {
        List<ReportedRecipientReplacementDTO> replacements = new ArrayList<>();
        replacements.add(new ReportedRecipientReplacementDTO("vastaanottaja", mapper.map(henkilo, SahkopostiHenkiloDto.class)));
        replacements.add(new ReportedRecipientReplacementDTO("kayttooikeusryhmat", getExpirationsText(kayttoOikeudet, langugeCode)));
        replacements.add(new ReportedRecipientReplacementDTO("linkki", expirationReminderPersonUrl));

        EmailRecipient recipient = new EmailRecipient(henkiloOid, email);
        recipient.setLanguageCode(langugeCode);
        recipient.setRecipientReplacements(replacements);

        return recipient;
    }

    private String getExpirationsText(List<ExpiringKayttoOikeusDto> kayttoOikeudet, String languageCode) {
        DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.SHORT, DEFAULT_LOCALE);
        return kayttoOikeudet.stream().map(kayttoOikeus -> {
            String kayttoOikeusRyhmaNimi = ofNullable(kayttoOikeus.getRyhmaDescription())
                    .flatMap(d -> d.getOrAny(languageCode)).orElse(kayttoOikeus.getRyhmaName());
            String voimassaLoppuPvmStr = dateFormat.format(Date.from(kayttoOikeus.getVoimassaLoppuPvm().atStartOfDay(ZoneId.systemDefault()).toInstant()));
            return String.format("%s (%s)", kayttoOikeusRyhmaNimi, voimassaLoppuPvmStr);
        }).collect(joining(", "));
    }

    @Override
    @Transactional
    public void sendNewRequisitionNotificationEmails(Set<String> henkiloOids) {
        henkiloOids.stream()
                .map(oid -> getEmailRecipient(oid, emailAddress -> henkilo -> createRecipient(henkilo, emailAddress)))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(groupingBy(EmailRecipient::getLanguageCode))
                .forEach(this::sendNewRequisitionNotificationEmail);
    }

    private EmailRecipient createRecipient(HenkiloDto henkilo, String sahkoposti) {
        String kieliKoodi = UserDetailsUtil.getLanguageCode(henkilo, "fi", "sv");
        EmailRecipient recipient = new EmailRecipient(henkilo.getOidHenkilo(), sahkoposti);
        recipient.setLanguageCode(kieliKoodi);
        List<ReportedRecipientReplacementDTO> replacements = new ArrayList<>();
        replacements.add(new ReportedRecipientReplacementDTO("vastaanottaja", mapper.map(henkilo, SahkopostiHenkiloDto.class)));
        replacements.add(new ReportedRecipientReplacementDTO(KAYTTOOIKEUSANOMUSILMOITUS_EMAIL_REPLACEMENT_LINKKI,
                urlProperties.url("henkilo-ui.anomukset")));
        recipient.setRecipientReplacements(replacements);
        return recipient;
    }

    private void sendNewRequisitionNotificationEmail(String kieliKoodi, List<EmailRecipient> recipients) {
        EmailData data = new EmailData();
        data.setEmail(generateEmailMessage(KAYTTOOIKEUSANOMUSILMOITUS_EMAIL_TEMPLATE_NAME, kieliKoodi));
        data.setRecipient(recipients);
        ryhmasahkopostiClient.sendRyhmasahkoposti(data);
    }

    public void sendInvitationEmail(Kutsu kutsu) {
        HenkiloDto kutsuja = this.oppijanumerorekisteriClient.getHenkiloByOid(kutsu.getKutsuja());

        EmailData emailData = new EmailData();

        EmailMessage email = new EmailMessage();
        email.setTemplateName(KUTSUTTU_EMAIL_TEMPLATE_NAME);
        email.setLanguageCode(kutsu.getKieliKoodi());
        email.setCallingProcess(CALLING_PROCESS);
        email.setCharset("UTF-8");
        email.setHtml(true);
        emailData.setEmail(email);

        EmailRecipient recipient = new EmailRecipient();
        recipient.setEmail(kutsu.getSahkoposti());
        recipient.setLanguageCode(kutsu.getKieliKoodi());
        recipient.setName(kutsu.getEtunimi() + " " + kutsu.getSukunimi());

        Map<String, String> targetUrlQueryParams = new HashMap<String, String>() {{
            put("kutsuToken", kutsu.getSalaisuus());
            put("locale", kutsu.getKieliKoodi());
        }};
        String kayttooikeusTunnistusUrl = this.urlProperties
                .url("kayttooikeus-service.cas.tunnistus", targetUrlQueryParams);
        Map<String, String> urlQueryParams = new HashMap<String, String>() {{
            put("service", kayttooikeusTunnistusUrl);
            put("locale", kutsu.getKieliKoodi().toUpperCase());
        }};
        recipient.setRecipientReplacements(asList(replacement("linkki", this.urlProperties
                        .url("cas.oppija.identification", urlQueryParams)),
                replacement("vastaanottaja", mapper.map(kutsu, SahkopostiHenkiloDto.class)),
                replacement("organisaatiot", kutsu.getOrganisaatiot().stream()
                        .map(org -> new OranizationReplacement(new TextGroupMapDto(
                                        this.organisaatioClient.getOrganisaatioPerustiedotCached(org.getOrganisaatioOid()
                                                )
                                                .orElseThrow(() -> new NotFoundException("Organisation not found with oid " + org.getOrganisaatioOid()))
                                                .getNimi()).getOrAny(kutsu.getKieliKoodi()).orElse(null),
                                        org.getRyhmat().stream().map(KayttoOikeusRyhma::getNimi)
                                                .map(desc -> desc.getOrAny(kutsu.getKieliKoodi()).orElse(null))
                                                .filter(Objects::nonNull).sorted().collect(toList())
                                )
                        ).sorted(comparing(OranizationReplacement::getName)).collect(toList())),
                replacement("saate", kutsu.getSaate()),
                replacement("kutsuja", mapper.map(kutsuja, SahkopostiHenkiloDto.class)),
                replacement("voimassa", kutsu.getAikaleima().plusMonths(1).format(DateTimeFormatter.ofPattern("dd.MM.yyyy")))
        ));
        emailData.setRecipient(singletonList(recipient));

        logger.info("Sending invitation email to {}", kutsu.getSahkoposti());
        String response = this.ryhmasahkopostiClient.sendRyhmasahkoposti(emailData);
        logger.info("Sent invitation email to {}, ryhmasahkoposti-result: {}", kutsu.getSahkoposti(), response);
    }

    @Override
    public void sendDiscardedInvitationNotification(Kutsu invitation) {
        EmailData emailData = new EmailData();
        emailData.setEmail(getMessageTemplate(DISCARDED_INVITATION_EMAIL_TEMPLATE, invitation.getKieliKoodi()));
        emailData.setRecipient(singletonList(resolveRecipient(invitation)));
        ryhmasahkopostiClient.sendRyhmasahkoposti(emailData);
    }

    @Override
    public void sendDiscardedApplicationNotification(Anomus application) {
        String languageCode = resolveLanguageCode(application.getHenkilo());
        EmailData emailData = new EmailData();
        emailData.setEmail(getMessageTemplate(DISCARDED_APPLICATION_EMAIL_TEMPLATE, languageCode));
        emailData.setRecipient(singletonList(resolveRecipient(application, languageCode)));
        ryhmasahkopostiClient.sendRyhmasahkoposti(emailData);
    }

    private String resolveLanguageCode(Henkilo henkilo) {
        return oppijanumerorekisteriClient.getHenkiloByOid(henkilo.getOidHenkilo())
                .getAsiointiKieli().getKieliKoodi();
    }

    private EmailMessage getMessageTemplate(String templateName, String languageCode) {
        EmailMessage email = new EmailMessage();
        email.setTemplateName(templateName);
        email.setLanguageCode(languageCode);
        email.setCallingProcess(CALLING_PROCESS);
        email.setCharset("UTF-8");
        email.setHtml(true);
        return email;
    }

    private EmailRecipient resolveRecipient(Anomus application, String languageCode) {
        EmailRecipient recipient = new EmailRecipient();
        recipient.setEmail(application.getSahkopostiosoite());
        recipient.setLanguageCode(languageCode);
        recipient.setName(String.format("%s %s", application.getHenkilo().getKutsumanimiCached(),
                application.getHenkilo().getSukunimiCached()));
        recipient.setRecipientReplacements(Collections.emptyList());
        return recipient;
    }

    private EmailRecipient resolveRecipient(Kutsu invitation) {
        EmailRecipient recipient = new EmailRecipient();
        recipient.setEmail(invitation.getSahkoposti());
        recipient.setLanguageCode(invitation.getKieliKoodi());
        recipient.setName(String.format("%s %s", invitation.getEtunimi(), invitation.getSukunimi()));
        recipient.setRecipientReplacements(Collections.emptyList());
        return recipient;
    }

    @NotNull
    private ReportedRecipientReplacementDTO replacement(String name, Object value) {
        ReportedRecipientReplacementDTO replacement = new ReportedRecipientReplacementDTO();
        replacement.setName(name);
        replacement.setValue(value);
        return replacement;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OranizationReplacement {
        private String name;
        private List<String> permissions;
    }
}
