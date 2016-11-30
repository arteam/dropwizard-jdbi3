package com.github.arteam.jdbi3;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.jdbi.v3.sqlobject.Bind;
import org.jdbi.v3.sqlobject.SqlQuery;

import java.time.LocalDate;
import java.util.Optional;

public interface GameDao {

    @SqlQuery("select id from games order by id")
    ImmutableList<Integer> findGameIds();

    @SqlQuery("select distinct home_team from games")
    ImmutableSet<String> findAllUniqueHomeTeams();

    @SqlQuery("select id " +
            "from games " +
            "where home_team = :home_team " +
            "and visitor_team = visitor_team " +
            "and played_at = :played_at")
    Optional<Integer> findIdByTeamsAndDate(@Bind("home_team") String homeTeam,
                                           @Bind("visitor_team") String visitorTeam,
                                           @Bind("played_at") LocalDate date);

    @SqlQuery("select played_at " +
            "from games " +
            "where played_at < :up " +
            "order by played_at desc " +
            "limit 1")
    LocalDate getFirstPlayedSince(@Bind("up") LocalDate up);

    @SqlQuery("select played_at " +
            "from games " +
            "where home_team = :home_team " +
            "and visitor_team = visitor_team " +
            "order by played_at desc " +
            "limit 1")
    Optional<LocalDate> getLastPlayedDateByTeams(@Bind("home_team") String homeTeam,
                                             @Bind("visitor_team") String visitorTeam);

    @SqlQuery("select home_team from games where id=:id")
    Optional<String> findHomeTeamByGameId(@Bind("id") Optional<Integer> id);
}
