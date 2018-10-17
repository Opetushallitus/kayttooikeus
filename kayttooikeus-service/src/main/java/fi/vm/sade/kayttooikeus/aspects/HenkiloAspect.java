package fi.vm.sade.kayttooikeus.aspects;

import fi.vm.sade.kayttooikeus.dto.KayttajatiedotCreateDto;
import fi.vm.sade.kayttooikeus.dto.KayttajatiedotUpdateDto;
import fi.vm.sade.kayttooikeus.repositories.dto.HenkiloCreateByKutsuDto;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;

@Aspect
@Component
public class HenkiloAspect {

    private HenkiloHelper henkiloHelper;

    @Autowired
    public HenkiloAspect(HenkiloHelper helper) {
        henkiloHelper = helper;
    }

    @Around(value = "execution(public * fi.vm.sade.kayttooikeus.service.HenkiloService.passivoi(..))" +
            "&& args(henkiloOid, kasittelijaOid)", argNames = "proceedingJoinPoint, henkiloOid, kasittelijaOid")
    private Object logPassivoiHenkilo(ProceedingJoinPoint proceedingJoinPoint, String henkiloOid, String kasittelijaOid) throws Throwable {
        Object result = proceedingJoinPoint.proceed();
        henkiloHelper.logPassivoiHenkilo(henkiloOid, kasittelijaOid, result);
        return result;
    }

    @Around(value = "execution(public * fi.vm.sade.kayttooikeus.service.KayttajatiedotService.changePasswordAsAdmin(..))" +
            "&& args(oid, newPassword)", argNames = "proceedingJoinPoint, oid, newPassword")
    private Object logChangePassword(ProceedingJoinPoint proceedingJoinPoint, String oid, String newPassword) throws Throwable {
        Object result = proceedingJoinPoint.proceed();
        henkiloHelper.logChangePassword(oid, newPassword, result);
        return result;
    }

    @Around(value = "execution(public * fi.vm.sade.kayttooikeus.service.IdentificationService.updateHakatunnuksetByHenkiloAndIdp(..))" +
            "&& args(oid, ipdKey, hakatunnisteet)", argNames = "proceedingJoinPoint, oid, ipdKey, hakatunnisteet")
    private Object logUpdateHakatunnisteet(ProceedingJoinPoint proceedingJoinPoint, String oid, String ipdKey, Set<String> hakatunnisteet) throws Throwable {
        Object result = proceedingJoinPoint.proceed();
        henkiloHelper.logUpdateHakaTunnisteet(oid, ipdKey, hakatunnisteet, result);
        return result;
    }

    @Around(value = "execution(public * fi.vm.sade.kayttooikeus.service.KayttajatiedotService.create(..))" +
            "&& args(henkiloOid, kayttajatiedot)", argNames = "proceedingJoinPoint, henkiloOid, kayttajatiedot")
    private Object logCreateKayttajatiedot(ProceedingJoinPoint proceedingJoinPoint, String henkiloOid, KayttajatiedotCreateDto kayttajatiedot) throws Throwable {
        Object result = proceedingJoinPoint.proceed();
        henkiloHelper.logCreateKayttajatiedot(henkiloOid, kayttajatiedot, result);
        return result;
    }

    @Around(value = "execution(public * fi.vm.sade.kayttooikeus.service.KayttajatiedotService.updateKayttajatiedot(..))" +
            "&& args(henkiloOid, kayttajatiedot)", argNames = "proceedingJoinPoint, henkiloOid, kayttajatiedot")
    private Object logUpdateKayttajatiedot(ProceedingJoinPoint proceedingJoinPoint, String henkiloOid, KayttajatiedotUpdateDto kayttajatiedot) throws Throwable {
        Object result = proceedingJoinPoint.proceed();
        henkiloHelper.logUpdateKayttajatiedot(henkiloOid, kayttajatiedot, result);
        return result;
    }

    @Around(value = "execution(public * fi.vm.sade.kayttooikeus.service.KutsuService.createHenkilo(..))" +
            "&& args(temporaryToken, henkiloCreateByKutsuDto)", argNames = "proceedingJoinPoint, temporaryToken, henkiloCreateByKutsuDto")
    private Object logUpdateKayttajatiedot(ProceedingJoinPoint proceedingJoinPoint,
                                           String temporaryToken,
                                           HenkiloCreateByKutsuDto henkiloCreateByKutsuDto) throws Throwable {
        Object result = proceedingJoinPoint.proceed();
        henkiloHelper.logCreateHenkilo(temporaryToken, henkiloCreateByKutsuDto, result);
        return result;
    }

}
