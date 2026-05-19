package pt.premodern.eventmanager.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

import pt.premodern.eventmanager.model.Event;
import pt.premodern.eventmanager.model.EventStatus;
import pt.premodern.eventmanager.model.Round;

public class EventInfoPanel extends JPanel {
    private final MainFrame frame;
    private final JLabel phase = valueLabel();
    private final JLabel players = valueLabel();
    private final JLabel round = valueLabel();
    private final JLabel matches = valueLabel();
    private final JLabel timer = valueLabel();
    private final List<JPanel> cardPanels = new ArrayList<>();
    private final List<JLabel> titleLabels = new ArrayList<>();
    private boolean darkMode = true;

    public EventInfoPanel(MainFrame frame) {
        super(new BorderLayout());
        this.frame = frame;
        setBackground(new Color(18, 18, 18));
        setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        JPanel cards = new JPanel(new GridLayout(1, 5, 16, 16));
        cards.setOpaque(false);
        cards.add(card("Current Phase", phase));
        cards.add(card("Active Players", players));
        cards.add(card("Current Round", round));
        cards.add(card("Matches Remaining", matches));
        cards.add(card("Round Timer", timer));
        add(cards, BorderLayout.NORTH);
    }

    public void refreshData() {
        Event event = frame.getEvent();
        phase.setText(phaseText(event.getStatus()));
        long activePlayers = event.getPlayers().stream().filter(player -> !player.isDropped()).count();
        players.setText(activePlayers + (activePlayers == 1 ? " player" : " players"));
        round.setText(event.getCurrentRoundNumber() == 0 ? "-" : "Round " + event.getCurrentRoundNumber());
        Round currentRound = event.getCurrentRound();
        long openMatches = currentRound == null ? 0 : currentRound.getMatches().stream()
                .filter(match -> !match.isCompleted())
                .count();
        matches.setText(openMatches + (openMatches == 1 ? " match" : " matches"));
        timer.setText(frame.clockText());
        timer.setForeground(frame.clockIsOvertime() ? new Color(220, 54, 46) : valueColor());
    }

    public void applyTheme(boolean darkMode) {
        this.darkMode = darkMode;
        Color background = darkMode ? new Color(18, 18, 18) : new Color(238, 238, 238);
        Color card = darkMode ? new Color(22, 22, 22) : Color.WHITE;
        Color border = darkMode ? new Color(55, 55, 55) : new Color(200, 200, 200);
        Color title = darkMode ? new Color(170, 170, 170) : new Color(80, 80, 80);
        Color value = darkMode ? Color.WHITE : new Color(28, 28, 28);
        setBackground(background);
        for (JPanel panel : cardPanels) {
            panel.setBackground(card);
            panel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(border),
                    BorderFactory.createEmptyBorder(14, 18, 14, 18)));
        }
        for (JLabel label : titleLabels) {
            label.setForeground(title);
        }
        phase.setForeground(value);
        players.setForeground(value);
        round.setForeground(value);
        matches.setForeground(value);
        timer.setForeground(frame.clockIsOvertime() ? new Color(220, 54, 46) : value);
    }

    public void applyStandardTheme() {
        this.darkMode = false;
        Color background = UIManager.getColor("Panel.background");
        Color value = UIManager.getColor("Label.foreground");
        Color title = value == null ? new Color(70, 70, 70) : value.darker();
        Color card = background == null ? getBackground() : background;
        setBackground(card);
        Color border = UIManager.getColor("Separator.foreground");
        if (border == null) {
            border = Color.LIGHT_GRAY;
        }
        for (JPanel panel : cardPanels) {
            panel.setBackground(card);
            panel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(border),
                    BorderFactory.createEmptyBorder(14, 18, 14, 18)));
        }
        for (JLabel label : titleLabels) {
            label.setForeground(title);
        }
        Color foreground = value == null ? Color.BLACK : value;
        phase.setForeground(foreground);
        players.setForeground(foreground);
        round.setForeground(foreground);
        matches.setForeground(foreground);
        timer.setForeground(frame.clockIsOvertime() ? new Color(220, 54, 46) : foreground);
    }

    private Color valueColor() {
        return darkMode ? Color.WHITE : new Color(28, 28, 28);
    }

    private JPanel card(String title, JLabel value) {
        JPanel panel = new JPanel(new BorderLayout(6, 12));
        panel.setBackground(new Color(18, 18, 18));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(55, 55, 55)),
                BorderFactory.createEmptyBorder(14, 18, 14, 18)));
        JLabel label = new JLabel(title);
        label.setForeground(new Color(170, 170, 170));
        label.setFont(label.getFont().deriveFont(Font.PLAIN, 16f));
        panel.add(label, BorderLayout.NORTH);
        panel.add(value, BorderLayout.CENTER);
        cardPanels.add(panel);
        titleLabels.add(label);
        return panel;
    }

    private JLabel valueLabel() {
        JLabel label = new JLabel("-", SwingConstants.LEFT);
        label.setForeground(Color.WHITE);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 18f));
        return label;
    }

    private String phaseText(EventStatus status) {
        return switch (status) {
            case CREATED -> "Setup";
            case SWISS_IN_PROGRESS -> "Suico";
            case SWISS_COMPLETED -> "Suico completo";
            case TOP_CUT_IN_PROGRESS -> "Top Cut";
            case FINISHED -> "Finalizado";
        };
    }
}
