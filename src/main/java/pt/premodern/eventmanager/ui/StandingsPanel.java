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
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;

import pt.premodern.eventmanager.model.Player;
import pt.premodern.eventmanager.model.Standing;
import pt.premodern.eventmanager.service.StandingService.TeamStanding;

public class StandingsPanel extends JPanel {
    private final MainFrame frame;
    private final NumberFormat percent = NumberFormat.getPercentInstance(Locale.getDefault());
    private final JLabel roundLabel = new JLabel();
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

    public StandingsPanel(MainFrame frame) {
        super(new BorderLayout(12, 12));
        this.frame = frame;
        percent.setMinimumFractionDigits(2);
        percent.setMaximumFractionDigits(2);
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
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
        toolbar.add(refresh);
        toolbar.add(print);
        toolbar.add(playerStandings);
        toolbar.add(teamStandings);
        toolbar.add(roundLabel);
        add(toolbar, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    public void refreshData() {
        model.setRowCount(0);
        teamStandings.setVisible(frame.getEvent().isTeamEvent());
        playerStandings.setVisible(frame.getEvent().isTeamEvent());
        if (!frame.getEvent().isTeamEvent()) {
            showingTeams = false;
        }
        roundLabel.setText("Standings after round " + frame.getEvent().getCurrentRoundNumber());
        if (showingTeams) {
            populateTeamStandings();
            return;
        }
        model.setColumnIdentifiers(new Object[] {"Position", "Name", "Team", "Match Points", "OMW%", "GW%", "OGW%"});
        for (Standing standing : frame.getStandingService().calculateStandings(frame.getEvent())) {
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
    }

    private void populateTeamStandings() {
        model.setColumnIdentifiers(new Object[] {"Position", "Team", "Players", "Match Points", "OMW%", "GW%", "OGW%"});
        for (TeamStanding standing : frame.getStandingService().calculateTeamStandings(frame.getEvent())) {
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

    public void printStandings() {
        refreshData();
        if (model.getRowCount() == 0) {
            frame.showInfo("There are no standings to print.");
            return;
        }

        JTable printTable = printableTable();
        String type = showingTeams ? "Team Standings" : "Standings";
        String title = frame.getEvent().getName() + " - " + type + " after round "
                + frame.getEvent().getCurrentRoundNumber();
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
}
