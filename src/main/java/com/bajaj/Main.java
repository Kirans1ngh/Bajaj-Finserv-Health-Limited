package com.bajaj;

import com.bajaj.api.QuizApiClient;
import com.bajaj.model.LeaderboardEntry;
import com.bajaj.model.QuizEvent;
import com.bajaj.model.SubmissionPayload;
import com.bajaj.processor.DataProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Application entry point.
 *
 * <p>Orchestrates the three-phase flow:
 * <ol>
 *   <li><strong>Poll</strong> — execute exactly 10 GET requests, one every 5 seconds.
 *   <li><strong>Process</strong> — deduplicate events and aggregate scores.
 *   <li><strong>Submit</strong> — POST the sorted leaderboard to the submission API.
 * </ol>
 */
public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    // ---- Configuration constants -------------------------------------------

    /** Registration number sent with every request. */
    private static final String REG_NO = "2024CS101";

    /** Total number of polls to execute (0 through 9). */
    private static final int TOTAL_POLLS = 10;

    /**
     * Mandatory delay between consecutive polls, in milliseconds.
     * The spec requires exactly 5 000 ms; do not reduce this value.
     */
    private static final long POLL_DELAY_MS = 5_000;

    // -------------------------------------------------------------------------

    public static void main(String[] args) throws Exception {

        log.info("=== Quiz Leaderboard Aggregator — starting (regNo={}) ===", REG_NO);

        QuizApiClient  apiClient  = new QuizApiClient(REG_NO);
        DataProcessor  processor  = new DataProcessor();
        ObjectMapper   mapper     = new ObjectMapper();

        // ------------------------------------------------------------------
        // Phase 1 — Polling
        // ------------------------------------------------------------------
        for (int poll = 0; poll < TOTAL_POLLS; poll++) {

            List<QuizEvent> events = apiClient.fetchEvents(poll);
            processor.ingest(events);

            boolean isLastPoll = (poll == TOTAL_POLLS - 1);
            if (!isLastPoll) {
                log.info("Waiting {}ms before next poll...", POLL_DELAY_MS);
                try {
                    Thread.sleep(POLL_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.warn("Poll delay interrupted — continuing");
                }
            }
        }

        log.info("Polling complete. Unique events: {}, Participants: {}",
                processor.uniqueEventCount(), processor.participantCount());

        // ------------------------------------------------------------------
        // Phase 2 — Build leaderboard
        // ------------------------------------------------------------------
        List<LeaderboardEntry> leaderboard = processor.buildLeaderboard();

        // ------------------------------------------------------------------
        // Phase 3 — Submit
        // ------------------------------------------------------------------
        SubmissionPayload payload     = new SubmissionPayload(REG_NO, leaderboard);
        String            payloadJson = mapper.writeValueAsString(payload);

        String response = apiClient.submitLeaderboard(payloadJson);

        log.info("=== Submission complete ===");
        log.info("Server response: {}", response);
    }
}
