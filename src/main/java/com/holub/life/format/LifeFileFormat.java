package com.holub.life.format;

import java.awt.Point;
import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * 원본 Holub .life 파일(직렬화한 Point 목록)을 {@link PatternFormat}에 맞추는 Adapter.
 * BoardModel.store()가 만든 파일과 서로 읽고 쓸 수 있다.
 */
public final class LifeFileFormat implements PatternFormat {
    /** 직렬화 파일에서 필요한 클래스만 허용한다. 다른 클래스가 들어 있으면 읽지 않는다. */
    private static final ObjectInputFilter FILTER = ObjectInputFilter.Config.createFilter(
            "java.awt.Point;java.util.LinkedList;java.util.ArrayList;maxdepth=4;!*");

    @Override
    public String description() {
        return "Life File (.life)";
    }

    @Override
    public List<String> extensions() {
        return List.of("life");
    }

    @Override
    public boolean preservesPosition() {
        return true;
    }

    @Override
    public CellPattern read(InputStream in) throws IOException {
        try {
            ObjectInputStream source = new ObjectInputStream(in);
            source.setObjectInputFilter(FILTER);
            Object data = source.readObject();
            if (!(data instanceof Collection<?> cells)) {
                throw new IOException("Invalid Life file");
            }
            CellPattern pattern = new CellPattern("life");
            for (Object cell : cells) {
                if (!(cell instanceof Point point)) {
                    throw new IOException("Invalid cell in Life file");
                }
                pattern.add(point.x, point.y);
            }
            return pattern;
        } catch (ClassNotFoundException | InvalidClassException e) {
            throw new IOException("Invalid Life file", e);
        }
    }

    @Override
    public void write(CellPattern pattern, OutputStream out) throws IOException {
        ObjectOutputStream sink = new ObjectOutputStream(out);
        sink.writeObject(new LinkedList<>(pattern.cells()));
        sink.flush();
    }
}
