package com.holub.life.control;

import java.util.Objects;

/** 한 세대 전으로 되돌린다. Go 메뉴의 Undo와 Cmd/Ctrl+Z. */
public final class UndoCommand implements Command {
    private final GameController game;

    public UndoCommand(GameController game) {
        this.game = Objects.requireNonNull(game);
    }

    @Override
    public void execute() {
        game.undo();
    }
}
