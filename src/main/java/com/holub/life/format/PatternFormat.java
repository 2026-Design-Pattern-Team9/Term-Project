package com.holub.life.format;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/** 파일 형식 하나가 {@link CellPattern}을 읽고 쓰는 방법. 스트림은 호출한 쪽이 닫는다. */
public interface PatternFormat {
    /** 파일 선택창에 보여 줄 이름. */
    String description();

    /** 점을 뺀 소문자 확장자. 첫 번째 값을 저장할 때 기본으로 쓴다. */
    List<String> extensions();

    /**
     * 파일에 보드 좌표가 그대로 들어 있으면 true.
     * false이면 모양만 저장하므로 불러올 때 보드 가운데에 놓는다.
     */
    boolean preservesPosition();

    CellPattern read(InputStream in) throws IOException;

    void write(CellPattern pattern, OutputStream out) throws IOException;
}
