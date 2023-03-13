package fi.vm.sade.kayttooikeus.service;

import fi.vm.sade.kayttooikeus.dto.IdentifiedHenkiloTypeDto;
import fi.vm.sade.kayttooikeus.model.Henkilo;
import fi.vm.sade.kayttooikeus.model.TunnistusToken;

import java.util.Optional;
import java.util.Set;

public interface IdentificationService {
    String generateAuthTokenForHenkilo(String oid, String idpKey, String idpIdentifier);

    String generateAuthTokenForHenkilo(Henkilo henkilo, String idpKey, String idpIdentifier);

    String getHenkiloOidByIdpAndIdentifier(String idpKey, String idpIdentifier);

    IdentifiedHenkiloTypeDto findByTokenAndInvalidateToken(String authToken);

    String updateIdentificationAndGenerateTokenForHenkiloByOid(String oidHenkilo);

    Set<String> getTunnisteetByHenkiloAndIdp(String identityProvider, String oid);
    Set<String> updateTunnisteetByHenkiloAndIdp(String identityProvider, String oid, Set<String> hakatunnisteet);
    boolean getMpassidLoginEnabled(String oid);
    void setMpassidLoginEnabled(String oid, boolean enabled);

    Optional<String> updateKutsuAndGenerateTemporaryKutsuToken(String kutsuToken, String hetu, String etunimet, String sukunimi);

    String createLoginToken(String oidHenkilo, Boolean salasananVaihto, String hetu);

    Optional<TunnistusToken> updateLoginToken(String loginToken, String hetu);

    TunnistusToken getByValidLoginToken(String loginToken);

    String consumeLoginToken(String loginToken, String identification_idp);

    String getIdpEntityIdForCurrentSession();
}
