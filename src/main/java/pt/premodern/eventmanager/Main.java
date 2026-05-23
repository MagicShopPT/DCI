package pt.premodern.eventmanager;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import pt.premodern.eventmanager.ui.MainFrame;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception ignored) {
                // Keep Swing's default look and feel if the cross-platform one is unavailable.
            }
            new MainFrame().setVisible(true);
        });
    }
}
