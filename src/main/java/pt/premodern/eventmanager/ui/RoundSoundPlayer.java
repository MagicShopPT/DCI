package pt.premodern.eventmanager.ui;

import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class RoundSoundPlayer {
    private static final String RESOURCE_PATH = "/audio/round_over.mp3";

    public void playRoundOverSound() {
        File sound = findSoundFile();
        if (sound == null) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }
        try {
            playWithPowerShell(sound);
        } catch (IOException exception) {
            Toolkit.getDefaultToolkit().beep();
        }
    }

    private File findSoundFile() {
        String configured = System.getProperty("round.over.sound");
        if (configured != null && !configured.isBlank()) {
            File file = new File(configured);
            if (file.isFile()) {
                return file;
            }
        }

        File embedded = extractEmbeddedSound();
        if (embedded != null) {
            return embedded;
        }

        File[] candidates = {
                new File("round_over.mp3"),
                new File("audio/round_over.mp3"),
                new File("src/main/resources/audio/round_over.mp3")
        };
        for (File candidate : candidates) {
            if (candidate.isFile()) {
                return candidate;
            }
        }
        return null;
    }

    private File extractEmbeddedSound() {
        try (InputStream input = RoundSoundPlayer.class.getResourceAsStream(RESOURCE_PATH)) {
            if (input == null) {
                return null;
            }
            File temp = File.createTempFile("round_over_", ".mp3");
            temp.deleteOnExit();
            Files.copy(input, temp.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return temp;
        } catch (IOException exception) {
            return null;
        }
    }

    private void playWithPowerShell(File sound) throws IOException {
        String uri = sound.toURI().toString().replace("'", "''");
        String command = "Add-Type -AssemblyName PresentationCore; "
                + "$player = New-Object System.Windows.Media.MediaPlayer; "
                + "$player.Open([Uri]'" + uri + "'); "
                + "$player.Play(); "
                + "Start-Sleep -Milliseconds 4500";
        new ProcessBuilder("powershell.exe", "-NoProfile", "-WindowStyle", "Hidden", "-Command", command).start();
    }
}
