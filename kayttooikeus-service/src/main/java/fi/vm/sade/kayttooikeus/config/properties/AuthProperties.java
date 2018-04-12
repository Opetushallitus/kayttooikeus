package fi.vm.sade.kayttooikeus.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "auth")
public class AuthProperties {
    private CryptoService cryptoService = new CryptoService();
    private Password password = new Password();

    @Getter @Setter
    public static class CryptoService {
        private String staticSalt;
    }
    @Getter @Setter
    public static class Password {
        private Integer minLen = 10;
        private Integer minAmountSpecialChars = 1;
        private Integer minAmountNumbers = 1;
        private Boolean lowerAndUpperCase = true;

    }

}
