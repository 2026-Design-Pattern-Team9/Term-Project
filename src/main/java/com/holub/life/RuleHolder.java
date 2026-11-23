package com.holub.life;

import java.util.Objects;

/**
 * 한 보드의 셀들이 함께 보는 현재 규칙.
 * 셀은 복제될 때 같은 홀더를 넘겨받으므로, 여기서 규칙을 바꾸면 보드 전체에 적용된다.
 * 보드에 연결된 홀더는 BoardModel의 잠금 안에서만 읽고 쓴다.
 */
final class RuleHolder {
    private Rule rule;

    RuleHolder(Rule rule) {
        set(rule);
    }

    Rule get() {
        return rule;
    }

    void set(Rule rule) {
        this.rule = Objects.requireNonNull(rule);
    }
}
