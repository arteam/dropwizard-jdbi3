package com.github.arteam.jdbi3;


import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.jersey.validation.Validators;
import io.dropwizard.logging.BootstrapLogging;
import io.dropwizard.setup.Environment;
import org.eclipse.jetty.util.component.LifeCycle;
import org.jdbi.v3.Jdbi;
import org.jdbi.v3.Query;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.sql.Timestamp;
import java.sql.Types;
import java.util.Optional;

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
    private PersonDAO dao;

    @Before
    public void setUp() throws Exception {
        environment = new Environment("test", new ObjectMapper(), Validators.newValidator(),
                new MetricRegistry(), ClassLoader.getSystemClassLoader());

        DataSourceFactory dataSourceFactory = new DataSourceFactory();
        dataSourceFactory.setUrl("jdbc:h2:mem:jdbi3-" + System.currentTimeMillis());
        dataSourceFactory.setUser("sa");
        dataSourceFactory.setDriverClass("org.h2.Driver");

        dbi = new JdbiFactory().build(environment, dataSourceFactory, "hsql");

        dbi.useHandle(h -> {
            h.createStatement("DROP TABLE people IF EXISTS").execute();
            h.createStatement(
                    "CREATE TABLE people (name varchar(100) primary key, email varchar(100), age int, created_at timestamp)")
                    .execute();
            h.createStatement("INSERT INTO people VALUES (?, ?, ?, ?)")
                    .bind(0, "Coda Hale")
                    .bind(1, "chale@yammer-inc.com")
                    .bind(2, 30)
                    .bind(3, new Timestamp(1365465078000L))
                    .execute();
            h.createStatement("INSERT INTO people VALUES (?, ?, ?, ?)")
                    .bind(0, "Kris Gale")
                    .bind(1, "kgale@yammer-inc.com")
                    .bind(2, 32)
                    .bind(3, new Timestamp(1365465078000L))
                    .execute();
            h.createStatement("INSERT INTO people VALUES (?, ?, ?, ?)")
                    .bind(0, "Old Guy")
                    .bindNull(1, Types.VARCHAR)
                    .bind(2, 99)
                    .bind(3, new Timestamp(1365465078000L))
                    .execute();
            h.createStatement("INSERT INTO people VALUES (?, ?, ?, ?)")
                    .bind(0, "Alice Example")
                    .bind(1, "alice@example.org")
                    .bind(2, 99)
                    .bindNull(3, Types.TIMESTAMP)
                    .execute();
        });
        dao = dbi.onDemand(PersonDAO.class);

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

    @Test
    public void createsAValidDBI() {
        dbi.useHandle(h -> {
            Query<String> names = h.createQuery("SELECT name FROM people WHERE age < ?")
                    .bind(0, 50)
                    .mapTo(String.class);
            assertThat(names).containsOnly("Coda Hale", "Kris Gale");
        });
    }

    @Test
    public void sqlObjectsCanAcceptOptionalParams() {
        assertThat(dao.findByName(Optional.of("Coda Hale")))
                .isEqualTo("Coda Hale");
    }

    @Test
    public void sqlObjectsCanReturnImmutableLists() {
        assertThat(dao.findAllNames())
                .containsOnly("Coda Hale", "Kris Gale", "Old Guy", "Alice Example");
    }

    @Test
    public void sqlObjectsCanReturnImmutableSets() {
        assertThat(dao.findAllUniqueNames())
                .containsOnly("Coda Hale", "Kris Gale", "Old Guy", "Alice Example");
    }

    @Test
    public void sqlObjectsCanReturnOptional() {
        Optional<String> found = dao.findByEmail("chale@yammer-inc.com");
        assertThat(found).isNotNull();
        assertThat(found.isPresent()).isTrue();
        assertThat(found.get()).isEqualTo("Coda Hale");


        Optional<String> missing = dao.findByEmail("cemalettin.koc@gmail.com");
        assertThat(missing).isNotNull();
        assertThat(missing.isPresent()).isFalse();
        assertThat(missing.orElse(null)).isNull();
    }

    @Test
    public void sqlObjectsCanReturnJodaDateTime() {
        DateTime found = dao.getLatestCreatedAt(new DateTime(1365465077000L));
        assertThat(found).isNotNull();
        assertThat(found.getMillis()).isEqualTo(1365465078000L);
        assertThat(found).isEqualTo(new DateTime(1365465078000L));

        DateTime notFound = dao.getCreatedAtByEmail("alice@example.org");
        assertThat(notFound).isNull();

        Optional<DateTime> absentDateTime = dao.getCreatedAtByName("Alice Example");
        assertThat(absentDateTime).isNotNull();
        assertThat(absentDateTime.isPresent()).isFalse();

        Optional<DateTime> presentDateTime = dao.getCreatedAtByName("Coda Hale");
        assertThat(presentDateTime).isNotNull();
        assertThat(presentDateTime.isPresent()).isTrue();
    }
}