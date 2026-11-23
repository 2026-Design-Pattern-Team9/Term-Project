package com.holub.life.control;

import java.util.Optional;

/**
 * State 패턴의 State.
 * 같은 입력(칸 클릭, 실행, 정지, 한 세대 진행)이라도 현재 상태에 따라 다르게 처리한다.
 *
 * <pre>
 * 입력          | 편집       | 실행                     | 일시정지
 * 칸 클릭       | 토글       | 일시정지로 바꾼 뒤 토글  | 토글
 * 실행(속도)    | 실행으로   | 속도만 변경              | 실행으로
 * 정지          | 무시       | 일시정지로               | 무시
 * 한 세대 진행  | 진행       | 무시                     | 진행
 * 실행/정지 전환| 기본 속도로 실행 | 일시정지로         | 직전 속도로 실행
 * 되돌리기      | 되돌리기   | 일시정지로 바꾼 뒤 되돌리기 | 되돌리기
 * </pre>
 *
 * 상태 객체는 직접 타이머나 보드를 만지지 않고 GameController가 열어 둔 동작만 호출한다.
 */
public interface GameState {
    enum Mode { EDITING, RUNNING, PAUSED }

    Mode mode();

    /** 실행 중이면 현재 속도, 일시정지면 이어서 실행할 속도. 편집 상태에는 없다. */
    Optional<Speed> speed();

    /** 화면 표시용 이름. */
    String displayName();

    void cellClicked(GameController game, int row, int column);

    void run(GameController game, Speed speed);

    void pause(GameController game);

    void step(GameController game);

    void togglePlay(GameController game);

    void undo(GameController game);
}
