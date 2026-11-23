package com.holub.life.control;

import java.util.Objects;
import java.util.Optional;

/** 실행하다 멈춘 상태. 이어서 실행할 때 쓰도록 직전 속도를 기억한다. */
final class PausedState implements GameState {
    private final Speed lastSpeed;

    PausedState(Speed lastSpeed) {
        this.lastSpeed = Objects.requireNonNull(lastSpeed);
    }

    @Override
    public Mode mode() {
        return Mode.PAUSED;
    }

    @Override
    public Optional<Speed> speed() {
        return Optional.of(lastSpeed);
    }

    @Override
    public String displayName() {
        return "Paused";
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
        run(game, lastSpeed);
    }

    @Override
    public void undo(GameController game) {
        game.undoOnce();
    }
}
