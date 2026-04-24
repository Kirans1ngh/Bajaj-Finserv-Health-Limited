package com.bajaj.processor;

import com.bajaj.model.EventKey;
import com.bajaj.model.LeaderboardEntry;
import com.bajaj.model.QuizEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Stateful component that accumulates quiz events across all polls,
 * deduplicates them, and produces a sorted leaderboard.
 *
 * <h2>Deduplication strategy</h2>
 * <p>Each incoming {@link QuizEvent} is represented by a composite key
 * {@code (roundId, participant)} stored as a {@link EventKey} Java record.
 * Because {@code record} types derive {@code equals} and {@code hashCode}
 * from their components, the {@code seenKeys} {@code HashSet} guarantees
 * O(1) look-up and insertion. An event whose key is already present in the
 * set is silently discarded — its score is <em>not</em> added again.
 *
 * <h2>Score aggregation</h2>
 * <p>For every new (unseen) event the score is added to a running total
 * stored in {@code scoreboard}, a {@code HashMap<String, Integer>} keyed
 * by participant name.
 */
public class DataProcessor {

    private static final Logger log = LoggerFactory.getLogger(DataProcessor.class);

    /** Tracks composite keys already processed. Prevents duplicate scoring. */
    private final Set<EventKey>          seenKeys  = new HashSet<>();

    /** Running total score per participant. */
    private final Map<String, Integer>   scoreboard = new HashMap<>();

    // -------------------------------------------------------------------------
    // Ingestion
    // -------------------------------------------------------------------------

    /**
     * Processes a batch of events received from a single poll.
     *
     * <p>For each event:
     * <ol>
     *   <li>Construct its {@link EventKey}.
     *   <li>Skip it if the key is already in {@code seenKeys}.
     *   <li>Otherwise, mark it seen and add its score to the participant's total.
     * </ol>
     *
     * @param events the raw events from one API call; may be empty but not null
     */
    public void ingest(List<QuizEvent> events) {
        int duplicates = 0;
        int accepted   = 0;

        for (QuizEvent event : events) {
            EventKey key = new EventKey(event.getRoundId(), event.getParticipant());

            if (!seenKeys.add(key)) {
                // add() returns false when the element was already present
                duplicates++;
                log.debug("Duplicate skipped: {}", key);
                continue;
            }

            scoreboard.merge(event.getParticipant(), event.getScore(), Integer::sum);
            accepted++;
        }

        log.info("Ingested {} events — {} accepted, {} duplicates discarded",
                events.size(), accepted, duplicates);
    }

    // -------------------------------------------------------------------------
    // Leaderboard generation
    // -------------------------------------------------------------------------

    /**
     * Builds a sorted leaderboard from the accumulated scores.
     *
     * <p>Entries are sorted by {@code totalScore} descending, with ties
     * broken alphabetically by participant name for deterministic output.
     *
     * @return an unmodifiable, sorted list of {@link LeaderboardEntry} objects
     */
    public List<LeaderboardEntry> buildLeaderboard() {
        List<LeaderboardEntry> leaderboard = new ArrayList<>(scoreboard.size());

        scoreboard.forEach((participant, total) ->
                leaderboard.add(new LeaderboardEntry(participant, total)));

        Collections.sort(leaderboard);

        log.info("Leaderboard built — {} unique participants", leaderboard.size());
        if (log.isInfoEnabled()) {
            leaderboard.forEach(e ->
                    log.info("  {} -> {}", e.getParticipant(), e.getTotalScore()));
        }

        return Collections.unmodifiableList(leaderboard);
    }

    // -------------------------------------------------------------------------
    // Diagnostics
    // -------------------------------------------------------------------------

    /** Total number of distinct events seen so far (after deduplication). */
    public int uniqueEventCount() {
        return seenKeys.size();
    }

    /** Total number of distinct participants recorded. */
    public int participantCount() {
        return scoreboard.size();
    }
}
