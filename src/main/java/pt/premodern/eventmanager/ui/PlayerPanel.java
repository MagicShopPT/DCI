package pt.premodern.eventmanager.ui;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import pt.premodern.eventmanager.model.Player;

public class PlayerPanel extends JPanel {
    private final MainFrame frame;
    private final JTextField firstNameField = new JTextField(14);
    private final JTextField lastNameField = new JTextField(14);
    private final JTextField emailField = new JTextField(20);
    private final JTextField teamField = new JTextField(14);
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[] {"Seed", "Name", "E-mail", "Team", "Points", "Dropped", "Bye"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(model);

    public PlayerPanel(MainFrame frame) {
        super(new BorderLayout(12, 12));
        this.frame = frame;
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        add(form(), BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                populateSelection();
            }
        });
    }

    public void refreshData() {
        model.setRowCount(0);
        for (Player player : frame.getEvent().getPlayers()) {
            model.addRow(new Object[] {
                    player.getInitialSeed(),
                    player.getFullName(),
                    player.getEmail(),
                    player.getTeam(),
                    player.getMatchPoints(),
                    player.isDropped() ? "Yes" : "No",
                    player.isReceivedBye() ? "Yes" : "No"
            });
        }
    }

    private JPanel form() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Player"));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.WEST;
        addField(panel, c, 0, "First name", firstNameField);
        addField(panel, c, 1, "Last name", lastNameField);
        addField(panel, c, 2, "E-mail", emailField);
        addField(panel, c, 3, "Team", teamField);

        JPanel buttons = new JPanel();
        JButton add = new JButton("Add");
        JButton edit = new JButton("Edit");
        JButton remove = new JButton("Remove");
        JButton importCsv = new JButton("Import CSV");
        JButton penalty = new JButton("Penalty Entry");
        JButton drop = new JButton("Drop");
        JButton clear = new JButton("Clear");
        add.addActionListener(e -> addPlayer());
        edit.addActionListener(e -> editPlayer());
        remove.addActionListener(e -> removePlayer());
        importCsv.addActionListener(e -> frame.importPlayersFromCsv());
        penalty.addActionListener(e -> openPenaltyEntry());
        drop.addActionListener(e -> dropPlayer());
        clear.addActionListener(e -> clearFields());
        buttons.add(add);
        buttons.add(edit);
        buttons.add(remove);
        buttons.add(importCsv);
        buttons.add(penalty);
        buttons.add(drop);
        buttons.add(clear);
        c.gridx = 1;
        c.gridy = 4;
        panel.add(buttons, c);
        return panel;
    }

    private void addField(JPanel panel, GridBagConstraints c, int row, String label, JTextField field) {
        c.gridx = 0;
        c.gridy = row;
        panel.add(new JLabel(label), c);
        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        panel.add(field, c);
        c.fill = GridBagConstraints.NONE;
    }

    private void addPlayer() {
        try {
            frame.getEventService().addPlayer(frame.getEvent(), firstNameField.getText(), lastNameField.getText(), emailField.getText(), teamField.getText());
            clearFields();
            frame.refreshAll();
        } catch (Exception exception) {
            frame.showError(exception);
        }
    }

    private void editPlayer() {
        Player player = selectedPlayer();
        if (player == null) {
            return;
        }
        try {
            frame.getEventService().updatePlayer(frame.getEvent(), player, firstNameField.getText(), lastNameField.getText(), emailField.getText(), teamField.getText());
            frame.refreshAll();
        } catch (Exception exception) {
            frame.showError(exception);
        }
    }

    private void removePlayer() {
        Player player = selectedPlayer();
        if (player == null) {
            return;
        }
        try {
            frame.getEventService().removePlayer(frame.getEvent(), player);
            clearFields();
            frame.refreshAll();
        } catch (Exception exception) {
            frame.showError(exception);
        }
    }

    private void dropPlayer() {
        Player player = selectedPlayer();
        if (player == null) {
            return;
        }
        frame.getEventService().dropPlayer(player);
        frame.refreshAll();
    }

    private void openPenaltyEntry() {
        Player player = selectedPlayer();
        if (player != null) {
            frame.showPenaltyEntry(player);
        }
    }

    private void populateSelection() {
        Player player = selectedPlayer(false);
        if (player == null) {
            return;
        }
        firstNameField.setText(player.getFirstName());
        lastNameField.setText(player.getLastName());
        emailField.setText(player.getEmail());
        teamField.setText(player.getTeam());
    }

    private Player selectedPlayer() {
        return selectedPlayer(true);
    }

    private Player selectedPlayer(boolean notify) {
        int row = table.getSelectedRow();
        if (row < 0) {
            if (notify) {
                frame.showInfo("Select a player.");
            }
            return null;
        }
        int modelRow = table.convertRowIndexToModel(row);
        List<Player> players = frame.getEvent().getPlayers();
        return modelRow >= 0 && modelRow < players.size() ? players.get(modelRow) : null;
    }

    private void clearFields() {
        firstNameField.setText("");
        lastNameField.setText("");
        emailField.setText("");
        teamField.setText("");
        table.clearSelection();
    }
}
