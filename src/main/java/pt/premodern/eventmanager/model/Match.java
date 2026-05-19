package pt.premodern.eventmanager.model;

import java.util.UUID;

public class Match {
    private UUID id;
    private int tableNumber;
    private Player player1;
    private Player player2;
    private int player1GamesWon;
    private int player2GamesWon;
    private int drawGames;
    private boolean completed;
    private boolean bye;
    private Player winner;

    public Match() {
        this.id = UUID.randomUUID();
    }

    public Match(int tableNumber, Player player1, Player player2) {
        this();
        this.tableNumber = tableNumber;
        this.player1 = player1;
        this.player2 = player2;
    }

    public static Match bye(int tableNumber, Player player) {
        Match match = new Match(tableNumber, player, null);
        match.setBye(true);
        match.setCompleted(true);
        match.setWinner(player);
        return match;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public int getTableNumber() {
        return tableNumber;
    }

    public void setTableNumber(int tableNumber) {
        this.tableNumber = tableNumber;
    }

    public Player getPlayer1() {
        return player1;
    }

    public void setPlayer1(Player player1) {
        this.player1 = player1;
    }

    public Player getPlayer2() {
        return player2;
    }

    public void setPlayer2(Player player2) {
        this.player2 = player2;
    }

    public int getPlayer1GamesWon() {
        return player1GamesWon;
    }

    public void setPlayer1GamesWon(int player1GamesWon) {
        this.player1GamesWon = player1GamesWon;
    }

    public int getPlayer2GamesWon() {
        return player2GamesWon;
    }

    public void setPlayer2GamesWon(int player2GamesWon) {
        this.player2GamesWon = player2GamesWon;
    }

    public int getDrawGames() {
        return drawGames;
    }

    public void setDrawGames(int drawGames) {
        this.drawGames = drawGames;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public boolean isBye() {
        return bye;
    }

    public void setBye(boolean bye) {
        this.bye = bye;
    }

    public Player getWinner() {
        return winner;
    }

    public void setWinner(Player winner) {
        this.winner = winner;
    }
}
