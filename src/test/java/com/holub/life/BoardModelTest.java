package com.holub.life;

import java.awt.Point;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BoardModelTest {
    @Test
    void blinkerEvolvesAcrossBlockBoundariesWithoutAWindow() {
        BoardModel model = new BoardModel();
        model.setAlive(8, 6, true);
        model.toggleCell(8, 7);
        model.setAlive(8, 8, true);

        assertTrue(model.step());
        assertEquals(1, model.snapshot().generation());
        assertBoard(model.snapshot(), new Point(7, 7), new Point(7, 8), new Point(7, 9));

        assertTrue(model.step());
        assertBoard(model.snapshot(), new Point(6, 8), new Point(7, 8), new Point(8, 8));
    }

    @Test
    void stableBoardStillNotifiesListenersThatTheGenerationFinished() {
        BoardModel model = new BoardModel();
        List<Point> block = List.of(new Point(2, 2), new Point(3, 2),
                new Point(2, 3), new Point(3, 3));
        for (Point point : block) {
            model.setAlive(point.y, point.x, true);
        }

        List<BoardModel.Snapshot> states = new LinkedList<>();
        model.addStepListener((before, after) -> {
            states.add(before);
            states.add(after);
        });

        assertFalse(model.step());
        assertEquals(2, states.size());
        assertEquals(0, states.get(0).generation());
        assertEquals(1, states.get(1).generation());
        assertBoard(states.get(0), block.toArray(Point[]::new));
        assertBoard(states.get(1), block.toArray(Point[]::new));
    }

    @Test
    void snapshotCanRestoreTheBoardAndGenerationAfterClear() {
        BoardModel model = new BoardModel();
        model.setAlive(8, 7, true);
        model.setAlive(8, 8, true);
        model.setAlive(8, 9, true);
        model.step();
        BoardModel.Snapshot saved = model.snapshot();

        model.clear();
        assertEquals(0, model.snapshot().generation());
        assertBoard(model.snapshot());

        model.restore(saved);
        assertEquals(1, model.snapshot().generation());
        assertBoard(model.snapshot(), new Point(8, 7), new Point(8, 8), new Point(8, 9));
        assertTrue(model.step());
        assertBoard(model.snapshot(), new Point(7, 8), new Point(8, 8), new Point(9, 8));
    }

    @Test
    void readsLegacyLifeFileAndStoresTheSameCoordinateFormat() throws IOException, ClassNotFoundException {
        BoardModel model = new BoardModel();
        model.setAlive(25, 25, true);
        model.step();
        model.setAlive(25, 25, true);

        List<Point> cells = List.of(new Point(7, 4), new Point(7, 5), new Point(7, 6));
        // 기존 Store는 같은 좌표 목록을 두 번 수집했다.
        LinkedList<Point> legacy = new LinkedList<>(cells);
        legacy.addAll(cells);
        model.load(serialized(legacy));

        assertEquals(0, model.snapshot().generation());
        assertBoard(model.snapshot(), cells.toArray(Point[]::new));

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        model.store(bytes);
        try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            assertEquals(cells, input.readObject());
        }

        model.step();
        assertBoard(model.snapshot(), new Point(6, 5), new Point(7, 5), new Point(8, 5));
    }

    @Test
    void invalidLifeFileLeavesTheCurrentBoardIntact() throws IOException {
        BoardModel model = new BoardModel();
        model.setAlive(2, 3, true);
        ByteArrayInputStream invalid = serialized(new LinkedList<>(List.of(new Point(8, 9), "wrong")));

        assertThrows(IOException.class, () -> model.load(invalid));
        assertBoard(model.snapshot(), new Point(3, 2));
    }

    private static ByteArrayInputStream serialized(Object value) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
            output.writeObject(value);
        }
        return new ByteArrayInputStream(bytes.toByteArray());
    }

    private static void assertBoard(BoardModel.Snapshot snapshot, Point... liveCells) {
        boolean[][] expected = new boolean[snapshot.size()][snapshot.size()];
        for (Point point : liveCells) {
            expected[point.y][point.x] = true;
        }
        for (int row = 0; row < snapshot.size(); row++) {
            boolean[] rowContents = new boolean[snapshot.size()];
            for (int column = 0; column < snapshot.size(); column++) {
                rowContents[column] = snapshot.isAlive(row, column);
            }
            assertArrayEquals(expected[row], rowContents, "row " + row);
        }
    }
}
