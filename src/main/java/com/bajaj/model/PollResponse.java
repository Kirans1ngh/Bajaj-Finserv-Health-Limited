package com.bajaj.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.List;

/**
 * Top-level wrapper returned by the poll endpoint.
 *
 * <p>The actual API response is an object, not a bare array:
 * <pre>
 * {
 *   "regNo": "2024CS101",
 *   "pollIndex": 0,
 *   "totalPolls": 527,
 *   "events": [ { "roundId": "R1", "participant": "Alice", "score": 120 }, ... ],
 *   "meta": { ... }
 * }
 * </pre>
 * Only the {@code events} field is used downstream.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PollResponse {

    @JsonProperty("events")
    private List<QuizEvent> events;

    public PollResponse() {}

    /** Never null — returns an empty list when the field is absent. */
    public List<QuizEvent> getEvents() {
        return (events != null) ? events : Collections.emptyList();
    }
}
