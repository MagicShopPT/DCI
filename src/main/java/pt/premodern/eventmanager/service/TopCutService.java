package pt.premodern.eventmanager.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import pt.premodern.eventmanager.model.Event;
import pt.premodern.eventmanager.model.EventStatus;
import pt.premodern.eventmanager.model.Match;
import pt.premodern.eventmanager.model.Player;
import pt.premodern.eventmanager.model.Round;
import pt.premodern.eventmanager.model.Standing;

public class TopCutService {
    private final StandingService standingService = new StandingService();

    public Round createInitialTopCutRound(Event event) {
        if (!event.isHasTopCut()) {
            throw new IllegalStateException("This event does not have Top Cut configured.");
        }
        int eligibleCount = event.isTeamEvent() ? countEligibleTeams(event) : event.getPlayers().size();
        if (event.getTopCutSize() > eligibleCount) {
            throw new IllegalStateException(event.isTeamEvent()
                    ? "The Top Cut cannot be larger than the number of teams."
                    : "The Top Cut cannot be larger than the number of players.");
        }
        if (event.getCurrentRoundNumber() < event.getTotalSwissRounds()) {
            throw new IllegalStateException("There are still Swiss rounds to play.");
        }
        Round current = event.getCurrentRound();
        if (current != null && !current.isCompleted()) {
            throw new IllegalStateException("The current round is not complete yet.");
        }
        if (event.getStatus() == EventStatus.TOP_CUT_IN_PROGRESS || event.getStatus() == EventStatus.FINISHED) {
            throw new IllegalStateException("The Top Cut has already been created.");
        }

        List<Player> cutPlayers = event.isTeamEvent()
                ? calculateTeamCut(event)
                : standingService.calculateStandings(event).stream()
                        .limit(event.getTopCutSize())
                        .map(Standing::getPlayer)
                        .toList();
        Round round = buildSeededRound(event.getCurrentRoundNumber() + 1, cutPlayers);
        event.getRounds().add(round);
        event.setCurrentRoundNumber(round.getNumber());
        event.setStatus(EventStatus.TOP_CUT_IN_PROGRESS);
        return round;
    }

    public Round generateNextTopCutRound(Event event) {
        Round current = event.getCurrentRound();
        if (current == null || !current.isPlayoffRound()) {
            throw new IllegalStateException("There is no active Top Cut round.");
        }
        if (!current.isCompleted()) {
            throw new IllegalStateException("The current Top Cut round is not complete yet.");
        }

        List<Player> winners = current.getMatches().stream()
                .map(Match::getWinner)
                .toList();
        if (winners.size() == 1) {
            event.setStatus(EventStatus.FINISHED);
            return null;
        }
        Round nextRound = buildSeededRound(current.getNumber() + 1, winners);
        event.getRounds().add(nextRound);
        event.setCurrentRoundNumber(nextRound.getNumber());
        return nextRound;
    }

    public Player getChampion(Event event) {
        if (event.getStatus() != EventStatus.FINISHED || event.getRounds().isEmpty()) {
            return null;
        }
        Round round = event.getCurrentRound();
        if (round == null || round.getMatches().isEmpty()) {
            return null;
        }
        return round.getMatches().get(0).getWinner();
    }

    private Round buildSeededRound(int number, List<Player> players) {
        Round round = new Round(number, true);
        int size = players.size();
        for (int i = 0; i < size / 2; i++) {
            round.getMatches().add(new Match(i + 1, players.get(i), players.get(size - 1 - i)));
        }
        return round;
    }

    private List<Player> calculateTeamCut(Event event) {
        Map<String, List<Player>> playersByTeam = event.getPlayers().stream()
                .filter(Player::hasTeam)
                .collect(Collectors.groupingBy(Player::getTeam));
        if (playersByTeam.isEmpty()) {
            throw new IllegalStateException("For team Top Cut, players must have a team filled in.");
        }

        Map<Player, Standing> individualStandings = standingService.calculateStandings(event).stream()
                .collect(Collectors.toMap(Standing::getPlayer, standing -> standing));

        List<TeamStanding> teamStandings = new ArrayList<>();
        for (Map.Entry<String, List<Player>> entry : playersByTeam.entrySet()) {
            List<Player> players = entry.getValue();
            int points = players.stream().mapToInt(Player::getMatchPoints).sum();
            double omw = average(players, individualStandings, TieBreaker.OMW);
            double gw = average(players, individualStandings, TieBreaker.GW);
            double ogw = average(players, individualStandings, TieBreaker.OGW);
            int seed = players.stream().mapToInt(Player::getInitialSeed).min().orElse(Integer.MAX_VALUE);
            teamStandings.add(new TeamStanding(entry.getKey(), points, omw, gw, ogw, seed));
        }

        return teamStandings.stream()
                .sorted(Comparator.comparingInt(TeamStanding::points).reversed()
                        .thenComparing(TeamStanding::omw, Comparator.reverseOrder())
                        .thenComparing(TeamStanding::gw, Comparator.reverseOrder())
                        .thenComparing(TeamStanding::ogw, Comparator.reverseOrder())
                        .thenComparingInt(TeamStanding::seed))
                .limit(event.getTopCutSize())
                .map(this::virtualTeamPlayer)
                .toList();
    }

    private int countEligibleTeams(Event event) {
        return (int) event.getPlayers().stream()
                .filter(Player::hasTeam)
                .map(player -> player.getTeam().toLowerCase())
                .distinct()
                .count();
    }

    private double average(List<Player> players, Map<Player, Standing> standings, TieBreaker tieBreaker) {
        return players.stream()
                .map(standings::get)
                .filter(standing -> standing != null)
                .mapToDouble(standing -> switch (tieBreaker) {
                    case OMW -> standing.getOmwPercentage();
                    case GW -> standing.getGwPercentage();
                    case OGW -> standing.getOgwPercentage();
                })
                .average()
                .orElse(0.0);
    }

    private Player virtualTeamPlayer(TeamStanding teamStanding) {
        Player player = new Player("Team", teamStanding.name(), "", teamStanding.name());
        player.setMatchPoints(teamStanding.points());
        player.setInitialSeed(teamStanding.seed());
        return player;
    }

    private enum TieBreaker {
        OMW,
        GW,
        OGW
    }

    private record TeamStanding(String name, int points, double omw, double gw, double ogw, int seed) {
    }
}
