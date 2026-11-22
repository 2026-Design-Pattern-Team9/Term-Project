package com.holub.life.control;

/**
 * 세대 진행 타이머.
 * GameController는 Clock 싱글턴을 직접 부르지 않고 이 인터페이스에만 의존한다.
 * 그래서 테스트에서는 실제 타이머 대신 가짜 구현을 넣을 수 있다.
 */
public interface Ticker {
    /** 주어진 간격으로 자동 진행을 시작한다. 이미 진행 중이면 새 간격으로 다시 시작한다. */
    void startTicking(int millisecondsBetweenTicks);

    /** 자동 진행을 멈춘다. */
    void stop();

    /** 한 세대만 진행한다. */
    void tick();
}
