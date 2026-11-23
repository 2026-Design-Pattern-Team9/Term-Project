package com.holub.life.control;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

import com.holub.life.BoardModel;
import com.holub.life.history.GenerationHistory;

/**
 * State 패턴의 Context.
 * 마우스, 메뉴, 단축키에서 들어온 입력을 직접 판단하지 않고 현재 상태 객체에 넘긴다.
 * 상태가 바뀌면 StateListener에게, 판이 세대 진행 말고 다른 이유로 바뀌면
 * BoardListener에게 알린다 (화면 다시 그리기, 통계·상태 패널 갱신).
 *
 * <p>잠금 순서: 세대 진행은 model → history 순서로 잠근다.
 * 이 클래스는 컨트롤러 잠금을 잡은 채로 history.undo(model)을 부르므로
 * 세대 완료 리스너(StepListener)에서 컨트롤러 메서드를 다시 부르지 않는다.
 * 리스너가 현재 상태를 읽을 수 있도록 state()는 잠금 없이 읽는다.</p>
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

    /**
     * 세대 진행이 아닌 이유(편집, 되돌리기, 비우기, 불러오기)로 판이 바뀌었을 때 호출된다.
     * 세대 진행은 BoardModel의 StepListener로 알 수 있다.
     */
    @FunctionalInterface
    public interface BoardListener {
        void boardChanged();
    }

    private final BoardModel model;
    private final Ticker ticker;
    private final GenerationHistory history;
    private final List<StateListener> stateListeners = new CopyOnWriteArrayList<>();
    private final List<BoardListener> boardListeners = new CopyOnWriteArrayList<>();
    private volatile GameState state = EditingState.INSTANCE;

    public GameController(BoardModel model, Ticker ticker, GenerationHistory history) {
        this.model = Objects.requireNonNull(model);
        this.ticker = Objects.requireNonNull(ticker);
        this.history = Objects.requireNonNull(history);
    }

    public GameState state() {
        return state;
    }

    public void addStateListener(StateListener listener) {
        stateListeners.add(Objects.requireNonNull(listener));
    }

    public void removeStateListener(StateListener listener) {
        stateListeners.remove(listener);
    }

    public void addBoardListener(BoardListener listener) {
        boardListeners.add(Objects.requireNonNull(listener));
    }

    public void removeBoardListener(BoardListener listener) {
        boardListeners.remove(listener);
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

    /** 한 세대 전으로 되돌린다. 실행 중이면 먼저 일시정지한다. */
    public synchronized void undo() {
        state.undo(this);
    }

    /**
     * 판 전체가 바뀐 뒤(파일 불러오기, 패턴 배치) 호출한다.
     * 어느 상태에서든 타이머를 멈추고, 이력을 새 시작점으로 바꾸고, 편집 상태로 돌아간다.
     */
    public synchronized void boardReplaced() {
        ticker.stop();
        history.reset(model.snapshot());
        changeState(EditingState.INSTANCE);
        notifyBoardChanged();
    }

    /**
     * 규칙이 바뀐 뒤 호출한다. 이전 규칙으로 계산한 세대와 섞이지 않도록 이력을 새로 시작한다.
     * 실행 중이어도 멈추지 않고 다음 세대부터 새 규칙으로 계산한다 (B의 설계).
     */
    public synchronized void ruleChanged() {
        history.reset(model.snapshot());
        notifyBoardChanged();
    }

    /** Grid > Clear. 멈추고 판을 비운 뒤 새 판으로 취급한다. */
    public synchronized void clearBoard() {
        ticker.stop();
        model.clear();
        history.reset(model.snapshot());
        changeState(EditingState.INSTANCE);
        notifyBoardChanged();
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

    /**
     * 칸 하나를 바꾼다. 손으로 고친 판은 지난 세대들과 이어지지 않으므로
     * 이력을 새 시작점으로 바꾼다. 그러지 않으면 되돌릴 때 고친 내용이 사라진다.
     */
    void toggleCell(int row, int column) {
        model.toggleCell(row, column);
        history.reset(model.snapshot());
        notifyBoardChanged();
    }

    /** 되돌릴 이력이 있으면 한 세대 전으로 복원하고 알린다. */
    void undoOnce() {
        if (history.undo(model)) {
            notifyBoardChanged();
        }
    }

    void changeState(GameState next) {
        if (next == state) {
            return;
        }
        state = next;
        for (StateListener listener : stateListeners) {
            listener.stateChanged(next);
        }
    }

    private void notifyBoardChanged() {
        for (BoardListener listener : boardListeners) {
            listener.boardChanged();
        }
    }
}
