package com.bajaj.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a single event returned by the polling API.
 *
 * <p>The API may return duplicate events across multiple polls. Deduplication
 * is performed externally using a {@link EventKey} composite key.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class QuizEvent {

    @JsonProperty("roundId")
    private String roundId;

    @JsonProperty("participant")
    private String participant;

    @JsonProperty("score")
    private int score;

    public QuizEvent() {}

    public QuizEvent(String roundId, String participant, int score) {
        this.roundId    = roundId;
        this.participant = participant;
        this.score      = score;
    }

    public String getRoundId()     { return roundId; }
    public String getParticipant() { return participant; }
    public int    getScore()       { return score; }

    @Override
    public String toString() {
        return "QuizEvent{roundId='" + roundId + "', participant='" + participant
                + "', score=" + score + '}';
    }
}
