package com.holub.life.format;

import java.awt.Point;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.holub.life.BoardModel;

/**
 * 파일 형식과 무관하게 "살아 있는 셀의 좌표 집합"을 나타낸다.
 * 좌표는 x = 열, y = 행이며 기존 .life 파일의 Point 의미와 같다.
 *
 * 프리셋 라이브러리의 원형(Prototype)으로도 쓰이므로 회전·뒤집기는 자기 자신을 바꾸고,
 * 원형을 건드리지 않으려면 {@link #copy()}로 복제한 뒤 사용한다.
 */
public final class CellPattern {
    private final String name;
    private final Set<Point> cells = new LinkedHashSet<>();

    public CellPattern(String name) {
        this.name = Objects.requireNonNull(name);
    }

    /** 사람이 읽기 쉬운 모양으로 패턴을 만든다. 'O' 또는 '*'가 살아 있는 셀이다. */
    public static CellPattern of(String name, String... rows) {
        CellPattern pattern = new CellPattern(name);
        for (int row = 0; row < rows.length; row++) {
            for (int column = 0; column < rows[row].length(); column++) {
                char c = rows[row].charAt(column);
                if (c == 'O' || c == '*') {
                    pattern.add(column, row);
                }
            }
        }
        return pattern;
    }

    public static CellPattern fromBoard(String name, BoardModel.Snapshot snapshot) {
        CellPattern pattern = new CellPattern(name);
        for (int row = 0; row < snapshot.size(); row++) {
            for (int column = 0; column < snapshot.size(); column++) {
                if (snapshot.isAlive(row, column)) {
                    pattern.add(column, row);
                }
            }
        }
        return pattern;
    }

    public String name() {
        return name;
    }

    public void add(int column, int row) {
        cells.add(new Point(column, row));
    }

    public boolean isEmpty() {
        return cells.isEmpty();
    }

    public int size() {
        return cells.size();
    }

    public boolean contains(int column, int row) {
        return cells.contains(new Point(column, row));
    }

    /** 복사본을 돌려준다. 호출한 쪽에서 바꿔도 이 패턴은 변하지 않는다. */
    public List<Point> cells() {
        List<Point> copy = new ArrayList<>(cells.size());
        for (Point cell : cells) {
            copy.add(new Point(cell));
        }
        return copy;
    }

    /** Prototype: 셀 좌표까지 새로 복제한다. */
    public CellPattern copy() {
        CellPattern copy = new CellPattern(name);
        for (Point cell : cells) {
            copy.cells.add(new Point(cell));
        }
        return copy;
    }

    public int minColumn() {
        return cells.stream().mapToInt(p -> p.x).min().orElse(0);
    }

    public int minRow() {
        return cells.stream().mapToInt(p -> p.y).min().orElse(0);
    }

    public int width() {
        return cells.isEmpty() ? 0 : cells.stream().mapToInt(p -> p.x).max().getAsInt() - minColumn() + 1;
    }

    public int height() {
        return cells.isEmpty() ? 0 : cells.stream().mapToInt(p -> p.y).max().getAsInt() - minRow() + 1;
    }

    /** 왼쪽 위 모서리가 (0, 0)이 되도록 옮긴다. */
    public CellPattern normalize() {
        int dx = -minColumn();
        int dy = -minRow();
        List<Point> moved = new ArrayList<>();
        for (Point cell : cells) {
            moved.add(new Point(cell.x + dx, cell.y + dy));
        }
        cells.clear();
        cells.addAll(moved);
        return this;
    }

    public CellPattern rotateClockwise() {
        List<Point> rotated = new ArrayList<>();
        for (Point cell : cells) {
            rotated.add(new Point(-cell.y, cell.x));
        }
        cells.clear();
        cells.addAll(rotated);
        return normalize();
    }

    public CellPattern flipHorizontal() {
        List<Point> flipped = new ArrayList<>();
        for (Point cell : cells) {
            flipped.add(new Point(-cell.x, cell.y));
        }
        cells.clear();
        cells.addAll(flipped);
        return normalize();
    }

    /**
     * 패턴의 왼쪽 위를 보드의 (row, column)에 맞춰 놓는다.
     * 이미 살아 있는 셀은 끄지 않고, 보드 밖에 나가는 셀은 건너뛴다.
     *
     * @return 실제로 놓은 셀 수
     */
    public int placeOn(BoardModel model, int row, int column) {
        return placeShifted(model, column - minColumn(), row - minRow());
    }

    /** 좌표를 옮기지 않고 그대로 놓는다. 기존 .life 파일을 불러올 때 쓴다. */
    public int placeAsIs(BoardModel model) {
        return placeShifted(model, 0, 0);
    }

    /** 패턴의 바깥 상자를 보드 한가운데에 놓는다. */
    public int placeCentered(BoardModel model) {
        return placeOn(model,
                (model.size() - height()) / 2,
                (model.size() - width()) / 2);
    }

    private int placeShifted(BoardModel model, int dx, int dy) {
        int placed = 0;
        for (Point cell : cells) {
            int row = cell.y + dy;
            int column = cell.x + dx;
            if (row >= 0 && row < model.size() && column >= 0 && column < model.size()) {
                model.setAlive(row, column, true);
                placed++;
            }
        }
        return placed;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof CellPattern that && cells.equals(that.cells);
    }

    @Override
    public int hashCode() {
        return cells.hashCode();
    }

    @Override
    public String toString() {
        return name + cells;
    }
}
