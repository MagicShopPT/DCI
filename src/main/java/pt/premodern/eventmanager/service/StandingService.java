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
        Map<UUID, Player> playersById = event.getPlayers().stream()
                .collect(Collectors.toMap(Player::getId, Function.identity()));

        List<Standing> standings = event.getPlayers().stream()
                .map(player -> new Standing(
                        player,
                        0,
                        player.getMatchPoints(),
                        calculateOpponentsMatchWinPercentage(event, player, playersById),
                        calculateGameWinPercentage(player),
                        calculateOpponentsGameWinPercentage(player, playersById)))
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
        Map<Player, Standing> individualStandings = calculateStandings(event).stream()
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
        int matchesPlayed = countMatchesPlayed(event, player);
        if (matchesPlayed == 0) {
            return 0.0;
        }
        return clamp(player.getMatchPoints() / (matchesPlayed * 3.0));
    }

    private double calculateGameWinPercentage(Player player) {
        int totalGames = player.getGamesWon() + player.getGamesLost() + player.getGamesDrawn();
        if (totalGames == 0) {
            return 0.0;
        }
        return clamp(player.getGamesWon() / (double) totalGames);
    }

    private double calculateOpponentsMatchWinPercentage(Event event, Player player, Map<UUID, Player> playersById) {
        List<Player> opponents = player.getOpponentsIds().stream()
                .map(playersById::get)
                .filter(Objects::nonNull)
                .toList();
        if (opponents.isEmpty()) {
            return 0.0;
        }
        return opponents.stream()
                .mapToDouble(opponent -> Math.max(OPPONENT_MINIMUM, calculateMatchWinPercentage(event, opponent)))
                .average()
                .orElse(0.0);
    }

    private double calculateOpponentsGameWinPercentage(Player player, Map<UUID, Player> playersById) {
        List<Player> opponents = player.getOpponentsIds().stream()
                .map(playersById::get)
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

    private int countMatchesPlayed(Event event, Player player) {
        int count = 0;
        for (Round round : event.getRounds()) {
            if (round.isPlayoffRound()) {
                continue;
            }
            for (Match match : round.getMatches()) {
                if (match.isCompleted()
                        && ((match.getPlayer1() != null && Objects.equals(match.getPlayer1().getId(), player.getId()))
                        || (match.getPlayer2() != null && Objects.equals(match.getPlayer2().getId(), player.getId())))) {
                    count++;
                }
            }
        }
        return count;
    }

    private double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private TeamStanding teamStanding(String team, List<Player> players, Map<Player, Standing> individualStandings) {
        int points = players.stream().mapToInt(Player::getMatchPoints).sum();
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

    private enum TieBreaker {
        OMW,
        GW,
        OGW
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
