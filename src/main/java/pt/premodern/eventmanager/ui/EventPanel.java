package pt.premodern.eventmanager.ui;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import pt.premodern.eventmanager.model.Event;
import pt.premodern.eventmanager.model.EventStatus;
import pt.premodern.eventmanager.model.EventType;

public class EventPanel extends JPanel {
    private final MainFrame frame;
    private final JTextField nameField = new JTextField(28);
    private final JComboBox<EventType> typeCombo = new JComboBox<>(EventType.values());
    private final JComboBox<Integer> topCutCombo = new JComboBox<>(new Integer[] {4, 8, 16, 32, 64});
    private final JCheckBox teamEventCheck = new JCheckBox("Team event");
    private final JLabel statusLabel = new JLabel();
    private final JLabel playersLabel = new JLabel();
    private final JLabel roundLabel = new JLabel();
    private final JLabel roundsLabel = new JLabel();
    private final JLabel winnerLabel = new JLabel("-");
    private final JButton createButton = new JButton("Create New Event");
    private final JButton finishButton = new JButton("End Current Event");
    private boolean currentEventLocked;

    public EventPanel(MainFrame frame) {
        super(new BorderLayout(12, 12));
        this.frame = frame;
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        configureSummaryLabels();
        add(form(), BorderLayout.NORTH);
        add(summary(), BorderLayout.CENTER);
    }

    public void refreshData() {
        Event event = frame.getEvent();
        nameField.setText(event.getName());
        typeCombo.setSelectedItem(event.getEventType());
        topCutCombo.setSelectedItem(event.getTopCutSize() == 0 ? 8 : event.getTopCutSize());
        teamEventCheck.setSelected(event.isTeamEvent());
        topCutCombo.setEnabled(typeCombo.getSelectedItem() == EventType.SWISS_WITH_TOP_CUT);
        statusLabel.setText(statusText(event.getStatus()));
        playersLabel.setText(String.valueOf(event.getPlayers().size()));
        roundLabel.setText(String.valueOf(event.getCurrentRoundNumber()));
        roundsLabel.setText(String.valueOf(event.getTotalSwissRounds()));
        winnerLabel.setText(frame.winnerSummaryHtml());
        createButton.setEnabled(!currentEventLocked);
        finishButton.setVisible(currentEventLocked);
    }

    public void lockCurrentEvent() {
        currentEventLocked = true;
        createButton.setEnabled(false);
        finishButton.setVisible(true);
    }

    private JPanel form() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Event Setup"));
        GridBagConstraints c = constraints();
        addRow(panel, c, 0, "Name", nameField);
        addRow(panel, c, 1, "Type", typeCombo);
        addRow(panel, c, 2, "Top Cut (4/8/16/32/64)", topCutCombo);
        addRow(panel, c, 3, "Format", teamEventCheck);
        createButton.addActionListener(e -> createEvent());
        finishButton.addActionListener(e -> finishCurrentEvent());
        finishButton.setVisible(false);
        JPanel buttons = new JPanel();
        buttons.add(createButton);
        buttons.add(finishButton);
        c.gridx = 1;
        c.gridy = 4;
        c.anchor = GridBagConstraints.WEST;
        panel.add(buttons, c);
        typeCombo.addActionListener(e -> topCutCombo.setEnabled(typeCombo.getSelectedItem() == EventType.SWISS_WITH_TOP_CUT));
        return panel;
    }

    private JPanel summary() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Current Status"));
        GridBagConstraints c = constraints();
        addRow(panel, c, 0, "Status", statusLabel);
        addRow(panel, c, 1, "Players", playersLabel);
        addRow(panel, c, 2, "Current round", roundLabel);
        addRow(panel, c, 3, "Swiss rounds", roundsLabel);
        addRow(panel, c, 4, "Winner", winnerLabel);
        c.gridx = 0;
        c.gridy = 5;
        c.gridwidth = 2;
        c.weightx = 1;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        panel.add(new JPanel(), c);
        return panel;
    }

    private void createEvent() {
        EventType type = (EventType) typeCombo.getSelectedItem();
        int topCut = type == EventType.SWISS_WITH_TOP_CUT ? (Integer) topCutCombo.getSelectedItem() : 0;
        currentEventLocked = true;
        frame.startNewEvent(frame.getEventService().createEvent(nameField.getText(), type, topCut, teamEventCheck.isSelected()));
    }

    private void finishCurrentEvent() {
        frame.getEvent().setStatus(EventStatus.FINISHED);
        currentEventLocked = false;
        frame.autoSaveEvent();
        frame.refreshAll();
    }

    private GridBagConstraints constraints() {
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.anchor = GridBagConstraints.NORTHWEST;
        return c;
    }

    private void addRow(JPanel panel, GridBagConstraints c, int row, String label, java.awt.Component component) {
        c.gridx = 0;
        c.gridy = row;
        c.gridwidth = 1;
        c.weightx = 0;
        c.weighty = 0;
        c.fill = GridBagConstraints.NONE;
        JLabel rowLabel = new JLabel(label);
        rowLabel.setHorizontalAlignment(JLabel.LEFT);
        rowLabel.setVerticalAlignment(JLabel.TOP);
        panel.add(rowLabel, c);
        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        panel.add(component, c);
        c.fill = GridBagConstraints.NONE;
    }

    private void configureSummaryLabels() {
        statusLabel.setHorizontalAlignment(JLabel.LEFT);
        playersLabel.setHorizontalAlignment(JLabel.LEFT);
        roundLabel.setHorizontalAlignment(JLabel.LEFT);
        roundsLabel.setHorizontalAlignment(JLabel.LEFT);
        winnerLabel.setHorizontalAlignment(JLabel.LEFT);
        statusLabel.setVerticalAlignment(JLabel.TOP);
        playersLabel.setVerticalAlignment(JLabel.TOP);
        roundLabel.setVerticalAlignment(JLabel.TOP);
        roundsLabel.setVerticalAlignment(JLabel.TOP);
        winnerLabel.setVerticalAlignment(JLabel.TOP);
    }

    private String statusText(pt.premodern.eventmanager.model.EventStatus status) {
        return switch (status) {
            case CREATED -> "Setup";
            case SWISS_IN_PROGRESS -> "Swiss in progress";
            case SWISS_COMPLETED -> "Swiss completed";
            case TOP_CUT_IN_PROGRESS -> "Top Cut in progress";
            case FINISHED -> "Finished";
        };
    }
}
