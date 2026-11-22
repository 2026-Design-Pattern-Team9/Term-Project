package com.holub.life.control;

import java.util.Objects;

/** 실행 중이면 멈추고, 멈춰 있으면 실행한다. 새로 추가한 Run/Pause 메뉴와 스페이스 키. */
public final class TogglePlayCommand implements Command {
    private final GameController game;

    public TogglePlayCommand(GameController game) {
        this.game = Objects.requireNonNull(game);
    }

    @Override
    public void execute() {
        game.togglePlay();
    }
}
