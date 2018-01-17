package fi.vm.sade.kayttooikeus.config.db;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableJpaRepositories(basePackages = {"fi.vm.sade.kayttooikeus.repositories"})
@EntityScan({"fi.vm.sade.kayttooikeus.model"})
@EnableTransactionManagement
public class JpaConfiguration {
}
