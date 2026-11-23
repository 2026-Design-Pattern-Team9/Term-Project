package com.holub.life.stats;

import com.holub.life.BoardModel;
import com.holub.life.Rule;
import com.holub.life.control.GameController;
import com.holub.life.history.GenerationHistory;

import java.util.Objects;
import java.util.Optional;

/**
 * 통계를 다른 담당자의 객체(보드, 이력, 컨트롤러)에 연결한다.
 * 연결 규칙을 Life의 생성자에 흩어 두지 않고 한곳에 모아서 창 없이 테스트할 수 있게 했다.
 * 이 클래스만 다른 패키지를 알고, 나머지 stats 클래스는 서로만 안다.
 *
 * <p>연결하는 것:
 * <ul>
 *   <li>세대가 끝날 때마다 통계를 계산해 기록한다. 규칙은 보드의 현재 규칙, 반복 정보는 이력에서 읽는다.</li>
 *   <li>세대 진행 말고 다른 이유로 판이 바뀌면(되돌리기, Clear, Load, 칸 편집) 현재 세대에 맞춘다.
 *       이유는 알림에 담겨 있지 않지만, 이때 하는 일은 "현재 세대로 되감기" 하나이므로 구분할 필요가 없다.</li>
 *   <li>규칙이 바뀌면 이전 규칙의 누적과 섞이지 않도록 초기화한다. 세대 번호가 안 바뀌어서 위 경로로는
 *       잡히지 않으므로 {@link #ruleChanged()}를 따로 부른다.</li>
 * </ul>
 */
public final class StatsConnector {
    private final StatsModel stats = new StatsModel();
    private final BoardModel model;
    private final GenerationHistory history;

    private StatsConnector(BoardModel model, GenerationHistory history) {
        this.model = model;
        this.history = history;
    }

    /**
     * 통계를 연결한다. 이력({@code history})을 BoardModel에 먼저 등록한 뒤에 불러야
     * 세대 완료 때 {@code history.period()}가 방금 끝난 세대를 반영한다.
     */
    public static StatsConnector connect(BoardModel model, GenerationHistory history,
                                         GameController controller) {
        Objects.requireNonNull(model);
        Objects.requireNonNull(history);
        Objects.requireNonNull(controller);
        StatsConnector connector = new StatsConnector(model, history);
        model.addStepListener(new StatsObserver(connector.stats, connector::currentRule,
                connector::currentRepeat));
        controller.addBoardListener(connector::boardChanged);
        return connector;
    }

    public StatsModel stats() {
        return stats;
    }

    /** 규칙 변경 뒤 호출한다. */
    public void ruleChanged() {
        stats.reset();
    }

    private void boardChanged() {
        stats.rewindTo(model.snapshot().generation(), currentRepeat().orElse(null));
    }

    private SurvivalRule currentRule() {
        Rule rule = model.rule();
        return neighbors -> rule.nextState(true, neighbors);
    }

    private Optional<RepeatInfo> currentRepeat() {
        return history.period().map(p -> new RepeatInfo(p.period()));
    }
}
