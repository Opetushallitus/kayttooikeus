package fi.vm.sade.kayttooikeus.service.impl.ldap;

import fi.vm.sade.kayttooikeus.config.OrikaBeanMapper;
import fi.vm.sade.kayttooikeus.model.Henkilo;
import fi.vm.sade.kayttooikeus.model.Kayttaja;
import fi.vm.sade.kayttooikeus.model.Ryhma;
import fi.vm.sade.kayttooikeus.repositories.IdentificationRepository;
import fi.vm.sade.kayttooikeus.repositories.KayttajaRepository;
import fi.vm.sade.kayttooikeus.repositories.MyonnettyKayttoOikeusRyhmaTapahtumaRepository;
import fi.vm.sade.kayttooikeus.repositories.RyhmaRepository;
import fi.vm.sade.kayttooikeus.service.exception.DataInconsistencyException;
import fi.vm.sade.oppijanumerorekisteri.dto.HenkiloDto;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static fi.vm.sade.kayttooikeus.service.impl.ldap.LdapUtils.generateRandomPassword;
import static java.util.stream.Collectors.partitioningBy;
import static java.util.stream.Collectors.toSet;

/**
 * Palvelu LDAP:n käsittelyyn ({@link Kayttaja} ja {@link Ryhma}).
 */
@Service
@RequiredArgsConstructor
public class LdapService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LdapService.class);

    private final KayttajaRepository kayttajaRepository;
    private final RyhmaRepository ryhmaRepository;
    private final OrikaBeanMapper mapper;
    private final IdentificationRepository identificationRepository;
    private final MyonnettyKayttoOikeusRyhmaTapahtumaRepository myonnettyKayttoOikeusRyhmaTapahtumaRepository;

    /**
     * Lisää tai päivittää henkilön tiedot.
     *
     * @param entity käyttöoikeuspalvelu henkilö
     * @param dto oppijanumerorekisteri henkilö
     */
    public void upsert(Henkilo entity, HenkiloDto dto) {
        String kayttajatunnus = entity.getKayttajatiedot().getUsername();
        Map<Boolean, List<Kayttaja>> kayttajat = kayttajaRepository.findByOid(dto.getOidHenkilo()).stream()
                .collect(partitioningBy(kayttaja -> kayttaja.getKayttajatunnus().equals(kayttajatunnus)));
        Kayttaja kayttaja = getOrCreateKayttaja(kayttajat.get(true), kayttajatunnus);

        // muodostetaan henkilön ldap-roolit
        LdapRoolitBuilder roolit = new LdapRoolitBuilder()
                // käyttöoikeuspalvelun roolit
                .kayttajaTyyppi(entity.getKayttajaTyyppi())
                .identifications(identificationRepository.findByHenkilo(entity))
                .myonnetyt(myonnettyKayttoOikeusRyhmaTapahtumaRepository.findValidMyonnettyKayttooikeus(dto.getOidHenkilo()))
                // oppijanumerorekisterin roolit
                .asiointikieli(dto.getAsiointiKieli());
        Set<String> dbRoolit = roolit.asSet();

        // päivitetään käyttäjän perustiedot
        mapper.map(entity, kayttaja);
        mapper.map(dto, kayttaja);
        roolit.kayttajatunnus(kayttajatunnus);
        kayttaja.setRoolit(roolit.asString());
        kayttaja = kayttajaRepository.save(kayttaja);
        LOGGER.info("Tallennetaan {}", kayttaja);

        // päivitetään käyttäjän ryhmät
        String kayttajaDn = kayttaja.getDnAsString();
        List<Ryhma> ldapRyhmat = ryhmaRepository.findByKayttajat(kayttajaDn);
        Set<String> ldapRoolit = ldapRyhmat.stream().map(Ryhma::getNimi).collect(toSet());
        ldapRyhmat.stream()
                .filter(ldapRyhma -> !dbRoolit.contains(ldapRyhma.getNimi()))
                .forEach(ldapRyhma -> deleteFromRyhma(ldapRyhma, kayttajaDn));
        dbRoolit.stream()
                .filter(dbRooli -> !ldapRoolit.contains(dbRooli))
                .forEach(dbRooli -> addToRyhma(dbRooli, kayttajaDn));

        // käyttäjätunnuksen muuttuessa poistetaan vanhan käyttäjätunnuksen salasana
        kayttajat.get(false).forEach(this::obsolete);
    }

    private Kayttaja getOrCreateKayttaja(List<Kayttaja> kayttajat, String kayttajatunnus) {
        if (kayttajat.size() > 1) {
            throw new DataInconsistencyException("Käyttäjätunnuksella " + kayttajatunnus + " löytyi useampi käyttäjä");
        }
        if (kayttajat.size() == 1) {
            return kayttajat.get(0);
        }
        return kayttajaRepository.findByKayttajatunnus(kayttajatunnus).orElseGet(Kayttaja::new);
    }

    /**
     * Poistaa henkilön tiedot.
     *
     * @param oid henkilö oid
     */
    public void deleteByOid(String oid) {
        kayttajaRepository.findByOid(oid).forEach(this::delete);
    }

    private void delete(Kayttaja kayttaja) {
        LOGGER.info("Poistetaan {}", kayttaja);
        kayttajaRepository.delete(kayttaja);
        String kayttajaDn = kayttaja.getDnAsString();
        ryhmaRepository.findByKayttajat(kayttajaDn)
                .forEach(t -> deleteFromRyhma(t, kayttajaDn));
    }

    private void obsolete(Kayttaja kayttaja) {
        LOGGER.info("Poistetaan salasana {}", kayttaja);
        kayttaja.setSalasana(generateRandomPassword());
        kayttajaRepository.save(kayttaja);
    }

    private void addToRyhma(String ryhmaNimi, String kayttajaDn) {
        Ryhma ryhma = ryhmaRepository.findByNimi(ryhmaNimi)
                .orElseGet(() -> Ryhma.builder().nimi(ryhmaNimi).build());
        addToRyhma(ryhma, kayttajaDn);
    }

    private void addToRyhma(Ryhma ryhma, String kayttajaDn) {
        LOGGER.info("Lisätään käyttäjä '{}' ryhmään '{}'", kayttajaDn, ryhma.getNimi());
        if (ryhma.addKayttaja(kayttajaDn)) {
            ryhmaRepository.save(ryhma);
        }
    }

    private void deleteFromRyhma(Ryhma ryhma, String kayttajaDn) {
        LOGGER.info("Poistetaan käyttäjältä '{}' ryhmä '{}'", kayttajaDn, ryhma.getNimi());
        if (ryhma.deleteKayttaja(kayttajaDn)) {
            if (ryhma.isEmpty()) {
                LOGGER.info("Poistetaan ryhmä '{}' koska sillä ei ole enää yhtään käyttäjää", ryhma.getNimi());
                ryhmaRepository.delete(ryhma);
            } else {
                ryhmaRepository.save(ryhma);
            }
        }
    }

}
