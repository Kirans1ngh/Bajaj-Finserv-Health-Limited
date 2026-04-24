package com.bajaj.api;

import com.bajaj.model.PollResponse;
import com.bajaj.model.QuizEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

/**
 * Thin wrapper around Java's built-in {@link HttpClient} for quiz API calls.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Build the poll and submit request URIs.
 *   <li>Execute HTTP calls with a configurable per-request timeout.
 *   <li>Retry on 5xx responses up to {@code MAX_RETRIES} times (with a short
 *       back-off), while still honouring the caller-imposed 5 000 ms inter-poll
 *       delay.
 *   <li>Parse the JSON response body into {@link QuizEvent} instances.
 * </ul>
 *
 * <p>This class is intentionally stateless — all state (seen keys, score
 * accumulator) lives in {@link com.bajaj.processor.DataProcessor}.
 */
public class QuizApiClient {

    private static final Logger log = LoggerFactory.getLogger(QuizApiClient.class);

    private static final String POLL_BASE_URL   =
            "https://devapigw.vidalhealthtpa.com/srm-quiz-task/quiz/messages";
    private static final String SUBMIT_URL       =
            "https://devapigw.vidalhealthtpa.com/srm-quiz-task/quiz/submit";

    /** Number of retry attempts on 5xx or network errors before giving up. */
    private static final int MAX_RETRIES = 3;

    /** Back-off between retries — kept short so as not to disturb the main 5 s delay. */
    private static final long RETRY_BACK_OFF_MS = 1_000;

    /** Per-request connect + read timeout. */
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    private final HttpClient   httpClient;
    private final ObjectMapper objectMapper;
    private final String       regNo;

    public QuizApiClient(String regNo) {
        this.regNo  = regNo;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Fetches events for a single poll index (0–9).
     *
     * @param pollIndex the poll number appended as the {@code poll} query param
     * @return list of {@link QuizEvent} objects; empty list on parse failure
     */
    public List<QuizEvent> fetchEvents(int pollIndex) {
        String url = POLL_BASE_URL + "?regNo=" + regNo + "&poll=" + pollIndex;
        log.info("Poll {}: GET {}", pollIndex, url);

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(REQUEST_TIMEOUT)
                        .GET()
                        .header("Accept", "application/json")
                        .build();

                HttpResponse<String> response = httpClient.send(
                        request, HttpResponse.BodyHandlers.ofString());

                int status = response.statusCode();
                log.info("Poll {} attempt {}: HTTP {}", pollIndex, attempt, status);

                if (status >= 200 && status < 300) {
                    return parseEvents(response.body());
                } else if (status >= 500) {
                    log.warn("Poll {} attempt {}: server error ({}), will retry",
                            pollIndex, attempt, status);
                    sleepQuietly(RETRY_BACK_OFF_MS);
                } else {
                    // 4xx — no point retrying
                    log.error("Poll {} attempt {}: client error ({}) — skipping this poll",
                            pollIndex, attempt, status);
                    return Collections.emptyList();
                }
            } catch (IOException | InterruptedException e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                log.warn("Poll {} attempt {}: request failed — {}", pollIndex, attempt, e.getMessage());
                if (attempt < MAX_RETRIES) {
                    sleepQuietly(RETRY_BACK_OFF_MS);
                }
            }
        }

        log.error("Poll {}: exhausted {} retries, returning empty result", pollIndex, MAX_RETRIES);
        return Collections.emptyList();
    }

    /**
     * POSTs the serialized {@code payload} to the submission endpoint.
     *
     * @param payloadJson pre-serialized JSON string
     * @return the raw HTTP response body, or an empty string on failure
     */
    public String submitLeaderboard(String payloadJson) {
        log.info("Submitting leaderboard to {}", SUBMIT_URL);
        log.debug("Payload: {}", payloadJson);

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(SUBMIT_URL))
                        .timeout(REQUEST_TIMEOUT)
                        .POST(HttpRequest.BodyPublishers.ofString(payloadJson))
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .build();

                HttpResponse<String> response = httpClient.send(
                        request, HttpResponse.BodyHandlers.ofString());

                int status = response.statusCode();
                log.info("Submit attempt {}: HTTP {}", attempt, status);

                if (status >= 200 && status < 300) {
                    log.info("Submission accepted. Response: {}", response.body());
                    return response.body();
                } else if (status >= 500) {
                    log.warn("Submit attempt {}: server error ({}), will retry", attempt, status);
                    sleepQuietly(RETRY_BACK_OFF_MS);
                } else {
                    log.error("Submit attempt {}: client error ({}) — {}", attempt, status, response.body());
                    return response.body();
                }
            } catch (IOException | InterruptedException e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                log.warn("Submit attempt {}: request failed — {}", attempt, e.getMessage());
                if (attempt < MAX_RETRIES) {
                    sleepQuietly(RETRY_BACK_OFF_MS);
                }
            }
        }

        log.error("Submission failed after {} retries", MAX_RETRIES);
        return "";
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Parses a JSON array body into a list of {@link QuizEvent} objects.
     * Returns an empty list if the body is blank or malformed.
     */
    private List<QuizEvent> parseEvents(String body) {
        if (body == null || body.isBlank()) {
            log.warn("Empty response body — treating as zero events");
            return Collections.emptyList();
        }
        try {
            PollResponse wrapper = objectMapper.readValue(body, PollResponse.class);
            return wrapper.getEvents();
        } catch (IOException e) {
            log.error("Failed to parse response body: {} — body was: {}", e.getMessage(), body);
            return Collections.emptyList();
        }
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
