package com.holub.life.stats;

import java.util.Locale;

/** 통계를 화면에 보일 문자열로 바꾼다. 화면 없이 테스트할 수 있도록 패널과 분리했다. */
public final class StatsFormatter {
    public static final String EMPTY = "-";

    /** 표의 행 이름. 순서가 화면 순서다. */
    public static final String[] ROW_NAMES = {"번식", "생존", "고독사", "과밀사", "기타 사망"};

    private StatsFormatter() {
    }

    /** 카드 다섯 칸의 값: 세대, 생존 수, 증감, 밀도, 반복 주기. 기록이 없으면 세대만 0이다. */
    public static String[] cards(StatsModel.State state) {
        GenerationStats s = state.latest();
        if (s == null) {
            return new String[] {"0", EMPTY, EMPTY, EMPTY, EMPTY};
        }
        return new String[] {
                Long.toString(s.generation()),
                Integer.toString(s.alive()),
                String.format(Locale.ROOT, "%+d", s.delta()),
                String.format(Locale.ROOT, "%.1f%%", s.density() * 100),
                state.repeat() == null ? EMPTY : "주기 " + state.repeat().period()
        };
    }

    /** 증감의 부호. 기록이 없으면 0이다. 색을 고를 때 쓴다. */
    public static int deltaSign(StatsModel.State state) {
        GenerationStats s = state.latest();
        return s == null ? 0 : Integer.signum(s.delta());
    }

    /** 표의 숫자. [행][0=이번 세대, 1=누적]. 이번 세대 기록이 없으면 "-"이다. */
    public static String[][] table(StatsModel.State state) {
        GenerationStats s = state.latest();
        StatsModel.Totals t = state.totals();
        long[] total = {t.births(), t.survived(), t.underpopulation(),
                t.overpopulation(), t.otherDeaths()};
        int[] now = s == null ? null : new int[] {s.births(), s.survived(),
                s.underpopulation(), s.overpopulation(), s.otherDeaths()};
        String[][] rows = new String[ROW_NAMES.length][2];
        for (int i = 0; i < rows.length; i++) {
            rows[i][0] = now == null ? EMPTY : Integer.toString(now[i]);
            rows[i][1] = Long.toString(total[i]);
        }
        return rows;
    }
}
