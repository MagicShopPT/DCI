package pt.premodern.eventmanager.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Event {
    private UUID id;
    private String name;
    private List<Player> players;
    private List<Round> rounds;
    private int currentRoundNumber;
    private int totalSwissRounds;
    private boolean hasTopCut;
    private int topCutSize;
    private boolean teamEvent;
    private EventStatus status;
    private EventType eventType;

    public Event() {
        this.id = UUID.randomUUID();
        this.players = new ArrayList<>();
        this.rounds = new ArrayList<>();
        this.status = EventStatus.CREATED;
        this.eventType = EventType.SWISS_ONLY;
    }

    public Round getCurrentRound() {
        if (getRounds().isEmpty()) {
            return null;
        }
        return getRounds().get(getRounds().size() - 1);
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Player> getPlayers() {
        if (players == null) {
            players = new ArrayList<>();
        }
        return players;
    }

    public void setPlayers(List<Player> players) {
        this.players = players;
    }

    public List<Round> getRounds() {
        if (rounds == null) {
            rounds = new ArrayList<>();
        }
        return rounds;
    }

    public void setRounds(List<Round> rounds) {
        this.rounds = rounds;
    }

    public int getCurrentRoundNumber() {
        return currentRoundNumber;
    }

    public void setCurrentRoundNumber(int currentRoundNumber) {
        this.currentRoundNumber = currentRoundNumber;
    }

    public int getTotalSwissRounds() {
        return totalSwissRounds;
    }

    public void setTotalSwissRounds(int totalSwissRounds) {
        this.totalSwissRounds = totalSwissRounds;
    }

    public boolean isHasTopCut() {
        return hasTopCut;
    }

    public void setHasTopCut(boolean hasTopCut) {
        this.hasTopCut = hasTopCut;
    }

    public int getTopCutSize() {
        return topCutSize;
    }

    public void setTopCutSize(int topCutSize) {
        this.topCutSize = topCutSize;
    }

    public boolean isTeamEvent() {
        return teamEvent;
    }

    public void setTeamEvent(boolean teamEvent) {
        this.teamEvent = teamEvent;
    }

    public EventStatus getStatus() {
        return status;
    }

    public void setStatus(EventStatus status) {
        this.status = status;
    }

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }
}
