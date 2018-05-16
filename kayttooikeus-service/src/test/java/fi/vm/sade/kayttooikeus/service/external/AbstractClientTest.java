package fi.vm.sade.kayttooikeus.service.external;

import fi.vm.sade.kayttooikeus.service.AbstractServiceTest;
import net.jadler.junit.rule.JadlerRule;
import net.jadler.stubbing.server.jdk.JdkStubHttpServer;
import org.apache.http.HttpStatus;
import org.jasig.cas.client.authentication.AttributePrincipal;
import org.jasig.cas.client.validation.AssertionImpl;
import org.junit.After;
import org.junit.Rule;
import org.springframework.security.cas.authentication.CasAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static fi.vm.sade.kayttooikeus.util.FreePortUtil.portNumberBySystemPropertyOrFree;
import static net.jadler.Jadler.onRequest;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;

public abstract class AbstractClientTest extends AbstractServiceTest {
    protected static final int OK = HttpStatus.SC_OK;
    protected static int MOCK_SERVER_PORT = portNumberBySystemPropertyOrFree("test.port");
    protected final JdkStubHttpServer stubHttpServer = new JdkStubHttpServer(MOCK_SERVER_PORT);
    @Rule
    public final JadlerRule jadler = new JadlerRule(stubHttpServer);

    @After
    public void tearDown() throws Exception {
        stubHttpServer.stop();
        Thread.sleep(20L);
    }

    protected void casAuthenticated(String henkiloOid) {
        SecurityContextHolder.getContext().setAuthentication(new CasAuthenticationToken("KEY", henkiloOid, "CRED",
                Collections.emptyList(), new User(henkiloOid, "", Collections.emptyList()), new AssertionImpl(new AttributePrincipal() {
            @Override
            public String getProxyTicketFor(String service) {
                return "TICKET";
            }

            @Override
            public Map<String, Object> getAttributes() {
                return new HashMap<>();
            }

            @Override
            public String getName() {
                return henkiloOid;
            }
        })));

        onRequest().havingMethod(is("POST")).havingPath(is("/cas/v1/tickets"))
                .respond().withStatus(201).withHeader("Location", "/TICKET");
        onRequest().havingMethod(is("POST")).havingPath(startsWith("/cas/v1/tickets/"))
                .respond().withStatus(OK).withBody("TICKET");
    }
}
