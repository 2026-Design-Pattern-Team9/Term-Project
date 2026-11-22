package com.holub.life.control;

import java.util.Optional;

/**
 * 판을 새로 그리는 상태. 프로그램을 시작했을 때와 파일을 불러온 뒤의 상태다.
 * 일시정지와 동작은 같지만 이어서 실행할 속도가 없다는 점이 다르다.
 * 보관하는 값이 없어서 인스턴스 하나를 공유한다.
 */
final class EditingState implements GameState {
    static final EditingState INSTANCE = new EditingState();

    private EditingState() {
    }

    @Override
    public Mode mode() {
        return Mode.EDITING;
    }

    @Override
    public Optional<Speed> speed() {
        return Optional.empty();
    }

    @Override
    public String displayName() {
        return "Editing";
    }

    @Override
    public void cellClicked(GameController game, int row, int column) {
        game.toggleCell(row, column);
    }

    @Override
    public void run(GameController game, Speed speed) {
        game.startTicking(speed);
        game.changeState(new RunningState(speed));
    }

    @Override
    public void pause(GameController game) {
        // 이미 멈춰 있다.
    }

    @Override
    public void step(GameController game) {
        game.tickOnce();
    }

    @Override
    public void togglePlay(GameController game) {
        run(game, GameController.DEFAULT_SPEED);
    }
}
