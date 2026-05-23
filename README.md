# MTG Event Manager

Java + Swing desktop application for managing Magic: The Gathering tournaments with Swiss rounds, standings, JSON persistence, and Top Cut.

## Requirements

- JDK 17 or higher
- Maven 3.9 or higher

## Run

```bash
mvn clean compile exec:java
```

## Build jar

```bash
mvn clean package
```

## Features

- Create Swiss-only events or Swiss events with Top Cut.
- Manage players with e-mail validation and duplicate blocking.
- Swiss pairings with bye support, opponent repeat prevention, and a preference for avoiding same-team pairings.
- Enter match results.
- Standings with OMW%, GW%, and OGW%.
- Top Cut by Swiss seed.
- Save and load events as JSON.
- Export players, pairings, standings, and results to CSV.
