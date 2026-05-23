package pt.premodern.eventmanager.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

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
        teamStandings.addActionListener(e -> {
            showingTeams = true;
            refreshData();
        });
        playerStandings.addActionListener(e -> {
            showingTeams = false;
            refreshData();
        });
        toolbar.add(refresh);
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
}
