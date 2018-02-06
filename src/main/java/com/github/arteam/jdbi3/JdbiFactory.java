package com.github.arteam.jdbi3;

import com.github.arteam.jdbi3.strategies.SmartNameStrategy;
import com.github.arteam.jdbi3.strategies.StatementNameStrategy;
import io.dropwizard.db.ManagedDataSource;
import io.dropwizard.db.PooledDataSourceFactory;
import io.dropwizard.setup.Environment;
import io.dropwizard.util.Duration;
import org.jdbi.v3.core.Jdbi;

/**
 * A factory which create a new managed {@link Jdbi} instance based on the Dropwizard configuration.
 */
public class JdbiFactory {

    private StatementNameStrategy nameStrategy;
    private boolean autoInstallPlugins;

    public JdbiFactory() {
        this(new SmartNameStrategy());
    }

    public JdbiFactory(boolean autoInstallPlugins) {
        this(new SmartNameStrategy(), autoInstallPlugins);
    }

    public JdbiFactory(StatementNameStrategy nameStrategy) {
        this(nameStrategy, true);
    }

    public JdbiFactory(StatementNameStrategy nameStrategy, boolean autoInstallPlugins) {
        this.nameStrategy = nameStrategy;
        this.autoInstallPlugins = autoInstallPlugins;
    }

    public Jdbi build(Environment environment,
                      PooledDataSourceFactory configuration,
                      String name) {
        ManagedDataSource dataSource = configuration.build(environment.metrics(), name);
        String validationQuery = configuration.getValidationQuery();
        Jdbi jdbi = Jdbi.create(dataSource);
        jdbi.setTimingCollector(new InstrumentedTimingCollector(environment.metrics(), nameStrategy));
        if (autoInstallPlugins) {
            jdbi.installPlugins();
        }

        environment.lifecycle().manage(dataSource);
        environment.healthChecks().register(name, new JdbiHealthCheck(
                environment.getHealthCheckExecutorService(),
                configuration.getValidationQueryTimeout().orElseGet(() -> Duration.seconds(5)),
                jdbi, validationQuery));

        return jdbi;
    }
}
