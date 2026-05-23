package pt.premodern.eventmanager.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.DefaultTableModel;

import pt.premodern.eventmanager.model.PenaltyEntry;
import pt.premodern.eventmanager.model.Player;
import pt.premodern.eventmanager.service.PenaltyService;

public class PenaltyEntryDialog extends JDialog {
    private static final String[] INFRACTIONS = {
            "Missed Trigger",
            "Looking at Extra Cards",
            "Hidden Card Error",
            "Mulligan Procedure Error",
            "Game Rule Violation",
            "Failure to Maintain Game State",
            "Tardiness",
            "Outside Assistance",
            "Slow Play",
            "Decklist Problem",
            "Deck Problem",
            "Limited Procedure Violation",
            "Communication Policy Violation",
            "Marked Cards",
            "Insufficient Shuffling",
            "Unsporting Conduct - Minor",
            "Unsporting Conduct - Major",
            "Improperly Determining a Winner",
            "Bribery and Wagering",
            "Aggressive Behavior",
            "Theft of Tournament Material",
            "Stalling",
            "Cheating"
    };

    private static final String[] PENALTIES = {
            PenaltyService.DISQUALIFICATION,
            PenaltyService.GAME_LOSS,
            PenaltyService.MATCH_LOSS,
            PenaltyService.WARNING
    };

    private final MainFrame frame;
    private final PenaltyService penaltyService = new PenaltyService();
    private final JComboBox<PlayerItem> playerCombo = new JComboBox<>();
    private final JComboBox<String> infractionCombo = new JComboBox<>(INFRACTIONS);
    private final JComboBox<String> penaltyCombo = new JComboBox<>(PENALTIES);
    private final JTextArea description = new JTextArea(4, 40);
    private final DefaultTableModel historyModel = new DefaultTableModel(
            new Object[] {"Round", "Infraction", "Penalty", "Applied", "Description"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable historyTable = new JTable(historyModel);

    public PenaltyEntryDialog(MainFrame frame, Player selectedPlayer) {
        super(frame, "Penalty Entry", true);
        this.frame = frame;
        setLayout(new BorderLayout(12, 12));
        setMinimumSize(new Dimension(820, 520));

        add(form(), BorderLayout.NORTH);
        add(history(), BorderLayout.CENTER);
        add(actions(), BorderLayout.SOUTH);
        populatePlayers(selectedPlayer);
        refreshHistory();
        getContentPane().setBackground(AppTheme.background(frame.isDarkMode()));
        AppTheme.install(frame.isDarkMode());
        AppTheme.styleTree(this, frame.isDarkMode());
        pack();
        setLocationRelativeTo(frame);
    }

    private JPanel form() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Penalty Entry"));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;

        addRow(panel, c, 0, "Player", playerCombo);
        addRow(panel, c, 1, "Infraction", infractionCombo);
        addRow(panel, c, 2, "Penalty", penaltyCombo);

        c.gridx = 0;
        c.gridy = 3;
        c.weightx = 0;
        panel.add(new JLabel("Description"), c);
        c.gridx = 1;
        c.weightx = 1;
        description.setLineWrap(true);
        description.setWrapStyleWord(true);
        panel.add(new JScrollPane(description), c);

        playerCombo.addActionListener(e -> refreshHistory());
        return panel;
    }

    private JScrollPane history() {
        historyTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        JScrollPane scroll = new JScrollPane(historyTable);
        scroll.setBorder(BorderFactory.createTitledBorder("Other penalties for this player"));
        scroll.setPreferredSize(new Dimension(760, 170));
        return scroll;
    }

    private JPanel actions() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton submit = new JButton("Submit");
        JButton cancel = new JButton("Cancel");
        submit.addActionListener(e -> submit());
        cancel.addActionListener(e -> dispose());
        panel.add(submit);
        panel.add(cancel);
        return panel;
    }

    private void addRow(JPanel panel, GridBagConstraints c, int row, String label, java.awt.Component component) {
        c.gridx = 0;
        c.gridy = row;
        c.weightx = 0;
        panel.add(new JLabel(label), c);
        c.gridx = 1;
        c.weightx = 1;
        panel.add(component, c);
    }

    private void populatePlayers(Player selectedPlayer) {
        DefaultComboBoxModel<PlayerItem> model = new DefaultComboBoxModel<>();
        List<Player> players = frame.getEvent().getPlayers();
        int selectedIndex = 0;
        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            model.addElement(new PlayerItem(player));
            if (selectedPlayer != null && player.getId().equals(selectedPlayer.getId())) {
                selectedIndex = i;
            }
        }
        playerCombo.setModel(model);
        if (model.getSize() > 0) {
            playerCombo.setSelectedIndex(selectedIndex);
        }
    }

    private void refreshHistory() {
        historyModel.setRowCount(0);
        Player player = selectedPlayer();
        if (player == null) {
            return;
        }
        for (PenaltyEntry entry : player.getPenaltyEntries()) {
            historyModel.addRow(new Object[] {
                    entry.getRoundNumber(),
                    entry.getInfraction(),
                    entry.getPenalty(),
                    entry.getAppliedPenalty(),
                    entry.getDescription()
            });
        }
    }

    private void submit() {
        Player player = selectedPlayer();
        if (player == null) {
            frame.showInfo("Select a player.");
            return;
        }
        try {
            PenaltyEntry entry = penaltyService.addPenalty(
                    frame.getEvent(),
                    player,
                    (String) infractionCombo.getSelectedItem(),
                    (String) penaltyCombo.getSelectedItem(),
                    description.getText());
            frame.afterPenaltySubmitted(entry);
            dispose();
        } catch (Exception exception) {
            frame.showError(exception);
        }
    }

    private Player selectedPlayer() {
        PlayerItem item = (PlayerItem) playerCombo.getSelectedItem();
        return item == null ? null : item.player();
    }

    private record PlayerItem(Player player) {
        @Override
        public String toString() {
            return player.getFullName();
        }
    }
}
