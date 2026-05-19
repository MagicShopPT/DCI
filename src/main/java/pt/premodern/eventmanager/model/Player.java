package pt.premodern.eventmanager.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Player {
    private UUID id;
    private String firstName;
    private String lastName;
    private String email;
    private String team;
    private int matchPoints;
    private int gamesWon;
    private int gamesLost;
    private int gamesDrawn;
    private List<UUID> opponentsIds;
    private boolean dropped;
    private int dropRoundNumber;
    private boolean receivedBye;
    private int initialSeed;
    private List<PenaltyEntry> penaltyEntries;

    public Player() {
        this.id = UUID.randomUUID();
        this.opponentsIds = new ArrayList<>();
        this.penaltyEntries = new ArrayList<>();
    }

    public Player(String firstName, String lastName, String email, String team) {
        this();
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.team = team;
    }

    public String getFullName() {
        return ((firstName == null ? "" : firstName) + " " + (lastName == null ? "" : lastName)).trim();
    }

    public boolean hasTeam() {
        return team != null && !team.isBlank();
    }

    public void addOpponent(UUID id) {
        if (id != null && !getOpponentsIds().contains(id)) {
            getOpponentsIds().add(id);
        }
    }

    public void addMatchPoints(int points) {
        this.matchPoints += points;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTeam() {
        return team;
    }

    public void setTeam(String team) {
        this.team = team;
    }

    public int getMatchPoints() {
        return matchPoints;
    }

    public void setMatchPoints(int matchPoints) {
        this.matchPoints = matchPoints;
    }

    public int getGamesWon() {
        return gamesWon;
    }

    public void setGamesWon(int gamesWon) {
        this.gamesWon = gamesWon;
    }

    public int getGamesLost() {
        return gamesLost;
    }

    public void setGamesLost(int gamesLost) {
        this.gamesLost = gamesLost;
    }

    public int getGamesDrawn() {
        return gamesDrawn;
    }

    public void setGamesDrawn(int gamesDrawn) {
        this.gamesDrawn = gamesDrawn;
    }

    public List<UUID> getOpponentsIds() {
        if (opponentsIds == null) {
            opponentsIds = new ArrayList<>();
        }
        return opponentsIds;
    }

    public void setOpponentsIds(List<UUID> opponentsIds) {
        this.opponentsIds = opponentsIds;
    }

    public boolean isDropped() {
        return dropped;
    }

    public void setDropped(boolean dropped) {
        this.dropped = dropped;
    }

    public int getDropRoundNumber() {
        return dropRoundNumber;
    }

    public void setDropRoundNumber(int dropRoundNumber) {
        this.dropRoundNumber = dropRoundNumber;
    }

    public boolean isReceivedBye() {
        return receivedBye;
    }

    public void setReceivedBye(boolean receivedBye) {
        this.receivedBye = receivedBye;
    }

    public int getInitialSeed() {
        return initialSeed;
    }

    public void setInitialSeed(int initialSeed) {
        this.initialSeed = initialSeed;
    }

    public List<PenaltyEntry> getPenaltyEntries() {
        if (penaltyEntries == null) {
            penaltyEntries = new ArrayList<>();
        }
        return penaltyEntries;
    }

    public void setPenaltyEntries(List<PenaltyEntry> penaltyEntries) {
        this.penaltyEntries = penaltyEntries;
    }
}
