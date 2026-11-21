package com.holub.life;

import java.util.Map;

import javax.swing.JOptionPane;

import com.holub.ui.MenuSite;

/** "Rule" 메뉴. 미리 정의한 규칙을 고르거나 B/S 표기를 직접 입력해 모델의 규칙을 바꾼다. */
final class RuleMenu {
    private static final String MENU = "Rule";
    private static final String TITLE = "The Game of Life";

    private final BoardModel model;

    private RuleMenu(BoardModel model) {
        this.model = model;
    }

    /** MenuSite가 준비된 뒤에 한 번 호출한다. */
    static void install(BoardModel model) {
        RuleMenu menu = new RuleMenu(model);
        for (Map.Entry<String, Rule> preset : Rules.presets().entrySet()) {
            Rule rule = preset.getValue();
            MenuSite.addLine(menu, MENU, preset.getKey() + " (" + rule.notation() + ")",
                    e -> model.setRule(rule));
        }
        MenuSite.addLine(menu, MENU, "Custom...", e -> menu.askForRule());
    }

    private void askForRule() {
        String input = (String) JOptionPane.showInputDialog(null,
                "Rule in B/S notation (for example B36/S23)", TITLE,
                JOptionPane.QUESTION_MESSAGE, null, null, model.rule().notation());
        if (input == null) {    // 취소
            return;
        }
        try {
            model.setRule(Rules.parse(input));
        }
        catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(null, e.getMessage(), TITLE, JOptionPane.ERROR_MESSAGE);
        }
    }
}
