package fi.vm.sade.kayttooikeus.config;

import fi.vm.sade.kayttooikeus.Application;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestExecutionListeners.MergeMode;
import org.springframework.test.context.support.AbstractTestExecutionListener;

import java.lang.annotation.*;

import static fi.vm.sade.kayttooikeus.util.FreePortUtil.portNumberBySystemPropertyOrFree;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
@SpringBootTest(classes = Application.class)
@TestExecutionListeners(mergeMode = MergeMode.MERGE_WITH_DEFAULTS,
        listeners = {ApplicationTest.SetEnvTestExecutionListener.class})
public @interface ApplicationTest {
    class SetEnvTestExecutionListener extends AbstractTestExecutionListener {
        @Override
        public void beforeTestClass(TestContext testContext) throws Exception {
            int port = portNumberBySystemPropertyOrFree("test.port");
            System.setProperty("host-virkailija", "localhost:"+port);
            System.setProperty("host-cas", "localhost:"+port);
            System.setProperty("host-shibboleth", "localhost:"+port);
            System.setProperty("url-virkailija", "http://localhost:"+port);
            System.setProperty("organisaatio-service.baseUrl", "http://localhost:"+port);
            System.setProperty("cas.url", "http://localhost:"+port+"/cas");
        }
    }
}
