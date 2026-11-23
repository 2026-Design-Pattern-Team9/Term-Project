package com.holub.life.control;

import java.util.Objects;

/** 한 세대만 진행한다. 원본의 Tick (Single Step) 메뉴. */
public final class StepCommand implements Command {
    private final GameController game;

    public StepCommand(GameController game) {
        this.game = Objects.requireNonNull(game);
    }

    @Override
    public void execute() {
        game.step();
    }
}
