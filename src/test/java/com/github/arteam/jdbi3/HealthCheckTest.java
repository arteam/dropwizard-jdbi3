package com.github.arteam.jdbi3;

import com.codahale.metrics.health.HealthCheck.Result;
import com.google.common.util.concurrent.MoreExecutors;
import io.dropwizard.util.Duration;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.MappingException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class HealthCheckTest {

    private JdbiHealthCheck healthCheck;

    @Mock
    private Jdbi jdbi;

    @Mock
    private Handle h;

    @Before
    public void init() {
        when(jdbi.open()).thenReturn(h);
        healthCheck = new JdbiHealthCheck(MoreExecutors.newDirectExecutorService(), Duration.seconds(5), jdbi,
                "select 1");
    }

    @Test
    public void shouldReturnNotHealthyBecauseOfErrorOnError() throws Exception {
        when(h.execute("select 1")).thenThrow(new MappingException("bad error here"));

        Result check = healthCheck.check();
        assertThat(check).isNotNull()
                .extracting(Result::getMessage)
                .containsOnly("bad error here");
    }
}
