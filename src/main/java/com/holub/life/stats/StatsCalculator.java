package com.holub.life.stats;

import com.holub.life.BoardModel.Snapshot;

import java.util.Objects;

/**
 * 세대 계산 전후의 스냅샷을 비교해 통계를 만든다. 화면이나 타이머와 무관한 순수 계산이다.
 * 이웃 수는 계산 전 스냅샷에서 센다. 보드 밖은 죽은 칸으로 본다(원본 보드의 가장자리와 같다).
 */
public final class StatsCalculator {
    private static final int MAX_NEIGHBORS = 8;

    public GenerationStats compute(Snapshot before, Snapshot after, SurvivalRule rule) {
        Objects.requireNonNull(before);
        Objects.requireNonNull(after);
        Objects.requireNonNull(rule);
        if (before.size() != after.size()) {
            throw new IllegalArgumentException("Snapshot sizes differ");
        }

        // 규칙이 사는 이웃 수의 범위를 알려 주면 그 범위 밖 사망을 고독사와 과밀사로 나눈다.
        int minSurvive = -1;
        int maxSurvive = -1;
        for (int n = 0; n <= MAX_NEIGHBORS; n++) {
            if (rule.survives(n)) {
                if (minSurvive < 0) {
                    minSurvive = n;
                }
                maxSurvive = n;
            }
        }

        int size = before.size();
        int survived = 0;
        int births = 0;
        int underpopulation = 0;
        int overpopulation = 0;
        int otherDeaths = 0;
        int alive = 0;
        int previousAlive = 0;

        for (int row = 0; row < size; row++) {
            for (int column = 0; column < size; column++) {
                boolean wasAlive = before.isAlive(row, column);
                boolean isAlive = after.isAlive(row, column);
                if (wasAlive) {
                    previousAlive++;
                }
                if (isAlive) {
                    alive++;
                }

                if (wasAlive && isAlive) {
                    survived++;
                } else if (!wasAlive && isAlive) {
                    births++;
                } else if (wasAlive) {
                    int neighbors = countNeighbors(before, row, column);
                    if (minSurvive < 0) {
                        otherDeaths++;
                    } else if (neighbors < minSurvive) {
                        underpopulation++;
                    } else if (neighbors > maxSurvive) {
                        overpopulation++;
                    } else {
                        otherDeaths++;
                    }
                }
            }
        }

        return new GenerationStats(after.generation(), survived, births, underpopulation,
                overpopulation, otherDeaths, alive, previousAlive, size * size);
    }

    private static int countNeighbors(Snapshot snapshot, int row, int column) {
        int size = snapshot.size();
        int count = 0;
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                if (dr == 0 && dc == 0) {
                    continue;
                }
                int r = row + dr;
                int c = column + dc;
                if (r >= 0 && r < size && c >= 0 && c < size && snapshot.isAlive(r, c)) {
                    count++;
                }
            }
        }
        return count;
    }
}
