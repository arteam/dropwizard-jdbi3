package com.github.arteam.jdbi3;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.jersey.validation.Validators;
import io.dropwizard.logging.BootstrapLogging;
import io.dropwizard.setup.Environment;
import org.eclipse.jetty.util.component.LifeCycle;
import org.jdbi.v3.core.Jdbi;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Artem Prigoda
 * @since 28.07.16
 */
public class JdbiTest {

    static {
        BootstrapLogging.bootstrap();
    }

    private Environment environment;

    private Jdbi dbi;
    private GameDao dao;
    private MetricRegistry metricRegistry = new MetricRegistry();

    @Before
    public void setUp() throws Exception {
        environment = new Environment("test", new ObjectMapper(), Validators.newValidator(),
                metricRegistry, ClassLoader.getSystemClassLoader());

        DataSourceFactory dataSourceFactory = new DataSourceFactory();
        dataSourceFactory.setUrl("jdbc:h2:mem:jdbi3-" + System.currentTimeMillis());
        dataSourceFactory.setUser("sa");
        dataSourceFactory.setDriverClass("org.h2.Driver");

        dbi = new JdbiFactory().build(environment, dataSourceFactory, "hsql");

        dbi.useHandle(h -> {
            h.createScript(Resources.toString(Resources.getResource("schema.sql"), Charsets.UTF_8)).execute();
            h.createScript(Resources.toString(Resources.getResource("data.sql"), Charsets.UTF_8)).execute();
        });
        dao = dbi.onDemand(GameDao.class);

        for (LifeCycle lc : environment.lifecycle().getManagedObjects()) {
            lc.start();
        }
    }

    @After
    public void tearDown() throws Exception {
        for (LifeCycle lc : environment.lifecycle().getManagedObjects()) {
            lc.stop();
        }
        System.out.println(metricRegistry.getTimers());
    }

    @Test
    public void fluentQueryWorks() {
        dbi.useHandle(h -> assertThat(h.createQuery("select id from games " +
                "where home_scored>visitor_scored " +
                "and played_at > :played_at")
                .bind("played_at", LocalDate.of(2016, 2, 15))
                .mapTo(Integer.class)
                .collect(Collectors.toList())).containsOnly(2, 5));
    }

    @Test
    public void canAcceptOptionalParams() {
        assertThat(dao.findHomeTeamByGameId(Optional.of(4))).contains("Dallas Stars");
    }

    @Test
    public void canAcceptEmptyOptionalParams() {
        assertThat(dao.findHomeTeamByGameId(Optional.empty())).isEmpty();
    }

    @Test
    public void canReturnImmutableLists() {
        assertThat(dao.findGameIds()).containsExactly(1, 2, 3, 4, 5);
    }

    @Test
    public void canReturnImmutableSets() {
        assertThat(dao.findAllUniqueHomeTeams()).containsOnly("NY Rangers", "Toronto Maple Leafs", "Dallas Stars");
    }

    @Test
    public void canReturnOptional() {
        Optional<Integer> id = dao.findIdByTeamsAndDate("NY Rangers", "Vancouver Canucks",
                LocalDate.of(2016, 5, 14));
        assertThat(id).contains(2);
    }

    @Test
    public void canReturnEmptyOptional() {
        Optional<Integer> id = dao.findIdByTeamsAndDate("Vancouver Canucks", "NY Rangers",
                LocalDate.of(2016, 5, 14));
        assertThat(id).isEmpty();
    }

    @Test
    public void worksWithDates() {
        LocalDate date = dao.getFirstPlayedSince(LocalDate.of(2016, 3, 1));
        assertThat(date).isEqualTo(LocalDate.of(2016, 2, 15));
    }

    @Test
    public void worksWithOptionalDates() {
        Optional<LocalDate> date = dao.getLastPlayedDateByTeams("NY Rangers", "Vancouver Canucks");
        assertThat(date).contains(LocalDate.of(2016, 5, 14));
    }

    @Test
    public void worksWithAbsentOptionalDates() {
        Optional<LocalDate> date = dao.getLastPlayedDateByTeams("Vancouver Canucks", "NY Rangers");
        assertThat(date).isEmpty();
    }
}