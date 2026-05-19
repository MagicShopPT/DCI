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
import pt.premodern.eventmanager.model.EventType;

public class EventPanel extends JPanel {
    private final MainFrame frame;
    private final JTextField nameField = new JTextField(28);
    private final JComboBox<EventType> typeCombo = new JComboBox<>(EventType.values());
    private final JComboBox<Integer> topCutCombo = new JComboBox<>(new Integer[] {4, 8, 16, 32, 64});
    private final JCheckBox teamEventCheck = new JCheckBox("Evento por equipas");
    private final JLabel statusLabel = new JLabel();
    private final JLabel playersLabel = new JLabel();
    private final JLabel roundLabel = new JLabel();
    private final JLabel roundsLabel = new JLabel();
    private final JLabel winnerLabel = new JLabel("-");

    public EventPanel(MainFrame frame) {
        super(new BorderLayout(12, 12));
        this.frame = frame;
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
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
        statusLabel.setText(event.getStatus().name());
        playersLabel.setText(String.valueOf(event.getPlayers().size()));
        roundLabel.setText(String.valueOf(event.getCurrentRoundNumber()));
        roundsLabel.setText(String.valueOf(event.getTotalSwissRounds()));
        winnerLabel.setText(frame.winnerSummaryHtml());
    }

    private JPanel form() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Configuracao do Evento"));
        GridBagConstraints c = constraints();
        addRow(panel, c, 0, "Nome", nameField);
        addRow(panel, c, 1, "Tipo", typeCombo);
        addRow(panel, c, 2, "Top Cut (4/8/16/32/64)", topCutCombo);
        addRow(panel, c, 3, "Formato", teamEventCheck);
        JButton create = new JButton("Criar Novo Evento");
        create.addActionListener(e -> createEvent());
        c.gridx = 1;
        c.gridy = 4;
        c.anchor = GridBagConstraints.WEST;
        panel.add(create, c);
        typeCombo.addActionListener(e -> topCutCombo.setEnabled(typeCombo.getSelectedItem() == EventType.SWISS_WITH_TOP_CUT));
        return panel;
    }

    private JPanel summary() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Estado Atual"));
        GridBagConstraints c = constraints();
        addRow(panel, c, 0, "Estado", statusLabel);
        addRow(panel, c, 1, "Jogadores", playersLabel);
        addRow(panel, c, 2, "Ronda atual", roundLabel);
        addRow(panel, c, 3, "Rondas suicas", roundsLabel);
        addRow(panel, c, 4, "Vencedor", winnerLabel);
        return panel;
    }

    private void createEvent() {
        EventType type = (EventType) typeCombo.getSelectedItem();
        int topCut = type == EventType.SWISS_WITH_TOP_CUT ? (Integer) topCutCombo.getSelectedItem() : 0;
        frame.setEvent(frame.getEventService().createEvent(nameField.getText(), type, topCut, teamEventCheck.isSelected()));
    }

    private GridBagConstraints constraints() {
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.anchor = GridBagConstraints.WEST;
        return c;
    }

    private void addRow(JPanel panel, GridBagConstraints c, int row, String label, java.awt.Component component) {
        c.gridx = 0;
        c.gridy = row;
        c.weightx = 0;
        panel.add(new JLabel(label), c);
        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        panel.add(component, c);
        c.fill = GridBagConstraints.NONE;
    }
}
