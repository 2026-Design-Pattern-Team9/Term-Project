package com.holub.life.control;

import java.util.Objects;

/** 지정한 속도로 실행한다. 원본의 Agonizing / Slow / Medium / Fast 메뉴. */
public final class RunCommand implements Command {
    private final GameController game;
    private final Speed speed;

    public RunCommand(GameController game, Speed speed) {
        this.game = Objects.requireNonNull(game);
        this.speed = Objects.requireNonNull(speed);
    }

    public Speed speed() {
        return speed;
    }

    @Override
    public void execute() {
        game.run(speed);
    }
}
