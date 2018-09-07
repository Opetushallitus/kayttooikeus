package fi.vm.sade.kayttooikeus.service;

import fi.vm.sade.kayttooikeus.dto.IdentifiedHenkiloTypeDto;
import fi.vm.sade.kayttooikeus.model.Henkilo;
import fi.vm.sade.kayttooikeus.model.TunnistusToken;

import java.util.Set;

public interface IdentificationService {
    String generateAuthTokenForHenkilo(String oid, String idpKey, String idpIdentifier);

    String generateAuthTokenForHenkilo(Henkilo henkilo, String idpKey, String idpIdentifier);

    String getHenkiloOidByIdpAndIdentifier(String idpKey, String idpIdentifier);

    IdentifiedHenkiloTypeDto findByTokenAndInvalidateToken(String authToken);

    String updateIdentificationAndGenerateTokenForHenkiloByOid(String oidHenkilo);

    String updateIdentificationAndGenerateTokenForHenkiloByHetu(String hetu);

    Set<String> getHakatunnuksetByHenkiloAndIdp(String oid);

    Set<String> updateHakatunnuksetByHenkiloAndIdp(String oid, Set<String> hakatunnisteet);

    String updateKutsuAndGenerateTemporaryKutsuToken(String kutsuToken, String hetu, String etunimet, String sukunimi);

    String createLoginToken(String oidHenkilo, Boolean salasananVaihto, String hetu);

    TunnistusToken updateLoginToken(String loginToken, String hetu);

    TunnistusToken getByValidLoginToken(String loginToken);

    String consumeLoginToken(String loginToken);

}
