package pt.premodern.eventmanager.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.print.PrinterException;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;

import pt.premodern.eventmanager.model.Player;
import pt.premodern.eventmanager.model.Round;
import pt.premodern.eventmanager.model.Standing;
import pt.premodern.eventmanager.service.StandingService.TeamStanding;

public class StandingsPanel extends JPanel {
    private final MainFrame frame;
    private final NumberFormat percent = NumberFormat.getPercentInstance(Locale.getDefault());
    private final JLabel roundLabel = new JLabel();
    private final JComboBox<RoundItem> roundCombo = new JComboBox<>();
    private final JButton teamStandings = new JButton("Team Standings");
    private final JButton playerStandings = new JButton("Player Standings");
    private final DefaultTableModel model = new DefaultTableModel(0, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(model);
    private boolean showingTeams;
    private boolean refreshing;

    public StandingsPanel(MainFrame frame) {
        super(new BorderLayout(12, 12));
        this.frame = frame;
        percent.setMinimumFractionDigits(2);
        percent.setMaximumFractionDigits(2);
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        roundCombo.addActionListener(e -> {
            if (!refreshing) {
                refreshData();
            }
        });
        JButton refresh = new JButton("Refresh Standings");
        refresh.addActionListener(e -> refreshData());
        JButton print = new JButton("Print Standings");
        print.addActionListener(e -> printStandings());
        teamStandings.addActionListener(e -> {
            showingTeams = true;
            refreshData();
        });
        playerStandings.addActionListener(e -> {
            showingTeams = false;
            refreshData();
        });
        toolbar.add(new JLabel("Round"));
        toolbar.add(roundCombo);
        toolbar.add(refresh);
        toolbar.add(print);
        toolbar.add(playerStandings);
        toolbar.add(teamStandings);
        toolbar.add(roundLabel);
        add(toolbar, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    public void refreshData() {
        Integer selectedRound = selectedRoundNumber();
        refreshing = true;
        populateRoundCombo(selectedRound);
        model.setRowCount(0);
        teamStandings.setVisible(frame.getEvent().isTeamEvent());
        playerStandings.setVisible(frame.getEvent().isTeamEvent());
        if (!frame.getEvent().isTeamEvent()) {
            showingTeams = false;
        }
        roundLabel.setText("Standings after round " + selectedRoundNumber());
        if (showingTeams) {
            populateTeamStandings();
            refreshing = false;
            return;
        }
        model.setColumnIdentifiers(new Object[] {"Position", "Name", "Team", "Match Points", "OMW%", "GW%", "OGW%"});
        for (Standing standing : frame.getStandingService().calculateStandingsAfterRound(frame.getEvent(), selectedRoundNumber())) {
            Player player = standing.getPlayer();
            model.addRow(new Object[] {
                    standing.getRank(),
                    player.getFullName(),
                    player.getTeam(),
                    standing.getMatchPoints(),
                    percent.format(standing.getOmwPercentage()),
                    percent.format(standing.getGwPercentage()),
                    percent.format(standing.getOgwPercentage())
            });
        }
        refreshing = false;
    }

    private void populateTeamStandings() {
        model.setColumnIdentifiers(new Object[] {"Position", "Team", "Players", "Match Points", "OMW%", "GW%", "OGW%"});
        for (TeamStanding standing : frame.getStandingService().calculateTeamStandingsAfterRound(frame.getEvent(), selectedRoundNumber())) {
            model.addRow(new Object[] {
                    standing.rank(),
                    standing.team(),
                    standing.players().stream().map(Player::getFullName).collect(Collectors.joining(", ")),
                    standing.matchPoints(),
                    percent.format(standing.omwPercentage()),
                    percent.format(standing.gwPercentage()),
                    percent.format(standing.ogwPercentage())
            });
        }
    }

    private void populateRoundCombo(Integer selectedRound) {
        roundCombo.removeAllItems();
        int latestRound = latestSwissRoundNumber();
        if (latestRound == 0) {
            roundCombo.addItem(new RoundItem(0));
            roundCombo.setSelectedIndex(0);
            return;
        }

        for (Round round : frame.getEvent().getRounds()) {
            if (!round.isPlayoffRound()) {
                roundCombo.addItem(new RoundItem(round.getNumber()));
            }
        }
        selectRound(selectedRound == null || selectedRound > latestRound ? latestRound : selectedRound);
    }

    private void selectRound(int roundNumber) {
        for (int i = 0; i < roundCombo.getItemCount(); i++) {
            if (roundCombo.getItemAt(i).roundNumber() == roundNumber) {
                roundCombo.setSelectedIndex(i);
                return;
            }
        }
        if (roundCombo.getItemCount() > 0) {
            roundCombo.setSelectedIndex(roundCombo.getItemCount() - 1);
        }
    }

    private Integer selectedRoundNumber() {
        RoundItem item = (RoundItem) roundCombo.getSelectedItem();
        return item == null ? latestSwissRoundNumber() : item.roundNumber();
    }

    private int latestSwissRoundNumber() {
        return frame.getEvent().getRounds().stream()
                .filter(round -> !round.isPlayoffRound())
                .mapToInt(Round::getNumber)
                .max()
                .orElse(0);
    }

    public void printStandings() {
        refreshData();
        if (model.getRowCount() == 0) {
            frame.showInfo("There are no standings to print.");
            return;
        }

        JTable printTable = printableTable();
        String type = showingTeams ? "Team Standings" : "Standings";
        String title = frame.getEvent().getName() + " - " + type + " after round "
                + selectedRoundNumber();
        try {
            boolean completed = printTable.print(
                    JTable.PrintMode.FIT_WIDTH,
                    new MessageFormat(title),
                    new MessageFormat("Page {0}"),
                    true,
                    null,
                    true);
            if (completed) {
                frame.showInfo("Standings sent to printer.");
            }
        } catch (PrinterException exception) {
            frame.showError(exception);
        }
    }

    private JTable printableTable() {
        JTable printTable = new JTable(model);
        printTable.setSize(table.getSize());
        printTable.setRowHeight(22);
        printTable.setForeground(Color.BLACK);
        printTable.setBackground(Color.WHITE);
        printTable.setGridColor(Color.LIGHT_GRAY);
        printTable.setShowGrid(true);
        printTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
        renderer.setForeground(Color.BLACK);
        renderer.setBackground(Color.WHITE);
        renderer.setHorizontalAlignment(SwingConstants.LEFT);
        printTable.setDefaultRenderer(Object.class, renderer);
        printTable.setDefaultRenderer(Number.class, renderer);
        printTable.setDefaultRenderer(Integer.class, renderer);

        JTableHeader header = printTable.getTableHeader();
        header.setForeground(Color.BLACK);
        header.setBackground(Color.WHITE);
        header.setFont(header.getFont().deriveFont(Font.BOLD));
        return printTable;
    }

    private record RoundItem(int roundNumber) {
        @Override
        public String toString() {
            return "After round " + roundNumber;
        }
    }
}
