# Quiz Leaderboard Aggregator

A Java 17 application that polls an external quiz API, deduplicates event data, aggregates scores, and submits a final leaderboard.

## Overview

This project solves a data synchronization problem where an external validator API returns quiz scores across multiple polls. Since the external system may broadcast duplicate events, the application must identify and filter out these duplicates before calculating the final leaderboard to ensure correct scoring.

## Build & Run

### Prerequisites
* Java 17 or higher
* Maven 3.8+

### Build
Build the executable JAR using Maven:
```bash
mvn clean package
```

### Run
Execute the compiled JAR:
```bash
java -jar target/quiz-leaderboard-1.0.0.jar
```
*Note: Execution takes ~50 seconds due to a mandatory 5-second delay required between each of the 10 API polls.*

## Architecture & Design Decisions

### 1. Deduplication Logic
The API frequently returns duplicate events across different polls. To handle this, the application uses a composite key consisting of `(roundId, participant)`. 

This is implemented using a Java `record` (`EventKey`). Since Java automatically provides `equals()` and `hashCode()` implementations for records, these keys can be safely stored in a `HashSet`. 

When processing an event:
1. An `EventKey` is generated for the incoming payload.
2. If `HashSet.add(key)` returns `false`, the event is a duplicate and is immediately discarded.
3. If it returns `true`, the event is new, and the score is accumulated in a `HashMap`.

This approach guarantees `O(1)` duplicate detection and eliminates boilerplate.

### 2. Resilience & Error Handling
The `QuizApiClient` utilizes the native Java `HttpClient`. It implements a retry mechanism specifically for HTTP 5xx server errors and network timeouts. The retry backoff is intentionally kept short (1000ms) to ensure the application still respects the strict 5000ms inter-poll delay required by the validator API.

### 3. Sorting
The leaderboard generation relies on a custom `LeaderboardEntry` class that implements `Comparable`. It automatically sorts participants by their total accumulated score in descending order before generating the final JSON payload.
