package com.bajaj.model;

import java.util.Objects;

/**
 * Composite deduplication key for a quiz event.
 *
 * <p>Two events with the same {@code roundId} and {@code participant} are
 * considered identical regardless of which poll returned them. Using a Java
 * {@code record} gives us correct {@code equals} / {@code hashCode} for free,
 * making it safe to store instances directly in a {@code HashSet}.
 */
public record EventKey(String roundId, String participant) {

    public EventKey {
        Objects.requireNonNull(roundId,     "roundId must not be null");
        Objects.requireNonNull(participant, "participant must not be null");
    }
}
