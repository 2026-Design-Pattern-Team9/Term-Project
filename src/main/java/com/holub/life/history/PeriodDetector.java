package com.holub.life.history;

import com.holub.life.BoardModel;

import java.util.List;
import java.util.Optional;

/**
 * 저장된 보드 배치에서 현재 배치와 정확히 같은 상태를 찾는다.
 * 세대 번호는 비교하지 않고 살아 있는 칸의 위치만 비교한다.
 */
public final class PeriodDetector {

    /**
     * 가장 최근에 같은 배치가 나타난 세대와 현재 세대 사이의 주기를 찾는다.
     * 이력에 없는 과거 세대까지 추정하지 않고, 전달받은 이력 범위 안에서만 검사한다.
     */
    public Optional<PeriodDetectionResult> detect(
            List<BoardModel.Snapshot> history,
            BoardModel.Snapshot current) {
        for (int index = history.size() - 1; index >= 0; index--) {
            BoardModel.Snapshot candidate = history.get(index);
            if (candidate.generation() >= current.generation()) {
                continue;
            }
            if (sameBoard(candidate, current)) {
                return Optional.of(new PeriodDetectionResult(
                        candidate.generation(), current.generation()));
            }
        }
        return Optional.empty();
    }

    private boolean sameBoard(BoardModel.Snapshot first, BoardModel.Snapshot second) {
        if (first.size() != second.size()) {
            return false;
        }
        for (int row = 0; row < first.size(); row++) {
            for (int column = 0; column < first.size(); column++) {
                if (first.isAlive(row, column) != second.isAlive(row, column)) {
                    return false;
                }
            }
        }
        return true;
    }
}
