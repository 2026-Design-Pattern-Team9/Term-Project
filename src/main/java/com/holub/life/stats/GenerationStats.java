package com.holub.life.stats;

/**
 * 한 세대가 진행되면서 일어난 일을 센 불변 결과.
 *
 * <p>규칙과 상관없이 항상 성립하는 값은 survived, births, deaths(), alive, previousAlive다.
 * underpopulation, overpopulation, otherDeaths는 규칙의 생존 조건에 따라 사망을 나눈 값이다.
 * 항등식: alive = survived + births, previousAlive = survived + deaths().
 */
public record GenerationStats(
        long generation,
        int survived,
        int births,
        int underpopulation,
        int overpopulation,
        int otherDeaths,
        int alive,
        int previousAlive,
        int cells) {

    public GenerationStats {
        if (survived < 0 || births < 0 || underpopulation < 0 || overpopulation < 0
                || otherDeaths < 0 || alive < 0 || previousAlive < 0 || cells <= 0) {
            throw new IllegalArgumentException("Negative count");
        }
    }

    /** 이번 세대에 죽은 셀의 총수. */
    public int deaths() {
        return underpopulation + overpopulation + otherDeaths;
    }

    /** 이전 세대 대비 산 셀 수의 증감. */
    public int delta() {
        return alive - previousAlive;
    }

    /** 보드 전체 칸 수 대비 산 셀의 비율(0.0 ~ 1.0). */
    public double density() {
        return (double) alive / cells;
    }
}
