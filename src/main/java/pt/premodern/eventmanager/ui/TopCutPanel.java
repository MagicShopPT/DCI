package pt.premodern.eventmanager.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.table.DefaultTableModel;

import pt.premodern.eventmanager.model.EventStatus;
import pt.premodern.eventmanager.model.Match;
import pt.premodern.eventmanager.model.Player;
import pt.premodern.eventmanager.model.Round;

public class TopCutPanel extends JPanel {
    private final MainFrame frame;
    private final JLabel status = new JLabel();
    private final JPanel cutButtonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 8));
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[] {"Mesa", "Jogador/Equipa 1", "Equipa 1", "Jogador/Equipa 2", "Equipa 2", "Resultado", "Vencedor", "Estado"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(model);
    private int selectedRoundNumber = -1;

    public TopCutPanel(MainFrame frame) {
        super(new BorderLayout(12, 12));
        this.frame = frame;
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        add(toolbar(), BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    public void refreshData() {
        int previousRound = selectedRoundNumber;
        List<Round> playoffRounds = playoffRounds();
        status.setText(statusText(playoffRounds));
        rebuildCutButtons(playoffRounds, previousRound);
        populateTable();
    }

    private JPanel toolbar() {
        JPanel wrapper = new JPanel(new BorderLayout(8, 8));
        wrapper.add(cutButtonsPanel, BorderLayout.NORTH);

        JPanel actions = new JPanel(new GridLayout(1, 4, 8, 8));
        JButton create = new JButton("Criar Top Cut");
        JButton next = new JButton("Gerar Proxima Ronda");
        JButton results = new JButton("Inserir Resultado na Aba Resultados");
        create.addActionListener(e -> frame.createTopCut());
        next.addActionListener(e -> frame.generateNextTopCutRound());
        results.addActionListener(e -> frame.showResultsTab());
        actions.add(create);
        actions.add(next);
        actions.add(results);
        actions.add(status);
        wrapper.add(actions, BorderLayout.SOUTH);
        return wrapper;
    }

    private void rebuildCutButtons(List<Round> playoffRounds, int previousRound) {
        cutButtonsPanel.removeAll();
        if (playoffRounds.isEmpty()) {
            selectedRoundNumber = -1;
            cutButtonsPanel.add(new JLabel("Top Cut ainda nao criado."));
            cutButtonsPanel.revalidate();
            cutButtonsPanel.repaint();
            return;
        }

        ButtonGroup group = new ButtonGroup();
        cutButtonsPanel.setLayout(new GridLayout(1, Math.max(1, playoffRounds.size()), 12, 8));
        boolean selected = false;
        for (Round round : playoffRounds) {
            JToggleButton button = new JToggleButton(roundName(round));
            button.addActionListener(e -> {
                selectedRoundNumber = round.getNumber();
                populateTable();
            });
            group.add(button);
            cutButtonsPanel.add(button);
            if (round.getNumber() == previousRound) {
                button.setSelected(true);
                selectedRoundNumber = round.getNumber();
                selected = true;
            }
        }
        if (!selected) {
            Round lastRound = playoffRounds.get(playoffRounds.size() - 1);
            selectedRoundNumber = lastRound.getNumber();
            ((JToggleButton) cutButtonsPanel.getComponent(playoffRounds.size() - 1)).setSelected(true);
        }
        cutButtonsPanel.revalidate();
        cutButtonsPanel.repaint();
    }

    private void populateTable() {
        model.setRowCount(0);
        Round round = selectedRound();
        if (round == null) {
            return;
        }
        for (Match match : round.getMatches()) {
            model.addRow(new Object[] {
                    match.getTableNumber(),
                    name(match.getPlayer1()),
                    team(match.getPlayer1()),
                    name(match.getPlayer2()),
                    team(match.getPlayer2()),
                    result(match),
                    name(match.getWinner()),
                    match.isCompleted() ? "Completo" : "Aberto"
            });
        }
    }

    private List<Round> playoffRounds() {
        return frame.getEvent().getRounds().stream()
                .filter(Round::isPlayoffRound)
                .toList();
    }

    private Round selectedRound() {
        return playoffRounds().stream()
                .filter(round -> round.getNumber() == selectedRoundNumber)
                .findFirst()
                .orElse(null);
    }

    private String statusText(List<Round> playoffRounds) {
        if (frame.getEvent().getStatus() == EventStatus.FINISHED && !playoffRounds.isEmpty()) {
            Round last = playoffRounds.get(playoffRounds.size() - 1);
            if (!last.getMatches().isEmpty() && last.getMatches().get(0).getWinner() != null) {
                return "Campeao: " + last.getMatches().get(0).getWinner().getFullName();
            }
        }
        if (playoffRounds.isEmpty()) {
            return "Top Cut ainda nao criado.";
        }
        return "Rondas de Top Cut: " + playoffRounds.size();
    }

    private String roundName(Round round) {
        int players = round.getMatches().size() * 2;
        if (players == 2) {
            return "Final";
        }
        return "Top " + players;
    }

    private String result(Match match) {
        if (!match.isCompleted()) {
            return "";
        }
        return match.getPlayer1GamesWon() + "-" + match.getPlayer2GamesWon()
                + (match.getDrawGames() > 0 ? " (" + match.getDrawGames() + " emp.)" : "");
    }

    private String name(Player player) {
        return player == null ? "" : player.getFullName();
    }

    private String team(Player player) {
        return player == null ? "" : player.getTeam();
    }
}
