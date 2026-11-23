package com.holub.life.history;

import com.holub.life.BoardModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 최근 세대의 보드 스냅샷을 보관하고 이전 상태로 되돌린다.
 *
 * <p>이 클래스는 화면이나 파일을 알지 않는다. BoardModel의 계산 완료
 * 이벤트를 구독하면 자동으로 다음 세대를 기록할 수 있다.</p>
 */
public final class GenerationHistory implements BoardModel.StepListener {
    private final int maximumSnapshots;
    private final PeriodDetector periodDetector;
    private final List<BoardModel.Snapshot> snapshots = new ArrayList<>();
    private int cursor;
    private PeriodDetectionResult period;

    /**
     * 최대 보관 개수는 현재 상태를 포함한다.
     * 예를 들어 6이면 현재 상태와 그 이전 5개 상태를 보관한다.
     */
    public GenerationHistory(int maximumSnapshots, BoardModel.Snapshot initial) {
        if (maximumSnapshots < 2) {
            throw new IllegalArgumentException("At least two snapshots are required");
        }
        this.maximumSnapshots = maximumSnapshots;
        this.periodDetector = new PeriodDetector();
        reset(initial);
    }

    /** 이력의 기준점을 새 상태 하나로 바꾼다. Clear·Load·규칙 변경 뒤 사용한다. */
    public synchronized void reset(BoardModel.Snapshot initial) {
        snapshots.clear();
        snapshots.add(Objects.requireNonNull(initial));
        cursor = 0;
        period = null;
    }

    /** BoardModel의 세대 완료 이벤트를 받아 다음 상태를 이력에 추가한다. */
    @Override
    public synchronized void stepCompleted(
            BoardModel.Snapshot before,
            BoardModel.Snapshot after) {
        record(after);
    }

    /** 외부에서 계산 완료 상태를 직접 기록할 때 사용한다. */
    public synchronized void record(BoardModel.Snapshot snapshot) {
        Objects.requireNonNull(snapshot);

        // Undo 후 새 세대가 진행되면 기존의 미래 이력은 폐기한다.
        while (snapshots.size() > cursor + 1) {
            snapshots.remove(snapshots.size() - 1);
        }
        snapshots.add(snapshot);
        cursor = snapshots.size() - 1;

        while (snapshots.size() > maximumSnapshots) {
            snapshots.remove(0);
            cursor--;
        }
        period = periodDetector.detect(snapshots, snapshot).orElse(null);
    }

    /** 현재 커서가 가리키는 상태를 복원하고 한 단계 이전으로 이동한다. */
    public boolean undo(BoardModel model) {
        Objects.requireNonNull(model);

        // BoardModel.step() holds the model lock while it notifies this
        // listener, which then takes the history lock.  Take the locks in
        // that same order here to prevent a step/undo deadlock.
        synchronized (model) {
            synchronized (this) {
                if (!canUndo()) {
                    return false;
                }
                cursor--;
                BoardModel.Snapshot target = snapshots.get(cursor);
                model.restore(target);
                period = periodDetector.detect(snapshots.subList(0, cursor + 1), target)
                        .orElse(null);
                return true;
            }
        }
    }

    public synchronized boolean canUndo() {
        return cursor > 0;
    }

    public synchronized int size() {
        return snapshots.size();
    }

    public int maximumSnapshots() {
        return maximumSnapshots;
    }

    public synchronized BoardModel.Snapshot current() {
        return snapshots.get(cursor);
    }

    /** 디버깅·테스트·상태 패널에서 사용할 읽기 전용 이력이다. */
    public synchronized List<BoardModel.Snapshot> snapshots() {
        return Collections.unmodifiableList(new ArrayList<>(snapshots));
    }

    public synchronized Optional<PeriodDetectionResult> period() {
        return Optional.ofNullable(period);
    }
}
