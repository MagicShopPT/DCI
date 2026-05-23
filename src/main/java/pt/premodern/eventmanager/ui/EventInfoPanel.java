package pt.premodern.eventmanager.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.JToggleButton;

import pt.premodern.eventmanager.model.Event;
import pt.premodern.eventmanager.model.EventStatus;
import pt.premodern.eventmanager.model.Round;

public class EventInfoPanel extends JPanel {
    private final MainFrame frame;
    private final JLabel title = new JLabel();
    private final JToggleButton themeSwitch = new ThemeSwitch();
    private final JLabel darkLabel = new JLabel("DARK");
    private final JLabel lightLabel = new JLabel("LIGHT");
    private final JButton statusBadge = new JButton();
    private final JButton actions = new JButton("Tournament Actions");
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
        setBackground(AppTheme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(10, 18, 12, 18));
        themeSwitch.putClientProperty(AppTheme.MANUAL_BUTTON, true);
        statusBadge.putClientProperty(AppTheme.MANUAL_BUTTON, true);
        actions.putClientProperty(AppTheme.MANUAL_BUTTON, true);
        themeSwitch.setToolTipText("Click the button to toggle between dark and light mode for this page.");
        themeSwitch.addActionListener(e -> frame.toggleTheme());

        JPanel header = new JPanel(new BorderLayout(12, 8));
        header.setOpaque(false);
        AppTheme.styleTitle(title);
        header.add(title, BorderLayout.CENTER);

        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionsPanel.setOpaque(false);
        actionsPanel.add(themeToggle());
        actionsPanel.add(statusBadge);
        actionsPanel.add(actions);
        header.add(actionsPanel, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        JPanel cards = new JPanel(new GridLayout(1, 5, 16, 16));
        cards.setOpaque(false);
        cards.add(card("Current Phase", phase));
        cards.add(card("Active Players", players));
        cards.add(card("Current Round", round));
        cards.add(card("Matches Remaining", matches));
        cards.add(card("Round Timer", timer));
        add(cards, BorderLayout.CENTER);
    }

    public void refreshData() {
        Event event = frame.getEvent();
        title.setText(event.getName());
        themeSwitch.setSelected(!frame.isDarkMode());
        statusBadge.setText("Status: " + phaseText(event.getStatus()));
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
        Color background = AppTheme.background(darkMode);
        Color card = darkMode ? new Color(20, 21, 20) : Color.WHITE;
        Color border = AppTheme.border(darkMode);
        Color titleColor = AppTheme.muted(darkMode);
        Color value = AppTheme.text(darkMode);
        setBackground(background);
        AppTheme.styleTitle(title, darkMode);
        themeSwitch.setSelected(!darkMode);
        darkLabel.setForeground(darkMode ? AppTheme.TEXT : AppTheme.muted(false));
        lightLabel.setForeground(darkMode ? AppTheme.MUTED : AppTheme.text(false));
        statusBadge.setForeground(Color.BLACK);
        statusBadge.setBackground(AppTheme.ORANGE);
        statusBadge.setFocusPainted(false);
        statusBadge.setBorderPainted(false);
        statusBadge.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        statusBadge.setFont(statusBadge.getFont().deriveFont(Font.BOLD, 13f));
        actions.setBackground(AppTheme.surfaceAlt(darkMode));
        actions.setForeground(AppTheme.text(darkMode));
        actions.setFocusPainted(false);
        actions.setBorderPainted(false);
        actions.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        actions.setFont(actions.getFont().deriveFont(Font.BOLD, 13f));
        for (JPanel panel : cardPanels) {
            panel.setBackground(card);
            panel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(border),
                    BorderFactory.createEmptyBorder(14, 18, 14, 18)));
        }
        for (JLabel label : titleLabels) {
            label.setForeground(titleColor);
        }
        phase.setForeground(value);
        players.setForeground(value);
        round.setForeground(value);
        matches.setForeground(value);
        timer.setForeground(frame.clockIsOvertime() ? new Color(220, 54, 46) : value);
    }

    public void applyStandardTheme() {
        applyTheme(true);
    }

    private Color valueColor() {
        return AppTheme.text(darkMode);
    }

    private JPanel themeToggle() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        panel.setOpaque(false);
        darkLabel.setFont(darkLabel.getFont().deriveFont(Font.BOLD, 11f));
        lightLabel.setFont(lightLabel.getFont().deriveFont(Font.BOLD, 11f));
        panel.add(darkLabel);
        panel.add(themeSwitch);
        panel.add(lightLabel);
        return panel;
    }

    private JPanel card(String title, JLabel value) {
        JPanel panel = new JPanel(new BorderLayout(6, 12));
        panel.setBackground(new Color(20, 21, 20));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.BORDER),
                BorderFactory.createEmptyBorder(14, 18, 14, 18)));
        JLabel label = new JLabel(title);
        label.setForeground(AppTheme.MUTED);
        label.setFont(label.getFont().deriveFont(Font.PLAIN, 16f));
        panel.add(label, BorderLayout.NORTH);
        panel.add(value, BorderLayout.CENTER);
        cardPanels.add(panel);
        titleLabels.add(label);
        return panel;
    }

    private JLabel valueLabel() {
        JLabel label = new JLabel("-", SwingConstants.LEFT);
        label.setForeground(AppTheme.TEXT);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 18f));
        return label;
    }

    private class ThemeSwitch extends JToggleButton {
        private ThemeSwitch() {
            setPreferredSize(new Dimension(58, 30));
            setMinimumSize(new Dimension(58, 30));
            setBorderPainted(false);
            setFocusPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int width = getWidth();
            int height = getHeight();
            int arc = height - 4;
            boolean lightMode = isSelected();
            g2.setColor(lightMode ? new Color(213, 218, 224) : new Color(62, 68, 74));
            g2.fillRoundRect(2, 3, width - 4, height - 6, arc, arc);
            g2.setColor(lightMode ? new Color(178, 184, 191) : new Color(88, 94, 101));
            g2.drawRoundRect(2, 3, width - 5, height - 7, arc, arc);

            int knob = height - 10;
            int x = lightMode ? width - knob - 6 : 6;
            g2.setColor(lightMode ? Color.WHITE : new Color(218, 224, 230));
            g2.fillOval(x, 5, knob, knob);
            g2.setColor(new Color(0, 0, 0, 45));
            g2.drawOval(x, 5, knob, knob);
            g2.dispose();
        }
    }

    private String phaseText(EventStatus status) {
        return switch (status) {
            case CREATED -> "Setup";
            case SWISS_IN_PROGRESS -> "Swiss";
            case SWISS_COMPLETED -> "Swiss completed";
            case TOP_CUT_IN_PROGRESS -> "Top Cut";
            case FINISHED -> "Finished";
        };
    }
}
