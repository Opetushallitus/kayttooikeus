package fi.vm.sade.kayttooikeus;

import fi.vm.sade.kayttooikeus.repositories.populate.Populator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import javax.persistence.EntityManager;
import java.sql.ResultSet;
import java.util.function.Supplier;

@Service
public class DatabaseService {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TransactionTemplate transactionTemplate;

    public void truncate() {
        jdbcTemplate.execute("set referential_integrity false");
        jdbcTemplate.query("show tables", (ResultSet rs, int rowNum) -> rs.getString("table_name"))
                .forEach(tableName -> jdbcTemplate.execute(String.format("truncate table %s", tableName)));
        jdbcTemplate.execute("set referential_integrity true");
    }

    public void runInTransaction(Runnable runnable) {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                runnable.run();
            }
        });
    }

    public <T> T runInTransaction(Supplier<T> supplier) {
        return transactionTemplate.execute(status -> supplier.get());
    }

    public <T> T populate(Populator<T> populator) {
        return runInTransaction(() -> {
            T entity = populator.apply(entityManager);
            entityManager.flush();
            return entity;
        });
    }

}
