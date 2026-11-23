## 2026-11-22 / B · 규칙 바꾸기

### 1. 기존 문제

- 규칙이 `Resident.figureNextState()` 안의 식 `neighbors==3 || (amAlive && neighbors==2)` 한 줄에 고정돼 있었다. HighLife나 Seeds를 보려면 이 줄을 고치고 다시 빌드해야 했다 (OCP 위반).
- 규칙만 따로 검사할 방법이 없어서, 규칙이 맞는지 보려면 셀과 이웃 여덟 개를 만들어야 했다.

### 2. 변경 내용

`Resident`는 이제 이웃 수를 센 뒤 `Rule`에게 다음 상태를 묻는다. 현재 규칙은 `BoardModel`이 갖고, "Rule" 메뉴가 `BoardModel.setRule()`을 호출한다. 작업은 A의 `refactor/board-model`(PR #3) 위에서 했다.

#### Rule.java, LifeLikeRule.java, Rules.java (신규)

- **Rule** — `nextState(alive, liveNeighbors)`와 `notation()` 두 메서드를 가진 인터페이스다.
- **LifeLikeRule** — 탄생(B)과 생존(S) 이웃 수 집합을 비트마스크 두 개로 보관한다. 값이 같으면 같은 규칙이다 (`equals`). 변하는 상태가 없어 셀 4,096개가 객체 하나를 공유한다.
- **Rules** — `CONWAY`, `HIGHLIFE`, `SEEDS`, `DAY_AND_NIGHT` 상수와 `presets()`(이름 → 규칙, 메뉴 순서)를 제공한다. `parse("B36/S23")`는 표기를 규칙으로 바꾸며 대소문자, 공백, 숫자 순서, 중복 숫자를 구분하지 않는다. 잘못된 표기와 `B0`은 `IllegalArgumentException`이다.

[Rule.java](../../src/main/java/com/holub/life/Rule.java) · [LifeLikeRule.java](../../src/main/java/com/holub/life/LifeLikeRule.java) · [Rules.java](../../src/main/java/com/holub/life/Rules.java)

#### RuleHolder.java (신규), Resident.java (수정)

- [RuleHolder.java](../../src/main/java/com/holub/life/RuleHolder.java)는 보드 하나의 현재 규칙을 담는다.
- [Resident.java](../../src/main/java/com/holub/life/Resident.java)는 홀더 참조를 필드로 갖고, `create()`에서 같은 홀더를 복제본에 넘긴다. 고정돼 있던 식은 `rules.get().nextState(amAlive, neighbors)`로 바꿨다. 인자 없는 생성자는 Conway 규칙을 쓰므로 기존 `ResidentTest`는 수정하지 않았다.

#### BoardModel.java, Universe.java (수정)

- [BoardModel.java](../../src/main/java/com/holub/life/BoardModel.java)에 `rule()`과 `setRule(rule)`을 추가했다. 모델이 홀더를 만들어 원형 셀에 넣고 보드를 생성한다. 그래서 `createBoard()`와 `BoardModel(Neighborhood)` 생성자를 없애고, 화면이 같은 보드를 그릴 수 있게 `board()`를 뒀다.
- `setRule()`은 배치와 세대 번호를 그대로 두고, 세대 완료 알림도 내지 않는다. `step()`과 같은 잠금을 쓰므로 규칙은 세대 사이에서만 바뀐다.
- [Universe.java](../../src/main/java/com/holub/life/Universe.java)는 생성자의 두 줄만 바꿨다 (`model = new BoardModel(); boardToDraw = model.board();`).

#### RuleMenu.java (신규), Life.java (수정)

- [RuleMenu.java](../../src/main/java/com/holub/life/RuleMenu.java)는 `Rules.presets()`를 읽어 "Rule" 메뉴 항목을 만들고 `Custom...`에서 표기를 입력받는다. 입력창의 초기값은 현재 규칙이고, 잘못된 입력이면 오류 창을 띄우고 기존 규칙을 유지한다.
- [Life.java](../../src/main/java/com/holub/life/Life.java)에는 `RuleMenu.install(...)` 한 줄을 추가했다. D와 E가 `Universe`의 메뉴 등록 부분을 고칠 예정이라 메뉴를 `Universe`에 넣지 않았다.

### 3. 설계 결정

- **규칙을 바꿀 수 있게 만드는 방법** — `Resident`를 상속해 규칙마다 하위 클래스를 두면(Template Method) 실행 중에 규칙을 바꿀 때 셀 4,096개를 새로 만들고 배치를 옮겨야 한다. enum은 미리 정한 규칙만 표현해서 직접 입력을 받을 수 없다. 실행 중 교체와 직접 입력을 둘 다 만족하는 Strategy를 골랐다.
- **규칙마다 클래스를 둘지** — `ConwayRule`, `HighLifeRule`을 따로 두면 숫자만 다른 클래스가 늘고 직접 입력한 규칙은 여전히 표현하지 못한다. 그래서 숫자 집합 두 개를 가진 `LifeLikeRule` 하나로 표현했다. 구현 클래스가 하나뿐이지만 `Resident`가 인터페이스에만 의존하도록 `Rule`은 남겼다.
- **규칙을 셀에 전달하는 방법** — 원본은 이웃을 `figureNextState()`의 인자로 넘긴다. 규칙도 인자로 넘기면 셀에 필드가 생기지 않아 원본 방식과 일관되지만, `Cell` 인터페이스가 바뀌어 `Neighborhood`, `DUMMY`, `BoardModel.step()`, `ResidentTest`를 모두 고쳐야 한다. PR #3이 리뷰 중이라 A의 파일 수정을 줄이는 쪽을 택해, 셀이 공유 홀더를 참조하게 했다. 원본의 Prototype(`create()`)이 홀더를 전파하므로 `Neighborhood`는 고치지 않았다. static 전역은 보드마다 규칙을 다르게 둘 수 없고 테스트끼리 영향을 줘서 제외했다.
- **멈춰 있는 배치와 규칙 교체** — `Neighborhood`는 변화가 없는 블록의 계산을 건너뛴다. 그래서 규칙만 바꾸면 2×2 블록 같은 정물은 다시 계산되지 않는다. `setRule()`에서 `restore(snapshot())`을 호출해 A의 `setAlive()` 경로로 살아 있는 셀 주변을 계산 대상으로 표시했다. 같은 전제 때문에 이웃 없이 태어나는 `B0` 규칙은 받지 않는다.
- **용어** — `Rules.parse()`는 하위 클래스가 생성을 결정하는 GoF Factory Method가 아니라 정적 팩토리다.

### 4. 테스트

| 확인할 내용 | 예상 결과 | 실제 결과 |
| --- | --- | --- |
| Conway 규칙과 원본 식 비교 (생사 2 × 이웃 0~8) | 18가지 경우 모두 원본 식과 같다. | 통과 |
| HighLife, Seeds, Day & Night | 각 규칙의 B/S 집합대로 답한다. | 통과 |
| 표기 해석 | 대소문자, 공백, 숫자 순서, 중복 숫자와 무관하게 같은 규칙이 된다. 빈 B, 빈 S도 받는다. | 통과 |
| 잘못된 표기와 `B0` | 예외가 난다. | 통과 |
| 보드에서 HighLife | 이웃이 여섯인 빈 칸이 Conway에서는 비어 있고 HighLife에서는 태어난다. | 통과 |
| 보드에서 Seeds | 나란한 두 셀이 죽고 둘 모두에 닿는 네 칸이 태어난다. | 통과 |
| 멈춰 있던 2×2 블록에 Seeds 적용 | 블록이 죽고 주변 여덟 칸이 태어난다. 블록 안쪽과 블록 경계에 걸친 경우 모두. | 통과 |
| 규칙 교체의 부수 효과 | 배치와 세대 번호가 그대로이고 세대 완료 알림이 없다. | 통과 |
| 보드별 규칙 | 한 모델의 규칙을 바꿔도 다른 모델은 Conway다. | 통과 |

- Java 17에서 Gradle로 실행했다. 기존 테스트 9개(`ResidentTest` 4, `BoardModelTest` 5)는 수정 없이 통과했고, 새 테스트는 `RulesTest` 18개와 `BoardModelRuleTest` 10개다. 모두 창을 띄우지 않는다.
- `setRule()`에서 `restore(snapshot())`을 뺀 상태로 돌리면 멈춰 있던 블록 테스트 두 개가 실패하는 것을 확인했다.
- 실제 프레임을 띄워 "Rule" 메뉴에 다섯 항목이 등록되고, Seeds와 Conway 항목을 누르면 모델의 규칙이 바뀌는 것을 확인했다. `Custom...` 입력창은 손으로 조작해 보지 않았다.

### 5. 남은 일

- A와 `Snapshot`에 규칙을 넣을지 정한다. 지금은 `restore()`가 배치와 세대 번호만 되돌리고 규칙은 현재 것을 유지한다.
- `.life` 파일에는 규칙이 저장되지 않는다. 불러온 뒤에도 현재 규칙을 쓴다.
- 현재 규칙을 화면에 표시하지 않는다. `Custom...` 입력창의 초기값으로만 확인할 수 있다.
- 제출용 UML과 스크린샷을 붙인다.
