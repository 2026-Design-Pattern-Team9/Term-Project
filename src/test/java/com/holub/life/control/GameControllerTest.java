package com.holub.life.control;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.holub.life.BoardModel;
import com.holub.life.control.GameState.Mode;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 창과 실제 타이머 없이 상태 전환을 검사한다.
 * FakeTicker는 호출을 기록하고, tick()은 실제 Clock처럼 모델을 한 세대 진행한다.
 */
class GameControllerTest {
    private BoardModel model;
    private FakeTicker ticker;
    private GameController game;
    private final List<GameState> notified = new ArrayList<>();

    @BeforeEach
    void setUp() {
        model = new BoardModel();
        ticker = new FakeTicker(model);
        game = new GameController(model, ticker);
        game.addStateListener(notified::add);
    }

    @Test
    void startsInEditingModeAndClicksToggleCells() {
        assertEquals(Mode.EDITING, game.state().mode());

        game.cellClicked(3, 4);
        assertTrue(model.isAlive(3, 4));
        game.cellClicked(3, 4);
        assertFalse(model.isAlive(3, 4));

        assertEquals(Mode.EDITING, game.state().mode());
        assertTrue(notified.isEmpty(), "상태가 안 바뀌면 알리지 않는다");
    }

    @Test
    void runStartsTheTickerWithTheOriginalMenuInterval() {
        game.run(Speed.SLOW);

        assertEquals(Mode.RUNNING, game.state().mode());
        assertEquals(Speed.SLOW, game.state().speed().orElseThrow());
        assertEquals(List.of(150), ticker.started);
    }

    /** 원래 문제: 실행 중에 찍은 칸은 이웃이 없으면 다음 틱에 사라졌다. */
    @Test
    void clickWhileRunningPausesFirstSoTheNewCellSurvives() {
        game.run(Speed.FAST);

        game.cellClicked(10, 10);

        assertEquals(Mode.PAUSED, game.state().mode());
        assertEquals(1, ticker.stopCount);
        assertTrue(model.isAlive(10, 10));
        assertEquals(0, ticker.tickCount, "멈춘 뒤에는 세대가 진행되지 않는다");
    }

    @Test
    void stepIsIgnoredWhileRunningButWorksWhenStopped() {
        game.step();
        assertEquals(1, ticker.tickCount, "편집 상태에서는 한 세대 진행");

        game.run(Speed.MEDIUM);
        game.step();
        assertEquals(1, ticker.tickCount, "실행 중에는 무시");

        game.pause();
        game.step();
        assertEquals(2, ticker.tickCount, "일시정지 상태에서는 한 세대 진행");
    }

    @Test
    void stepAdvancesTheBoardOneGeneration() {
        // 가로 블링커
        model.setAlive(8, 7, true);
        model.setAlive(8, 8, true);
        model.setAlive(8, 9, true);

        game.step();

        assertTrue(model.isAlive(7, 8));
        assertTrue(model.isAlive(9, 8));
        assertFalse(model.isAlive(8, 7));
        assertEquals(1, model.snapshot().generation());
    }

    @Test
    void togglePlayUsesDefaultSpeedFirstAndThenResumesTheLastSpeed() {
        game.togglePlay();
        assertEquals(Mode.RUNNING, game.state().mode());
        assertEquals(GameController.DEFAULT_SPEED, game.state().speed().orElseThrow());

        game.run(Speed.AGONIZING);
        game.togglePlay();
        assertEquals(Mode.PAUSED, game.state().mode());

        game.togglePlay();
        assertEquals(Mode.RUNNING, game.state().mode());
        assertEquals(Speed.AGONIZING, game.state().speed().orElseThrow(), "일시정지 전 속도로 이어서 실행");
    }

    @Test
    void changingSpeedWhileRunningRestartsTheTickerOnlyWhenTheSpeedDiffers() {
        game.run(Speed.SLOW);
        game.run(Speed.SLOW);
        game.run(Speed.FAST);

        assertEquals(List.of(150, 30), ticker.started);
        assertEquals(Speed.FAST, game.state().speed().orElseThrow());
    }

    @Test
    void haltOnlyMattersWhileRunning() {
        game.pause();
        assertEquals(Mode.EDITING, game.state().mode());

        game.run(Speed.MEDIUM);
        game.pause();
        game.pause();
        assertEquals(Mode.PAUSED, game.state().mode());
        assertEquals(1, ticker.stopCount);
    }

    @Test
    void boardReplacedStopsAndReturnsToEditingFromAnyState() {
        game.run(Speed.MEDIUM);

        game.boardReplaced();

        assertEquals(Mode.EDITING, game.state().mode());
        assertTrue(game.state().speed().isEmpty());
        assertEquals(1, ticker.stopCount);
    }

    @Test
    void listenersSeeEveryStateChangeInOrder() {
        game.run(Speed.SLOW);
        game.cellClicked(0, 0);
        game.togglePlay();
        game.boardReplaced();

        List<Mode> modes = notified.stream().map(GameState::mode).toList();
        assertEquals(List.of(Mode.RUNNING, Mode.PAUSED, Mode.RUNNING, Mode.EDITING), modes);
    }

    /** 실제 Clock 대신 쓰는 타이머. 자동 진행은 하지 않고 호출만 기록한다. */
    private static final class FakeTicker implements Ticker {
        private final BoardModel model;
        final List<Integer> started = new ArrayList<>();
        int stopCount;
        int tickCount;

        FakeTicker(BoardModel model) {
            this.model = model;
        }

        @Override
        public void startTicking(int millisecondsBetweenTicks) {
            started.add(millisecondsBetweenTicks);
        }

        @Override
        public void stop() {
            stopCount++;
        }

        @Override
        public void tick() {
            tickCount++;
            model.step();
        }
    }
}
