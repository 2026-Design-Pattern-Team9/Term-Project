package com.holub.life;

/**
 * 셀 하나가 다음 세대에 살아 있을지를 정하는 규칙.
 * 구현체는 상태를 바꾸지 않으므로 보드의 모든 셀이 객체 하나를 공유한다.
 */
public interface Rule {
    /**
     * @param alive 현재 살아 있는지
     * @param liveNeighbors 살아 있는 이웃 수 (0~8)
     * @return 다음 세대에 살아 있으면 true
     */
    boolean nextState(boolean alive, int liveNeighbors);

    /** "B3/S23" 형식의 표기. */
    String notation();
}
