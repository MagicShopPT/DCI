package pt.premodern.eventmanager.service;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import pt.premodern.eventmanager.model.Event;
import pt.premodern.eventmanager.model.EventStatus;
import pt.premodern.eventmanager.model.EventType;
import pt.premodern.eventmanager.model.Match;
import pt.premodern.eventmanager.model.Player;
import pt.premodern.eventmanager.model.Round;
import pt.premodern.eventmanager.model.Standing;

public class EventService {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    public Event createEvent(String name, EventType type, int topCutSize) {
        return createEvent(name, type, topCutSize, false);
    }

    public Event createEvent(String name, EventType type, int topCutSize, boolean teamEvent) {
        Event event = new Event();
        event.setName(name == null || name.isBlank() ? "MTG Event Manager" : name.trim());
        event.setEventType(type);
        event.setHasTopCut(type == EventType.SWISS_WITH_TOP_CUT);
        event.setTopCutSize(event.isHasTopCut() ? topCutSize : 0);
        event.setTeamEvent(teamEvent);
        event.setTotalSwissRounds(0);
        return event;
    }

    public int calculateSwissRounds(int playerCount) {
        if (playerCount <= 8) {
            return 3;
        }
        return (int) Math.ceil(Math.log(playerCount) / Math.log(2));
    }

    public Player addPlayer(Event event, String firstName, String lastName, String email, String team) {
        requireEditablePlayers(event);
        validatePlayerData(event, null, firstName, lastName, email);
        Player player = new Player(firstName.trim(), lastName.trim(), email.trim().toLowerCase(Locale.ROOT), clean(team));
        player.setInitialSeed(event.getPlayers().size() + 1);
        event.getPlayers().add(player);
        event.setTotalSwissRounds(calculateSwissRounds(event.getPlayers().size()));
        return player;
    }

    public void updatePlayer(Event event, Player player, String firstName, String lastName, String email, String team) {
        requireEditablePlayers(event);
        validatePlayerData(event, player.getId(), firstName, lastName, email);
        player.setFirstName(firstName.trim());
        player.setLastName(lastName.trim());
        player.setEmail(email.trim().toLowerCase(Locale.ROOT));
        player.setTeam(clean(team));
    }

    public void removePlayer(Event event, Player player) {
        requireEditablePlayers(event);
        event.getPlayers().remove(player);
        reseedPlayers(event);
        event.setTotalSwissRounds(calculateSwissRounds(event.getPlayers().size()));
    }

    public void dropPlayer(Player player) {
        player.setDropped(true);
    }

    public void dropPlayerAtRound(Event event, Player player, int roundNumber) {
        player.setDropped(true);
        player.setDropRoundNumber(roundNumber);
        removeRoundsAfterDrop(event, roundNumber);
    }

    public void restoreDroppedPlayer(Player player) {
        player.setDropped(false);
        player.setDropRoundNumber(0);
    }

    public Optional<Player> findPlayer(Event event, UUID id) {
        return event.getPlayers().stream()
                .filter(player -> Objects.equals(player.getId(), id))
                .findFirst();
    }

    public boolean isStarted(Event event) {
        return event != null && event.getStatus() != EventStatus.CREATED;
    }

    public void exportPlayers(Event event, File file) throws IOException {
        try (PrintWriter writer = writer(file)) {
            writer.println("seed,firstName,lastName,email,team,matchPoints,dropped,receivedBye");
            for (Player player : event.getPlayers()) {
                writer.printf(Locale.ROOT, "%d,%s,%s,%s,%s,%d,%s,%s%n",
                        player.getInitialSeed(),
                        csv(player.getFirstName()),
                        csv(player.getLastName()),
                        csv(player.getEmail()),
                        csv(player.getTeam()),
                        player.getMatchPoints(),
                        player.isDropped(),
                        player.isReceivedBye());
            }
        }
    }

    public int importPlayers(Event event, File file) throws IOException {
        requireEditablePlayers(event);
        List<Player> imported = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
            String firstLine = reader.readLine();
            if (firstLine == null) {
                throw new IllegalArgumentException("The CSV file is empty.");
            }

            List<String> firstRow = parseCsvLine(firstLine);
            Map<String, Integer> header = header(firstRow);
            boolean hasHeader = !header.isEmpty();
            if (!hasHeader) {
                imported.add(playerFromCsvRow(event, imported, firstRow, null, 1));
            }

            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank()) {
                    continue;
                }
                imported.add(playerFromCsvRow(event, imported, parseCsvLine(line), hasHeader ? header : null, lineNumber));
            }
        }

        int nextSeed = event.getPlayers().size() + 1;
        for (Player player : imported) {
            player.setInitialSeed(nextSeed++);
            event.getPlayers().add(player);
        }
        event.setTotalSwissRounds(calculateSwissRounds(event.getPlayers().size()));
        return imported.size();
    }

    public void exportCurrentPairings(Event event, File file) throws IOException {
        Round round = event.getCurrentRound();
        if (round == null) {
            throw new IllegalStateException("There is no current round to export.");
        }
        try (PrintWriter writer = writer(file)) {
            writer.println("round,table,player1,team1,player2,team2,status,result");
            for (Match match : round.getMatches()) {
                writer.printf(Locale.ROOT, "%d,%d,%s,%s,%s,%s,%s,%s%n",
                        round.getNumber(),
                        match.getTableNumber(),
                        csv(name(match.getPlayer1())),
                        csv(team(match.getPlayer1())),
                        csv(name(match.getPlayer2())),
                        csv(team(match.getPlayer2())),
                        match.isCompleted() ? "completed" : "open",
                        csv(resultText(match)));
            }
        }
    }

    public void exportStandings(List<Standing> standings, File file) throws IOException {
        try (PrintWriter writer = writer(file)) {
            writer.println("rank,name,team,matchPoints,omw,gw,ogw");
            for (Standing standing : standings) {
                Player player = standing.getPlayer();
                writer.printf(Locale.ROOT, "%d,%s,%s,%d,%.4f,%.4f,%.4f%n",
                        standing.getRank(),
                        csv(player.getFullName()),
                        csv(player.getTeam()),
                        standing.getMatchPoints(),
                        standing.getOmwPercentage(),
                        standing.getGwPercentage(),
                        standing.getOgwPercentage());
            }
        }
    }

    public void exportResults(Event event, File file) throws IOException {
        try (PrintWriter writer = writer(file)) {
            writer.println("round,playoff,table,player1,player2,p1Games,p2Games,drawGames,winner,status");
            for (Round round : event.getRounds()) {
                for (Match match : round.getMatches()) {
                    writer.printf(Locale.ROOT, "%d,%s,%d,%s,%s,%d,%d,%d,%s,%s%n",
                            round.getNumber(),
                            round.isPlayoffRound(),
                            match.getTableNumber(),
                            csv(name(match.getPlayer1())),
                            csv(name(match.getPlayer2())),
                            match.getPlayer1GamesWon(),
                            match.getPlayer2GamesWon(),
                            match.getDrawGames(),
                            csv(name(match.getWinner())),
                            match.isCompleted() ? "completed" : "open");
                }
            }
        }
    }

    private void requireEditablePlayers(Event event) {
        if (isStarted(event)) {
            throw new IllegalStateException("Players cannot be added, edited, or removed after the event has started.");
        }
    }

    private void validatePlayerData(Event event, UUID currentPlayerId, String firstName, String lastName, String email) {
        if (firstName == null || firstName.isBlank()) {
            throw new IllegalArgumentException("First name is required.");
        }
        if (lastName == null || lastName.isBlank()) {
            throw new IllegalArgumentException("Last name is required.");
        }
        if (email == null || !EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new IllegalArgumentException("The e-mail address is not valid.");
        }
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        boolean duplicate = event.getPlayers().stream()
                .filter(player -> !Objects.equals(player.getId(), currentPlayerId))
                .anyMatch(player -> normalized.equalsIgnoreCase(player.getEmail()));
        if (duplicate) {
            throw new IllegalArgumentException("A player with that e-mail already exists.");
        }
    }

    private void reseedPlayers(Event event) {
        event.getPlayers().sort(Comparator.comparingInt(Player::getInitialSeed));
        for (int i = 0; i < event.getPlayers().size(); i++) {
            event.getPlayers().get(i).setInitialSeed(i + 1);
        }
    }

    private void removeRoundsAfterDrop(Event event, int roundNumber) {
        boolean removed = event.getRounds().removeIf(round -> round.getNumber() > roundNumber);
        if (!removed) {
            return;
        }
        event.setCurrentRoundNumber(roundNumber);
        if (roundNumber >= event.getTotalSwissRounds()) {
            event.setStatus(event.isHasTopCut() ? EventStatus.SWISS_COMPLETED : EventStatus.FINISHED);
        } else {
            event.setStatus(EventStatus.SWISS_IN_PROGRESS);
        }
    }

    private PrintWriter writer(File file) throws IOException {
        return new PrintWriter(new FileWriter(file, StandardCharsets.UTF_8));
    }

    private Player playerFromCsvRow(Event event, List<Player> imported, List<String> row, Map<String, Integer> header, int lineNumber) {
        String firstName;
        String lastName;
        String email;
        String team;
        if (header == null) {
            firstName = value(row, 0);
            lastName = value(row, 1);
            email = value(row, 2);
            team = value(row, 3);
        } else {
            firstName = value(row, header.getOrDefault("firstname", -1));
            lastName = value(row, header.getOrDefault("lastname", -1));
            email = value(row, header.getOrDefault("email", -1));
            team = value(row, header.getOrDefault("team", -1));
            if ((firstName.isBlank() || lastName.isBlank()) && header.containsKey("name")) {
                String[] parts = value(row, header.get("name")).trim().split("\\s+", 2);
                firstName = firstName.isBlank() && parts.length > 0 ? parts[0] : firstName;
                lastName = lastName.isBlank() && parts.length > 1 ? parts[1] : lastName;
            }
        }

        try {
            validatePlayerData(event, null, firstName, lastName, email);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Line " + lineNumber + ": " + exception.getMessage(), exception);
        }

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        boolean duplicateInImport = imported.stream().anyMatch(player -> normalizedEmail.equalsIgnoreCase(player.getEmail()));
        if (duplicateInImport) {
            throw new IllegalArgumentException("Line " + lineNumber + ": duplicate e-mail in CSV.");
        }
        return new Player(firstName.trim(), lastName.trim(), normalizedEmail, clean(team));
    }

    private Map<String, Integer> header(List<String> row) {
        Set<String> known = new HashSet<>(List.of("firstname", "first_name", "lastname", "last_name", "email", "team", "name"));
        Map<String, Integer> header = new HashMap<>();
        for (int i = 0; i < row.size(); i++) {
            String key = normalizeHeader(row.get(i));
            if (known.contains(key)) {
                header.put(key.replace("_", ""), i);
            }
        }
        return header.containsKey("email") ? header : Map.of();
    }

    private String normalizeHeader(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT).replace(" ", "_");
    }

    private String value(List<String> row, int index) {
        return index >= 0 && index < row.size() ? row.get(index).trim() : "";
    }

    private List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (ch == ',' && !quoted) {
                values.add(current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        values.add(current.toString());
        return values;
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? "" : value.trim();
    }

    private String name(Player player) {
        return player == null ? "" : player.getFullName();
    }

    private String team(Player player) {
        return player == null ? "" : player.getTeam();
    }

    private String resultText(Match match) {
        if (match.isBye()) {
            return "BYE";
        }
        if (!match.isCompleted()) {
            return "";
        }
        return match.getPlayer1GamesWon() + "-" + match.getPlayer2GamesWon()
                + (match.getDrawGames() > 0 ? "-" + match.getDrawGames() : "");
    }

    private String csv(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }
}
