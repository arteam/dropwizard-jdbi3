package com.github.arteam.jdbi3;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.arteam.jdbi3.strategies.TimedAnnotationNameStrategy;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.jersey.validation.Validators;
import io.dropwizard.logging.BootstrapLogging;
import io.dropwizard.setup.Environment;
import org.eclipse.jetty.util.component.LifeCycle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.extension.NoSuchExtensionException;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class JdbiPluginTest {

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
        dataSourceFactory.setUrl("jdbc:h2:mem:jdbi3-test");
        dataSourceFactory.setUser("sa");
        dataSourceFactory.setDriverClass("org.h2.Driver");
        dataSourceFactory.asSingleConnectionPool();

        dbi = new JdbiFactory(false).build(environment, dataSourceFactory, "h2");
        dbi.useTransaction(h -> {
            h.createScript(Resources.toString(Resources.getResource("schema.sql"), Charsets.UTF_8)).execute();
            h.createScript(Resources.toString(Resources.getResource("data.sql"), Charsets.UTF_8)).execute();
        });
        for (LifeCycle lc : environment.lifecycle().getManagedObjects()) {
            lc.start();
        }
    }

    @After
    public void tearDown() throws Exception {
        for (LifeCycle lc : environment.lifecycle().getManagedObjects()) {
            lc.stop();
        }
    }

    @Test(expected = NoSuchExtensionException.class)
    public void sqlObjectPluginNotInstalled() {
        dbi.onDemand(GameDao.class);
    }

    @Test
    public void fluentQueryWorks() {
        dbi.useHandle(h -> assertThat(h.createQuery("SELECT id FROM games " +
                "WHERE home_scored>visitor_scored " +
                "AND played_at > :played_at")
                .bind("played_at", LocalDate.of(2016, 2, 15))
                .mapTo(Integer.class)
                .collect(Collectors.toList())).containsOnly(2, 5));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void jodaPluginNotInstalledEither() {
        dbi.useHandle(h -> assertThat(h.createQuery("SELECT played_at FROM games " +
                "WHERE home_scored > visitor_scored " +
                "AND played_at > :played_at")
                .bind("played_at", org.joda.time.LocalDate.parse("2016-02-15").toDateTimeAtStartOfDay())
                .mapTo(DateTime.class)
                .stream()
                .map(DateTime::toLocalDate)
                .collect(Collectors.toList())).containsOnly(
                org.joda.time.LocalDate.parse("2016-05-14"), org.joda.time.LocalDate.parse("2016-03-10")));
    }
}