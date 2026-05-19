package pt.premodern.eventmanager.model;

public class Standing {
    private Player player;
    private int rank;
    private int matchPoints;
    private double omwPercentage;
    private double gwPercentage;
    private double ogwPercentage;

    public Standing() {
    }

    public Standing(Player player, int rank, int matchPoints, double omwPercentage, double gwPercentage, double ogwPercentage) {
        this.player = player;
        this.rank = rank;
        this.matchPoints = matchPoints;
        this.omwPercentage = omwPercentage;
        this.gwPercentage = gwPercentage;
        this.ogwPercentage = ogwPercentage;
    }

    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public int getMatchPoints() {
        return matchPoints;
    }

    public void setMatchPoints(int matchPoints) {
        this.matchPoints = matchPoints;
    }

    public double getOmwPercentage() {
        return omwPercentage;
    }

    public void setOmwPercentage(double omwPercentage) {
        this.omwPercentage = omwPercentage;
    }

    public double getGwPercentage() {
        return gwPercentage;
    }

    public void setGwPercentage(double gwPercentage) {
        this.gwPercentage = gwPercentage;
    }

    public double getOgwPercentage() {
        return ogwPercentage;
    }

    public void setOgwPercentage(double ogwPercentage) {
        this.ogwPercentage = ogwPercentage;
    }
}
