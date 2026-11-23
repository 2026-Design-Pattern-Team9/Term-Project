package com.holub.life.stats;

import com.holub.life.BoardModel;
import com.holub.life.BoardModel.Snapshot;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** 화면과 타이머 없이 BoardModel 한 세대의 전후 스냅샷으로 통계 계산을 검증한다. */
class StatsCalculatorTest {
    private final StatsCalculator calculator = new StatsCalculator();

    private static BoardModel boardWith(int[][] cells) {
        BoardModel board = new BoardModel();
        for (int[] cell : cells) {
            board.setAlive(cell[0], cell[1], true);
        }
        return board;
    }

    private GenerationStats stepAndCompute(BoardModel board, SurvivalRule rule) {
        Snapshot before = board.snapshot();
        board.step();
        return calculator.compute(before, board.snapshot(), rule);
    }

    @Test
    void blinkerKeepsCenterAndLosesTwoEndsToLoneliness() {
        BoardModel board = boardWith(new int[][] {{10, 10}, {10, 11}, {10, 12}});

        GenerationStats s = stepAndCompute(board, SurvivalRule.CONWAY);

        assertEquals(1, s.generation());
        assertEquals(1, s.survived());
        assertEquals(2, s.births());
        assertEquals(2, s.underpopulation());
        assertEquals(0, s.overpopulation());
        assertEquals(0, s.otherDeaths());
        assertEquals(3, s.alive());
        assertEquals(3, s.previousAlive());
        assertEquals(0, s.delta());
    }

    @Test
    void stillBlockOnlySurvives() {
        BoardModel board = boardWith(new int[][] {{5, 5}, {5, 6}, {6, 5}, {6, 6}});

        GenerationStats s = stepAndCompute(board, SurvivalRule.CONWAY);

        assertEquals(4, s.survived());
        assertEquals(0, s.births());
        assertEquals(0, s.deaths());
    }

    @Test
    void plusShapeCenterDiesOfOvercrowding() {
        BoardModel board = boardWith(new int[][] {{10, 10}, {9, 10}, {11, 10}, {10, 9}, {10, 11}});

        GenerationStats s = stepAndCompute(board, SurvivalRule.CONWAY);

        assertEquals(4, s.survived());
        assertEquals(1, s.overpopulation());
        assertEquals(0, s.underpopulation());
        assertEquals(4, s.births());
        assertEquals(8, s.alive());
    }

    @Test
    void singleCellDiesAlone() {
        BoardModel board = boardWith(new int[][] {{3, 3}});

        GenerationStats s = stepAndCompute(board, SurvivalRule.CONWAY);

        assertEquals(1, s.underpopulation());
        assertEquals(0, s.alive());
        assertEquals(-1, s.delta());
    }

    @Test
    void emptyBoardHasNothingToCount() {
        GenerationStats s = stepAndCompute(new BoardModel(), SurvivalRule.CONWAY);

        assertEquals(0, s.alive());
        assertEquals(0, s.births());
        assertEquals(0, s.deaths());
        assertEquals(0.0, s.density());
    }

    @Test
    void cornerCellsAreCountedWithDeadNeighborsOutsideTheBoard() {
        BoardModel board = boardWith(new int[][] {{0, 0}, {0, 1}, {1, 0}, {1, 1}});

        GenerationStats s = stepAndCompute(board, SurvivalRule.CONWAY);

        assertEquals(4, s.survived());
        assertEquals(0, s.deaths());
    }

    @Test
    void deathInsideSurvivalGapIsOtherDeath() {
        // 생존 집합 {2, 3, 5}: 이웃 4는 최소와 최대 사이의 구멍이라 고독사도 과밀사도 아니다.
        SurvivalRule gap = n -> n == 2 || n == 3 || n == 5;
        BoardModel board = boardWith(new int[][] {{10, 10}, {9, 10}, {11, 10}, {10, 9}, {10, 11}});

        GenerationStats s = stepAndCompute(board, gap);

        assertEquals(0, s.overpopulation());
        assertEquals(0, s.underpopulation());
        assertEquals(1, s.otherDeaths());
    }

    @Test
    void ruleWithNoSurvivalCountsEveryDeathAsOther() {
        BoardModel board = boardWith(new int[][] {{10, 10}, {10, 11}, {10, 12}});

        GenerationStats s = stepAndCompute(board, n -> false);

        assertEquals(0, s.underpopulation());
        assertEquals(0, s.overpopulation());
        assertEquals(2, s.otherDeaths());
    }

    @Test
    void identitiesHoldOnRandomBoardsOverManyGenerations() {
        Random random = new Random(42);
        BoardModel board = new BoardModel();
        for (int row = 0; row < board.size(); row++) {
            for (int column = 0; column < board.size(); column++) {
                if (random.nextInt(100) < 30) {
                    board.setAlive(row, column, true);
                }
            }
        }

        for (int generation = 1; generation <= 20; generation++) {
            Snapshot before = board.snapshot();
            board.step();
            Snapshot after = board.snapshot();
            GenerationStats s = calculator.compute(before, after, SurvivalRule.CONWAY);

            assertEquals(s.alive(), s.survived() + s.births(), "alive at " + generation);
            assertEquals(s.previousAlive(), s.survived() + s.deaths(), "previous at " + generation);
            assertEquals(0, s.otherDeaths(), "Conway has no other deaths at " + generation);
            assertEquals(countAlive(after), s.alive());
        }
    }

    @Test
    void nullSnapshotIsRejected() {
        Snapshot before = new BoardModel().snapshot();
        assertThrows(NullPointerException.class, () -> calculator.compute(before, null, SurvivalRule.CONWAY));
    }

    private static int countAlive(Snapshot snapshot) {
        int count = 0;
        for (int row = 0; row < snapshot.size(); row++) {
            for (int column = 0; column < snapshot.size(); column++) {
                if (snapshot.isAlive(row, column)) {
                    count++;
                }
            }
        }
        return count;
    }
}
