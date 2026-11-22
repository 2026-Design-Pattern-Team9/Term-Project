package com.holub.life.control;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

/**
 * 단축키. 메뉴와 같은 Command 객체를 쓴다.
 *   스페이스      실행/정지 전환
 *   오른쪽 화살표 한 세대 진행
 */
public final class KeyBindings {
    private KeyBindings() {
    }

    public static void install(JComponent target, GameController game) {
        bind(target, "SPACE", "togglePlay", new TogglePlayCommand(game));
        bind(target, "RIGHT", "step", new StepCommand(game));
    }

    private static void bind(JComponent target, String key, String name, Command command) {
        target.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
              .put(KeyStroke.getKeyStroke(key), name);
        target.getActionMap().put(name, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                command.execute();
            }
        });
    }
}
