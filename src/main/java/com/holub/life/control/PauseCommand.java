package com.holub.life.control;

import java.util.Objects;

/** 자동 진행을 멈춘다. 원본의 Halt 메뉴. */
public final class PauseCommand implements Command {
    private final GameController game;

    public PauseCommand(GameController game) {
        this.game = Objects.requireNonNull(game);
    }

    @Override
    public void execute() {
        game.pause();
    }
}
