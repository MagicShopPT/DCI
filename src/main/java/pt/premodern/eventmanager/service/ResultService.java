package pt.premodern.eventmanager.service;

import pt.premodern.eventmanager.model.Event;
import pt.premodern.eventmanager.model.Match;
import pt.premodern.eventmanager.model.Player;
import pt.premodern.eventmanager.model.Round;

public class ResultService {
    public void submitResult(Match match, int p1GamesWon, int p2GamesWon, int draws) {
        submitResult(match, p1GamesWon, p2GamesWon, draws, false);
    }

    public void submitTopCutResult(Match match, int p1GamesWon, int p2GamesWon, int draws) {
        submitResult(match, p1GamesWon, p2GamesWon, draws, true);
    }

    private void submitResult(Match match, int p1GamesWon, int p2GamesWon, int draws, boolean topCut) {
        if (match == null || match.isBye()) {
            throw new IllegalArgumentException("Este match não aceita resultado manual.");
        }
        validateGames(p1GamesWon, p2GamesWon, draws);
        if (topCut && p1GamesWon == p2GamesWon) {
            throw new IllegalArgumentException("No Top Cut não pode haver empate.");
        }

        match.setPlayer1GamesWon(p1GamesWon);
        match.setPlayer2GamesWon(p2GamesWon);
        match.setDrawGames(draws);
        match.setCompleted(true);

        if (p1GamesWon > p2GamesWon) {
            match.setWinner(match.getPlayer1());
        } else if (p2GamesWon > p1GamesWon) {
            match.setWinner(match.getPlayer2());
        } else {
            match.setWinner(null);
        }
    }

    public void recalculateEvent(Event event) {
        for (Player player : event.getPlayers()) {
            player.setMatchPoints(0);
            player.setGamesWon(0);
            player.setGamesLost(0);
            player.setGamesDrawn(0);
            player.setReceivedBye(false);
            player.getOpponentsIds().clear();
        }

        for (Round round : event.getRounds()) {
            if (round.isPlayoffRound()) {
                continue;
            }
            for (Match match : round.getMatches()) {
                if (!match.isCompleted()) {
                    continue;
                }
                if (match.isBye()) {
                    Player player = match.getPlayer1();
                    if (player != null) {
                        player.setReceivedBye(true);
                        player.addMatchPoints(3);
                    }
                    continue;
                }
                applySwissResult(match);
            }
        }
    }

    public void updateRoundCompletion(Round round) {
        if (round != null) {
            round.updateCompleted();
        }
    }

    private void validateGames(int p1GamesWon, int p2GamesWon, int draws) {
        if (p1GamesWon < 0 || p1GamesWon > 2 || p2GamesWon < 0 || p2GamesWon > 2 || draws < 0 || draws > 2) {
            throw new IllegalArgumentException("Cada campo de jogos só pode ser 0, 1 ou 2.");
        }
        int total = p1GamesWon + p2GamesWon + draws;
        if (total == 0 || total > 3) {
            throw new IllegalArgumentException("Um match à melhor de 3 deve ter entre 1 e 3 jogos registados.");
        }
    }

    private void applySwissResult(Match match) {
        Player p1 = match.getPlayer1();
        Player p2 = match.getPlayer2();
        if (p1 == null || p2 == null) {
            return;
        }

        int p1GamesWon = match.getPlayer1GamesWon();
        int p2GamesWon = match.getPlayer2GamesWon();
        int draws = match.getDrawGames();

        p1.setGamesWon(p1.getGamesWon() + p1GamesWon);
        p1.setGamesLost(p1.getGamesLost() + p2GamesWon);
        p1.setGamesDrawn(p1.getGamesDrawn() + draws);
        p2.setGamesWon(p2.getGamesWon() + p2GamesWon);
        p2.setGamesLost(p2.getGamesLost() + p1GamesWon);
        p2.setGamesDrawn(p2.getGamesDrawn() + draws);

        if (p1GamesWon > p2GamesWon) {
            p1.addMatchPoints(3);
            match.setWinner(p1);
        } else if (p2GamesWon > p1GamesWon) {
            p2.addMatchPoints(3);
            match.setWinner(p2);
        } else {
            p1.addMatchPoints(1);
            p2.addMatchPoints(1);
            match.setWinner(null);
        }
        p1.addOpponent(p2.getId());
        p2.addOpponent(p1.getId());
    }
}
