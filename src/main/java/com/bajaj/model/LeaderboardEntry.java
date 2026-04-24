package com.bajaj.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A single row in the final leaderboard payload sent to the submission API.
 */
public class LeaderboardEntry implements Comparable<LeaderboardEntry> {

    @JsonProperty("participant")
    private final String participant;

    @JsonProperty("totalScore")
    private final int totalScore;

    public LeaderboardEntry(String participant, int totalScore) {
        this.participant = participant;
        this.totalScore  = totalScore;
    }

    public String getParticipant() { return participant; }
    public int    getTotalScore()  { return totalScore; }

    /**
     * Natural ordering: descending by {@code totalScore}.
     * Ties are broken alphabetically by participant name to keep output stable.
     */
    @Override
    public int compareTo(LeaderboardEntry other) {
        int cmp = Integer.compare(other.totalScore, this.totalScore); // descending
        return (cmp != 0) ? cmp : this.participant.compareTo(other.participant);
    }

    @Override
    public String toString() {
        return "LeaderboardEntry{participant='" + participant + "', totalScore=" + totalScore + '}';
    }
}
