package pt.premodern.eventmanager.model;

import java.util.ArrayList;
import java.util.List;

public class Round {
    private int number;
    private List<Match> matches;
    private boolean completed;
    private boolean playoffRound;

    public Round() {
        this.matches = new ArrayList<>();
    }

    public Round(int number, boolean playoffRound) {
        this();
        this.number = number;
        this.playoffRound = playoffRound;
    }

    public void updateCompleted() {
        this.completed = getMatches().stream().allMatch(Match::isCompleted);
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public List<Match> getMatches() {
        if (matches == null) {
            matches = new ArrayList<>();
        }
        return matches;
    }

    public void setMatches(List<Match> matches) {
        this.matches = matches;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public boolean isPlayoffRound() {
        return playoffRound;
    }

    public void setPlayoffRound(boolean playoffRound) {
        this.playoffRound = playoffRound;
    }
}
