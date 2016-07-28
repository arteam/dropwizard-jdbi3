package com.github.arteam.jdbi3;

import com.codahale.metrics.health.HealthCheck;
import io.dropwizard.db.TimeBoundHealthCheck;
import io.dropwizard.util.Duration;
import org.jdbi.v3.Handle;
import org.jdbi.v3.Jdbi;

import java.util.concurrent.ExecutorService;

/**
 * @author Artem Prigoda
 * @since 28.07.16
 */
public class JdbiHealthCheck extends HealthCheck {

    private final Jdbi jdbi;
    private final String validationQuery;
    private final TimeBoundHealthCheck timeBoundHealthCheck;

    public JdbiHealthCheck(ExecutorService executorService, Duration duration, Jdbi dbi, String validationQuery) {
        this.jdbi = dbi;
        this.validationQuery = validationQuery;
        this.timeBoundHealthCheck = new TimeBoundHealthCheck(executorService, duration);
    }

    @Override
    protected Result check() throws Exception {
        return timeBoundHealthCheck.check(() -> {
            try (Handle handle = jdbi.open()) {
                handle.execute(validationQuery);
                return Result.healthy();
            }
        });
    }
}
