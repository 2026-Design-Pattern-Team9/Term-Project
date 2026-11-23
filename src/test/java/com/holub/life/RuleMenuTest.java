package com.holub.life;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.holub.life.control.GameController;
import com.holub.life.control.Ticker;
import com.holub.life.history.GenerationHistory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Rule 메뉴에서 규칙을 바꿨을 때 세대 이력이 새로 시작되는지 확인한다.
 * 이전 규칙으로 계산한 세대가 이력에 남아 있으면, 되돌렸을 때 판은 예전 규칙 시절로 돌아가는데
 * 규칙은 그대로여서 이후 진행과 반복 주기 감지가 섞인다.
 * 메뉴 등록(MenuSite) 없이 규칙 적용 부분만 검사한다.
 */
class RuleMenuTest {
    private BoardModel model;
    private GenerationHistory history;
    private GameController game;

    @BeforeEach
    void setUp() {
        model = new BoardModel();
        model.setAlive(8, 7, true);
        model.setAlive(8, 8, true);
        model.setAlive(8, 9, true);
        history = new GenerationHistory(20, model.snapshot());
        model.addStepListener(history);
        game = new GameController(model, new IdleTicker(), history);
        model.step();
        model.step();
    }

    @Test
    void changingTheRuleStartsANewHistory() {
        assertTrue(history.canUndo());

        RuleMenu.applyRule(model, Rules.HIGHLIFE, game::ruleChanged);

        assertEquals(Rules.HIGHLIFE, model.rule());
        assertFalse(history.canUndo(), "이전 규칙의 세대는 되돌릴 수 없음");
        assertEquals(2, model.snapshot().generation(), "판과 세대 번호는 그대로");
    }

    @Test
    void choosingTheSameRuleAgainKeepsTheHistory() {
        RuleMenu.applyRule(model, Rules.CONWAY, game::ruleChanged);

        assertTrue(history.canUndo());
    }

    @Test
    void callbackRunsOnlyWhenTheRuleActuallyChanges() {
        int[] calls = new int[1];

        RuleMenu.applyRule(model, Rules.SEEDS, () -> calls[0]++);
        RuleMenu.applyRule(model, Rules.SEEDS, () -> calls[0]++);

        assertEquals(1, calls[0]);
    }

    private static final class IdleTicker implements Ticker {
        @Override
        public void startTicking(int millisecondsBetweenTicks) {
        }

        @Override
        public void stop() {
        }

        @Override
        public void tick() {
        }
    }
}
