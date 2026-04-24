package com.bajaj.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * The top-level JSON body posted to the submission endpoint.
 *
 * <pre>
 * {
 *   "regNo": "2024CS101",
 *   "leaderboard": [ { "participant": "...", "totalScore": 42 }, ... ]
 * }
 * </pre>
 */
public class SubmissionPayload {

    @JsonProperty("regNo")
    private final String regNo;

    @JsonProperty("leaderboard")
    private final List<LeaderboardEntry> leaderboard;

    public SubmissionPayload(String regNo, List<LeaderboardEntry> leaderboard) {
        this.regNo       = regNo;
        this.leaderboard = leaderboard;
    }

    public String                  getRegNo()       { return regNo; }
    public List<LeaderboardEntry>  getLeaderboard() { return leaderboard; }
}
