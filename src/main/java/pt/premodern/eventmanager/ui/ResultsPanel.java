package pt.premodern.eventmanager.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.DefaultComboBoxModel;
import javax.swing.table.DefaultTableModel;

import pt.premodern.eventmanager.model.Match;
import pt.premodern.eventmanager.model.PenaltyEntry;
import pt.premodern.eventmanager.model.Player;
import pt.premodern.eventmanager.model.Round;
import pt.premodern.eventmanager.service.PenaltyService;
import pt.premodern.eventmanager.service.ResultService;

public class ResultsPanel extends JPanel {
    private static final String EMPTY = "";

    private final MainFrame frame;
    private final ResultService resultService = new ResultService();
    private final JComboBox<RoundItem> roundCombo = new JComboBox<>();
    private final JComboBox<String> p1Games = gameCombo();
    private final JComboBox<String> p2Games = gameCombo();
    private final JComboBox<String> drawGames = gameCombo();
    private final JLabel p1Label = new JLabel("Jogador 1");
    private final JLabel p2Label = new JLabel("Jogador 2");
    private boolean refreshing;

    private final DefaultTableModel model = new DefaultTableModel(
            new Object[] {"Mesa", "Jogador 1", "Equipa J1", "DROP J1", "Jogador 2", "Equipa J2", "DROP J2", "Resultado", "Vencedor", "Estado"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return column == 3 || column == 6;
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return columnIndex == 3 || columnIndex == 6 ? Boolean.class : String.class;
        }
    };
    private final JTable table = new JTable(model);

    public ResultsPanel(MainFrame frame) {
        super(new BorderLayout(12, 12));
        this.frame = frame;
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        add(toolbar(), BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(resultForm(), BorderLayout.SOUTH);

        roundCombo.addActionListener(e -> {
            if (!refreshing) {
                populateTable();
            }
        });
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                resetResultBoxes();
                updateSelectedPlayerLabels();
                updateGameComboOptions();
            }
        });
        model.addTableModelListener(e -> {
            if (!refreshing && e.getType() == TableModelEvent.UPDATE) {
                applyDropChange(e.getFirstRow(), e.getColumn());
            }
        });
    }

    public void refreshData() {
        Integer selectedRound = selectedRoundNumber();
        refreshing = true;
        roundCombo.removeAllItems();
        for (Round round : frame.getEvent().getRounds()) {
            roundCombo.addItem(new RoundItem(round));
        }
        selectRound(selectedRound);
        refreshing = false;
        populateTable();
        resetResultBoxes();
        updateGameComboOptions();
    }

    public void selectLatestRoundForEntry() {
        refreshing = true;
        if (roundCombo.getItemCount() != frame.getEvent().getRounds().size()) {
            roundCombo.removeAllItems();
            for (Round round : frame.getEvent().getRounds()) {
                roundCombo.addItem(new RoundItem(round));
            }
        }
        selectLastRound();
        refreshing = false;
        populateTable();
        resetResultBoxes();
        updateGameComboOptions();
    }

    private JPanel toolbar() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.add(new JLabel("Ronda"));
        panel.add(roundCombo);
        JButton slips = new JButton("Gerar Match Result Slips");
        slips.addActionListener(e -> showMatchSlips());
        panel.add(slips);
        return panel;
    }

    private JPanel resultForm() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Inserir ou alterar resultado"));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);

        c.gridx = 0;
        c.gridy = 0;
        panel.add(p1Label, c);
        c.gridx = 1;
        panel.add(p1Games, c);
        c.gridx = 2;
        panel.add(p2Label, c);
        c.gridx = 3;
        panel.add(p2Games, c);
        c.gridx = 4;
        panel.add(new JLabel("Empates"), c);
        c.gridx = 5;
        panel.add(drawGames, c);

        JButton confirm = new JButton("Confirmar / Alterar Resultado");
        confirm.addActionListener(e -> submitResult());
        c.gridx = 6;
        panel.add(confirm, c);
        return panel;
    }

    private void populateTable() {
        refreshing = true;
        model.setRowCount(0);
        Round round = selectedRound();
        if (round != null) {
            for (Match match : round.getMatches()) {
                model.addRow(new Object[] {
                        String.valueOf(match.getTableNumber()),
                        name(match.getPlayer1()),
                        team(match.getPlayer1()),
                        droppedInRound(match.getPlayer1(), round),
                        match.isBye() ? "BYE" : name(match.getPlayer2()),
                        team(match.getPlayer2()),
                        droppedInRound(match.getPlayer2(), round),
                        result(match),
                        name(match.getWinner()),
                        match.isCompleted() ? "Completo" : "Aberto"
                });
            }
        }
        refreshing = false;
    }

    private void submitResult() {
        Round round = selectedRound();
        Match match = selectedMatch(round);
        if (match == null) {
            return;
        }
        try {
            int p1 = selectedGame(p1Games, "Jogos do jogador 1");
            int p2 = selectedGame(p2Games, "Jogos do jogador 2");
            int draws = optionalGame(drawGames);
            validateGameLossMinimums(round, match, p1, p2);
            if (round.isPlayoffRound()) {
                resultService.submitTopCutResult(match, p1, p2, draws);
            } else {
                resultService.submitResult(match, p1, p2, draws);
            }
            resultService.updateRoundCompletion(round);
            frame.afterResultSubmitted(round);
        } catch (Exception exception) {
            frame.showError(exception);
        }
    }

    private void applyDropChange(int row, int column) {
        if (row < 0 || (column != 3 && column != 6)) {
            return;
        }
        Round round = selectedRound();
        if (round == null || row >= round.getMatches().size()) {
            return;
        }
        Match match = round.getMatches().get(row);
        Player player = column == 3 ? match.getPlayer1() : match.getPlayer2();
        if (player != null) {
            if (Boolean.TRUE.equals(model.getValueAt(row, column))) {
                frame.getEventService().dropPlayerAtRound(frame.getEvent(), player, round.getNumber());
            } else {
                frame.getEventService().restoreDroppedPlayer(player);
            }
            resultService.recalculateEvent(frame.getEvent());
            frame.refreshAll();
        }
    }

    private Match selectedMatch(Round round) {
        if (round == null) {
            frame.showInfo("Não existe ronda selecionada.");
            return null;
        }
        int row = table.getSelectedRow();
        if (row < 0) {
            frame.showInfo("Seleciona um match.");
            return null;
        }
        int modelRow = table.convertRowIndexToModel(row);
        List<Match> matches = round.getMatches();
        Match match = modelRow >= 0 && modelRow < matches.size() ? matches.get(modelRow) : null;
        if (match != null && match.isBye()) {
            frame.showInfo("Um bye não aceita resultado manual.");
            return null;
        }
        return match;
    }

    private int selectedGame(JComboBox<String> combo, String label) {
        String value = (String) combo.getSelectedItem();
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + ": escolhe 0, 1 ou 2.");
        }
        return Integer.parseInt(value);
    }

    private int optionalGame(JComboBox<String> combo) {
        String value = (String) combo.getSelectedItem();
        return value == null || value.isBlank() ? 0 : Integer.parseInt(value);
    }

    private void updateSelectedPlayerLabels() {
        Round round = selectedRound();
        Match match = selectedMatchSilently(round);
        p1Label.setText(match == null ? "Jogador 1" : name(match.getPlayer1()));
        p2Label.setText(match == null ? "Jogador 2" : name(match.getPlayer2()));
    }

    private void updateGameComboOptions() {
        Round round = selectedRound();
        Match match = selectedMatchSilently(round);
        if (round == null || match == null) {
            setGameComboOptions(p1Games, false);
            setGameComboOptions(p2Games, false);
            return;
        }
        setGameComboOptions(p1Games, hasGameLossInRound(match.getPlayer2(), round));
        setGameComboOptions(p2Games, hasGameLossInRound(match.getPlayer1(), round));
    }

    private void setGameComboOptions(JComboBox<String> combo, boolean minimumOne) {
        String selected = (String) combo.getSelectedItem();
        combo.setModel(new DefaultComboBoxModel<>(minimumOne
                ? new String[] {EMPTY, "1", "2"}
                : new String[] {EMPTY, "0", "1", "2"}));
        if (selected != null && !selected.isBlank() && (minimumOne && "0".equals(selected))) {
            combo.setSelectedItem(EMPTY);
        } else {
            combo.setSelectedItem(selected);
        }
    }

    private void validateGameLossMinimums(Round round, Match match, int p1, int p2) {
        if (hasGameLossInRound(match.getPlayer2(), round) && p1 < 1) {
            throw new IllegalArgumentException(name(match.getPlayer1()) + " recebeu 1 game win automático por Game Loss do adversário.");
        }
        if (hasGameLossInRound(match.getPlayer1(), round) && p2 < 1) {
            throw new IllegalArgumentException(name(match.getPlayer2()) + " recebeu 1 game win automático por Game Loss do adversário.");
        }
    }

    private boolean hasGameLossInRound(Player player, Round round) {
        if (player == null || round == null) {
            return false;
        }
        return player.getPenaltyEntries().stream()
                .anyMatch(entry -> isGameLoss(entry) && entry.getRoundNumber() == round.getNumber());
    }

    private boolean isGameLoss(PenaltyEntry entry) {
        return PenaltyService.GAME_LOSS.equals(entry.getAppliedPenalty());
    }

    private Match selectedMatchSilently(Round round) {
        int row = table.getSelectedRow();
        if (round == null || row < 0) {
            return null;
        }
        int modelRow = table.convertRowIndexToModel(row);
        List<Match> matches = round.getMatches();
        return modelRow >= 0 && modelRow < matches.size() ? matches.get(modelRow) : null;
    }

    private void resetResultBoxes() {
        p1Games.setSelectedItem(EMPTY);
        p2Games.setSelectedItem(EMPTY);
        drawGames.setSelectedItem(EMPTY);
    }

    private JComboBox<String> gameCombo() {
        return new JComboBox<>(new String[] {EMPTY, "0", "1", "2"});
    }

    private Round selectedRound() {
        RoundItem item = (RoundItem) roundCombo.getSelectedItem();
        return item == null ? null : item.round();
    }

    private Integer selectedRoundNumber() {
        Round round = selectedRound();
        return round == null ? null : round.getNumber();
    }

    private void selectLastRound() {
        if (roundCombo.getItemCount() == 0) {
            return;
        }
        roundCombo.setSelectedIndex(roundCombo.getItemCount() - 1);
    }

    private void selectRound(Integer selectedRound) {
        if (roundCombo.getItemCount() == 0) {
            return;
        }
        if (selectedRound == null) {
            selectLastRound();
            return;
        }
        for (int i = 0; i < roundCombo.getItemCount(); i++) {
            if (roundCombo.getItemAt(i).round().getNumber() == selectedRound) {
                roundCombo.setSelectedIndex(i);
                return;
            }
        }
        selectLastRound();
    }

    private void showMatchSlips() {
        Round round = selectedRound();
        if (round == null) {
            frame.showInfo("Não existe ronda selecionada.");
            return;
        }
        if (round.getMatches().stream().noneMatch(match -> !match.isBye())) {
            frame.showInfo("A ronda selecionada não tem matches para imprimir.");
            return;
        }
        new MatchSlipPreviewDialog(frame, frame.getEvent(), round).setVisible(true);
    }

    private String result(Match match) {
        if (match.isBye()) {
            return "BYE";
        }
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

    private boolean droppedInRound(Player player, Round round) {
        return player != null && player.isDropped() && player.getDropRoundNumber() == round.getNumber();
    }

    private record RoundItem(Round round) {
        @Override
        public String toString() {
            return (round.isPlayoffRound() ? "Top Cut " : "Suíça ") + round.getNumber();
        }
    }
}
