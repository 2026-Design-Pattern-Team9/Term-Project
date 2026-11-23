package com.holub.life.stats;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class StatsFormatterTest {

    @Test
    void emptyStateShowsPlaceholders() {
        StatsModel.State state = new StatsModel().state();

        assertArrayEquals(new String[] {"0", "-", "-", "-", "-"}, StatsFormatter.cards(state));
        assertEquals(0, StatsFormatter.deltaSign(state));
        String[][] table = StatsFormatter.table(state);
        assertArrayEquals(new String[] {"-", "0"}, table[0]);
        assertArrayEquals(new String[] {"-", "0"}, table[4]);
    }

    @Test
    void recordedStateShowsTheNumbers() {
        StatsModel model = new StatsModel();
        model.record(new GenerationStats(3, 1, 2, 2, 0, 0, 3, 3, 4096));

        StatsModel.State state = model.state();

        assertArrayEquals(new String[] {"3", "3", "+0", "0.1%", "-"}, StatsFormatter.cards(state));
        String[][] table = StatsFormatter.table(state);
        assertArrayEquals(new String[] {"2", "2"}, table[0]); // 번식
        assertArrayEquals(new String[] {"1", "1"}, table[1]); // 생존
        assertArrayEquals(new String[] {"2", "2"}, table[2]); // 고독사
        assertArrayEquals(new String[] {"0", "0"}, table[3]); // 과밀사
        assertArrayEquals(new String[] {"0", "0"}, table[4]); // 기타 사망
    }

    @Test
    void deltaSignFollowsPopulationChange() {
        StatsModel model = new StatsModel();
        model.record(new GenerationStats(1, 2, 3, 0, 0, 0, 5, 2, 4096));
        assertEquals(1, StatsFormatter.deltaSign(model.state()));
        assertEquals("+3", StatsFormatter.cards(model.state())[2]);

        model.record(new GenerationStats(2, 2, 0, 3, 0, 0, 2, 5, 4096));
        assertEquals(-1, StatsFormatter.deltaSign(model.state()));
        assertEquals("-3", StatsFormatter.cards(model.state())[2]);
    }

    @Test
    void repeatShowsOnlyThePeriod() {
        StatsModel model = new StatsModel();

        model.record(new GenerationStats(7, 1, 2, 2, 0, 0, 3, 3, 4096), new RepeatInfo(2));

        assertEquals("주기 2", StatsFormatter.cards(model.state())[4]);
    }
}
