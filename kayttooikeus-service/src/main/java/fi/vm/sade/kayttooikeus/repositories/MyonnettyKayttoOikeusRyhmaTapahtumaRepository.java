package fi.vm.sade.kayttooikeus.repositories;

import fi.vm.sade.kayttooikeus.model.MyonnettyKayttoOikeusRyhmaTapahtuma;
import fi.vm.sade.kayttooikeus.model.OrganisaatioHenkilo;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static fi.vm.sade.kayttooikeus.service.impl.PermissionCheckerServiceImpl.PALVELU_ANOMUSTENHALLINTA;
import static fi.vm.sade.kayttooikeus.service.impl.PermissionCheckerServiceImpl.ROLE_CRUD;

@Repository
@Transactional(propagation = Propagation.MANDATORY)
public interface MyonnettyKayttoOikeusRyhmaTapahtumaRepository extends CrudRepository<MyonnettyKayttoOikeusRyhmaTapahtuma, Long>,
        MyonnettyKayttoOikeusRyhmaTapahtumaRepositoryCustom {

    List<MyonnettyKayttoOikeusRyhmaTapahtuma>
    findByOrganisaatioHenkiloHenkiloOidHenkiloAndVoimassaAlkuPvmLessThanEqualAndVoimassaLoppuPvmGreaterThanEqualAndOrganisaatioHenkiloPassivoitu(
            String oidHenkilo, LocalDate voimassaAlkuPvm, LocalDate voimassaLoppuPvm, boolean organisaatiohenkiloPassivoitu);
    default List<MyonnettyKayttoOikeusRyhmaTapahtuma> findValidMyonnettyKayttooikeus(String oidHenkilo) {
        return findByOrganisaatioHenkiloHenkiloOidHenkiloAndVoimassaAlkuPvmLessThanEqualAndVoimassaLoppuPvmGreaterThanEqualAndOrganisaatioHenkiloPassivoitu(
                oidHenkilo, LocalDate.now(), LocalDate.now(), false);
    }

    Optional<MyonnettyKayttoOikeusRyhmaTapahtuma> findFirstByKayttoOikeusRyhmaIdAndOrganisaatioHenkiloOrganisaatioOidAndOrganisaatioHenkiloHenkiloOidHenkilo(
            Long kayttooikeusryhmaId, String organisaatioOid, String oidHenkilo
    );
    default Optional<MyonnettyKayttoOikeusRyhmaTapahtuma> findMyonnettyTapahtuma(Long kayttooikeusryhmaId, String organisaatioOid, String oidHenkilo) {
        return findFirstByKayttoOikeusRyhmaIdAndOrganisaatioHenkiloOrganisaatioOidAndOrganisaatioHenkiloHenkiloOidHenkilo(
                kayttooikeusryhmaId, organisaatioOid, oidHenkilo);
    }

    /**
     * Hakee henkilön kaikki voimassa olevat käyttöoikeudet
     * @param oidHenkilo henkilön oid
     * @return käyttöoikeudet
     */
    List<MyonnettyKayttoOikeusRyhmaTapahtuma> findByOrganisaatioHenkiloHenkiloOidHenkilo(String oidHenkilo);

    List<MyonnettyKayttoOikeusRyhmaTapahtuma> findByKayttoOikeusRyhmaId(long id);
}
