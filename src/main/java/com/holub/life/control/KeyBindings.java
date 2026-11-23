package com.holub.life.control;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

/**
 * 단축키. 메뉴와 같은 Command 객체를 쓴다.
 *   스페이스        실행/정지 전환
 *   오른쪽 화살표   한 세대 진행
 *   Cmd+Z / Ctrl+Z  한 세대 되돌리기 (Mac은 Cmd, 그 외는 Ctrl)
 */
public final class KeyBindings {
    private KeyBindings() {
    }

    public static void install(JComponent target, GameController game) {
        bind(target, KeyStroke.getKeyStroke("SPACE"), "togglePlay", new TogglePlayCommand(game));
        bind(target, KeyStroke.getKeyStroke("RIGHT"), "step", new StepCommand(game));
        int menuShortcut = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        bind(target, KeyStroke.getKeyStroke(KeyEvent.VK_Z, menuShortcut), "undo", new UndoCommand(game));
    }

    private static void bind(JComponent target, KeyStroke key, String name, Command command) {
        target.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(key, name);
        target.getActionMap().put(name, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                command.execute();
            }
        });
    }
}
