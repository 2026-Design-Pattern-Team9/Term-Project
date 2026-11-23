package com.holub.life.format;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Run Length Encoded(.rle) 형식. 'b'는 죽은 셀, 'o'는 살아 있는 셀, '$'는 줄 바꿈이고
 * 앞의 숫자는 반복 횟수, '!'는 끝이다. 헤더의 rule은 읽고 무시한다(Conway B3/S23로 처리).
 */
public final class RleFormat implements PatternFormat {
    static final int MAX_RUN = 100_000;
    private static final int LINE_WIDTH = 70;

    @Override
    public String description() {
        return "Run Length Encoded (.rle)";
    }

    @Override
    public List<String> extensions() {
        return List.of("rle");
    }

    @Override
    public boolean preservesPosition() {
        return false;
    }

    @Override
    public CellPattern read(InputStream in) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        String name = "rle";
        StringBuilder body = new StringBuilder();
        boolean sawHeader = false;
        String line;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }
            if (line.startsWith("#")) {
                if (line.startsWith("#N")) {
                    name = line.substring(2).trim();
                }
                continue;
            }
            if (!sawHeader) {
                if (!line.startsWith("x")) {
                    throw new IOException("Missing header line in rle file");
                }
                sawHeader = true;
                continue;
            }
            body.append(line);
            if (line.contains("!")) {
                break;
            }
        }
        if (!sawHeader) {
            throw new IOException("Missing header line in rle file");
        }

        CellPattern pattern = new CellPattern(name);
        int row = 0;
        int column = 0;
        int count = 0;
        boolean ended = false;
        for (int i = 0; i < body.length() && !ended; i++) {
            char c = body.charAt(i);
            if (Character.isDigit(c)) {
                count = count * 10 + (c - '0');
                if (count > MAX_RUN) {
                    throw new IOException("Run too long in rle file");
                }
                continue;
            }
            int run = count == 0 ? 1 : count;
            count = 0;
            switch (c) {
                case 'b', '.' -> column += run;
                case 'o', 'O' -> {
                    for (int k = 0; k < run; k++) {
                        pattern.add(column++, row);
                    }
                }
                case '$' -> {
                    row += run;
                    column = 0;
                    if (row > MAX_RUN) {
                        throw new IOException("Too many rows in rle file");
                    }
                }
                case '!' -> ended = true;
                default -> throw new IOException("Unexpected character '" + c + "' in rle file");
            }
        }
        if (!ended) {
            throw new IOException("Missing '!' terminator in rle file");
        }
        return pattern;
    }

    @Override
    public void write(CellPattern pattern, OutputStream out) throws IOException {
        CellPattern shape = pattern.copy().normalize();
        List<String> tokens = new ArrayList<>();
        for (int row = 0; row < shape.height(); row++) {
            int column = 0;
            while (column < shape.width()) {
                boolean alive = shape.contains(column, row);
                int end = column;
                while (end < shape.width() && shape.contains(end, row) == alive) {
                    end++;
                }
                if (alive) {                         // 줄 끝의 죽은 셀은 적지 않는다.
                    tokens.add(run(end - column, 'o'));
                } else if (end < shape.width()) {
                    tokens.add(run(end - column, 'b'));
                }
                column = end;
            }
            if (row < shape.height() - 1) {
                tokens.add("$");
            }
        }
        tokens = mergeNewlines(tokens);
        tokens.add("!");

        Writer writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
        writer.write("#N " + pattern.name() + "\n");
        writer.write("x = " + shape.width() + ", y = " + shape.height() + ", rule = B3/S23\n");
        int lineLength = 0;
        for (String token : tokens) {
            if (lineLength + token.length() > LINE_WIDTH) {
                writer.write("\n");
                lineLength = 0;
            }
            writer.write(token);
            lineLength += token.length();
        }
        writer.write("\n");
        writer.flush();
    }

    private static String run(int length, char symbol) {
        return length == 1 ? String.valueOf(symbol) : length + String.valueOf(symbol);
    }

    private static List<String> mergeNewlines(List<String> tokens) {
        List<String> merged = new ArrayList<>();
        int newlines = 0;
        for (String token : tokens) {
            if (token.equals("$")) {
                newlines++;
                continue;
            }
            if (newlines > 0) {
                merged.add(run(newlines, '$'));
                newlines = 0;
            }
            merged.add(token);
        }
        if (newlines > 0) {
            merged.add(run(newlines, '$'));
        }
        return merged;
    }
}
