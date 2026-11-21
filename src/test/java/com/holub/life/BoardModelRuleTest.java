package com.holub.life;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** 화면 없이 모델만으로 규칙 교체를 검사한다. Point는 (열, 행) 순서다. */
class BoardModelRuleTest {
    @Test
    void newBoardStartsWithTheOriginalConwayRule() {
        assertEquals(Rules.CONWAY, new BoardModel().rule());
    }

    @Test
    void highLifeGivesBirthWhereConwayDoesNot() {
        // 가운데 (5, 5)가 비어 있고 이웃 여섯 칸이 살아 있다.
        List<Point> ring = List.of(new Point(4, 4), new Point(5, 4), new Point(6, 4),
                new Point(4, 6), new Point(5, 6), new Point(6, 6));

        BoardModel conway = boardWith(ring);
        conway.step();
        assertFalse(conway.isAlive(5, 5));

        BoardModel highLife = boardWith(ring);
        highLife.setRule(Rules.HIGHLIFE);
        highLife.step();
        assertTrue(highLife.isAlive(5, 5));
    }

    @Test
    void seedsTurnsTwoCellsIntoTheFourCellsThatTouchBoth() {
        BoardModel model = boardWith(List.of(new Point(10, 10), new Point(11, 10)));
        model.setRule(Rules.SEEDS);

        assertTrue(model.step());
        assertLiveCells(model, new Point(10, 9), new Point(11, 9), new Point(10, 11), new Point(11, 11));
    }

    @Test
    void ruleChangeAppliesToAPatternThatHadStoppedChanging() {
        // 블록 안쪽에 있는 2x2 정물. Conway에서는 변하지 않아 계산 대상에서 빠진다.
        BoardModel model = boardWith(List.of(new Point(2, 2), new Point(3, 2),
                new Point(2, 3), new Point(3, 3)));
        assertFalse(model.step());
        assertFalse(model.step());

        model.setRule(Rules.SEEDS);

        assertTrue(model.step());
        assertLiveCells(model,
                new Point(2, 1), new Point(3, 1), new Point(1, 2), new Point(4, 2),
                new Point(1, 3), new Point(4, 3), new Point(2, 4), new Point(3, 4));
    }

    @Test
    void ruleChangeAlsoReachesNeighboringBlocksAcrossABlockBoundary() {
        // 네 개의 8x8 블록이 만나는 모서리에 걸친 2x2 정물
        BoardModel model = boardWith(List.of(new Point(7, 7), new Point(8, 7),
                new Point(7, 8), new Point(8, 8)));
        assertFalse(model.step());
        assertFalse(model.step());

        model.setRule(Rules.SEEDS);

        assertTrue(model.step());
        assertLiveCells(model,
                new Point(7, 6), new Point(8, 6), new Point(6, 7), new Point(9, 7),
                new Point(6, 8), new Point(9, 8), new Point(7, 9), new Point(8, 9));
    }

    @Test
    void ruleChangeKeepsTheLayoutAndGenerationAndDoesNotNotifyStepListeners() {
        BoardModel model = boardWith(List.of(new Point(7, 8), new Point(8, 8), new Point(9, 8)));
        model.step();
        List<BoardModel.Snapshot> notified = new ArrayList<>();
        model.addStepListener((before, after) -> notified.add(after));

        model.setRule(Rules.DAY_AND_NIGHT);

        assertEquals(Rules.DAY_AND_NIGHT, model.rule());
        assertEquals(1, model.snapshot().generation());
        assertLiveCells(model, new Point(8, 7), new Point(8, 8), new Point(8, 9));
        assertTrue(notified.isEmpty());
    }

    @Test
    void conwayPatternBehavesTheSameAfterSwitchingAwayAndBack() {
        BoardModel model = boardWith(List.of(new Point(7, 8), new Point(8, 8), new Point(9, 8)));
        model.setRule(Rules.SEEDS);
        model.setRule(Rules.CONWAY);

        model.step();
        assertLiveCells(model, new Point(8, 7), new Point(8, 8), new Point(8, 9));
        model.step();
        assertLiveCells(model, new Point(7, 8), new Point(8, 8), new Point(9, 8));
    }

    @Test
    void eachBoardKeepsItsOwnRule() {
        BoardModel first = new BoardModel();
        BoardModel second = new BoardModel();

        first.setRule(Rules.SEEDS);

        assertEquals(Rules.SEEDS, first.rule());
        assertEquals(Rules.CONWAY, second.rule());
    }

    @Test
    void clearKeepsTheSelectedRule() {
        BoardModel model = new BoardModel();
        model.setRule(Rules.HIGHLIFE);

        model.clear();

        assertEquals(Rules.HIGHLIFE, model.rule());
    }

    @Test
    void missingRuleIsRejectedAndTheCurrentRuleStays() {
        BoardModel model = new BoardModel();

        assertThrows(NullPointerException.class, () -> model.setRule(null));
        assertEquals(Rules.CONWAY, model.rule());
    }

    private static BoardModel boardWith(List<Point> liveCells) {
        BoardModel model = new BoardModel();
        for (Point cell : liveCells) {
            model.setAlive(cell.y, cell.x, true);
        }
        return model;
    }

    private static void assertLiveCells(BoardModel model, Point... expected) {
        List<Point> actual = new ArrayList<>();
        for (int row = 0; row < model.size(); row++) {
            for (int column = 0; column < model.size(); column++) {
                if (model.isAlive(row, column)) {
                    actual.add(new Point(column, row));
                }
            }
        }
        Comparator<Point> byRowThenColumn = Comparator.<Point>comparingInt(p -> p.y).thenComparingInt(p -> p.x);
        List<Point> sorted = new ArrayList<>(List.of(expected));
        sorted.sort(byRowThenColumn);
        assertEquals(sorted, actual);
    }
}
