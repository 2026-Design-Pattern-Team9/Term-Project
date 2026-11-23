package com.holub.life.stats;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.Objects;

/**
 * 통계 모델을 구독해 대시보드로 보여 주는 패널.
 * 위쪽은 카드 네 칸(세대, 생존, 증감, 밀도, 반복 주기), 아래쪽은 번식/생존/고독사/과밀사 표다.
 * 갱신은 이벤트 스레드에서 한다.
 */
public final class StatsPanel extends JPanel implements StatsListener {
    private static final Color BACKGROUND = new Color(0xFAF6E6);
    private static final Color LINE = new Color(0xD9D2B0);
    private static final Color TEXT = new Color(0x333333);
    private static final Color MUTED = new Color(0x777777);
    private static final Color UP = new Color(0x2E7D32);
    private static final Color DOWN = new Color(0xC62828);

    private static final String GAME_STATE_PREFIX = "상태  ";

    private static final String[] CARD_TITLES = {"세대", "생존", "증감", "밀도", "반복"};

    private final JLabel gameState = new JLabel(GAME_STATE_PREFIX + StatsFormatter.EMPTY);
    private final JLabel[] cardValues = new JLabel[CARD_TITLES.length];
    private final JLabel[][] cells = new JLabel[StatsFormatter.ROW_NAMES.length][2];

    public StatsPanel(StatsModel model) {
        Objects.requireNonNull(model);
        setLayout(new BorderLayout(0, 6));
        setBackground(BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        gameState.setForeground(TEXT);
        gameState.setFont(gameState.getFont().deriveFont(Font.BOLD, 12f));
        JPanel top = new JPanel(new BorderLayout(0, 4));
        top.setOpaque(false);
        top.add(gameState, BorderLayout.NORTH);
        top.add(buildCards(), BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);
        add(buildTable(), BorderLayout.CENTER);
        show(model.state());
        model.addListener(this);
    }

    @Override
    public void statsChanged(StatsModel.State state) {
        if (SwingUtilities.isEventDispatchThread()) {
            show(state);
        } else {
            SwingUtilities.invokeLater(() -> show(state));
        }
    }

    /**
     * 게임의 현재 상태(편집, 실행, 일시정지)를 보여 준다. 통계가 아니므로 StatsModel을 거치지 않고
     * 조립하는 쪽이 상태 알림을 받아 직접 넘긴다. 어느 스레드에서 불러도 된다.
     */
    public void setGameState(String name) {
        String text = GAME_STATE_PREFIX + Objects.requireNonNull(name);
        if (SwingUtilities.isEventDispatchThread()) {
            gameState.setText(text);
        } else {
            SwingUtilities.invokeLater(() -> gameState.setText(text));
        }
    }

    String gameStateText() {
        return gameState.getText();
    }

    private JPanel buildCards() {
        JPanel row = new JPanel(new GridLayout(1, CARD_TITLES.length, 6, 0));
        row.setOpaque(false);
        for (int i = 0; i < CARD_TITLES.length; i++) {
            JPanel card = new JPanel(new BorderLayout());
            card.setBackground(Color.WHITE);
            card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(LINE),
                    BorderFactory.createEmptyBorder(4, 6, 4, 6)));
            JLabel title = new JLabel(CARD_TITLES[i], SwingConstants.CENTER);
            title.setForeground(MUTED);
            title.setFont(title.getFont().deriveFont(Font.PLAIN, 11f));
            cardValues[i] = new JLabel(StatsFormatter.EMPTY, SwingConstants.CENTER);
            cardValues[i].setFont(cardValues[i].getFont().deriveFont(Font.BOLD, 20f));
            cardValues[i].setForeground(TEXT);
            card.add(title, BorderLayout.NORTH);
            card.add(cardValues[i], BorderLayout.CENTER);
            row.add(card);
        }
        return row;
    }

    private JPanel buildTable() {
        JPanel grid = new JPanel(new GridLayout(StatsFormatter.ROW_NAMES.length + 1, 3, 0, 1));
        grid.setBackground(LINE);
        grid.setBorder(BorderFactory.createLineBorder(LINE));
        grid.add(tableCell("", Font.PLAIN, MUTED, SwingConstants.LEFT));
        grid.add(tableCell("이번 세대", Font.PLAIN, MUTED, SwingConstants.RIGHT));
        grid.add(tableCell("누적", Font.PLAIN, MUTED, SwingConstants.RIGHT));
        for (int r = 0; r < StatsFormatter.ROW_NAMES.length; r++) {
            grid.add(tableCell(StatsFormatter.ROW_NAMES[r], Font.PLAIN, TEXT, SwingConstants.LEFT));
            for (int c = 0; c < 2; c++) {
                cells[r][c] = tableCell(StatsFormatter.EMPTY, Font.BOLD, TEXT, SwingConstants.RIGHT);
                grid.add(cells[r][c]);
            }
        }
        return grid;
    }

    private static JLabel tableCell(String text, int style, Color color, int align) {
        JLabel label = new JLabel(text, align);
        label.setOpaque(true);
        label.setBackground(Color.WHITE);
        label.setForeground(color);
        label.setFont(label.getFont().deriveFont(style, 12f));
        label.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
        return label;
    }

    private void show(StatsModel.State state) {
        String[] card = StatsFormatter.cards(state);
        for (int i = 0; i < card.length; i++) {
            cardValues[i].setText(card[i]);
        }
        int sign = StatsFormatter.deltaSign(state);
        cardValues[2].setForeground(sign > 0 ? UP : sign < 0 ? DOWN : TEXT);

        String[][] table = StatsFormatter.table(state);
        for (int r = 0; r < table.length; r++) {
            for (int c = 0; c < 2; c++) {
                cells[r][c].setText(table[r][c]);
            }
        }
    }
}
