package fi.vm.sade.kayttooikeus.service;

import fi.vm.sade.kayttooikeus.config.properties.AuthProperties;
import fi.vm.sade.kayttooikeus.service.impl.CryptoServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

public class CryptoServiceTest {

    private CryptoService cryptoService;

    @Before
    public void setup() {
        AuthProperties authProperties = new AuthProperties();
        authProperties.getCryptoService().setStaticSalt("a4hgs4c4CSy54CS59hyjhs48gfsdFAA42V43a3Daefs2f84hdaESFayh3a3gc2aW");
        this.cryptoService = spy(new CryptoServiceImpl(authProperties));

        // Note: these should match settings of AuthProperties.Password
        ReflectionTestUtils.setField(this.cryptoService, "passwordMinLen", 20);
    }

    @Test
    public void testGenerateSalt() {
        String salt = cryptoService.generateSalt();
        assertThat(StringUtils.isNotBlank(salt)).isTrue();
    }

    @Test
    public void testGenerateHash() {
        String password ="salasana";
        String salt = cryptoService.generateSalt();
        assertThat(StringUtils.isNotBlank(salt)).isTrue();
        String hash = cryptoService.getSaltedHash(password, salt);
        assertThat(StringUtils.isNotBlank(hash)).isTrue();
    }

    @Test
    public void testCheck() {
        String password ="salasana";
        String salt = cryptoService.generateSalt();
        assertThat(StringUtils.isNotBlank(salt)).isTrue();
        String hash = cryptoService.getSaltedHash(password, salt);
        assertThat(StringUtils.isNotBlank(hash)).isTrue();
        boolean passok = cryptoService.check(password, hash, salt);
        assertThat(passok).isTrue();
    }

    /**
     * Test that known configuration works
     */
    @Test
    public void testKnownConfiguration() {
        String passwd = "!23Qwerty";
        String salt = "4jcaigak2g918sqlhc3lfkcd1d";
        String hash = "WlElVQd59HgK3Qdg8iVECQ==";
        assertThat(cryptoService.check(passwd, hash, salt)).isTrue();
    }

    /**
     * Use this to create new passwords and salts
     */
    @Test
    public void testCreateNewPassword() {
        doReturn("2l2rdj4q3lkq537no8trcm58bu").when(this.cryptoService).generateSalt();
        String passwd = "!23Qwerty";
        String salt = cryptoService.generateSalt();
        String hash = cryptoService.getSaltedHash(passwd, salt);

        String combined = salt + "|" + hash;
        assertThat(combined).isEqualTo("2l2rdj4q3lkq537no8trcm58bu|6dqepFH7b7Crl3nDjtVElA==");
    }

    /**
     * Use this to measure actual execution time for encryption if you change algorithm, ignored for now, takes too long on bamboo
     */
    @Ignore
    @Test
    public void testExecutionTime() {
        long rounds = 10000;
        long time = 5000;
        long now = System.currentTimeMillis();
        for(int i = 0; i<rounds; i++) {
            this.testGenerateSalt();
        }
        long end = System.currentTimeMillis();
        System.out.println("TIME: " + (end-now) );

        assertThat((end-now) < time).isTrue();// "testGenerateSalt Execution time failed, " + rounds + " rounds took longer than " + time,  ) ;

        rounds = 100;
        time = 5000;
        now = System.currentTimeMillis();
        for(int i = 0; i<rounds; i++) {
            this.testGenerateHash();
        }
        end = System.currentTimeMillis();
        System.out.println("TIME: " + (end-now) );

        assertThat((end-now) < time ).isTrue(); //"testGenerateHash Execution time failed, " + rounds + " rounds took longer than " + time,

        rounds = 10;
        time = 2000;
        now = System.currentTimeMillis();
        for(int i = 0; i<rounds; i++) {
            this.testCheck();
        }
        end = System.currentTimeMillis();
        System.out.println("TIME: " + (end-now) );

        assertThat((end-now) < time ).isTrue(); //"testCheck Execution time failed, " + rounds + " rounds took longer than " + time,
    }


    @Test
    public void testPasswordStrengthBad() {
        String[] fixtures = new String[]{
                "",
                "aaaaaaaaaaaaaaaaaaa",
                "Testi12345!#€%",
                "test Test 1234 #"
        };
        Arrays.asList(fixtures).forEach(candidate -> {
            assertThat(cryptoService.isStrongPassword(candidate)).isNotEmpty();
        });
    }

    @Test
    public void testPasswordStrengthOK() {
        String[] fixtures = new String[]{
                "____________________aA!1",
                "____________________A!1",
                "____________________a!1",
                "____________________aA!"
        };
        Arrays.asList(fixtures).forEach(candidate -> {
            assertThat(cryptoService.isStrongPassword(candidate)).isEmpty();
        });
    }

    @Test
    public void testPasswordStrengthNOK() {
        Object[][] fixtures = new Object[][]{
                {null, Arrays.asList("validPassword.empty")},
                {"", Arrays.asList("validPassword.short;20")},
                {"AAAAAAAAAAAAAAAAAAAA-", Arrays.asList("validPassword.forbidden")},
                {"AAAAAAAAAAAAAAAAAAAA!", Arrays.asList("validPassword.lowercase", "validPassword.numbers")},
                {"AAAAAAAAAAAAAAAAAAAA1", Arrays.asList("validPassword.lowercase", "validPassword.nospecial")},
                {"aaaaaaaaaaaaaaaaaaaa!", Arrays.asList("validPassword.uppercase", "validPassword.numbers")},
                {"aaaaaaaaaaaaaaaaaaaa1", Arrays.asList("validPassword.uppercase", "validPassword.nospecial")},
                {"AAAAAAAAAAAAAAAAAAAAa", Arrays.asList("validPassword.nospecial", "validPassword.numbers")},
        };
        Arrays.asList(fixtures).forEach(testCase -> {
            assertThat(cryptoService.isStrongPassword((String)testCase[0])).isEqualTo(testCase[1]);
        });
    }
}
