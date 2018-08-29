package fi.vm.sade.kayttooikeus.repositories;

import fi.vm.sade.kayttooikeus.enumeration.OrderByAnomus;
import fi.vm.sade.kayttooikeus.model.HaettuKayttoOikeusRyhma;
import fi.vm.sade.kayttooikeus.model.QAnomus;
import fi.vm.sade.kayttooikeus.model.QHaettuKayttoOikeusRyhma;
import fi.vm.sade.kayttooikeus.model.QKayttoOikeusRyhma;
import fi.vm.sade.kayttooikeus.repositories.criteria.AnomusCriteria;

import java.util.List;

public interface HaettuKayttooikeusRyhmaRepositoryCustom {

    List<HaettuKayttoOikeusRyhma> findBy(AnomusCriteria.AnomusCriteriaFunction<QAnomus, QKayttoOikeusRyhma, QHaettuKayttoOikeusRyhma> criteriaFunction, Long limit, Long offset, OrderByAnomus orderBy);
    List<HaettuKayttoOikeusRyhma> findBy(AnomusCriteria.AnomusCriteriaFunction<QAnomus, QKayttoOikeusRyhma, QHaettuKayttoOikeusRyhma> criteriaFunction);

}
