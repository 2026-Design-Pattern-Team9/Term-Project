package com.holub.life.control;

/**
 * 자동 진행 속도.
 * 원본 Clock은 메뉴 이름의 첫 글자로 간격을 골랐다('A' 500, 'S' 150, 'M' 70, 'F' 30).
 * 같은 값을 이름 있는 상수로 옮겨, 메뉴 이름과 간격이 따로 놀지 않게 했다.
 */
public enum Speed {
    AGONIZING("Agonizing", 500),
    SLOW("Slow", 150),
    MEDIUM("Medium", 70),
    FAST("Fast", 30);

    private final String menuName;
    private final int millisecondsBetweenTicks;

    Speed(String menuName, int millisecondsBetweenTicks) {
        this.menuName = menuName;
        this.millisecondsBetweenTicks = millisecondsBetweenTicks;
    }

    /** Go 메뉴에 표시되는 이름. 원본 메뉴 이름과 같다. */
    public String menuName() {
        return menuName;
    }

    public int millisecondsBetweenTicks() {
        return millisecondsBetweenTicks;
    }
}
