package pt.premodern.eventmanager.service;

import java.util.Objects;

import pt.premodern.eventmanager.model.Event;
import pt.premodern.eventmanager.model.Match;
import pt.premodern.eventmanager.model.PenaltyEntry;
import pt.premodern.eventmanager.model.Player;
import pt.premodern.eventmanager.model.Round;

public class PenaltyService {
    public static final String WARNING = "Warning";
    public static final String GAME_LOSS = "Game Loss";
    public static final String MATCH_LOSS = "Match Loss";
    public static final String DISQUALIFICATION = "Disqualification";

    private final EventService eventService = new EventService();

    public PenaltyEntry addPenalty(Event event, Player player, String infraction, String penalty, String description) {
        if (event == null || player == null) {
            throw new IllegalArgumentException("Seleciona um jogador.");
        }
        if (infraction == null || infraction.isBlank()) {
            throw new IllegalArgumentException("Seleciona uma infração.");
        }
        if (penalty == null || penalty.isBlank()) {
            throw new IllegalArgumentException("Seleciona uma penalty.");
        }

        String appliedPenalty = effectivePenalty(player, infraction, penalty);
        validatePenaltyCanApply(event, player, appliedPenalty);
        PenaltyEntry entry = new PenaltyEntry(event.getCurrentRoundNumber(), infraction, penalty, appliedPenalty, clean(description));
        player.getPenaltyEntries().add(entry);
        applyPenalty(event, player, appliedPenalty);
        return entry;
    }

    private String effectivePenalty(Player player, String infraction, String penalty) {
        if (!WARNING.equals(penalty)) {
            return penalty;
        }
        boolean repeated = player.getPenaltyEntries().stream()
                .anyMatch(entry -> infraction.equals(entry.getInfraction()));
        return repeated ? GAME_LOSS : WARNING;
    }

    private void validatePenaltyCanApply(Event event, Player player, String appliedPenalty) {
        if (GAME_LOSS.equals(appliedPenalty) || MATCH_LOSS.equals(appliedPenalty)) {
            Match match = currentMatch(event, player);
            if (match == null || match.isBye() || opponent(match, player) == null) {
                throw new IllegalStateException("Esta penalty precisa de um adversário na ronda atual.");
            }
        }
    }

    private void applyPenalty(Event event, Player player, String appliedPenalty) {
        if (DISQUALIFICATION.equals(appliedPenalty)) {
            Match match = currentMatch(event, player);
            if (match != null && !match.isBye() && opponent(match, player) != null) {
                applyMatchLoss(match, player);
            }
            int roundNumber = Math.max(0, event.getCurrentRoundNumber());
            if (roundNumber > 0) {
                eventService.dropPlayerAtRound(event, player, roundNumber);
            } else {
                eventService.dropPlayer(player);
            }
            return;
        }

        Match match = currentMatch(event, player);
        if (match == null) {
            return;
        }
        if (GAME_LOSS.equals(appliedPenalty)) {
            applyGameLoss(match, player);
        } else if (MATCH_LOSS.equals(appliedPenalty)) {
            applyMatchLoss(match, player);
        }
        Round round = event.getCurrentRound();
        if (round != null) {
            round.updateCompleted();
        }
    }

    private void applyGameLoss(Match match, Player penalized) {
        if (isPlayer1(match, penalized)) {
            match.setPlayer2GamesWon(Math.max(1, match.getPlayer2GamesWon()));
        } else {
            match.setPlayer1GamesWon(Math.max(1, match.getPlayer1GamesWon()));
        }
        if (match.isCompleted()) {
            updateWinner(match);
        }
    }

    private void applyMatchLoss(Match match, Player penalized) {
        if (isPlayer1(match, penalized)) {
            match.setPlayer1GamesWon(0);
            match.setPlayer2GamesWon(2);
            match.setWinner(match.getPlayer2());
        } else {
            match.setPlayer1GamesWon(2);
            match.setPlayer2GamesWon(0);
            match.setWinner(match.getPlayer1());
        }
        match.setDrawGames(0);
        match.setCompleted(true);
    }

    private void updateWinner(Match match) {
        if (match.getPlayer1GamesWon() > match.getPlayer2GamesWon()) {
            match.setWinner(match.getPlayer1());
        } else if (match.getPlayer2GamesWon() > match.getPlayer1GamesWon()) {
            match.setWinner(match.getPlayer2());
        } else {
            match.setWinner(null);
        }
    }

    private Match currentMatch(Event event, Player player) {
        Round round = event.getCurrentRound();
        if (round == null) {
            return null;
        }
        return round.getMatches().stream()
                .filter(match -> isPlayer1(match, player)
                        || (match.getPlayer2() != null && Objects.equals(match.getPlayer2().getId(), player.getId())))
                .findFirst()
                .orElse(null);
    }

    private Player opponent(Match match, Player player) {
        if (isPlayer1(match, player)) {
            return match.getPlayer2();
        }
        return match.getPlayer1();
    }

    private boolean isPlayer1(Match match, Player player) {
        return match.getPlayer1() != null && Objects.equals(match.getPlayer1().getId(), player.getId());
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
