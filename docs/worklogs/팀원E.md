## 2026-11-22 / E · 다른 파일 형식 지원 + 패턴 미리 만들어 배치

### 1. 기존 문제

- `Universe.doLoad()` / `doStore()`가 `.life` 하나만 알고 있었다. `Files.userSelected(".", ".life", ...)`로 확장자가 고정되어 있고, 새 형식을 넣으려면 `Universe`의 파일 선택·읽기·오류 처리를 다시 고쳐야 했다.
- 대표 패턴(글라이더 등)을 쓰려면 매번 클릭으로 셀을 하나씩 찍어야 했다.
- 읽기에 쓰는 `ObjectInputStream`에 클래스 제한이 없어서, 낯선 `.life` 파일이 들어와도 아무 클래스나 역직렬화될 수 있었다.

### 2. 변경 내용

변경 전에는 `Universe`가 `.life` 읽기·쓰기 흐름을 직접 가지고 있었다. 이제 `Universe`는 `PatternFileActions`에 맡기고, 형식마다 다른 부분은 `PatternFormat` 구현체가 맡는다.

#### com.holub.life.format (신규 패키지)

- **CellPattern** — 형식과 무관한 "살아 있는 셀 좌표 집합". `copy()`, `rotateClockwise()`, `flipHorizontal()`, `normalize()`, 보드에 놓기(`placeOn`, `placeCentered`, `placeAsIs`)를 가진다. 보드 밖으로 나가는 셀은 건너뛰고, 이미 살아 있는 셀은 끄지 않는다. 파일 헤더의 규칙 표기를 문자열로 담아 두는 `rule()`도 가진다(어떤 규칙인지 해석하는 일은 하지 않는다).
- **PatternFormat** — `read` / `write` / `extensions` / `preservesPosition`을 가진 인터페이스.
- **LifeFileFormat** — 원본 `.life`(직렬화한 Point 목록)를 `PatternFormat`에 맞추는 Adapter. `BoardModel.store()`가 만든 파일을 읽고, 이 형식이 쓴 파일을 `BoardModel.load()`가 읽는다. 읽을 때 `ObjectInputFilter`로 `Point`, `LinkedList`, `ArrayList`만 허용한다.
- **PlaintextFormat** (`.cells`), **RleFormat** (`.rle`) — 새 형식. 잘못된 문자, 너무 긴 반복, `!` 누락은 `IOException`으로 거부한다. RLE는 헤더의 `rule =` 값을 `CellPattern.rule()`에 담고, 저장할 때는 패턴에 담긴 규칙을 적는다(없으면 `B3/S23`).
- **PatternFormats** — 파일 이름의 확장자(대소문자 무시)로 형식을 고르는 Factory.
- **PatternLibrary** — 대표 패턴 8개(Block, Blinker, Toad, Beacon, Glider, R-pentomino, Lightweight spaceship, Gosper glider gun)를 원형으로 보관하고 `create(name)`이 복제본을 돌려준다.
- **PatternFileActions** — Load/Store 파일 선택창과 Patterns 메뉴. 읽기에 성공한 뒤에만 보드를 지우므로 읽기 실패 시 현재 보드가 유지된다.
  - 불러올 때 파일의 규칙이 현재 규칙과 다르면 B의 `Rules`로 해석해서 `model.setRule()`로 조용히 바꾼다. `Life`는 Conway의 다른 이름으로 본다. 해석할 수 없는 규칙(`23/3`, `B0` 등)이면 현재 규칙을 유지하고 경고창을 띄운다.
  - 저장할 때는 `model.rule().notation()`을 헤더에 적는다. 이전에는 규칙과 상관없이 `B3/S23`을 적었다.
  - 보드보다 큰 패턴은 들어가는 부분만 놓고 놓지 못한 셀 수를 경고한다.

#### Universe.java (수정)

- `doLoad()` / `doStore()`와 `Files` import를 삭제하고 `patternActions.load()` / `store()`를 호출한다.
- 필드 1개, 생성 1줄, 메뉴 등록 1줄(`installPresetMenu`)을 추가했다. 보드 접근은 A가 만든 `model()`·`BoardModel` API(`setAlive`, `snapshot`, `clear`)만 쓰므로 `Universe`에 새 접근자는 필요 없었다.

### 3. 설계 결정

| 결정 | 선택 | 검토한 대안 | 이유 / 남은 단점 |
| --- | --- | --- | --- |
| 형식별 읽기·쓰기 | **형식마다 `PatternFormat` 구현 + `PatternFormats`(Factory)가 확장자로 선택** | `Universe`에서 `switch (확장자)` | 형식을 추가해도 `Universe`와 기존 형식을 건드리지 않는다. 단점: 형식이 늘면 목록(`ALL`)에 직접 등록해야 한다. |
| 원본 `.life` | **Adapter** (`LifeFileFormat`) | `BoardModel.load/store`를 그대로 호출 | `BoardModel.load/store`는 보드 전체를 대상으로 하는 API라 `CellPattern`과 맞지 않는다. 같은 바이트 형식을 `PatternFormat` 모양으로 맞춰서 세 형식이 같은 인터페이스를 쓴다. 단점: 직렬화 형식 지식이 `Neighborhood`와 `LifeFileFormat` 두 곳에 있다(호환 테스트로 보호). |
| 프리셋 | **Prototype** (`PatternLibrary` 원형 → `copy()`) | 정적 좌표 배열을 매번 새로 만들기 | 회전·뒤집기를 하는 `CellPattern`이 변경 가능해서, 원형을 보호하려면 복제가 필요하다. 회전·뒤집기를 쓰지 않으면 불변 객체를 공유해도 충분하므로, 이 기능이 없다면 Prototype은 과한 선택이다. |
| 저장 시 좌표 | `.life`만 보드 좌표 유지, `.cells`/`.rle`은 모양만 저장하고 불러올 때 가운데에 배치 | 모든 형식에 좌표 저장 | 일반 Life 도구와 파일이 호환된다. 단점: `.rle`로 저장 후 불러오면 위치가 달라진다. |
| 규칙 헤더 | **불러올 때 파일의 규칙을 조용히 적용**, 저장할 때 현재 규칙을 기록 | 헤더를 읽고 무시 / 다를 때 사용자에게 물어봄 | 파일이 의도한 규칙으로 돌아가야 패턴이 제대로 움직인다(예: Seeds 상태에서 Conway용 글라이더를 불러오면 날아가지 않는다). 물어보는 방식은 대화상자가 늘어서 택하지 않았다. 단점: Rule 메뉴에 현재 규칙 표시가 없어서 규칙이 바뀐 것을 사용자가 알기 어렵다. 지원하지 않는 규칙은 경고만 하고 현재 규칙을 유지한다. |

### 4. 테스트

`src/test/java/com/holub/life/format/PatternFormatsTest.java` (25개, UI 없이 실행)

- 형식 선택: 확장자 대소문자 무시, 알 수 없는 확장자 거부, 기본 확장자 붙이기.
- RLE: 표준 글라이더 텍스트 읽기, 모든 프리셋과 빈 줄·빈 패턴 왕복, 잘못된 파일 4종 거부.
- Plaintext: 주석·이름 처리, 왕복, 잘못된 문자 거부.
- 호환: 원본 `store()` 파일을 `LifeFileFormat`이 읽고, `LifeFileFormat`이 쓴 파일을 원본 `load()`가 읽는다. 허용하지 않은 클래스가 든 직렬화 파일은 거부한다.
- Prototype: `create()`가 독립 복제본을 돌려준다(회전 후에도 원형 유지), 회전 4번·뒤집기 2번은 원래 모양, Gosper gun은 36셀(36×9).
- 배치: 기존 셀을 끄지 않음, 보드 밖은 건너뜀, 가운데 배치, 글라이더가 4세대 뒤 (1, 1)칸 이동. 폭 100인 패턴은 보드 폭(64)만 놓이고 그 수를 돌려준다.
- 규칙 헤더: RLE가 `rule =` 값을 읽고(공백 유무 모두, 없으면 null), 저장할 때 패턴의 규칙을 적고 없으면 `B3/S23`을 적는다. 불러올 때 다른 규칙이면 조용히 바꾸고, `Life`는 Conway로 보고, 규칙이 없으면 바꾸지 않고, 지원하지 않는 규칙(`23/3`, `B0/S`, `Generations`, `B3/S23V`)은 현재 규칙을 유지하며 경고 문구를 돌려준다.

결과: `./gradlew test` 전체 62개 통과, 실패 0(B의 규칙 테스트와 기존 `BoardModelTest`·`ResidentTest` 포함).

GUI 확인: 앱을 띄워 메뉴를 코드로 눌러 확인했다(마우스로 직접 누른 것은 아님).
- LWSS, Pentadecathlon, Diehard RLE를 불러와 Tick으로 진행: LWSS는 이동, Pentadecathlon은 15세대 주기, Diehard는 130세대에 소멸.
- 폭 100인 RLE를 불러오면 "72 of 200 live cells were outside..." 경고가 뜨고, 잘못된 파일은 오류창이 뜨며 보드가 유지된다.
- Seeds를 고른 상태에서 `rule = B3/S23` 파일을 불러오면 규칙이 `B3/S23`으로 바뀌고, HighLife로 저장한 파일의 헤더는 `rule = B36/S23`이며, 그 파일을 Conway 상태에서 불러오면 HighLife로 돌아간다. `rule = 23/3` 파일은 경고창이 뜨고 규칙이 유지된다.

참고: Windows에서 임시 폴더 경로가 길면 Gradle이 `Unable to establish loopback connection`으로 실패한다. `TEMP`/`TMP`를 `C:\tmp9` 같은 짧은 경로로 지정하면 된다.

### 5. 관련 자료와 남은 일

- 이슈: [#8](https://github.com/2026-Design-Pattern-Team9/Term-Project/issues/8)
- 브랜치: `feature/e-pattern-formats` (B의 PR #7이 머지된 main 위로 리베이스함)
- PR 링크: 아직 없음
- 스크린샷·UML: 아직 없음
- 미완료
  - 사람이 마우스로 직접 눌러 보는 확인은 하지 못했다. 위 GUI 확인은 메뉴를 코드로 눌러서 했다.
  - Rule 메뉴에 현재 규칙 표시가 없어서, 불러오며 규칙이 바뀐 것을 사용자가 알기 어렵다. 메뉴 선택 표시나 제목 표시줄 표시를 B·D와 정해야 한다.
  - 편집 모드(D)가 생기면 실행 중에는 패턴 배치와 Load를 막을지 정해야 한다. 지금은 `beforeChange`로 시계를 정지한다. D의 `controller.pause()`와 `boardReplaced()`는 `beforeChange`/`afterChange`에 연결하면 된다.
  - Undo(C)와 연결할 때 불러오기와 패턴 배치를 이력에 남길지, 새로 시작할지 정해야 한다.
  - 담당 범위(RLE + Adapter)보다 `.cells` 형식과 프리셋·Patterns 메뉴가 더 들어 있다. 팀에서 빼기로 하면 `PlaintextFormat`, `PatternLibrary`, `installPresetMenu`를 지운다.
