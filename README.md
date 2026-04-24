# Quiz Leaderboard Aggregator

Hey there! This is my solution for the Quiz Leaderboard Aggregator assignment. 

The goal of this project was to build a backend Java application that pulls quiz data from an external API, processes the events to filter out duplicates, calculates everyone's total scores, and submits the final leaderboard back to the server. 

## The Challenge

The tricky part of this assignment wasn't just pulling data from an API—it was dealing with the reality of distributed systems. The validator API I was pulling from simulates a real-world scenario where the same event (like a participant scoring points in a specific round) might get sent multiple times across different network requests.

If I just blindly added up every score I received, the totals would be completely wrong. I had to make sure that a specific participant's score for a specific round was only counted **once**, no matter how many times the API sent it to me over the 10 polls.

## How I Solved It

### 1. Polling the API
I set up a loop to hit the `GET /quiz/messages` endpoint exactly 10 times. The assignment strictly required a 5-second delay between each poll, so I added a `Thread.sleep(5000)` to ensure I wasn't hammering the server too fast.

### 2. The Deduplication Strategy (The Fun Part)
To make sure I didn't count duplicate events, I created a composite key made up of the `roundId` and the `participant`. I implemented this using a Java 14+ `record` called `EventKey`. 

The beauty of using a `record` is that Java automatically handles the `equals()` and `hashCode()` methods behind the scenes. This meant I could just toss every incoming event's `EventKey` into a `HashSet`. 
* If the key was already in the set? It's a duplicate. Ignore it.
* If the key was new? Add it to the set, and add the score to the participant's running total in a `HashMap`.

This gave me blazing fast O(1) lookups for duplicates without writing a bunch of messy, boilerplate code.

### 3. Sorting and Submitting
Once all 10 polls finished and the scores were safely aggregated, I converted the map into a list of `LeaderboardEntry` objects. I implemented the `Comparable` interface on this class so it would automatically sort the participants by their total score in descending order (highest score first). 

Finally, I wrapped the sorted leaderboard in a JSON payload and `POST`ed it to the submission endpoint. 

---

## How to Run the Project

If you want to run this code yourself, you'll need **Java 17+** and **Maven**. 

### Building the Project
Open your terminal in the project folder and run:
```bash
mvn clean package
```
This will download the required libraries (Jackson for JSON parsing and SLF4J/Logback for logging) and compile everything into a single, executable JAR file in the `target/` directory.

### Running the App
Once it's built, just run:
```bash
java -jar target/quiz-leaderboard-1.0.0.jar
```
The app will start polling. Since it has to wait 5 seconds between each of the 10 polls, it takes about 50 seconds to finish. You'll see live logs in the console telling you exactly what it's doing, how many events it ingested, and how many duplicates it threw out. 

All logs are also saved to `quiz-leaderboard.log` so you can review them later.

---
*Built with Java 17 and Maven.*
