package com.holub.life.control;

import java.lang.management.ManagementFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

import com.holub.life.BoardModel;
import com.holub.life.history.GenerationHistory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 실제 앱처럼 타이머 스레드가 세대를 진행하는 동안 다른 스레드가 되돌리기를 눌러도
 * 프로그램이 멈추지 않는지 확인한다.
 * C 리뷰에서 발견한 잠금 순서 문제(step: model → history, undo: history → model)의 회귀 테스트다.
 */
class UndoWhileRunningTest {
    @Test
    void stepOnTheTimerThreadAndUndoOnAnotherThreadDoNotDeadlock() throws Exception {
        BoardModel model = new BoardModel();
        model.setAlive(8, 7, true);
        model.setAlive(8, 8, true);
        model.setAlive(8, 9, true);
        GenerationHistory history = new GenerationHistory(20, model.snapshot());
        model.addStepListener(history);
        GameController game = new GameController(model, new IdleTicker(), history);

        AtomicBoolean stop = new AtomicBoolean();
        AtomicLong steps = new AtomicLong();
        AtomicLong undos = new AtomicLong();
        Thread timer = new Thread(() -> {
            while (!stop.get()) {
                model.step();
                steps.incrementAndGet();
            }
        }, "timer");
        Thread ui = new Thread(() -> {
            while (!stop.get()) {
                game.undo();
                undos.incrementAndGet();
            }
        }, "ui");
        timer.setDaemon(true);
        ui.setDaemon(true);
        timer.start();
        ui.start();

        long[] deadlocked = null;
        for (int i = 0; i < 10 && deadlocked == null; i++) {
            Thread.sleep(100);
            deadlocked = ManagementFactory.getThreadMXBean().findDeadlockedThreads();
        }
        stop.set(true);
        timer.join(1000);
        ui.join(1000);

        assertNull(deadlocked, "세대 진행과 되돌리기가 서로의 잠금을 기다리며 멈춤");
        assertTrue(steps.get() > 0 && undos.get() > 0, "두 스레드가 모두 실행됨");
    }

    /** 이 테스트에서는 타이머 스레드를 직접 돌리므로 Ticker는 아무 일도 하지 않는다. */
    private static final class IdleTicker implements Ticker {
        @Override
        public void startTicking(int millisecondsBetweenTicks) {
        }

        @Override
        public void stop() {
        }

        @Override
        public void tick() {
        }
    }
}
