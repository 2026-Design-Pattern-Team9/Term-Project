package com.holub.life.control;

import java.util.Objects;
import java.util.Optional;

/** 타이머가 돌면서 세대가 자동으로 진행되는 상태. */
final class RunningState implements GameState {
    private final Speed speed;

    RunningState(Speed speed) {
        this.speed = Objects.requireNonNull(speed);
    }

    @Override
    public Mode mode() {
        return Mode.RUNNING;
    }

    @Override
    public Optional<Speed> speed() {
        return Optional.of(speed);
    }

    @Override
    public String displayName() {
        return "Running (" + speed.menuName() + ")";
    }

    /**
     * 실행 중에 찍은 칸은 이웃이 없으면 다음 틱에 바로 죽어서 클릭이 무시된 것처럼 보였다.
     * 먼저 일시정지한 뒤 칸을 바꿔서 사용자가 결과를 확인할 수 있게 한다.
     */
    @Override
    public void cellClicked(GameController game, int row, int column) {
        game.stopTicking();
        game.changeState(new PausedState(speed));
        game.toggleCell(row, column);
    }

    @Override
    public void run(GameController game, Speed newSpeed) {
        if (newSpeed == speed) {
            return;
        }
        game.startTicking(newSpeed);
        game.changeState(new RunningState(newSpeed));
    }

    @Override
    public void pause(GameController game) {
        game.stopTicking();
        game.changeState(new PausedState(speed));
    }

    /** 자동 진행 중에는 한 세대 진행 요청을 무시한다. 원본은 틱을 하나 더 끼워 넣었다. */
    @Override
    public void step(GameController game) {
    }

    @Override
    public void togglePlay(GameController game) {
        pause(game);
    }
}
