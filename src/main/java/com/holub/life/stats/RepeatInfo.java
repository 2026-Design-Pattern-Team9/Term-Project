package com.holub.life.stats;

/**
 * 반복 감지 결과. {@code period}는 같은 모양이 다시 나타나기까지 걸린 세대 수다(정지한 모양은 1,
 * 블링커는 2). 판정은 이력을 가진 쪽(C)이 하고, 통계 패널은 주기를 보여 주기만 한다.
 *
 * <p>C의 결과에는 같은 모양이 가장 최근에 나타났던 세대도 들어 있지만, 모양이 계속 같으면 그 값이
 * 현재 세대를 따라 바뀌어서 사용자에게 의미가 없으므로 가져오지 않는다.
 */
public record RepeatInfo(long period) {
    public RepeatInfo {
        if (period < 1) {
            throw new IllegalArgumentException("invalid period: " + period);
        }
    }
}
