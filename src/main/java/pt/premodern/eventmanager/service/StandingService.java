package pt.premodern.eventmanager.service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import pt.premodern.eventmanager.model.Event;
import pt.premodern.eventmanager.model.Match;
import pt.premodern.eventmanager.model.Player;
import pt.premodern.eventmanager.model.Round;
import pt.premodern.eventmanager.model.Standing;

public class StandingService {
    private static final double OPPONENT_MINIMUM = 0.33;

    public List<Standing> calculateStandings(Event event) {
        return calculateStandingsAfterRound(event, latestSwissRoundNumber(event));
    }

    public List<Standing> calculateStandingsAfterRound(Event event, int roundNumber) {
        Map<UUID, Player> playersById = event.getPlayers().stream()
                .collect(Collectors.toMap(Player::getId, Function.identity()));
        Map<UUID, PlayerStats> stats = calculateStats(event, Math.max(0, roundNumber));

        List<Standing> standings = event.getPlayers().stream()
                .map(player -> {
                    PlayerStats playerStats = statsFor(stats, player);
                    return new Standing(
                            player,
                            0,
                            playerStats.matchPoints,
                            calculateOpponentsMatchWinPercentage(playerStats, stats, playersById),
                            calculateGameWinPercentage(playerStats),
                            calculateOpponentsGameWinPercentage(playerStats, stats, playersById));
                })
                .sorted(Comparator.comparingInt(Standing::getMatchPoints).reversed()
                        .thenComparing(Standing::getOmwPercentage, Comparator.reverseOrder())
                        .thenComparing(Standing::getGwPercentage, Comparator.reverseOrder())
                        .thenComparing(Standing::getOgwPercentage, Comparator.reverseOrder())
                        .thenComparingInt(standing -> standing.getPlayer().getInitialSeed()))
                .toList();

        for (int i = 0; i < standings.size(); i++) {
            standings.get(i).setRank(i + 1);
        }
        return standings;
    }

    public List<TeamStanding> calculateTeamStandings(Event event) {
        return calculateTeamStandingsAfterRound(event, latestSwissRoundNumber(event));
    }

    public List<TeamStanding> calculateTeamStandingsAfterRound(Event event, int roundNumber) {
        Map<Player, Standing> individualStandings = calculateStandingsAfterRound(event, roundNumber).stream()
                .collect(Collectors.toMap(Standing::getPlayer, Function.identity()));
        List<TeamStanding> standings = event.getPlayers().stream()
                .filter(Player::hasTeam)
                .collect(Collectors.groupingBy(Player::getTeam))
                .entrySet()
                .stream()
                .map(entry -> teamStanding(entry.getKey(), entry.getValue(), individualStandings))
                .sorted(Comparator.comparingInt(TeamStanding::matchPoints).reversed()
                        .thenComparing(TeamStanding::omwPercentage, Comparator.reverseOrder())
                        .thenComparing(TeamStanding::gwPercentage, Comparator.reverseOrder())
                        .thenComparing(TeamStanding::ogwPercentage, Comparator.reverseOrder())
                        .thenComparingInt(TeamStanding::seed))
                .toList();

        for (int i = 0; i < standings.size(); i++) {
            standings.get(i).setRank(i + 1);
        }
        return standings;
    }

    public double calculateMatchWinPercentage(Event event, Player player) {
        Map<UUID, PlayerStats> stats = calculateStats(event, latestSwissRoundNumber(event));
        return calculateMatchWinPercentage(statsFor(stats, player));
    }

    private double calculateGameWinPercentage(PlayerStats stats) {
        int totalGames = stats.gamesWon + stats.gamesLost + stats.gamesDrawn;
        if (totalGames == 0) {
            return 0.0;
        }
        return clamp(stats.gamesWon / (double) totalGames);
    }

    private double calculateOpponentsMatchWinPercentage(PlayerStats playerStats, Map<UUID, PlayerStats> statsById,
            Map<UUID, Player> playersById) {
        List<PlayerStats> opponents = playerStats.opponentsIds.stream()
                .filter(playersById::containsKey)
                .map(statsById::get)
                .filter(Objects::nonNull)
                .toList();
        if (opponents.isEmpty()) {
            return 0.0;
        }
        return opponents.stream()
                .mapToDouble(opponent -> Math.max(OPPONENT_MINIMUM, calculateMatchWinPercentage(opponent)))
                .average()
                .orElse(0.0);
    }

    private double calculateOpponentsGameWinPercentage(PlayerStats playerStats, Map<UUID, PlayerStats> statsById,
            Map<UUID, Player> playersById) {
        List<PlayerStats> opponents = playerStats.opponentsIds.stream()
                .filter(playersById::containsKey)
                .map(statsById::get)
                .filter(Objects::nonNull)
                .toList();
        if (opponents.isEmpty()) {
            return 0.0;
        }
        return opponents.stream()
                .mapToDouble(opponent -> Math.max(OPPONENT_MINIMUM, calculateGameWinPercentage(opponent)))
                .average()
                .orElse(0.0);
    }

    private double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private TeamStanding teamStanding(String team, List<Player> players, Map<Player, Standing> individualStandings) {
        int points = players.stream()
                .map(individualStandings::get)
                .filter(Objects::nonNull)
                .mapToInt(Standing::getMatchPoints)
                .sum();
        double omw = average(players, individualStandings, TieBreaker.OMW);
        double gw = average(players, individualStandings, TieBreaker.GW);
        double ogw = average(players, individualStandings, TieBreaker.OGW);
        int seed = players.stream().mapToInt(Player::getInitialSeed).min().orElse(Integer.MAX_VALUE);
        return new TeamStanding(0, team, players, points, omw, gw, ogw, seed);
    }

    private double average(List<Player> players, Map<Player, Standing> individualStandings, TieBreaker tieBreaker) {
        return players.stream()
                .map(individualStandings::get)
                .filter(Objects::nonNull)
                .mapToDouble(standing -> switch (tieBreaker) {
                    case OMW -> standing.getOmwPercentage();
                    case GW -> standing.getGwPercentage();
                    case OGW -> standing.getOgwPercentage();
                })
                .average()
                .orElse(0.0);
    }

    private int latestSwissRoundNumber(Event event) {
        return event.getRounds().stream()
                .filter(round -> !round.isPlayoffRound())
                .mapToInt(Round::getNumber)
                .max()
                .orElse(0);
    }

    private Map<UUID, PlayerStats> calculateStats(Event event, int roundNumber) {
        Map<UUID, PlayerStats> stats = event.getPlayers().stream()
                .collect(Collectors.toMap(Player::getId, player -> new PlayerStats()));

        for (Round round : event.getRounds()) {
            if (round.isPlayoffRound() || round.getNumber() > roundNumber) {
                continue;
            }
            for (Match match : round.getMatches()) {
                if (!match.isCompleted()) {
                    continue;
                }
                if (match.isBye()) {
                    PlayerStats playerStats = statsFor(stats, match.getPlayer1());
                    playerStats.matchPoints += 3;
                    playerStats.matchesPlayed++;
                    continue;
                }
                applyMatchStats(match, stats);
            }
        }
        return stats;
    }

    private void applyMatchStats(Match match, Map<UUID, PlayerStats> stats) {
        Player p1 = match.getPlayer1();
        Player p2 = match.getPlayer2();
        if (p1 == null || p2 == null) {
            return;
        }

        PlayerStats p1Stats = statsFor(stats, p1);
        PlayerStats p2Stats = statsFor(stats, p2);
        p1Stats.matchesPlayed++;
        p2Stats.matchesPlayed++;

        p1Stats.gamesWon += match.getPlayer1GamesWon();
        p1Stats.gamesLost += match.getPlayer2GamesWon();
        p1Stats.gamesDrawn += match.getDrawGames();
        p2Stats.gamesWon += match.getPlayer2GamesWon();
        p2Stats.gamesLost += match.getPlayer1GamesWon();
        p2Stats.gamesDrawn += match.getDrawGames();

        if (match.getPlayer1GamesWon() > match.getPlayer2GamesWon()) {
            p1Stats.matchPoints += 3;
        } else if (match.getPlayer2GamesWon() > match.getPlayer1GamesWon()) {
            p2Stats.matchPoints += 3;
        } else {
            p1Stats.matchPoints++;
            p2Stats.matchPoints++;
        }
        p1Stats.opponentsIds.add(p2.getId());
        p2Stats.opponentsIds.add(p1.getId());
    }

    private PlayerStats statsFor(Map<UUID, PlayerStats> stats, Player player) {
        return stats.computeIfAbsent(player.getId(), id -> new PlayerStats());
    }

    private double calculateMatchWinPercentage(PlayerStats stats) {
        if (stats.matchesPlayed == 0) {
            return 0.0;
        }
        return clamp(stats.matchPoints / (stats.matchesPlayed * 3.0));
    }

    private enum TieBreaker {
        OMW,
        GW,
        OGW
    }

    private static class PlayerStats {
        private int matchPoints;
        private int gamesWon;
        private int gamesLost;
        private int gamesDrawn;
        private int matchesPlayed;
        private final List<UUID> opponentsIds = new java.util.ArrayList<>();
    }

    public static class TeamStanding {
        private int rank;
        private final String team;
        private final List<Player> players;
        private final int matchPoints;
        private final double omwPercentage;
        private final double gwPercentage;
        private final double ogwPercentage;
        private final int seed;

        private TeamStanding(int rank, String team, List<Player> players, int matchPoints,
                double omwPercentage, double gwPercentage, double ogwPercentage, int seed) {
            this.rank = rank;
            this.team = team;
            this.players = players;
            this.matchPoints = matchPoints;
            this.omwPercentage = omwPercentage;
            this.gwPercentage = gwPercentage;
            this.ogwPercentage = ogwPercentage;
            this.seed = seed;
        }

        public int rank() {
            return rank;
        }

        public void setRank(int rank) {
            this.rank = rank;
        }

        public String team() {
            return team;
        }

        public List<Player> players() {
            return players;
        }

        public int matchPoints() {
            return matchPoints;
        }

        public double omwPercentage() {
            return omwPercentage;
        }

        public double gwPercentage() {
            return gwPercentage;
        }

        public double ogwPercentage() {
            return ogwPercentage;
        }

        public int seed() {
            return seed;
        }
    }
}
