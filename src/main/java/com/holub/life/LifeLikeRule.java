package com.holub.life;

/**
 * 탄생(B)과 생존(S) 이웃 수 집합 두 개로 표현하는 규칙.
 * Conway, HighLife, Seeds처럼 숫자만 다른 규칙을 클래스 하나로 나타낸다.
 * 집합은 비트마스크로 보관하며 n번 비트가 켜져 있으면 이웃 n개가 집합에 속한다.
 */
public final class LifeLikeRule implements Rule {
    private static final int ALL_COUNTS = 0x1FF;    // 이웃 0~8개

    private final int birthMask;
    private final int survivalMask;

    LifeLikeRule(int birthMask, int survivalMask) {
        if ((birthMask & ~ALL_COUNTS) != 0 || (survivalMask & ~ALL_COUNTS) != 0) {
            throw new IllegalArgumentException("Neighbor counts must be between 0 and 8");
        }
        // 보드는 변화가 없는 빈 구역의 계산을 건너뛴다. 이웃 없이 태어나는 규칙은 이 전제와 맞지 않는다.
        if ((birthMask & 1) != 0) {
            throw new IllegalArgumentException("B0 rules are not supported");
        }
        this.birthMask = birthMask;
        this.survivalMask = survivalMask;
    }

    @Override
    public boolean nextState(boolean alive, int liveNeighbors) {
        int mask = alive ? survivalMask : birthMask;
        return (mask >> liveNeighbors & 1) != 0;
    }

    @Override
    public String notation() {
        return "B" + digits(birthMask) + "/S" + digits(survivalMask);
    }

    private static String digits(int mask) {
        StringBuilder digits = new StringBuilder();
        for (int count = 0; count <= 8; count++) {
            if ((mask >> count & 1) != 0) {
                digits.append(count);
            }
        }
        return digits.toString();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof LifeLikeRule rule
                && rule.birthMask == birthMask
                && rule.survivalMask == survivalMask;
    }

    @Override
    public int hashCode() {
        return birthMask << 9 | survivalMask;
    }

    @Override
    public String toString() {
        return notation();
    }
}
