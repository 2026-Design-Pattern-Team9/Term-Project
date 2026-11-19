package com.holub.life;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Gradle/JUnit 환경 확인용 스모크 테스트.
 * Resident(단일 셀)를 UI 없이 독립적으로 생성해 원본 B3/S23 규칙을 검증한다.
 */
class ResidentTest {

    private static Cell alive() {
        Resident r = new Resident();
        r.userClicked(null, null);   // 원본 API: 클릭 시 생사 토글
        return r;
    }

    private static Cell dead() {
        return Cell.DUMMY;
    }

    @Test
    void deadCellWithThreeLiveNeighborsIsBorn() {
        Resident cell = new Resident();
        cell.figureNextState(alive(), alive(), alive(), dead(),
                             dead(), dead(), dead(), dead());
        cell.transition();
        assertTrue(cell.isAlive());
    }

    @Test
    void liveCellWithTwoLiveNeighborsSurvives() {
        Resident cell = (Resident) alive();
        cell.figureNextState(alive(), alive(), dead(), dead(),
                             dead(), dead(), dead(), dead());
        cell.transition();
        assertTrue(cell.isAlive());
    }

    @Test
    void liveCellWithOneLiveNeighborDies() {
        Resident cell = (Resident) alive();
        cell.figureNextState(alive(), dead(), dead(), dead(),
                             dead(), dead(), dead(), dead());
        cell.transition();
        assertFalse(cell.isAlive());
    }

    @Test
    void liveCellWithFourLiveNeighborsDies() {
        Resident cell = (Resident) alive();
        cell.figureNextState(alive(), alive(), alive(), alive(),
                             dead(), dead(), dead(), dead());
        cell.transition();
        assertFalse(cell.isAlive());
    }
}
