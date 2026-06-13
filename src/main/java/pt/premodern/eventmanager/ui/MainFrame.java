package pt.premodern.eventmanager.ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import pt.premodern.eventmanager.model.Event;
import pt.premodern.eventmanager.model.EventStatus;
import pt.premodern.eventmanager.model.EventType;
import pt.premodern.eventmanager.model.Match;
import pt.premodern.eventmanager.model.PenaltyEntry;
import pt.premodern.eventmanager.model.Player;
import pt.premodern.eventmanager.model.Round;
import pt.premodern.eventmanager.model.Standing;
import pt.premodern.eventmanager.persistence.JsonStorageService;
import pt.premodern.eventmanager.service.EventService;
import pt.premodern.eventmanager.service.PairingService;
import pt.premodern.eventmanager.service.ResultService;
import pt.premodern.eventmanager.service.StandingService;
import pt.premodern.eventmanager.service.TopCutService;

public class MainFrame extends JFrame {
    private final EventService eventService = new EventService();
    private final PairingService pairingService = new PairingService();
    private final ResultService resultService = new ResultService();
    private final StandingService standingService = new StandingService();
    private final TopCutService topCutService = new TopCutService();
    private final JsonStorageService storageService = new JsonStorageService();

    private Event event;
    private File currentFile;
    private final JPanel cards = new JPanel(new CardLayout());
    private final JPanel sidebar = new JPanel(new GridBagLayout());
    private final Map<Component, JButton> navigationButtons = new LinkedHashMap<>();
    private JLabel sidebarBrand;
    private final EventPanel eventPanel;
    private final EventInfoPanel eventInfoPanel;
    private final PlayerPanel playerPanel;
    private final PairingsPanel pairingsPanel;
    private final ResultsPanel resultsPanel;
    private final StandingsPanel standingsPanel;
    private final TopCutPanel topCutPanel;
    private final ClockPanel clockPanel;
    private Component previousPanel;
    private boolean darkMode = false;

    public MainFrame() {
        super("MTG Event Manager");
        AppTheme.install(darkMode);
        this.event = eventService.createEvent("New Event", EventType.SWISS_ONLY, 0);

        this.eventPanel = new EventPanel(this);
        this.eventInfoPanel = new EventInfoPanel(this);
        this.playerPanel = new PlayerPanel(this);
        this.pairingsPanel = new PairingsPanel(this);
        this.resultsPanel = new ResultsPanel(this);
        this.standingsPanel = new StandingsPanel(this);
        this.topCutPanel = new TopCutPanel(this);
        this.clockPanel = new ClockPanel(this);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1040, 700));
        setLocationByPlatform(true);
        setJMenuBar(createMenuBar());
        getContentPane().setBackground(AppTheme.background(darkMode));
        add(eventInfoPanel, BorderLayout.NORTH);
        add(mainArea(), BorderLayout.CENTER);
        showPanel(eventPanel);

        refreshAll();
        pack();
    }

    public Event getEvent() {
        return event;
    }

    public EventService getEventService() {
        return eventService;
    }

    public StandingService getStandingService() {
        return standingService;
    }

    public void setEvent(Event event) {
        this.event = event;
        this.currentFile = null;
        refreshAll();
    }

    public void startNewEvent(Event event) {
        this.event = event;
        this.currentFile = null;
        autoSaveEvent();
        refreshAll();
    }

    public void refreshAll() {
        eventPanel.refreshData();
        eventInfoPanel.refreshData();
        playerPanel.refreshData();
        pairingsPanel.refreshData();
        resultsPanel.refreshData();
        standingsPanel.refreshData();
        topCutPanel.refreshData();
        updateSidebarLabels();
        applyStandardColors();
        setTitle("MTG Event Manager - " + event.getName());
    }

    public void refreshEventInfo() {
        if (eventInfoPanel != null) {
            eventInfoPanel.refreshData();
        }
    }

    public String clockText() {
        return clockPanel == null || !clockPanel.isRunning() ? "-" : clockPanel.getClockText();
    }

    public boolean clockIsOvertime() {
        return clockPanel != null && clockPanel.isOvertime();
    }

    public boolean isDarkMode() {
        return darkMode;
    }

    public void toggleTheme() {
        darkMode = !darkMode;
        applyStandardColors();
    }

    public String winnerSummaryHtml() {
        if (event.getStatus() != EventStatus.FINISHED) {
            return "-";
        }
        if (event.isTeamEvent()) {
            String team = winningTeamName();
            if (team == null || team.isBlank()) {
                return "-";
            }
            String players = event.getPlayers().stream()
                    .filter(player -> player.hasTeam() && player.getTeam().equalsIgnoreCase(team))
                    .map(player -> "<div style='font-size:16pt;font-weight:normal;'>" + escapeHtml(player.getFullName()) + "</div>")
                    .collect(Collectors.joining());
            return "<html><div style='text-align:left;'>"
                    + "<div style='font-size:18pt;font-weight:bold;'>" + escapeHtml(team) + "</div>"
                    + players
                    + "</div></html>";
        }
        PlayerWinner winner = individualWinner();
        return winner.player() == null ? "-" : winner.player().getFullName();
    }

    public void showInfo(String message) {
        JOptionPane.showMessageDialog(this, message, "MTG Event Manager", JOptionPane.INFORMATION_MESSAGE);
    }

    public void showError(Exception exception) {
        JOptionPane.showMessageDialog(this, exception.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }

    public void generateSwissRound() {
        try {
            Round round = pairingService.generateNextSwissRound(event);
            warnSameTeamPairings(round);
            refreshAfterRoundGeneration(round);
        } catch (Exception exception) {
            showError(exception);
        }
    }

    public void createTopCut() {
        try {
            Round round = topCutService.createInitialTopCutRound(event);
            refreshAfterRoundGeneration(round);
        } catch (Exception exception) {
            showError(exception);
        }
    }

    public void generateNextTopCutRound() {
        try {
            Round round = topCutService.generateNextTopCutRound(event);
            if (round == null) {
                autoSaveEvent();
                refreshAll();
                showWinnerPopup();
            } else {
                refreshAfterRoundGeneration(round);
            }
        } catch (Exception exception) {
            showError(exception);
        }
    }

    public void showResultsTab() {
        showPanel(resultsPanel);
    }

    public void afterResultSubmitted(Round round) {
        boolean finishedBefore = event.getStatus() == EventStatus.FINISHED;
        resultService.recalculateEvent(event);
        round.updateCompleted();
        if (round.isCompleted()) {
            if (!round.isPlayoffRound() && event.getCurrentRoundNumber() == event.getTotalSwissRounds()) {
                event.setStatus(event.isHasTopCut() ? EventStatus.SWISS_COMPLETED : EventStatus.FINISHED);
            } else if (round.isPlayoffRound()) {
                try {
                    topCutService.generateNextTopCutRound(event);
                } catch (Exception ignored) {
                    // The Top Cut panel still offers manual generation if the round is not ready.
                }
            }
        }
        autoSaveEvent();
        refreshAll();
        if (!finishedBefore && event.getStatus() == EventStatus.FINISHED) {
            showWinnerPopup();
        }
    }

    public void importPlayersFromCsv() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Import Players CSV");
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                int imported = eventService.importPlayers(event, chooser.getSelectedFile());
                autoSaveEvent();
                refreshAll();
                showInfo(imported + " player(s) imported.");
            } catch (Exception exception) {
                showError(exception);
            }
        }
    }

    public void showPenaltyEntry(Player player) {
        if (event.getPlayers().isEmpty()) {
            showInfo("There are no players in the event.");
            return;
        }
        new PenaltyEntryDialog(this, player).setVisible(true);
    }

    public void afterPenaltySubmitted(PenaltyEntry entry) {
        resultService.recalculateEvent(event);
        Round round = event.getCurrentRound();
        if (round != null) {
            round.updateCompleted();
        }
        if (round != null && round.isCompleted()) {
            afterResultSubmitted(round);
        } else {
            autoSaveEvent();
            refreshAll();
        }
        showInfo("Penalty recorded: " + entry.getAppliedPenalty() + ".");
    }

    private JPanel mainArea() {
        JPanel shell = new JPanel(new BorderLayout());
        shell.setBackground(AppTheme.background(darkMode));
        shell.add(createSidebar(), BorderLayout.WEST);

        cards.setBackground(AppTheme.background(darkMode));
        cards.add(eventPanel, "Event");
        cards.add(playerPanel, "Players");
        cards.add(pairingsPanel, "Pairings");
        cards.add(resultsPanel, "Results");
        cards.add(standingsPanel, "Standings");
        cards.add(topCutPanel, "Top Cut");
        cards.add(clockPanel, "Clock");
        shell.add(cards, BorderLayout.CENTER);
        return shell;
    }

    private JPanel createSidebar() {
        sidebar.setBackground(AppTheme.sidebar(darkMode));
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, AppTheme.border(darkMode)));

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.insets = new Insets(0, 0, 0, 0);

        sidebarBrand = new JLabel("<html><b>MTG Event<br>Manager</b></html>");
        sidebarBrand.setForeground(AppTheme.BLUE);
        sidebarBrand.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, AppTheme.border(darkMode)),
                BorderFactory.createEmptyBorder(24, 18, 28, 18)));
        sidebarBrand.setFont(sidebarBrand.getFont().deriveFont(22f));
        sidebarBrand.setPreferredSize(new Dimension(230, 112));
        sidebar.add(sidebarBrand, c);

        addNavButton(c, eventPanel, "Quick Settings");
        addNavButton(c, playerPanel, "Players");
        addNavButton(c, pairingsPanel, "Pairings");
        addNavButton(c, resultsPanel, "Results");
        addNavButton(c, standingsPanel, "Standings");
        addNavButton(c, topCutPanel, "Top Cut");
        addNavButton(c, clockPanel, "Round Timer");

        c.gridy++;
        c.weighty = 1;
        JPanel spacer = new JPanel();
        spacer.setBackground(AppTheme.sidebar(darkMode));
        sidebar.add(spacer, c);
        sidebar.setPreferredSize(new Dimension(230, 10));
        return sidebar;
    }

    private void addNavButton(GridBagConstraints c, Component panel, String title) {
        JButton button = new JButton(title);
        button.putClientProperty(AppTheme.MANUAL_BUTTON, true);
        button.addActionListener(e -> showPanel(panel));
        navigationButtons.put(panel, button);
        c.gridy++;
        c.weighty = 0;
        sidebar.add(button, c);
    }

    private void showPanel(Component panel) {
        if (panel == resultsPanel && previousPanel != resultsPanel) {
            resultsPanel.selectLatestRoundForEntry();
        }
        ((CardLayout) cards.getLayout()).show(cards, cardName(panel));
        previousPanel = panel;
        navigationButtons.forEach((component, button) -> AppTheme.styleSidebarButton(button, component == panel, darkMode));
    }

    private String cardName(Component panel) {
        if (panel == eventPanel) {
            return "Event";
        }
        if (panel == playerPanel) {
            return "Players";
        }
        if (panel == pairingsPanel) {
            return "Pairings";
        }
        if (panel == resultsPanel) {
            return "Results";
        }
        if (panel == standingsPanel) {
            return "Standings";
        }
        if (panel == topCutPanel) {
            return "Top Cut";
        }
        return "Clock";
    }

    private void updateSidebarLabels() {
        setNavText(eventPanel, "Quick Settings");
        setNavText(playerPanel, "Players", event.getPlayers().size());
        setNavText(pairingsPanel, "Pairings", event.getRounds().size());
        setNavText(resultsPanel, "Results", openMatchCount());
        setNavText(standingsPanel, "Standings");
        long topCutRounds = event.getRounds().stream().filter(Round::isPlayoffRound).count();
        setNavText(topCutPanel, "Top Cut", topCutRounds);
        setNavText(clockPanel, "Round Timer");
    }

    private void setNavText(Component panel, String label) {
        JButton button = navigationButtons.get(panel);
        if (button != null) {
            button.setText(label);
        }
    }

    private void setNavText(Component panel, String label, long count) {
        JButton button = navigationButtons.get(panel);
        if (button != null) {
            button.setText(label + "    " + count);
        }
    }

    private long openMatchCount() {
        Round round = event.getCurrentRound();
        if (round == null) {
            return 0;
        }
        return round.getMatches().stream().filter(match -> !match.isCompleted()).count();
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        menuBar.add(fileMenu());
        menuBar.add(playersMenu());
        menuBar.add(eventMenu());
        menuBar.add(helpMenu());
        return menuBar;
    }

    private JMenu fileMenu() {
        JMenu file = new JMenu("File");
        JMenuItem newEvent = item("New Event", () -> showPanel(eventPanel));
        JMenuItem open = item("Open Event", this::openEvent);
        JMenuItem save = item("Save Event", this::saveEvent);
        JMenuItem importPlayers = item("Import Players CSV", this::importPlayersFromCsv);
        JMenuItem printStandings = item("Print Standings", standingsPanel::printStandings);
        JMenu export = new JMenu("Export CSV");
        export.add(item("Player List", () -> exportCsv("players", selected -> eventService.exportPlayers(event, selected))));
        export.add(item("Current Round Pairings", () -> exportCsv("pairings", selected -> eventService.exportCurrentPairings(event, selected))));
        export.add(item("Standings", () -> exportCsv("standings", selected -> eventService.exportStandings(standingService.calculateStandings(event), selected))));
        export.add(item("Results", () -> exportCsv("results", selected -> eventService.exportResults(event, selected))));
        JMenuItem exit = item("Exit", this::dispose);
        file.add(newEvent);
        file.add(open);
        file.add(save);
        file.add(importPlayers);
        file.add(printStandings);
        file.add(export);
        file.addSeparator();
        file.add(exit);
        return file;
    }

    private JMenu playersMenu() {
        JMenu players = new JMenu("Players");
        players.add(item("Player List", () -> showPanel(playerPanel)));
        players.add(item("Import Players CSV", this::importPlayersFromCsv));
        players.add(item("Penalty Entry", () -> showPenaltyEntry(null)));
        return players;
    }

    private JMenu eventMenu() {
        JMenu eventMenu = new JMenu("Event");
        eventMenu.add(item("Generate Round", this::generateSwissRound));
        eventMenu.add(item("Enter Results", () -> showPanel(resultsPanel)));
        eventMenu.add(item("View Standings", () -> showPanel(standingsPanel)));
        eventMenu.add(item("Print Standings", standingsPanel::printStandings));
        eventMenu.add(item("Create Top Cut", this::createTopCut));
        eventMenu.add(item("Clock", () -> showPanel(clockPanel)));
        return eventMenu;
    }

    private void applyStandardColors() {
        AppTheme.install(darkMode);
        getContentPane().setBackground(AppTheme.background(darkMode));
        cards.setBackground(AppTheme.background(darkMode));
        AppTheme.styleTree(this, darkMode);
        restyleSidebar();
        eventInfoPanel.applyTheme(darkMode);
        clockPanel.applyTheme(darkMode);
        navigationButtons.forEach((component, button) -> AppTheme.styleSidebarButton(button, component == previousPanel, darkMode));
        repaint();
    }

    private void restyleSidebar() {
        sidebar.setBackground(AppTheme.sidebar(darkMode));
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, AppTheme.border(darkMode)));
        if (sidebarBrand != null) {
            sidebarBrand.setForeground(AppTheme.BLUE);
            sidebarBrand.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, AppTheme.border(darkMode)),
                    BorderFactory.createEmptyBorder(24, 18, 28, 18)));
        }
        for (Component component : sidebar.getComponents()) {
            if (component instanceof JPanel panel) {
                panel.setBackground(AppTheme.sidebar(darkMode));
            }
        }
    }

    private JMenu helpMenu() {
        JMenu help = new JMenu("Help");
        help.add(item("User Manual", this::showHelpManual));
        help.add(item("About", this::showAboutDialog));
        return help;
    }

    private void showHelpManual() {
        showInfo("""
                MTG Event Manager User Manual

                Event: create a new event, choose Swiss only or Swiss with Top Cut, set the Top Cut size, and enable team events when players belong to teams.
                Players: add, edit, remove, import, drop players, and open Penalty Entry for the selected player.
                Pairings: generate the next Swiss round, inspect each round's pairings, and search the pairing table.
                Results: choose a round, enter match scores, mark player drops for that round, generate match result slips, and click table headers to sort results ascending or descending.
                Standings: choose the round to view or print standings, refresh player standings, and switch to team standings when the event is a team event.
                Top Cut: create the playoff bracket after Swiss rounds, generate following playoff rounds, and enter playoff results from the Results tab.
                Clock: set the round duration, start, pause, reset, and hear the round-over alert.
                File: open and save events, import players from CSV, export players, pairings, standings, and results to CSV.
                Penalty Entry: record infractions, penalties, automatic upgrades, and related player history.
                """);
    }

    private void showAboutDialog() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.add(new JLabel("<html><b>MTG Event Manager</b><br>"
                + "Swiss tournament management with Top Cut.<br><br>"
                + "Creator: Vitor Silva aka MagicShop<br>"
                + "Premodern Portugal: www.premodern.pt</html>"), BorderLayout.CENTER);
        JLabel version = new JLabel("v3.1");
        version.setHorizontalAlignment(JLabel.RIGHT);
        panel.add(version, BorderLayout.SOUTH);
        JOptionPane.showMessageDialog(this, panel, "About", JOptionPane.INFORMATION_MESSAGE);
    }

    private JMenuItem item(String label, ThrowingRunnable action) {
        JMenuItem item = new JMenuItem(label);
        item.addActionListener(e -> {
            try {
                action.run();
            } catch (Exception exception) {
                showError(exception);
            }
        });
        return item;
    }

    private void openEvent() throws IOException {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            currentFile = chooser.getSelectedFile();
            event = storageService.loadEvent(currentFile);
            eventPanel.lockCurrentEvent();
            resultService.recalculateEvent(event);
            refreshAll();
        }
    }

    private void saveEvent() throws IOException {
        if (isBrowserRuntime()) {
            saveEventForBrowserDownload();
            return;
        }
        if (currentFile == null) {
            saveEventAs();
            return;
        }
        storageService.saveEvent(event, currentFile);
        showInfo("Event saved.");
    }

    private void saveEventAs() throws IOException {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(safeFileName(event.getName()) + ".json"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            currentFile = chooser.getSelectedFile();
            storageService.saveEvent(event, currentFile);
            showInfo("Event saved.");
        }
    }

    private void saveEventForBrowserDownload() throws IOException {
        String fileName = safeFileName(event.getName()) + ".json";
        File file = new File("/files/" + fileName);
        File parent = file.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        storageService.saveEvent(event, file);
        requestBrowserDownload(file.getPath().replace('\\', '/'), fileName);
        showInfo("Download started.");
    }

    private void requestBrowserDownload(String path, String fileName) throws IOException {
        try {
            Runtime.getRuntime().exec(new String[] { "eventmanager-download", path, fileName });
        } catch (IOException exception) {
            throw new IOException("Event saved in the browser virtual filesystem at " + path
                    + ", but the browser download could not be started.", exception);
        }
    }

    private boolean isBrowserRuntime() {
        return Boolean.getBoolean("eventmanager.browser");
    }

    private void exportCsv(String defaultName, CsvAction action) {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(defaultName + ".csv"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                action.export(chooser.getSelectedFile());
                showInfo("CSV exported.");
            } catch (Exception exception) {
                showError(exception);
            }
        }
    }

    private String safeFileName(String value) {
        return value == null ? "event" : value.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private void refreshAfterRoundGeneration(Round round) {
        showPanel(round.isPlayoffRound() ? topCutPanel : pairingsPanel);
        autoSaveEvent();
        refreshAll();
    }

    public void autoSaveEvent() {
        try {
            File file = currentFile == null ? autoSaveFile() : currentFile;
            storageService.saveEvent(event, file);
            currentFile = file;
        } catch (IOException exception) {
            showError(new IOException("Autosave failed: " + exception.getMessage(), exception));
        }
    }

    private File autoSaveFile() {
        File directory = currentFile != null && currentFile.getParentFile() != null
                ? currentFile.getParentFile()
                : new File(System.getProperty("user.dir"));
        return new File(directory, safeFileName(event.getName()) + "-autosave.json");
    }

    private void warnSameTeamPairings(Round round) {
        List<Match> sameTeamMatches = round.getMatches().stream()
                .filter(match -> !match.isBye()
                        && match.getPlayer1().hasTeam()
                        && match.getPlayer2().hasTeam()
                        && match.getPlayer1().getTeam().equalsIgnoreCase(match.getPlayer2().getTeam()))
                .toList();
        if (!sameTeamMatches.isEmpty()) {
            showInfo("Warning: some pairings are between players from the same team because no other valid pairing was available.");
        }
    }

    private void showWinnerPopup() {
        if (event.isTeamEvent()) {
            showTeamWinnerPopup();
            return;
        }
        PlayerWinner winner = individualWinner();
        if (winner.player() == null) {
            return;
        }
        JOptionPane.showMessageDialog(this,
                htmlWinner(winner.player().getFullName(), winner.subtitle()),
                "Winner",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void showTeamWinnerPopup() {
        String team = winningTeamName();
        if (team == null || team.isBlank()) {
            return;
        }
        String players = event.getPlayers().stream()
                .filter(player -> player.hasTeam() && player.getTeam().equalsIgnoreCase(team))
                .map(player -> "<div>" + escapeHtml(player.getFullName()) + "</div>")
                .collect(Collectors.joining());
        JOptionPane.showMessageDialog(this,
                htmlWinner(team, players.isBlank() ? "Winning team" : players),
                "Winning Team",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private PlayerWinner individualWinner() {
        Player champion = topCutService.getChampion(event);
        if (champion != null) {
            return new PlayerWinner(champion, "Event champion");
        }
        List<Standing> standings = standingService.calculateStandings(event);
        return standings.isEmpty() ? new PlayerWinner(null, "") : new PlayerWinner(standings.get(0).getPlayer(), "Standings winner");
    }

    private String winningTeamName() {
        Player champion = topCutService.getChampion(event);
        if (champion != null && champion.hasTeam()) {
            return champion.getTeam();
        }
        return standingService.calculateTeamStandings(event).stream()
                .findFirst()
                .map(standing -> standing.team())
                .orElse("");
    }

    private String htmlWinner(String title, String body) {
        return "<html><div style='text-align:center;width:360px;'>"
                + "<div style='font-size:14px;'>Winner</div>"
                + "<h1 style='color:#148c50;'>" + escapeHtml(title) + "</h1>"
                + "<div style='font-size:12px;'>" + body + "</div>"
                + "</div></html>";
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    @FunctionalInterface
    private interface CsvAction {
        void export(File file) throws Exception;
    }

    private record PlayerWinner(Player player, String subtitle) {
    }
}
