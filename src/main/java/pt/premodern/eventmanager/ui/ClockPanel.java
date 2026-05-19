package pt.premodern.eventmanager.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.UIManager;

public class ClockPanel extends JPanel {
    private final MainFrame frame;
    private final JComboBox<String> modeCombo = new JComboBox<>(new String[] {"Until Round Ends"});
    private final JSpinner minutes = new JSpinner(new SpinnerNumberModel(50, 1, 300, 1));
    private final JLabel display = new JLabel("50:00", SwingConstants.CENTER);
    private final JButton start = new JButton("Start");
    private final JButton reset = new JButton("Reset");
    private final RoundSoundPlayer soundPlayer = new RoundSoundPlayer();
    private final Timer timer;
    private long endTimeMillis;
    private long remainingMillis = 50L * 60L * 1000L;
    private boolean running;
    private boolean roundOverShown;
    private boolean darkMode = true;
    private JPanel displayPanel;

    public ClockPanel(MainFrame frame) {
        super(new BorderLayout(12, 12));
        this.frame = frame;
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        add(displayPanel(), BorderLayout.CENTER);
        add(controls(), BorderLayout.SOUTH);
        minutes.addChangeListener(e -> {
            if (!running) {
                remainingMillis = ((Integer) minutes.getValue()) * 60L * 1000L;
                roundOverShown = false;
                updateDisplay();
                frame.refreshEventInfo();
            }
        });
        timer = new Timer(250, e -> tick());
        updateDisplay();
    }

    public String getClockText() {
        return formatTime(remainingMillis);
    }

    public boolean isOvertime() {
        return remainingMillis < 0;
    }

    public boolean isRunning() {
        return running;
    }

    public void applyTheme(boolean darkMode) {
        this.darkMode = darkMode;
        if (displayPanel != null) {
            displayPanel.setBackground(darkMode ? new Color(18, 18, 18) : Color.WHITE);
            displayPanel.setBorder(BorderFactory.createLineBorder(darkMode ? new Color(50, 50, 50) : new Color(205, 205, 205)));
        }
        updateDisplay();
    }

    public void applyStandardTheme() {
        this.darkMode = false;
        if (displayPanel != null) {
            Color background = UIManager.getColor("Panel.background");
            Color border = UIManager.getColor("Separator.foreground");
            if (border == null) {
                border = Color.LIGHT_GRAY;
            }
            displayPanel.setBackground(background == null ? getBackground() : background);
            displayPanel.setBorder(BorderFactory.createLineBorder(border));
        }
        updateDisplay();
    }

    private JPanel displayPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        displayPanel = panel;
        panel.setBackground(new Color(18, 18, 18));
        panel.setBorder(BorderFactory.createLineBorder(new Color(50, 50, 50)));
        display.setForeground(Color.WHITE);
        display.setFont(display.getFont().deriveFont(Font.BOLD, 96f));
        display.setPreferredSize(new Dimension(600, 240));
        panel.add(display, BorderLayout.CENTER);
        return panel;
    }

    private JPanel controls() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 4;
        modeCombo.setPreferredSize(new Dimension(420, 32));
        panel.add(modeCombo, c);

        c.gridwidth = 1;
        c.gridy = 1;
        c.gridx = 0;
        panel.add(minutes, c);
        c.gridx = 1;
        start.addActionListener(e -> toggle());
        panel.add(start, c);
        c.gridx = 2;
        reset.addActionListener(e -> resetTimer());
        panel.add(reset, c);
        return panel;
    }

    private void toggle() {
        if (running) {
            pauseTimer();
            return;
        }
        int selectedMinutes = (Integer) minutes.getValue();
        if (remainingMillis <= 0 || remainingMillis == selectedMinutes * 60L * 1000L) {
            remainingMillis = selectedMinutes * 60L * 1000L;
            roundOverShown = false;
        }
        endTimeMillis = System.currentTimeMillis() + remainingMillis;
        running = true;
        start.setText("Pause");
        timer.start();
        frame.refreshEventInfo();
    }

    private void pauseTimer() {
        running = false;
        timer.stop();
        start.setText("Start");
        frame.refreshEventInfo();
    }

    private void resetTimer() {
        running = false;
        timer.stop();
        remainingMillis = ((Integer) minutes.getValue()) * 60L * 1000L;
        roundOverShown = false;
        start.setText("Start");
        updateDisplay();
        frame.refreshEventInfo();
    }

    private void tick() {
        remainingMillis = endTimeMillis - System.currentTimeMillis();
        if (remainingMillis <= 0 && !roundOverShown) {
            roundOverShown = true;
            soundPlayer.playRoundOverSound();
            showRoundOverPopup();
        }
        updateDisplay();
        frame.refreshEventInfo();
    }

    private void showRoundOverPopup() {
        JDialog dialog = new JDialog(frame, "Clock", false);
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));
        JLabel label = new JLabel("Round over", SwingConstants.CENTER);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 24f));
        JButton close = new JButton("OK");
        close.addActionListener(e -> dialog.dispose());
        panel.add(label, BorderLayout.CENTER);
        panel.add(close, BorderLayout.SOUTH);
        dialog.setContentPane(panel);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void updateDisplay() {
        display.setText(formatTime(remainingMillis));
        display.setForeground(remainingMillis < 0 ? new Color(220, 54, 46) : (darkMode ? Color.WHITE : new Color(28, 28, 28)));
    }

    private String formatTime(long millis) {
        boolean negative = millis < 0;
        long totalSeconds = Math.abs(millis) / 1000L;
        long mins = totalSeconds / 60L;
        long secs = totalSeconds % 60L;
        return (negative ? "-" : "") + String.format("%02d:%02d", mins, secs);
    }
}
