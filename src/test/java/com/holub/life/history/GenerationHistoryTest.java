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
