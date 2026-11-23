package com.holub.life.stats;

import com.holub.life.BoardModel;
import com.holub.life.Rules;
import com.holub.life.control.GameController;
import com.holub.life.control.Ticker;
import com.holub.life.history.GenerationHistory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Life와 같은 순서로 보드, 이력, 컨트롤러, 통계를 연결해서 창 없이 확인한다.
 * 각각을 따로 검사하는 테스트로는 잡히지 않는, 연결했을 때의 동작을 본다.
 */
class StatsConnectorTest {
    private BoardModel model;
    private GenerationHistory history;
    private GameController controller;
    private StatsConnector connector;

    @BeforeEach
    void connectAsLifeDoes() {
        model = new BoardModel();
        history = new GenerationHistory(20, model.snapshot());
        model.addStepListener(history); // 통계보다 먼저 등록
        controller = new GameController(model, new Ticker() {
            @Override public void startTicking(int millisecondsBetweenTicks) { }
            @Override public void stop() { }
            @Override public void tick() { model.step(); }
        }, history);
        connector = StatsConnector.connect(model, history, controller);
    }

    private StatsModel.State state() {
        return connector.stats().state();
    }

    private void drawBlinker() {
        controller.cellClicked(10, 10);
        controller.cellClicked(10, 11);
        controller.cellClicked(10, 12);
    }

    @Test
    void eachStepIsRecordedWithTheBoardsRule() {
        drawBlinker();

        model.step();

        assertEquals(1, state().latest().generation());
        assertEquals(2, state().latest().underpopulation());
        assertEquals(2, state().latest().births());
    }

    @Test
    void ruleIsReadFromTheBoardEachStep() {
        drawBlinker();
        model.setRule(Rules.parse("B3/S"));  // 생존 조건이 없는 규칙

        model.step();

        assertEquals(0, state().latest().survived());
        assertEquals(3, state().latest().otherDeaths());
    }

    @Test
    void repeatIsReadFromTheHistoryAfterItWasUpdated() {
        drawBlinker();

        model.step();
        model.step();
        model.step();

        // 블링커는 주기 2. history가 먼저 등록되어 있어야 방금 끝난 세대가 반영된다.
        assertEquals(2, state().repeat().period());
    }

    @Test
    void undoRewindsTheStatsToTheRestoredGeneration() {
        drawBlinker();
        model.step();
        model.step();
        model.step();

        controller.undo();

        assertEquals(2, model.snapshot().generation());
        assertEquals(2, state().latest().generation());
        assertEquals(2, state().totals().generations());
    }

    @Test
    void clearEmptiesTheStatsEvenAfterManyGenerations() {
        drawBlinker();
        for (int i = 0; i < StatsModel.MAX_ENTRIES + 20; i++) {
            model.step();
        }
        assertTrue(state().totals().generations() > StatsModel.MAX_ENTRIES);

        controller.clearBoard();

        assertNull(state().latest());
        assertEquals(0, state().totals().generations());
        assertNull(state().repeat());
    }

    @Test
    void editingACellClearsTheRepeatBecauseTheHistoryStartedOver() {
        drawBlinker();
        for (int i = 0; i < 5; i++) {
            model.step();
        }
        assertTrue(state().repeat() != null);

        controller.cellClicked(30, 30); // 편집: 이력이 새로 시작된다

        assertNull(state().repeat());
        assertEquals(5, state().totals().generations()); // 통계는 그대로
    }

    @Test
    void ruleChangeResetsTheStats() {
        drawBlinker();
        model.step();
        model.step();

        connector.ruleChanged();

        assertNull(state().latest());
        assertEquals(0, state().totals().generations());
    }

    @Test
    void boardReplacedAtGenerationZeroEmptiesTheStats() {
        drawBlinker();
        model.step();
        model.clear();  // 파일 불러오기와 패턴 불러오기도 clear()로 시작해 0세대가 된다

        controller.boardReplaced();

        assertNull(state().latest());
        assertEquals(0, state().totals().generations());
    }
}
