package com.holub.life.history;

/**
 * 반복된 보드 상태를 발견했을 때의 정보다.
 *
 * <p>보드의 모양이 다시 나타난 세대와 현재 세대의 차이가 주기다.
 * 예를 들어 0세대와 2세대의 보드가 같으면 주기는 2다.</p>
 */
public final class PeriodDetectionResult {
    private final long startGeneration;
    private final long currentGeneration;
    private final long period;

    PeriodDetectionResult(long startGeneration, long currentGeneration) {
        if (startGeneration < 0 || currentGeneration < startGeneration) {
            throw new IllegalArgumentException("Invalid generation range");
        }
        if (startGeneration == currentGeneration) {
            throw new IllegalArgumentException("A period needs two different generations");
        }
        this.startGeneration = startGeneration;
        this.currentGeneration = currentGeneration;
        this.period = currentGeneration - startGeneration;
    }

    public long startGeneration() {
        return startGeneration;
    }

    public long currentGeneration() {
        return currentGeneration;
    }

    public long period() {
        return period;
    }

    @Override
    public String toString() {
        return "period " + period + " (from generation " + startGeneration
                + " to " + currentGeneration + ")";
    }
}
