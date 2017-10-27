package com.github.arteam.jdbi3;

import com.codahale.metrics.health.HealthCheck;
import io.dropwizard.db.TimeBoundHealthCheck;
import io.dropwizard.util.Duration;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;

/**
 * Health check which verifies the availability of the database
 */
public class JdbiHealthCheck extends HealthCheck {
    private static final Logger log = LoggerFactory.getLogger(JdbiHealthCheck.class);

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
            } catch (Exception e) {
                log.error("JDBI Health check failed. Validation query={}", validationQuery, e);
                return Result.unhealthy(e);
            }
        });
    }
}
