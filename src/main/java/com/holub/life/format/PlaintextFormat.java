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
 * Life 커뮤니티의 Plaintext(.cells) 형식. '!'로 시작하는 줄은 설명이고
 * 'O'나 '*'는 살아 있는 셀, '.'은 죽은 셀이다.
 */
public final class PlaintextFormat implements PatternFormat {
    static final int MAX_LINES = 10_000;

    @Override
    public String description() {
        return "Plaintext (.cells)";
    }

    @Override
    public List<String> extensions() {
        return List.of("cells");
    }

    @Override
    public boolean preservesPosition() {
        return false;
    }

    @Override
    public CellPattern read(InputStream in) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        String name = "cells";
        List<String> rows = new ArrayList<>();
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("!")) {
                if (line.startsWith("!Name:")) {
                    name = line.substring("!Name:".length()).trim();
                }
                continue;
            }
            if (rows.size() >= MAX_LINES) {
                throw new IOException("Too many lines in cells file");
            }
            rows.add(line);
        }

        CellPattern pattern = new CellPattern(name);
        for (int row = 0; row < rows.size(); row++) {
            String text = rows.get(row);
            for (int column = 0; column < text.length(); column++) {
                char c = text.charAt(column);
                if (c == 'O' || c == '*') {
                    pattern.add(column, row);
                } else if (c != '.' && c != ' ' && c != '\t') {
                    throw new IOException("Unexpected character '" + c + "' in cells file");
                }
            }
        }
        return pattern;
    }

    @Override
    public void write(CellPattern pattern, OutputStream out) throws IOException {
        Writer writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
        writer.write("!Name: " + pattern.name() + "\n");
        CellPattern shape = pattern.copy().normalize();
        for (int row = 0; row < shape.height(); row++) {
            StringBuilder text = new StringBuilder();
            for (int column = 0; column < shape.width(); column++) {
                text.append(shape.contains(column, row) ? 'O' : '.');
            }
            writer.write(text + "\n");
        }
        writer.flush();
    }
}
