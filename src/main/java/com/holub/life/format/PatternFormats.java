package com.holub.life.format;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/** 파일 이름의 확장자로 알맞은 {@link PatternFormat}을 골라 주는 Factory. */
public final class PatternFormats {
    private static final List<PatternFormat> ALL = List.of(
            new LifeFileFormat(), new PlaintextFormat(), new RleFormat());

    private PatternFormats() {
    }

    public static List<PatternFormat> all() {
        return ALL;
    }

    public static PatternFormat forFileName(String fileName) throws IOException {
        int dot = fileName.lastIndexOf('.');
        String extension = dot < 0 ? "" : fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
        for (PatternFormat format : ALL) {
            if (format.extensions().contains(extension)) {
                return format;
            }
        }
        throw new IOException("Unsupported file format: " + fileName);
    }

    /** 확장자를 안 붙이고 저장하려고 할 때 형식의 기본 확장자를 붙인다. */
    public static String withDefaultExtension(String fileName, PatternFormat format) {
        int dot = fileName.lastIndexOf('.');
        if (dot >= 0 && format.extensions().contains(fileName.substring(dot + 1).toLowerCase(Locale.ROOT))) {
            return fileName;
        }
        return fileName + "." + format.extensions().get(0);
    }
}
