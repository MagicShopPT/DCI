package pt.premodern.eventmanager.ui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.util.List;
import java.util.Objects;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;

import pt.premodern.eventmanager.model.Event;
import pt.premodern.eventmanager.model.Match;
import pt.premodern.eventmanager.model.Player;
import pt.premodern.eventmanager.model.Round;

public class MatchSlipPreviewDialog extends JDialog {
    private static final int PAGE_WIDTH = 794;
    private static final int PAGE_HEIGHT = 1123;
    private static final int SLIPS_PER_PAGE = 3;

    private final Event event;
    private final Round round;
    private final List<Match> matches;

    public MatchSlipPreviewDialog(MainFrame owner, Event event, Round round) {
        super(owner, "Match Result Slips - Round " + round.getNumber(), true);
        this.event = event;
        this.round = round;
        this.matches = round.getMatches().stream()
                .filter(match -> !match.isBye())
                .toList();

        setLayout(new BorderLayout(8, 8));
        add(previewTabs(), BorderLayout.CENTER);
        add(actions(), BorderLayout.SOUTH);
        setPreferredSize(new Dimension(900, 760));
        pack();
        setLocationRelativeTo(owner);
    }

    private JTabbedPane previewTabs() {
        JTabbedPane tabs = new JTabbedPane();
        int pages = Math.max(1, (int) Math.ceil(matches.size() / (double) SLIPS_PER_PAGE));
        for (int page = 0; page < pages; page++) {
            tabs.addTab("Página " + (page + 1), new JScrollPane(new SlipPagePanel(page)));
        }
        return tabs;
    }

    private JPanel actions() {
        JPanel panel = new JPanel();
        JButton print = new JButton("Imprimir A4");
        JButton close = new JButton("Fechar");
        print.addActionListener(e -> printSlips());
        close.addActionListener(e -> dispose());
        panel.add(print);
        panel.add(close);
        return panel;
    }

    private void printSlips() {
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName("Match Result Slips - Round " + round.getNumber());
        job.setPrintable((graphics, pageFormat, pageIndex) -> {
            int pages = Math.max(1, (int) Math.ceil(matches.size() / (double) SLIPS_PER_PAGE));
            if (pageIndex >= pages) {
                return Printable.NO_SUCH_PAGE;
            }
            Graphics2D g2 = (Graphics2D) graphics.create();
            double scale = Math.min(pageFormat.getImageableWidth() / PAGE_WIDTH, pageFormat.getImageableHeight() / PAGE_HEIGHT);
            g2.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
            g2.scale(scale, scale);
            drawPage(g2, pageIndex);
            g2.dispose();
            return Printable.PAGE_EXISTS;
        });
        if (job.printDialog()) {
            try {
                job.print();
            } catch (PrinterException exception) {
                JOptionPane.showMessageDialog(this, exception.getMessage(), "Erro de impressão", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void drawPage(Graphics2D g2, int pageIndex) {
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, PAGE_WIDTH, PAGE_HEIGHT);
        g2.setColor(Color.BLACK);
        g2.setStroke(new BasicStroke(1f));
        int slipHeight = PAGE_HEIGHT / SLIPS_PER_PAGE;
        for (int slot = 0; slot < SLIPS_PER_PAGE; slot++) {
            int matchIndex = pageIndex * SLIPS_PER_PAGE + slot;
            int y = slot * slipHeight;
            g2.drawLine(0, y + 8, PAGE_WIDTH, y + 8);
            if (matchIndex < matches.size()) {
                drawSlip(g2, matches.get(matchIndex), y, slipHeight);
            }
            g2.drawLine(0, y + slipHeight - 8, PAGE_WIDTH, y + slipHeight - 8);
        }
    }

    private void drawSlip(Graphics2D g2, Match match, int y, int height) {
        int left = 14;
        int center = 350;
        int right = PAGE_WIDTH - 70;
        Player p1 = match.getPlayer1();
        Player p2 = match.getPlayer2();

        g2.setFont(new Font(Font.SERIF, Font.PLAIN, 16));
        g2.drawString("Match Result Slip - Round " + round.getNumber() + " - " + event.getName(), left, y + 42);
        g2.setFont(new Font(Font.SERIF, Font.PLAIN, 15));
        g2.drawString("Table:", PAGE_WIDTH - 90, y + 62);
        g2.setFont(new Font(Font.SERIF, Font.BOLD, 32));
        g2.drawString(String.valueOf(match.getTableNumber()), PAGE_WIDTH - 30, y + 58);

        g2.setFont(new Font(Font.SERIF, Font.PLAIN, 13));
        g2.drawString("Games Won", center + 18, y + 88);
        g2.drawString("Drop/No Show", PAGE_WIDTH - 220, y + 88);
        g2.drawString("Penalty", PAGE_WIDTH - 75, y + 88);

        drawPlayer(g2, "Player 1", p1, left, y + 126, pointsBeforeRound(p1));
        drawPlayer(g2, "Player 2", p2, left, y + 198, pointsBeforeRound(p2));

        drawLine(g2, center + 8, y + 146, center + 88, y + 146);
        drawLine(g2, center + 8, y + 218, center + 88, y + 218);
        drawLine(g2, center + 8, y + 278, center + 88, y + 278);
        g2.drawString("Games Drawn", center + 14, y + 300);

        drawBox(g2, PAGE_WIDTH - 190, y + 118);
        drawBox(g2, PAGE_WIDTH - 190, y + 190);
        drawBox(g2, PAGE_WIDTH - 65, y + 118);
        drawBox(g2, PAGE_WIDTH - 65, y + 190);

        drawLine(g2, left + 2, y + height - 54, left + 250, y + height - 54);
        drawLine(g2, PAGE_WIDTH - 270, y + height - 54, PAGE_WIDTH - 20, y + height - 54);
        g2.setFont(new Font(Font.SERIF, Font.PLAIN, 13));
        g2.drawString("Signature: " + shortName(p1), left + 8, y + height - 36);
        g2.drawString("Signature: " + shortName(p2), PAGE_WIDTH - 260, y + height - 36);
    }

    private void drawPlayer(Graphics2D g2, String label, Player player, int x, int y, int points) {
        g2.setFont(new Font(Font.SERIF, Font.PLAIN, 16));
        g2.drawString(label + ": ", x, y);
        g2.setFont(new Font(Font.SERIF, Font.BOLD, 16));
        g2.drawString(shortName(player), x + 70, y);
        g2.setFont(new Font(Font.SERIF, Font.PLAIN, 16));
        g2.drawString("Team: " + team(player), x, y + 22);
        g2.drawString("Match Points: " + points, x, y + 44);
    }

    private int pointsBeforeRound(Player player) {
        if (player == null) {
            return 0;
        }
        int points = 0;
        for (Round eventRound : event.getRounds()) {
            if (eventRound.isPlayoffRound() || eventRound.getNumber() >= round.getNumber()) {
                continue;
            }
            for (Match match : eventRound.getMatches()) {
                if (!match.isCompleted()) {
                    continue;
                }
                if (match.isBye() && samePlayer(match.getPlayer1(), player)) {
                    points += 3;
                } else if (samePlayer(match.getPlayer1(), player) || samePlayer(match.getPlayer2(), player)) {
                    if (match.getWinner() == null) {
                        points += 1;
                    } else if (samePlayer(match.getWinner(), player)) {
                        points += 3;
                    }
                }
            }
        }
        return points;
    }

    private boolean samePlayer(Player a, Player b) {
        return a != null && b != null && Objects.equals(a.getId(), b.getId());
    }

    private String shortName(Player player) {
        return player == null ? "" : player.getFullName();
    }

    private String team(Player player) {
        return player == null || player.getTeam() == null || player.getTeam().isBlank() ? "-" : player.getTeam();
    }

    private void drawLine(Graphics2D g2, int x1, int y1, int x2, int y2) {
        g2.drawLine(x1, y1, x2, y2);
    }

    private void drawBox(Graphics2D g2, int x, int y) {
        g2.drawRect(x, y, 20, 20);
    }

    private class SlipPagePanel extends JPanel {
        private final int pageIndex;

        private SlipPagePanel(int pageIndex) {
            this.pageIndex = pageIndex;
            setPreferredSize(new Dimension(PAGE_WIDTH, PAGE_HEIGHT));
            setBackground(Color.WHITE);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            drawPage((Graphics2D) graphics, pageIndex);
        }
    }
}
