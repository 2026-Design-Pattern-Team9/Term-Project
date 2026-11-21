package com.holub.life;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 창이나 타이머 없이 보드의 상태를 수정하고 세대를 진행한다.
 * 기존 화면은 이 모델과 같은 보드를 그리므로 그리는 동안에도 모델의 잠금을 사용한다.
 */
public final class BoardModel {
    private static final int BLOCK_SIZE = 8;

    private final RuleHolder rules = new RuleHolder(Rules.CONWAY);
    private final Neighborhood board;
    private final List<StepListener> stepListeners = new ArrayList<>();
    private long generation;

    public BoardModel() {
        // 원형 셀이 복제되면서 모든 셀이 이 모델의 규칙 홀더를 넘겨받는다.
        board = new Neighborhood(BLOCK_SIZE, new Neighborhood(BLOCK_SIZE, new Resident(rules)));
    }

    /** 기존 화면이 같은 보드를 그릴 때 사용한다. */
    Neighborhood board() {
        return board;
    }

    public synchronized Rule rule() {
        return rules.get();
    }

    /**
     * 다음 세대부터 적용할 규칙을 바꾼다. 현재 배치와 세대 번호는 그대로다.
     * 보드는 변화가 없는 블록의 계산을 건너뛰므로, 멈춰 있던 배치도 새 규칙으로
     * 다시 계산되도록 살아 있는 셀 주변을 계산 대상으로 표시한다.
     */
    public synchronized void setRule(Rule rule) {
        if (rule.equals(rules.get())) {
            return;
        }
        rules.set(rule);
        restore(snapshot());
    }

    public int size() {
        return board.widthInCells();
    }

    /** 보드 좌표는 왼쪽 위를 (0, 0)으로 하는 행, 열 순서다. */
    public synchronized boolean isAlive(int row, int column) {
        checkPosition(row, column);
        return board.isAlive(row, column);
    }

    /** 클릭에서는 현재 상태를 뒤집는다. */
    public synchronized void toggleCell(int row, int column) {
        checkPosition(row, column);
        board.setAlive(row, column, !board.isAlive(row, column));
    }

    /** 패턴 배치에서는 이미 켜진 셀을 끄지 않고 지정한 상태로 설정한다. */
    public synchronized void setAlive(int row, int column, boolean alive) {
        checkPosition(row, column);
        board.setAlive(row, column, alive);
    }

    /** 보드를 비우고 세대 번호를 초기화한다. */
    public synchronized void clear() {
        board.clear();
        generation = 0;
    }

    /**
     * 다음 상태를 먼저 계산한 뒤 보드에 한꺼번에 반영한다.
     * 반환값은 셀 배치가 바뀌었는지 나타낸다. 그대로여도 세대 번호는 증가한다.
     */
    public synchronized boolean step() {
        List<StepListener> listeners = List.copyOf(stepListeners);
        Snapshot before = listeners.isEmpty() ? null : snapshot();
        boolean changed = board.figureNextState(
                Cell.DUMMY, Cell.DUMMY, Cell.DUMMY, Cell.DUMMY,
                Cell.DUMMY, Cell.DUMMY, Cell.DUMMY, Cell.DUMMY);
        if (changed) {
            board.transition();
        }
        generation++;

        if (!listeners.isEmpty()) {
            Snapshot after = snapshot();
            for (StepListener listener : listeners) {
                listener.stepCompleted(before, after);
            }
        }
        return changed;
    }

    /**
     * 세대 반영 직후, step()을 호출한 스레드에서 순서대로 알린다.
     * 리스너는 모델을 수정하는 대신 전달받은 스냅샷을 읽는다.
     * Swing 화면을 바꿀 때는 이벤트 스레드에 갱신을 요청한다.
     */
    public synchronized void addStepListener(StepListener listener) {
        stepListeners.add(Objects.requireNonNull(listener));
    }

    public synchronized void removeStepListener(StepListener listener) {
        stepListeners.remove(listener);
    }

    /** 보드가 나중에 바뀌어도 읽어 둔 배치와 세대 번호는 유지된다. */
    public synchronized Snapshot snapshot() {
        int size = size();
        boolean[][] cells = new boolean[size][size];
        for (int row = 0; row < size; row++) {
            for (int column = 0; column < size; column++) {
                cells[row][column] = board.isAlive(row, column);
            }
        }
        return new Snapshot(cells, generation);
    }

    /** 이력에서 고른 배치를 적용한다. 복원 자체는 새 세대 진행으로 알리지 않는다. */
    public synchronized void restore(Snapshot snapshot) {
        if (snapshot.size() != size()) {
            throw new IllegalArgumentException("Snapshot size does not match the board");
        }
        board.clear();
        for (int row = 0; row < snapshot.size(); row++) {
            for (int column = 0; column < snapshot.size(); column++) {
                if (snapshot.isAlive(row, column)) {
                    board.setAlive(row, column, true);
                }
            }
        }
        generation = snapshot.generation();
    }

    /** 기존 .life 파일에는 배치만 저장되므로 읽기에 성공하면 0세대로 시작한다. */
    public synchronized void load(InputStream in) throws IOException {
        board.load(in);
        generation = 0;
    }

    /** 기존 .life 형식으로 현재 배치를 기록한다. 스트림은 호출한 쪽에서 닫는다. */
    public synchronized void store(OutputStream out) throws IOException {
        board.createMemento().flush(out);
    }

    private void checkPosition(int row, int column) {
        Objects.checkIndex(row, size());
        Objects.checkIndex(column, size());
    }

    @FunctionalInterface
    public interface StepListener {
        void stepCompleted(Snapshot before, Snapshot after);
    }

    /** 특정 세대의 배치를 읽기 전용으로 보관한다. */
    public static final class Snapshot {
        private final boolean[][] cells;
        private final long generation;

        private Snapshot(boolean[][] cells, long generation) {
            this.cells = cells;
            this.generation = generation;
        }

        public int size() {
            return cells.length;
        }

        public long generation() {
            return generation;
        }

        public boolean isAlive(int row, int column) {
            return cells[row][column];
        }
    }
}
