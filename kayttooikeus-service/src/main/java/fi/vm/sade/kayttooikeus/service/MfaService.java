package fi.vm.sade.kayttooikeus.service;

import fi.vm.sade.kayttooikeus.dto.GoogleAuthSetupDto;

public interface MfaService {
    /**
     * Saves a new Google Auth token for the current user and returns everything needed to setup it in the frontend
     */
    GoogleAuthSetupDto setupGoogleAuth();

    /**
     * Enables the current Google Auth token for the user and sets the MFA provider
     * @param tokenToVerify token to verify that the user has a working Google Authenticator
     * @return true if successfully enabled
     */
    boolean enableGoogleAuth(String tokenToVerify);

    /**
     * Sets users MFA provider to null
     * @return true if successfully disabled
     */
    boolean disableGoogleAuth();
}
