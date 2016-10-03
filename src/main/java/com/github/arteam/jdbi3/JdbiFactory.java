package com.github.arteam.jdbi3;

import com.github.arteam.jdbi3.strategies.DelegatingStatementNameStrategy;
import com.github.arteam.jdbi3.strategies.NameStrategies;
import io.dropwizard.db.ManagedDataSource;
import io.dropwizard.db.PooledDataSourceFactory;
import io.dropwizard.setup.Environment;
import io.dropwizard.util.Duration;
import org.jdbi.v3.core.Jdbi;

import static com.codahale.metrics.MetricRegistry.name;

/**
 * @author Artem Prigoda
 * @since 28.07.16
 */
public class JdbiFactory {

    private static class SanerNamingStrategy extends DelegatingStatementNameStrategy {
        private SanerNamingStrategy() {
            super(NameStrategies.CHECK_EMPTY,
                    NameStrategies.CONTEXT_CLASS,
                    NameStrategies.CONTEXT_NAME,
                    NameStrategies.SQL_OBJECT,
                    statementContext -> name(Jdbi.class, "raw-sql"));
        }
    }

    public Jdbi build(Environment environment,
                      PooledDataSourceFactory configuration,
                      String name) {
        ManagedDataSource dataSource = configuration.build(environment.metrics(), name);
        String validationQuery = configuration.getValidationQuery();
        Jdbi jdbi = Jdbi.create(dataSource);
        jdbi.setTimingCollector(new InstrumentedTimingCollector(environment.metrics(), new SanerNamingStrategy()));
        jdbi.installPlugins();

        environment.lifecycle().manage(dataSource);
        environment.healthChecks().register(name, new JdbiHealthCheck(
                environment.getHealthCheckExecutorService(),
                configuration.getValidationQueryTimeout().orElseGet(() -> Duration.seconds(5)),
                jdbi, validationQuery));

        return jdbi;
    }
}
