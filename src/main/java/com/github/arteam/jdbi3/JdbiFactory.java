package com.github.arteam.jdbi3;

import io.dropwizard.db.ManagedDataSource;
import io.dropwizard.db.PooledDataSourceFactory;
import io.dropwizard.setup.Environment;
import io.dropwizard.util.Duration;
import org.jdbi.v3.Jdbi;

/**
 * @author Artem Prigoda
 * @since 28.07.16
 */
public class JdbiFactory {

    public Jdbi build(Environment environment,
                      PooledDataSourceFactory configuration,
                      String name) {
        ManagedDataSource dataSource = configuration.build(environment.metrics(), name);
        String validationQuery = configuration.getValidationQuery();
        Jdbi jdbi = Jdbi.create(dataSource);
        jdbi.installPlugins();

        environment.lifecycle().manage(dataSource);
        environment.healthChecks().register(name, new JdbiHealthCheck(
                environment.getHealthCheckExecutorService(),
                configuration.getValidationQueryTimeout().orElseGet(() -> Duration.seconds(5)),
                jdbi, validationQuery));
        return jdbi;
    }
}
