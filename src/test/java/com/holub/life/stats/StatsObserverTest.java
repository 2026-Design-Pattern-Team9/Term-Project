package com.holub.life.stats;

import com.holub.life.BoardModel;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** BoardModel에 등록한 StatsObserver가 step()마다 통계를 기록하는지 검증한다. */
class StatsObserverTest {

    private static BoardModel blinkerBoard() {
        BoardModel board = new BoardModel();
        board.setAlive(10, 10, true);
        board.setAlive(10, 11, true);
        board.setAlive(10, 12, true);
        return board;
    }

    @Test
    void recordsOneEntryPerStep() {
        BoardModel board = blinkerBoard();
        StatsModel stats = new StatsModel();
        board.addStepListener(new StatsObserver(stats));

        board.step();
        board.step();

        StatsModel.State state = stats.state();
        assertEquals(2, state.latest().generation());
        assertEquals(2, state.totals().generations());
        assertEquals(2, state.totals().survived());
        assertEquals(4, state.totals().births());
        assertEquals(4, state.totals().underpopulation());
    }

    @Test
    void nothingIsRecordedWithoutAStep() {
        BoardModel board = blinkerBoard();
        StatsModel stats = new StatsModel();
        board.addStepListener(new StatsObserver(stats));

        board.toggleCell(0, 0);
        board.clear();

        assertNull(stats.state().latest());
    }

    @Test
    void stableBoardStillRecordsEachGeneration() {
        BoardModel board = new BoardModel();
        board.setAlive(5, 5, true);
        board.setAlive(5, 6, true);
        board.setAlive(6, 5, true);
        board.setAlive(6, 6, true);
        StatsModel stats = new StatsModel();
        board.addStepListener(new StatsObserver(stats));

        board.step();
        board.step();

        assertEquals(2, stats.state().totals().generations());
        assertEquals(8, stats.state().totals().survived());
        assertEquals(0, stats.state().totals().deaths());
    }

    @Test
    void ruleIsReadFromTheSupplierEveryGeneration() {
        BoardModel board = blinkerBoard();
        StatsModel stats = new StatsModel();
        SurvivalRule[] current = {SurvivalRule.CONWAY};
        board.addStepListener(new StatsObserver(stats, () -> current[0]));

        board.step();
        assertEquals(2, stats.state().latest().underpopulation());

        current[0] = n -> false;
        board.step();
        assertEquals(0, stats.state().latest().underpopulation());
        assertEquals(2, stats.state().latest().otherDeaths());
    }

    @Test
    void repeatIsReadAfterEachStep() {
        BoardModel board = blinkerBoard();
        StatsModel stats = new StatsModel();
        AtomicReference<Optional<RepeatInfo>> current = new AtomicReference<>(Optional.empty());
        board.addStepListener(new StatsObserver(stats, () -> SurvivalRule.CONWAY, current::get));

        board.step();
        assertNull(stats.state().repeat());

        current.set(Optional.of(new RepeatInfo(2)));
        board.step();
        assertEquals(new RepeatInfo(2), stats.state().repeat());
    }
}
