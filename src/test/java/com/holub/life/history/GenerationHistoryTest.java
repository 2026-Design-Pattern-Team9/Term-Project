package com.holub.life.history;

import com.holub.life.BoardModel;
import org.junit.jupiter.api.Test;

import java.awt.Point;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GenerationHistoryTest {
    @Test
    void detectsPeriodOneForAStableBlock() {
        BoardModel model = new BoardModel();
        setAlive(model, new Point(2, 2), new Point(2, 3),
                new Point(3, 2), new Point(3, 3));

        GenerationHistory history = new GenerationHistory(6, model.snapshot());
        model.addStepListener(history);

        assertFalse(model.step());
        PeriodDetectionResult result = history.period().orElseThrow();
        assertEquals(0, result.startGeneration());
        assertEquals(1, result.currentGeneration());
        assertEquals(1, result.period());
    }

    @Test
    void detectsPeriodTwoForABlinker() {
        BoardModel model = new BoardModel();
        setAlive(model, new Point(8, 6), new Point(8, 7), new Point(8, 8));

        GenerationHistory history = new GenerationHistory(6, model.snapshot());
        model.addStepListener(history);

        assertTrue(model.step());
        assertTrue(model.step());

        PeriodDetectionResult result = history.period().orElseThrow();
        assertEquals(0, result.startGeneration());
        assertEquals(2, result.currentGeneration());
        assertEquals(2, result.period());
    }

    @Test
    void doesNotTreatAMovingGliderAsARepeatingBoard() {
        BoardModel model = new BoardModel();
        setAlive(model,
                new Point(1, 0),
                new Point(2, 1),
                new Point(0, 2),
                new Point(1, 2),
                new Point(2, 2));

        GenerationHistory history = new GenerationHistory(8, model.snapshot());
        model.addStepListener(history);

        for (int generation = 0; generation < 4; generation++) {
            assertTrue(model.step());
        }

        // 모양은 반복되어도 위치가 달라졌으므로 같은 보드 배치가 아니다.
        assertTrue(history.period().isEmpty());
    }

    @Test
    void undoRestoresBoardAndGenerationAndNewStepBranchesHistory() {
        BoardModel model = new BoardModel();
        setAlive(model, new Point(8, 6), new Point(8, 7), new Point(8, 8));
        GenerationHistory history = new GenerationHistory(6, model.snapshot());
        model.addStepListener(history);

        model.step();
        model.step();
        assertEquals(2, model.snapshot().generation());

        assertTrue(history.undo(model));
        assertEquals(1, model.snapshot().generation());
        assertBoard(model.snapshot(), new Point(7, 7), new Point(8, 7), new Point(9, 7));
        assertTrue(history.canUndo());

        // 복원 뒤 진행한 상태는 기존 미래 이력을 재사용하지 않는다.
        assertTrue(model.step());
        assertEquals(2, model.snapshot().generation());
        assertEquals(3, history.size());
    }

    @Test
    void undoRecomputesThePeriodAtTheRestoredGeneration() {
        BoardModel model = new BoardModel();
        setAlive(model, new Point(8, 6), new Point(8, 7), new Point(8, 8));
        GenerationHistory history = new GenerationHistory(6, model.snapshot());
        model.addStepListener(history);

        model.step();
        model.step();
        model.step();
        PeriodDetectionResult beforeUndo = history.period().orElseThrow();
        assertEquals(3, beforeUndo.currentGeneration());

        // 되돌린 2세대 기준으로 다시 판정한다. 3세대의 결과가 남아 있으면 안 된다.
        assertTrue(history.undo(model));
        PeriodDetectionResult afterUndo = history.period().orElseThrow();
        assertEquals(0, afterUndo.startGeneration());
        assertEquals(2, afterUndo.currentGeneration());
        assertEquals(2, afterUndo.period());

        // 1세대 앞에는 같은 배치가 없으므로 주기가 사라진다.
        assertTrue(history.undo(model));
        assertTrue(history.period().isEmpty());
    }

    @Test
    void keepsOnlyTheConfiguredNumberOfSnapshots() {
        BoardModel model = new BoardModel();
        setAlive(model, new Point(8, 6), new Point(8, 7), new Point(8, 8));
        GenerationHistory history = new GenerationHistory(3, model.snapshot());
        model.addStepListener(history);

        model.step();
        model.step();
        model.step();

        assertEquals(3, history.size());
        assertEquals(1, history.snapshots().get(0).generation());
        assertEquals(3, history.current().generation());
    }

    @Test
    void resetStartsASeparateHistoryAfterEditingOrLoading() {
        BoardModel model = new BoardModel();
        setAlive(model, new Point(8, 6), new Point(8, 7), new Point(8, 8));
        GenerationHistory history = new GenerationHistory(6, model.snapshot());
        model.addStepListener(history);
        model.step();

        model.clear();
        history.reset(model.snapshot());

        assertEquals(1, history.size());
        assertEquals(0, history.current().generation());
        assertTrue(history.period().isEmpty());
        assertFalse(history.canUndo());
    }

    private static void setAlive(BoardModel model, Point... cells) {
        for (Point cell : cells) {
            model.setAlive(cell.y, cell.x, true);
        }
    }

    private static void assertBoard(BoardModel.Snapshot snapshot, Point... liveCells) {
        List<Point> expected = List.of(liveCells);
        for (int row = 0; row < snapshot.size(); row++) {
            for (int column = 0; column < snapshot.size(); column++) {
                boolean alive = expected.contains(new Point(column, row));
                assertEquals(alive, snapshot.isAlive(row, column));
            }
        }
    }
}
