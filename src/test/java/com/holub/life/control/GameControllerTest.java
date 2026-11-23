package com.holub.life.control;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.holub.life.BoardModel;
import com.holub.life.control.GameState.Mode;
import com.holub.life.history.GenerationHistory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 창과 실제 타이머 없이 상태 전환을 검사한다.
 * FakeTicker는 호출을 기록하고, tick()은 실제 Clock처럼 모델을 한 세대 진행한다.
 * 세대 이력은 Life와 같은 방식으로 모델의 세대 완료 리스너로 등록한다.
 */
class GameControllerTest {
    private BoardModel model;
    private FakeTicker ticker;
    private GenerationHistory history;
    private GameController game;
    private final List<GameState> notified = new ArrayList<>();
    private int boardChanges;

    @BeforeEach
    void setUp() {
        model = new BoardModel();
        ticker = new FakeTicker(model);
        history = new GenerationHistory(20, model.snapshot());
        model.addStepListener(history);
        game = new GameController(model, ticker, history);
        game.addStateListener(notified::add);
        game.addBoardListener(() -> boardChanges++);
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

    // ---------------------------------------------------------------
    // 되돌리기 (C의 GenerationHistory 연결)

    private void placeHorizontalBlinker() {
        model.setAlive(8, 7, true);
        model.setAlive(8, 8, true);
        model.setAlive(8, 9, true);
        history.reset(model.snapshot());
    }

    @Test
    void undoRestoresTheBoardAndGenerationOfTheStepBefore() {
        placeHorizontalBlinker();
        game.step();
        game.step();
        BoardModel.Snapshot afterOneStep = history.snapshots().get(1);

        game.undo();

        assertEquals(1, model.snapshot().generation());
        assertTrue(model.isAlive(7, 8), "1세대의 세로 블링커로 돌아감");
        assertFalse(model.isAlive(8, 7));
        assertEquals(afterOneStep.generation(), model.snapshot().generation());
        assertEquals(1, boardChanges, "되돌린 뒤 화면 갱신 알림");
    }

    @Test
    void undoWhileRunningPausesFirstSoTheNextTickDoesNotOverwriteIt() {
        placeHorizontalBlinker();
        game.run(Speed.FAST);
        ticker.tick();
        ticker.tick();

        game.undo();

        assertEquals(Mode.PAUSED, game.state().mode());
        assertEquals(Speed.FAST, game.state().speed().orElseThrow(), "직전 속도는 기억함");
        assertEquals(1, ticker.stopCount);
        assertEquals(1, model.snapshot().generation());
    }

    @Test
    void undoWithNothingToUndoChangesNothing() {
        game.undo();

        assertEquals(Mode.EDITING, game.state().mode());
        assertEquals(0, boardChanges);
        assertEquals(0, model.snapshot().generation());
    }

    /** 손으로 고친 판은 지난 세대와 이어지지 않으므로 고친 내용이 되돌리기로 사라지면 안 된다. */
    @Test
    void editingACellStartsANewHistory() {
        placeHorizontalBlinker();
        game.step();

        game.cellClicked(0, 0);
        game.undo();

        assertTrue(model.isAlive(0, 0), "고친 칸이 그대로 남음");
        assertFalse(history.canUndo());
    }

    @Test
    void boardReplacedStartsANewHistoryAndNotifies() {
        placeHorizontalBlinker();
        game.step();
        game.step();

        game.boardReplaced();

        assertFalse(history.canUndo());
        assertEquals(1, boardChanges);
    }

    @Test
    void clearBoardStopsClearsAndStartsANewHistory() {
        placeHorizontalBlinker();
        game.run(Speed.MEDIUM);
        ticker.tick();

        game.clearBoard();

        assertEquals(Mode.EDITING, game.state().mode());
        assertEquals(1, ticker.stopCount);
        assertEquals(0, model.snapshot().generation());
        assertFalse(model.isAlive(7, 8));
        assertFalse(history.canUndo());
        assertEquals(1, boardChanges);
    }

    /** 규칙 변경은 실행을 멈추지 않고(B의 설계) 이력만 새로 시작한다. */
    @Test
    void ruleChangedStartsANewHistoryWithoutStoppingTheRun() {
        placeHorizontalBlinker();
        game.run(Speed.SLOW);
        ticker.tick();
        ticker.tick();

        game.ruleChanged();

        assertEquals(Mode.RUNNING, game.state().mode());
        assertEquals(0, ticker.stopCount);
        assertFalse(history.canUndo());
        assertEquals(1, boardChanges);
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
