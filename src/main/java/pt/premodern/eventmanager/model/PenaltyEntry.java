package pt.premodern.eventmanager.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public class PenaltyEntry {
    private UUID id;
    private int roundNumber;
    private String infraction;
    private String penalty;
    private String appliedPenalty;
    private String description;
    private String createdAt;

    public PenaltyEntry() {
        this.id = UUID.randomUUID();
        this.createdAt = OffsetDateTime.now().toString();
    }

    public PenaltyEntry(int roundNumber, String infraction, String penalty, String appliedPenalty, String description) {
        this();
        this.roundNumber = roundNumber;
        this.infraction = infraction;
        this.penalty = penalty;
        this.appliedPenalty = appliedPenalty;
        this.description = description;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public int getRoundNumber() {
        return roundNumber;
    }

    public void setRoundNumber(int roundNumber) {
        this.roundNumber = roundNumber;
    }

    public String getInfraction() {
        return infraction;
    }

    public void setInfraction(String infraction) {
        this.infraction = infraction;
    }

    public String getPenalty() {
        return penalty;
    }

    public void setPenalty(String penalty) {
        this.penalty = penalty;
    }

    public String getAppliedPenalty() {
        return appliedPenalty;
    }

    public void setAppliedPenalty(String appliedPenalty) {
        this.appliedPenalty = appliedPenalty;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
