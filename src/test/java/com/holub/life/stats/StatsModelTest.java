package com.holub.life.stats;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class StatsModelTest {
    private static GenerationStats stats(long generation, int survived, int births, int under, int over) {
        int alive = survived + births;
        return new GenerationStats(generation, survived, births, under, over, 0,
                alive, survived + under + over, 4096);
    }

    @Test
    void startsEmpty() {
        StatsModel.State state = new StatsModel().state();

        assertNull(state.latest());
        assertEquals(0, state.totals().generations());
    }

    @Test
    void recordKeepsLatestAndAccumulatesTotals() {
        StatsModel model = new StatsModel();
        GenerationStats second = stats(2, 5, 1, 0, 2);

        model.record(stats(1, 1, 2, 2, 0));
        model.record(second);

        StatsModel.State state = model.state();
        assertSame(second, state.latest());
        assertEquals(2, state.totals().generations());
        assertEquals(6, state.totals().survived());
        assertEquals(3, state.totals().births());
        assertEquals(2, state.totals().underpopulation());
        assertEquals(2, state.totals().overpopulation());
        assertEquals(4, state.totals().deaths());
    }

    @Test
    void listenersReceiveEachRecordAndTheReset() {
        StatsModel model = new StatsModel();
        List<StatsModel.State> received = new ArrayList<>();
        model.addListener(received::add);

        model.record(stats(1, 1, 2, 2, 0));
        model.reset();

        assertEquals(2, received.size());
        assertEquals(1, received.get(0).totals().generations());
        assertNull(received.get(1).latest());
        assertEquals(0, received.get(1).totals().generations());
    }

    @Test
    void resetClearsLatestAndTotals() {
        StatsModel model = new StatsModel();
        model.record(stats(1, 1, 2, 2, 0));

        model.reset();

        assertNull(model.state().latest());
        assertEquals(0, model.state().totals().births());
    }

    @Test
    void removedListenerIsNotNotified() {
        StatsModel model = new StatsModel();
        List<StatsModel.State> received = new ArrayList<>();
        StatsListener listener = received::add;
        model.addListener(listener);
        model.removeListener(listener);

        model.record(stats(1, 1, 2, 2, 0));

        assertEquals(0, received.size());
    }

    @Test
    void notifiedStateIsNotChangedByLaterRecords() {
        StatsModel model = new StatsModel();
        List<StatsModel.State> received = new ArrayList<>();
        model.addListener(received::add);

        model.record(stats(1, 1, 2, 2, 0));
        model.record(stats(2, 3, 0, 0, 0));

        assertEquals(1, received.get(0).totals().generations());
        assertEquals(1, received.get(0).latest().generation());
    }

    @Test
    void rewindDropsLaterGenerationsAndRestoresTotals() {
        StatsModel model = new StatsModel();
        model.record(stats(1, 1, 2, 2, 0));
        model.record(stats(2, 5, 1, 0, 2));
        model.record(stats(3, 3, 0, 1, 0));

        model.rewindTo(2, null);

        StatsModel.State state = model.state();
        assertEquals(2, state.latest().generation());
        assertEquals(2, state.totals().generations());
        assertEquals(6, state.totals().survived());
        assertEquals(3, state.totals().births());
    }

    @Test
    void rewindToGenerationZeroEmptiesTheModel() {
        StatsModel model = new StatsModel();
        model.record(stats(1, 1, 2, 2, 0));

        model.rewindTo(0, null);

        assertNull(model.state().latest());
        assertEquals(0, model.state().totals().generations());
    }

    @Test
    void rewindToGenerationZeroAlsoClearsTotalsOfDroppedOldGenerations() {
        StatsModel model = new StatsModel();
        for (int g = 1; g <= StatsModel.MAX_ENTRIES + 20; g++) {
            model.record(stats(g, 1, 1, 0, 0));
        }
        assertEquals(StatsModel.MAX_ENTRIES + 20, model.state().totals().generations());

        model.rewindTo(0, null); // Clear나 Load 뒤: 오래된 세대의 몫도 남지 않아야 한다

        assertEquals(0, model.state().totals().generations());
        assertEquals(0, model.state().totals().births());
    }

    @Test
    void recordingAfterRewindContinuesFromThatGeneration() {
        StatsModel model = new StatsModel();
        model.record(stats(1, 1, 2, 2, 0));
        model.record(stats(2, 5, 1, 0, 2));
        model.rewindTo(1, null);

        model.record(stats(2, 9, 0, 0, 0));

        assertEquals(2, model.state().totals().generations());
        assertEquals(10, model.state().totals().survived());
        assertEquals(2, model.state().totals().births());
    }

    @Test
    void rewindNotifiesListeners() {
        StatsModel model = new StatsModel();
        model.record(stats(1, 1, 2, 2, 0));
        List<StatsModel.State> received = new ArrayList<>();
        model.addListener(received::add);

        model.rewindTo(0, null);

        assertEquals(1, received.size());
        assertNull(received.get(0).latest());
    }

    @Test
    void rewindToTheCurrentGenerationKeepsTheStatsAndOnlyRefreshesTheRepeat() {
        StatsModel model = new StatsModel();
        model.record(stats(1, 1, 2, 2, 0));
        model.record(stats(2, 1, 2, 2, 0), new RepeatInfo(2));

        model.rewindTo(2, null); // 칸 편집: 이력이 새로 시작되어 반복 정보가 사라졌다

        assertEquals(2, model.state().totals().generations());
        assertNull(model.state().repeat());
    }

    @Test
    void repeatIsTheLastValueGivenAndIsNotStoredPerGeneration() {
        StatsModel model = new StatsModel();
        model.record(stats(1, 1, 2, 2, 0));
        model.record(stats(2, 1, 2, 2, 0), new RepeatInfo(2));
        assertEquals(new RepeatInfo(2), model.state().repeat());

        model.rewindTo(1, null); // 이력이 되돌린 세대에는 반복이 없다고 알려 줌

        assertNull(model.state().repeat());

        model.rewindTo(1, new RepeatInfo(1));

        assertEquals(new RepeatInfo(1), model.state().repeat());
    }

    @Test
    void resetClearsTheRepeat() {
        StatsModel model = new StatsModel();
        model.record(stats(1, 1, 2, 2, 0), new RepeatInfo(2));

        model.reset();

        assertNull(model.state().repeat());
    }

    @Test
    void oldEntriesAreDroppedButStayInTheTotals() {
        StatsModel model = new StatsModel();
        int count = StatsModel.MAX_ENTRIES + 20;
        for (int g = 1; g <= count; g++) {
            model.record(stats(g, 1, 1, 0, 0));
        }

        model.rewindTo(count - 10, null); // 보관 범위 안으로는 정확히 되감긴다

        assertEquals(count - 10, model.state().totals().generations());
        assertEquals(count - 10, model.state().latest().generation());
    }
}
