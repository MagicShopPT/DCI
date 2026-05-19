package pt.premodern.eventmanager.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import pt.premodern.eventmanager.model.Event;
import pt.premodern.eventmanager.model.EventStatus;
import pt.premodern.eventmanager.model.Match;
import pt.premodern.eventmanager.model.Player;
import pt.premodern.eventmanager.model.Round;

public class PairingService {
    private final Random random = new Random();
    private final ResultService resultService = new ResultService();

    public Round generateNextSwissRound(Event event) {
        prepareForGeneration(event);
        validateCanGenerate(event);
        if (event.getTotalSwissRounds() <= 0) {
            event.setTotalSwissRounds(new EventService().calculateSwissRounds(event.getPlayers().size()));
        }

        int roundNumber = event.getCurrentRoundNumber() + 1;
        List<Player> activePlayers = getActivePlayers(event);
        if (activePlayers.size() < 2) {
            throw new IllegalStateException("São necessários pelo menos 2 jogadores ativos.");
        }

        if (roundNumber == 1) {
            Collections.shuffle(activePlayers, random);
        } else {
            activePlayers.sort(Comparator.comparingInt(Player::getMatchPoints).reversed()
                    .thenComparingInt(Player::getInitialSeed));
        }

        Round round = new Round(roundNumber, false);
        int table = 1;
        if (activePlayers.size() % 2 != 0) {
            Player byePlayer = roundNumber == 1 ? selectRandomByePlayer(activePlayers) : selectByePlayer(activePlayers);
            byePlayer.setReceivedBye(true);
            byePlayer.addMatchPoints(3);
            Match bye = Match.bye(table++, byePlayer);
            bye.setPlayer1GamesWon(2);
            round.getMatches().add(bye);
            activePlayers.remove(byePlayer);
        }

        List<Match> pairings = buildPairings(activePlayers);
        for (Match match : pairings) {
            match.setTableNumber(table++);
            round.getMatches().add(match);
        }
        round.updateCompleted();

        event.getRounds().add(round);
        event.setCurrentRoundNumber(roundNumber);
        event.setStatus(EventStatus.SWISS_IN_PROGRESS);
        return round;
    }

    private void prepareForGeneration(Event event) {
        if (event == null) {
            return;
        }
        Round current = event.getCurrentRound();
        if (!canRegenerateCurrentRound(current)) {
            return;
        }
        event.getRounds().remove(current);
        event.setCurrentRoundNumber(Math.max(0, current.getNumber() - 1));
        resultService.recalculateEvent(event);
        event.setStatus(event.getRounds().isEmpty() ? EventStatus.CREATED : EventStatus.SWISS_IN_PROGRESS);
    }

    public List<Player> getActivePlayers(Event event) {
        return new ArrayList<>(event.getPlayers().stream()
                .filter(player -> !player.isDropped())
                .toList());
    }

    private void validateCanGenerate(Event event) {
        if (event == null) {
            throw new IllegalStateException("Não existe evento ativo.");
        }
        if (event.getStatus() == EventStatus.FINISHED || event.getStatus() == EventStatus.TOP_CUT_IN_PROGRESS) {
            throw new IllegalStateException("O evento já não aceita rondas suíças.");
        }
        if (event.getCurrentRoundNumber() >= event.getTotalSwissRounds() && event.getTotalSwissRounds() > 0) {
            throw new IllegalStateException("Todas as rondas suíças já foram geradas.");
        }
        Round current = event.getCurrentRound();
        if (current != null && !current.isCompleted()) {
            throw new IllegalStateException("A ronda atual ainda não está completa.");
        }
        if (event.getPlayers().size() < 2) {
            throw new IllegalStateException("São necessários pelo menos 2 jogadores.");
        }
        for (int i = 0; i < event.getPlayers().size(); i++) {
            if (event.getPlayers().get(i).getInitialSeed() <= 0) {
                event.getPlayers().get(i).setInitialSeed(i + 1);
            }
        }
    }

    private Player selectRandomByePlayer(List<Player> players) {
        return players.get(random.nextInt(players.size()));
    }

    private Player selectByePlayer(List<Player> players) {
        return players.stream()
                .filter(player -> !player.isReceivedBye())
                .min(Comparator.comparingInt(Player::getMatchPoints)
                        .thenComparingInt(Player::getInitialSeed))
                .orElseGet(() -> players.stream()
                        .min(Comparator.comparingInt(Player::getMatchPoints)
                                .thenComparingInt(Player::getInitialSeed))
                        .orElseThrow());
    }

    private boolean alreadyPlayed(Player a, Player b) {
        return a != null && b != null
                && (a.getOpponentsIds().contains(b.getId()) || b.getOpponentsIds().contains(a.getId()));
    }

    private boolean sameTeam(Player a, Player b) {
        return a != null && b != null
                && a.hasTeam()
                && b.hasTeam()
                && a.getTeam().equalsIgnoreCase(b.getTeam());
    }

    private int pairingPenalty(Player a, Player b) {
        int penalty = Math.abs(a.getMatchPoints() - b.getMatchPoints()) * 100;
        if (sameTeam(a, b)) {
            penalty += 1000;
        }
        if (alreadyPlayed(a, b)) {
            penalty += 10000;
        }
        return penalty;
    }

    private List<Match> buildPairings(List<Player> players) {
        List<Player> available = new ArrayList<>(players);
        List<Match> matches = new ArrayList<>();

        while (available.size() >= 2) {
            Player player = available.remove(0);
            Player opponent = chooseBestOpponent(player, available);
            available.remove(opponent);
            matches.add(new Match(matches.size() + 1, player, opponent));
        }
        return matches;
    }

    private Player chooseBestOpponent(Player player, List<Player> available) {
        List<Player> preferred = available.stream()
                .filter(candidate -> !sameTeam(player, candidate))
                .toList();
        List<Player> candidates = preferred.isEmpty() ? available : preferred;
        return candidates.stream()
                .min(Comparator.comparingInt((Player candidate) -> pairingPenalty(player, candidate))
                        .thenComparingInt(candidate -> Math.abs(player.getMatchPoints() - candidate.getMatchPoints()))
                        .thenComparing(candidate -> sameTeam(player, candidate))
                        .thenComparingInt(Player::getInitialSeed))
                .orElseThrow();
    }

    private boolean canRegenerateCurrentRound(Round round) {
        return round != null
                && !round.isPlayoffRound()
                && !round.isCompleted()
                && round.getMatches().stream().noneMatch(this::hasEnteredResult);
    }

    private boolean hasEnteredResult(Match match) {
        return !match.isBye()
                && (match.isCompleted()
                || match.getPlayer1GamesWon() > 0
                || match.getPlayer2GamesWon() > 0
                || match.getDrawGames() > 0);
    }
}
