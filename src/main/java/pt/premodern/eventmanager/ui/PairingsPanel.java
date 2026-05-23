package pt.premodern.eventmanager.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.RowFilter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import pt.premodern.eventmanager.model.Match;
import pt.premodern.eventmanager.model.Player;
import pt.premodern.eventmanager.model.Round;

public class PairingsPanel extends JPanel {
    private final MainFrame frame;
    private final NumberFormat percent = NumberFormat.getPercentInstance(Locale.getDefault());
    private final JPanel roundButtonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 8));
    private final JTextField searchField = new JTextField(24);
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[] {
                    "Table",
                    "Player/Team 1", "Team 1", "Match Record 1", "Game Record 1", "Points 1", "OMW% 1", "TGW% 1", "OGW% 1",
                    "Player/Team 2", "Team 2", "Match Record 2", "Game Record 2", "Points 2", "OMW% 2", "TGW% 2", "OGW% 2",
                    "Status"
            }, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(model);
    private final TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
    private int selectedRoundNumber = -1;

    public PairingsPanel(MainFrame frame) {
        super(new BorderLayout(12, 12));
        this.frame = frame;
        percent.setMinimumFractionDigits(5);
        percent.setMaximumFractionDigits(5);
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        table.setAutoCreateRowSorter(true);
        table.setRowSorter(sorter);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        add(toolbar(), BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                applySearch();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                applySearch();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                applySearch();
            }
        });
    }

    public void refreshData() {
        int previousRound = selectedRoundNumber;
        rebuildRoundButtons(previousRound);
        populateTable();
    }

    private JPanel toolbar() {
        JPanel wrapper = new JPanel(new BorderLayout(8, 8));
        roundButtonsPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 10, 4));
        wrapper.add(roundButtonsPanel, BorderLayout.NORTH);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 6));
        JButton generate = new JButton("Generate Next Swiss Round");
        JButton refresh = new JButton("Refresh");
        generate.addActionListener(e -> frame.generateSwissRound());
        refresh.addActionListener(e -> frame.refreshAll());
        controls.add(new JLabel("Search results"));
        controls.add(searchField);
        controls.add(generate);
        controls.add(refresh);
        wrapper.add(controls, BorderLayout.SOUTH);
        return wrapper;
    }

    private void rebuildRoundButtons(int previousRound) {
        roundButtonsPanel.removeAll();
        List<Round> rounds = frame.getEvent().getRounds();
        if (rounds.isEmpty()) {
            selectedRoundNumber = -1;
            roundButtonsPanel.add(new JLabel("No rounds generated"));
            roundButtonsPanel.revalidate();
            roundButtonsPanel.repaint();
            return;
        }

        ButtonGroup group = new ButtonGroup();
        roundButtonsPanel.setLayout(new GridLayout(1, Math.max(1, rounds.size()), 12, 8));
        boolean selected = false;
        for (Round round : rounds) {
            JToggleButton button = new JToggleButton("Round " + round.getNumber());
            button.addActionListener(e -> {
                selectedRoundNumber = round.getNumber();
                populateTable();
            });
            group.add(button);
            roundButtonsPanel.add(button);
            if (round.getNumber() == previousRound) {
                button.setSelected(true);
                selectedRoundNumber = round.getNumber();
                selected = true;
            }
        }
        if (!selected) {
            Round lastRound = rounds.get(rounds.size() - 1);
            selectedRoundNumber = lastRound.getNumber();
            ((JToggleButton) roundButtonsPanel.getComponent(rounds.size() - 1)).setSelected(true);
        }
        roundButtonsPanel.revalidate();
        roundButtonsPanel.repaint();
    }

    private void populateTable() {
        model.setRowCount(0);
        Round round = selectedRound();
        if (round == null) {
            return;
        }
        Map<UUID, SnapshotStats> stats = buildSnapshotStats(selectedRoundNumber);

        for (Match match : round.getMatches()) {
            Player p1 = match.getPlayer1();
            Player p2 = match.getPlayer2();
            model.addRow(new Object[] {
                    match.getTableNumber(),
                    name(p1),
                    team(p1),
                    matchRecord(stats, p1),
                    gameRecord(stats, p1),
                    points(stats, p1),
                    omw(stats, p1),
                    gw(stats, p1),
                    ogw(stats, p1),
                    match.isBye() ? "BYE" : name(p2),
                    team(p2),
                    matchRecord(stats, p2),
                    gameRecord(stats, p2),
                    points(stats, p2),
                    omw(stats, p2),
                    gw(stats, p2),
                    ogw(stats, p2),
                    match.isCompleted() ? "Completed" : "Open"
            });
        }
        applySearch();
    }

    private Map<UUID, SnapshotStats> buildSnapshotStats(int roundNumber) {
        Map<UUID, SnapshotStats> stats = new HashMap<>();
        for (Player player : frame.getEvent().getPlayers()) {
            stats.put(player.getId(), new SnapshotStats());
        }

        for (Round round : frame.getEvent().getRounds()) {
            if (round.isPlayoffRound() || round.getNumber() > roundNumber) {
                continue;
            }
            for (Match match : round.getMatches()) {
                if (!match.isCompleted()) {
                    continue;
                }
                if (match.isBye()) {
                    SnapshotStats playerStats = stats.get(match.getPlayer1().getId());
                    if (playerStats != null) {
                        playerStats.wins++;
                        playerStats.matchPoints += 3;
                        playerStats.gamesWon += Math.max(2, match.getPlayer1GamesWon());
                        playerStats.matchesPlayed++;
                    }
                    continue;
                }

                Player p1 = match.getPlayer1();
                Player p2 = match.getPlayer2();
                if (p1 == null || p2 == null) {
                    continue;
                }
                SnapshotStats s1 = stats.get(p1.getId());
                SnapshotStats s2 = stats.get(p2.getId());
                if (s1 == null || s2 == null) {
                    continue;
                }
                s1.gamesWon += match.getPlayer1GamesWon();
                s1.gamesLost += match.getPlayer2GamesWon();
                s1.gamesDrawn += match.getDrawGames();
                s2.gamesWon += match.getPlayer2GamesWon();
                s2.gamesLost += match.getPlayer1GamesWon();
                s2.gamesDrawn += match.getDrawGames();
                s1.matchesPlayed++;
                s2.matchesPlayed++;
                s1.opponents.add(p2.getId());
                s2.opponents.add(p1.getId());

                if (match.getWinner() == null) {
                    s1.draws++;
                    s2.draws++;
                    s1.matchPoints++;
                    s2.matchPoints++;
                } else if (Objects.equals(match.getWinner().getId(), p1.getId())) {
                    s1.wins++;
                    s2.losses++;
                    s1.matchPoints += 3;
                } else {
                    s2.wins++;
                    s1.losses++;
                    s2.matchPoints += 3;
                }
            }
        }
        return stats;
    }

    private Round selectedRound() {
        return frame.getEvent().getRounds().stream()
                .filter(round -> round.getNumber() == selectedRoundNumber)
                .findFirst()
                .orElse(null);
    }

    private void applySearch() {
        String text = searchField.getText();
        sorter.setRowFilter(text == null || text.isBlank() ? null : RowFilter.regexFilter("(?i)" + Pattern.quote(text.trim())));
    }

    private String name(Player player) {
        return player == null ? "" : player.getFullName();
    }

    private String team(Player player) {
        return player == null ? "" : player.getTeam();
    }

    private String gameRecord(Map<UUID, SnapshotStats> stats, Player player) {
        SnapshotStats playerStats = stats(player, stats);
        return playerStats == null ? "" : playerStats.gamesWon + "-" + playerStats.gamesLost + "-" + playerStats.gamesDrawn;
    }

    private String matchRecord(Map<UUID, SnapshotStats> stats, Player player) {
        SnapshotStats playerStats = stats(player, stats);
        return playerStats == null ? "" : playerStats.wins + "-" + playerStats.losses + "-" + playerStats.draws;
    }

    private String points(Map<UUID, SnapshotStats> stats, Player player) {
        SnapshotStats playerStats = stats(player, stats);
        return playerStats == null ? "" : String.valueOf(playerStats.matchPoints);
    }

    private String omw(Map<UUID, SnapshotStats> stats, Player player) {
        SnapshotStats playerStats = stats(player, stats);
        if (playerStats == null || playerStats.opponents.isEmpty()) {
            return "";
        }
        double value = playerStats.opponents.stream()
                .map(stats::get)
                .filter(opponent -> opponent != null)
                .mapToDouble(opponent -> Math.max(0.33, opponent.matchWinPercentage()))
                .average()
                .orElse(0.0);
        return percent.format(value);
    }

    private String gw(Map<UUID, SnapshotStats> stats, Player player) {
        SnapshotStats playerStats = stats(player, stats);
        return playerStats == null ? "" : percent.format(playerStats.gameWinPercentage());
    }

    private String ogw(Map<UUID, SnapshotStats> stats, Player player) {
        SnapshotStats playerStats = stats(player, stats);
        if (playerStats == null || playerStats.opponents.isEmpty()) {
            return "";
        }
        double value = playerStats.opponents.stream()
                .map(stats::get)
                .filter(opponent -> opponent != null)
                .mapToDouble(opponent -> Math.max(0.33, opponent.gameWinPercentage()))
                .average()
                .orElse(0.0);
        return percent.format(value);
    }

    private SnapshotStats stats(Player player, Map<UUID, SnapshotStats> stats) {
        return player == null ? null : stats.get(player.getId());
    }

    private static class SnapshotStats {
        private int wins;
        private int losses;
        private int draws;
        private int matchesPlayed;
        private int matchPoints;
        private int gamesWon;
        private int gamesLost;
        private int gamesDrawn;
        private final List<UUID> opponents = new ArrayList<>();

        private double matchWinPercentage() {
            if (matchesPlayed == 0) {
                return 0.0;
            }
            return Math.max(0.0, Math.min(1.0, matchPoints / (matchesPlayed * 3.0)));
        }

        private double gameWinPercentage() {
            int totalGames = gamesWon + gamesLost + gamesDrawn;
            if (totalGames == 0) {
                return 0.0;
            }
            return Math.max(0.0, Math.min(1.0, gamesWon / (double) totalGames));
        }
    }
}
