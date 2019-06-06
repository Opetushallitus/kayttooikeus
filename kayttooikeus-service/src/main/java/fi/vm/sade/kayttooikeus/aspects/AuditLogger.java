package fi.vm.sade.kayttooikeus.aspects;

import fi.vm.sade.auditlog.*;
import lombok.RequiredArgsConstructor;
import org.ietf.jgss.Oid;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.util.Optional;

import static fi.vm.sade.kayttooikeus.aspects.AuditHelper.*;

@Component
@RequiredArgsConstructor
public class AuditLogger {

    private final Audit audit;

    public void log(Operation operation, Target target, Changes changes) {
        log(getUser(), operation, target, changes);
    }

    public void log(User user, Operation operation, Target target, Changes changes) {
        audit.log(user, operation, target, changes);
    }

    public static User getUser() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes instanceof ServletRequestAttributes) {
            return getUser(((ServletRequestAttributes) requestAttributes).getRequest());
        }
        return new User(getIp(), null, null);
    }

    public static User getUser(HttpServletRequest request) {
        Optional<Oid> oid = getOid(request);
        InetAddress ip = getIp(request);
        String session = getSession(request).orElse(null);
        String userAgent = getUserAgent(request).orElse(null);

        if (oid.isPresent()) {
            return new User(oid.get(), ip, session, userAgent);
        } else {
            return new User(ip, session, userAgent);
        }
    }

}
