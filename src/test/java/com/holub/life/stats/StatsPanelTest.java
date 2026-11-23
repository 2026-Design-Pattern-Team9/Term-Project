package com.holub.life.stats;

import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** 화면 배치는 스크린샷으로 확인하고, 여기서는 패널이 받은 값을 화면 글자에 반영하는지만 본다. */
class StatsPanelTest {

    private static void flushEventQueue() throws Exception {
        SwingUtilities.invokeAndWait(() -> { });
    }

    @Test
    void gameStateIsShownWithAPrefix() throws Exception {
        StatsPanel panel = new StatsPanel(new StatsModel());

        panel.setGameState("Paused");
        flushEventQueue();

        assertEquals("상태  Paused", panel.gameStateText());
    }

    @Test
    void gameStateCanBeChangedFromAnotherThread() throws Exception {
        StatsPanel panel = new StatsPanel(new StatsModel());

        Thread timer = new Thread(() -> panel.setGameState("Running (Medium)"));
        timer.start();
        timer.join();
        flushEventQueue();

        assertEquals("상태  Running (Medium)", panel.gameStateText());
    }
}
