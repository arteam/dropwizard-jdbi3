package com.github.arteam.jdbi3;

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.stringtemplate4.UseStringTemplateSqlLocator;

import java.time.LocalDate;
import java.util.Optional;

@UseStringTemplateSqlLocator
public interface GameDao {

    @SqlQuery
    ImmutableList<Integer> findGameIds();

    @SqlQuery
    ImmutableSet<String> findAllUniqueHomeTeams();

    @SqlQuery
    Optional<Integer> findIdByTeamsAndDate(@Bind("home_team") String homeTeam,
                                           @Bind("visitor_team") String visitorTeam,
                                           @Bind("played_at") LocalDate date);

    @SqlQuery
    LocalDate getFirstPlayedSince(@Bind("up") LocalDate up);

    @SqlQuery
    @Timed(name = "get-last-played")
    Optional<LocalDate> getLastPlayedDateByTeams(@Bind("home_team") String homeTeam,
                                                 @Bind("visitor_team") String visitorTeam);

    @SqlQuery
    Optional<String> findHomeTeamByGameId(@Bind("id") Optional<Integer> id);
}
