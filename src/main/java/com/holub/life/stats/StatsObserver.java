package com.holub.life.stats;

import com.holub.life.BoardModel;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * BoardModel의 세대 완료 알림을 받아 통계를 계산하고 StatsModel에 기록한다.
 * 규칙은 세대마다 Supplier에서 읽으므로, 규칙이 바뀌어도 이 클래스를 고칠 필요가 없다.
 */
public final class StatsObserver implements BoardModel.StepListener {
    private final StatsModel model;
    private final Supplier<SurvivalRule> rule;
    private final Supplier<Optional<RepeatInfo>> repeat;
    private final StatsCalculator calculator = new StatsCalculator();

    public StatsObserver(StatsModel model) {
        this(model, () -> SurvivalRule.CONWAY);
    }

    public StatsObserver(StatsModel model, Supplier<SurvivalRule> rule) {
        this(model, rule, Optional::empty);
    }

    /**
     * @param repeat 세대마다 읽는 반복 정보. 이력을 가진 쪽의 결과를 조립 지점에서 연결한다.
     *               이력 객체가 이 옵서버보다 먼저 BoardModel에 등록되어 있어야 최신 값을 읽는다.
     */
    public StatsObserver(StatsModel model, Supplier<SurvivalRule> rule,
                         Supplier<Optional<RepeatInfo>> repeat) {
        this.model = Objects.requireNonNull(model);
        this.rule = Objects.requireNonNull(rule);
        this.repeat = Objects.requireNonNull(repeat);
    }

    @Override
    public void stepCompleted(BoardModel.Snapshot before, BoardModel.Snapshot after) {
        model.record(calculator.compute(before, after, rule.get()), repeat.get().orElse(null));
    }
}
