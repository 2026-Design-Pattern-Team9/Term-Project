package com.holub.life;

import java.util.Map;

import javax.swing.JOptionPane;

import com.holub.ui.MenuSite;

/** "Rule" 메뉴. 미리 정의한 규칙을 고르거나 B/S 표기를 직접 입력해 모델의 규칙을 바꾼다. */
final class RuleMenu {
    private static final String MENU = "Rule";
    private static final String TITLE = "The Game of Life";

    private final BoardModel model;
    private final Runnable afterRuleChange;

    private RuleMenu(BoardModel model, Runnable afterRuleChange) {
        this.model = model;
        this.afterRuleChange = afterRuleChange;
    }

    /** MenuSite가 준비된 뒤에 한 번 호출한다. */
    static void install(BoardModel model) {
        install(model, () -> { });
    }

    /**
     * 규칙이 실제로 바뀐 뒤 afterRuleChange를 부른다 (예: 세대 이력 초기화).
     * 같은 규칙을 다시 고르면 부르지 않는다.
     */
    static void install(BoardModel model, Runnable afterRuleChange) {
        RuleMenu menu = new RuleMenu(model, afterRuleChange);
        for (Map.Entry<String, Rule> preset : Rules.presets().entrySet()) {
            Rule rule = preset.getValue();
            MenuSite.addLine(menu, MENU, preset.getKey() + " (" + rule.notation() + ")",
                    e -> menu.apply(rule));
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
            apply(Rules.parse(input));
        }
        catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(null, e.getMessage(), TITLE, JOptionPane.ERROR_MESSAGE);
        }
    }

    private void apply(Rule rule) {
        applyRule(model, rule, afterRuleChange);
    }

    /** 규칙을 적용하고, 실제로 바뀌었을 때만 afterRuleChange를 부른다. 화면 없이 테스트할 수 있게 분리했다. */
    static void applyRule(BoardModel model, Rule rule, Runnable afterRuleChange) {
        Rule before = model.rule();
        model.setRule(rule);
        if (!model.rule().equals(before)) {
            afterRuleChange.run();
        }
    }
}
