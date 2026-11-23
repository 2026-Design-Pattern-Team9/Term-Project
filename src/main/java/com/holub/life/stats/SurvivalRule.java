package com.holub.life.stats;

/**
 * 산 셀이 살아있는 이웃 수 n에서 다음 세대에도 사는지 판정한다.
 * 통계는 사망 원인을 나누는 데만 이 정보를 쓰므로 규칙 전체를 알 필요가 없다.
 */
@FunctionalInterface
public interface SurvivalRule {

    /** 기본 규칙 B3/S23. 이웃이 2개 또는 3개일 때 산다. */
    SurvivalRule CONWAY = neighbors -> neighbors == 2 || neighbors == 3;

    boolean survives(int neighbors);
}
