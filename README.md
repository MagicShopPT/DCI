# MTG Event Manager

Java + Swing desktop application for managing Magic: The Gathering tournaments with Swiss rounds, standings, JSON persistence, penalties, a round clock, and Top Cut.

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
- Light/Dark interface with a dashboard-style sidebar and tournament status header.
- The app starts in Light mode and can be toggled between Light and Dark from the header.
- `Create New Event` is protected after event creation: it becomes disabled until `End Current Event` is clicked.
- Loading an existing event also disables `Create New Event` and enables `End Current Event`.
- Autosave writes to the current event JSON file when available, or creates an event autosave file when no file exists yet.
- Autosave runs after creating an event, adding/editing/removing/dropping players, importing players, generating rounds, entering/changing results, entering penalties, and ending the current event.
- Manage players with e-mail validation and duplicate blocking.
- Swiss pairings with bye support, opponent repeat prevention, and a preference for avoiding same-team pairings.
- Enter match results and generate match result slips.
- Penalty Entry with player penalty history.
- Standings with OMW%, GW%, and OGW%.
- Top Cut by Swiss seed.
- Save and load events as JSON.
- Export players, pairings, standings, and results to CSV.
