package com.holub.life.control;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

import com.holub.life.BoardModel;

/**
 * State 패턴의 Context.
 * 마우스, 메뉴, 단축키에서 들어온 입력을 직접 판단하지 않고 현재 상태 객체에 넘긴다.
 * 상태가 바뀌면 등록된 리스너에게 알린다 (통계·상태 패널 등에서 구독).
 */
public final class GameController {
    /** 편집 상태에서 실행/정지 전환을 눌렀을 때의 속도. */
    public static final Speed DEFAULT_SPEED = Speed.MEDIUM;

    @FunctionalInterface
    public interface StateListener {
        /**
         * 상태가 바뀐 직후, 바꾼 스레드에서 호출된다.
         * Swing 화면을 바꿀 때는 이벤트 스레드에 갱신을 요청한다.
         */
        void stateChanged(GameState state);
    }

    private final BoardModel model;
    private final Ticker ticker;
    private final List<StateListener> listeners = new CopyOnWriteArrayList<>();
    private GameState state = EditingState.INSTANCE;

    public GameController(BoardModel model, Ticker ticker) {
        this.model = Objects.requireNonNull(model);
        this.ticker = Objects.requireNonNull(ticker);
    }

    public synchronized GameState state() {
        return state;
    }

    public void addStateListener(StateListener listener) {
        listeners.add(Objects.requireNonNull(listener));
    }

    public void removeStateListener(StateListener listener) {
        listeners.remove(listener);
    }

    // ---------------------------------------------------------------
    // 입력: 모두 현재 상태에게 넘긴다.

    public synchronized void cellClicked(int row, int column) {
        state.cellClicked(this, row, column);
    }

    public synchronized void run(Speed speed) {
        state.run(this, Objects.requireNonNull(speed));
    }

    public synchronized void pause() {
        state.pause(this);
    }

    public synchronized void step() {
        state.step(this);
    }

    public synchronized void togglePlay() {
        state.togglePlay(this);
    }

    /**
     * 판 전체가 바뀐 뒤(파일 불러오기 등) 호출한다.
     * 어느 상태에서든 타이머를 멈추고 편집 상태로 돌아간다.
     */
    public synchronized void boardReplaced() {
        ticker.stop();
        changeState(EditingState.INSTANCE);
    }

    // ---------------------------------------------------------------
    // 상태 객체가 쓰는 동작. 같은 패키지의 상태 클래스만 호출한다.

    void startTicking(Speed speed) {
        ticker.startTicking(speed.millisecondsBetweenTicks());
    }

    void stopTicking() {
        ticker.stop();
    }

    void tickOnce() {
        ticker.tick();
    }

    void toggleCell(int row, int column) {
        model.toggleCell(row, column);
    }

    void changeState(GameState next) {
        if (next == state) {
            return;
        }
        state = next;
        for (StateListener listener : listeners) {
            listener.stateChanged(next);
        }
    }
}
