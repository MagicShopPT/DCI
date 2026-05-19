package pt.premodern.eventmanager.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

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
    private final JTabbedPane tabs = new JTabbedPane();
    private final EventPanel eventPanel;
    private final EventInfoPanel eventInfoPanel;
    private final PlayerPanel playerPanel;
    private final PairingsPanel pairingsPanel;
    private final ResultsPanel resultsPanel;
    private final StandingsPanel standingsPanel;
    private final TopCutPanel topCutPanel;
    private final ClockPanel clockPanel;
    private Component previousTab;

    public MainFrame() {
        super("MTG Event Manager");
        this.event = eventService.createEvent("Novo Evento", EventType.SWISS_ONLY, 0);

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
        add(eventInfoPanel, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);

        tabs.addTab("Evento", eventPanel);
        tabs.addTab("Jogadores", playerPanel);
        tabs.addTab("Pairings", pairingsPanel);
        tabs.addTab("Resultados", resultsPanel);
        tabs.addTab("Standings", standingsPanel);
        tabs.addTab("Top Cut", topCutPanel);
        tabs.addTab("Clock", clockPanel);
        previousTab = tabs.getSelectedComponent();
        tabs.addChangeListener(e -> {
            Component selected = tabs.getSelectedComponent();
            if (selected == resultsPanel && previousTab != resultsPanel) {
                resultsPanel.selectLatestRoundForEntry();
            }
            previousTab = selected;
        });

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

    public void refreshAll() {
        eventPanel.refreshData();
        eventInfoPanel.refreshData();
        playerPanel.refreshData();
        pairingsPanel.refreshData();
        resultsPanel.refreshData();
        standingsPanel.refreshData();
        topCutPanel.refreshData();
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
                    .map(player -> "<div>" + escapeHtml(player.getFullName()) + "</div>")
                    .collect(Collectors.joining());
            return "<html><b>" + escapeHtml(team) + "</b>" + players + "</html>";
        }
        PlayerWinner winner = individualWinner();
        return winner.player() == null ? "-" : winner.player().getFullName();
    }

    public void showInfo(String message) {
        JOptionPane.showMessageDialog(this, message, "MTG Event Manager", JOptionPane.INFORMATION_MESSAGE);
    }

    public void showError(Exception exception) {
        JOptionPane.showMessageDialog(this, exception.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
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
        tabs.setSelectedComponent(resultsPanel);
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
                    Round next = topCutService.generateNextTopCutRound(event);
                    if (next != null) {
                        autoSaveEvent();
                    }
                } catch (Exception ignored) {
                    // The Top Cut panel still offers manual generation if the round is not ready.
                }
            }
        }
        refreshAll();
        if (!finishedBefore && event.getStatus() == EventStatus.FINISHED) {
            showWinnerPopup();
        }
    }

    public void importPlayersFromCsv() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Importar Jogadores CSV");
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                int imported = eventService.importPlayers(event, chooser.getSelectedFile());
                refreshAll();
                showInfo(imported + " jogadores importados.");
            } catch (Exception exception) {
                showError(exception);
            }
        }
    }

    public void showPenaltyEntry(Player player) {
        if (event.getPlayers().isEmpty()) {
            showInfo("Não existem jogadores no evento.");
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
            refreshAll();
        }
        showInfo("Penalty registada: " + entry.getAppliedPenalty() + ".");
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
        JMenu file = new JMenu("Ficheiro");
        JMenuItem newEvent = item("Novo Evento", () -> tabs.setSelectedComponent(eventPanel));
        JMenuItem open = item("Abrir Evento", this::openEvent);
        JMenuItem save = item("Guardar Evento", this::saveEvent);
        JMenuItem saveAs = item("Guardar Como", this::saveEventAs);
        JMenuItem importPlayers = item("Importar Jogadores CSV", this::importPlayersFromCsv);
        JMenu export = new JMenu("Exportar CSV");
        export.add(item("Lista de Jogadores", () -> exportCsv("jogadores", selected -> eventService.exportPlayers(event, selected))));
        export.add(item("Pairings da Ronda Atual", () -> exportCsv("pairings", selected -> eventService.exportCurrentPairings(event, selected))));
        export.add(item("Standings", () -> exportCsv("standings", selected -> eventService.exportStandings(standingService.calculateStandings(event), selected))));
        export.add(item("Resultados", () -> exportCsv("resultados", selected -> eventService.exportResults(event, selected))));
        JMenuItem exit = item("Sair", this::dispose);
        file.add(newEvent);
        file.add(open);
        file.add(save);
        file.add(saveAs);
        file.add(importPlayers);
        file.add(export);
        file.addSeparator();
        file.add(exit);
        return file;
    }

    private JMenu playersMenu() {
        JMenu players = new JMenu("Jogadores");
        players.add(item("Adicionar Jogador", () -> tabs.setSelectedComponent(playerPanel)));
        players.add(item("Lista de Jogadores", () -> tabs.setSelectedComponent(playerPanel)));
        players.add(item("Importar Jogadores CSV", this::importPlayersFromCsv));
        players.add(item("Penalty Entry", () -> showPenaltyEntry(null)));
        return players;
    }

    private JMenu eventMenu() {
        JMenu eventMenu = new JMenu("Evento");
        eventMenu.add(item("Gerar Ronda", this::generateSwissRound));
        eventMenu.add(item("Inserir Resultados", () -> tabs.setSelectedComponent(resultsPanel)));
        eventMenu.add(item("Ver Standings", () -> tabs.setSelectedComponent(standingsPanel)));
        eventMenu.add(item("Criar Top Cut", this::createTopCut));
        eventMenu.add(item("Clock", () -> tabs.setSelectedComponent(clockPanel)));
        return eventMenu;
    }

    private void applyStandardColors() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // Keep the current Look & Feel if the system one is not available.
        }
        SwingUtilities.updateComponentTreeUI(this);
        eventInfoPanel.applyStandardTheme();
        clockPanel.applyStandardTheme();
        repaint();
    }

    private JMenu helpMenu() {
        JMenu help = new JMenu("Ajuda");
        help.add(item("Sobre", () -> showInfo("MTG Event Manager\nGestao de torneios suicos com Top Cut.")));
        return help;
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
            resultService.recalculateEvent(event);
            refreshAll();
        }
    }

    private void saveEvent() throws IOException {
        if (currentFile == null) {
            saveEventAs();
            return;
        }
        storageService.saveEvent(event, currentFile);
        showInfo("Evento guardado.");
    }

    private void saveEventAs() throws IOException {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(safeFileName(event.getName()) + ".json"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            currentFile = chooser.getSelectedFile();
            storageService.saveEvent(event, currentFile);
            showInfo("Evento guardado.");
        }
    }

    private void exportCsv(String defaultName, CsvAction action) {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(defaultName + ".csv"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                action.export(chooser.getSelectedFile());
                showInfo("CSV exportado.");
            } catch (Exception exception) {
                showError(exception);
            }
        }
    }

    private String safeFileName(String value) {
        return value == null ? "evento" : value.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private void refreshAfterRoundGeneration(Round round) {
        tabs.setSelectedComponent(round.isPlayoffRound() ? topCutPanel : pairingsPanel);
        autoSaveEvent();
        refreshAll();
    }

    private void autoSaveEvent() {
        try {
            storageService.saveEvent(event, autoSaveFile());
        } catch (IOException exception) {
            showError(new IOException("A ronda foi gerada, mas o autosave falhou: " + exception.getMessage(), exception));
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
            showInfo("Aviso: existem emparelhamentos entre jogadores da mesma equipa por impossibilidade tecnica.");
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
                "Vencedor",
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
                htmlWinner(team, players.isBlank() ? "Equipa vencedora" : players),
                "Equipa Vencedora",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private PlayerWinner individualWinner() {
        Player champion = topCutService.getChampion(event);
        if (champion != null) {
            return new PlayerWinner(champion, "Campeao do evento");
        }
        List<Standing> standings = standingService.calculateStandings(event);
        return standings.isEmpty() ? new PlayerWinner(null, "") : new PlayerWinner(standings.get(0).getPlayer(), "Vencedor dos standings");
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
                + "<div style='font-size:14px;'>Vencedor</div>"
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
