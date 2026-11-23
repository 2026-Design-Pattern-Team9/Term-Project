## 2026-11-22 / E · 다른 파일 형식 지원 + 패턴 미리 만들어 배치

### 1. 기존 문제

- `Universe.doLoad()` / `doStore()`가 `.life` 하나만 알고 있었다. `Files.userSelected(".", ".life", ...)`로 확장자가 고정되어 있고, 새 형식을 넣으려면 `Universe`의 파일 선택·읽기·오류 처리를 다시 고쳐야 했다.
- 대표 패턴(글라이더 등)을 쓰려면 매번 클릭으로 셀을 하나씩 찍어야 했다.
- 읽기에 쓰는 `ObjectInputStream`에 클래스 제한이 없어서, 낯선 `.life` 파일이 들어와도 아무 클래스나 역직렬화될 수 있었다.

### 2. 변경 내용

변경 전에는 `Universe`가 `.life` 읽기·쓰기 흐름을 직접 가지고 있었다. 이제 `Universe`는 `PatternFileActions`에 맡기고, 형식마다 다른 부분은 `PatternFormat` 구현체가 맡는다.

#### com.holub.life.format (신규 패키지)

- **CellPattern** — 형식과 무관한 "살아 있는 셀 좌표 집합". `copy()`, `rotateClockwise()`, `flipHorizontal()`, `normalize()`, 보드에 놓기(`placeOn`, `placeCentered`, `placeAsIs`)를 가진다. 보드 밖으로 나가는 셀은 건너뛰고, 이미 살아 있는 셀은 끄지 않는다.
- **PatternFormat** — `read` / `write` / `extensions` / `preservesPosition`을 가진 인터페이스.
- **LifeFileFormat** — 원본 `.life`(직렬화한 Point 목록)를 `PatternFormat`에 맞추는 Adapter. `BoardModel.store()`가 만든 파일을 읽고, 이 형식이 쓴 파일을 `BoardModel.load()`가 읽는다. 읽을 때 `ObjectInputFilter`로 `Point`, `LinkedList`, `ArrayList`만 허용한다.
- **PlaintextFormat** (`.cells`), **RleFormat** (`.rle`) — 새 형식. 잘못된 문자, 너무 긴 반복, `!` 누락은 `IOException`으로 거부한다.
- **PatternFormats** — 파일 이름의 확장자(대소문자 무시)로 형식을 고르는 Factory.
- **PatternLibrary** — 대표 패턴 8개(Block, Blinker, Toad, Beacon, Glider, R-pentomino, Lightweight spaceship, Gosper glider gun)를 원형으로 보관하고 `create(name)`이 복제본을 돌려준다.
- **PatternFileActions** — Load/Store 파일 선택창과 Patterns 메뉴. 읽기에 성공한 뒤에만 보드를 지우므로 읽기 실패 시 현재 보드가 유지된다.

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
| 규칙 헤더 | RLE의 `rule =`은 읽고 무시 | Conway가 아니면 거부 | B(대철)의 규칙 교체가 정해진 뒤 연결한다. |

### 4. 테스트

`src/test/java/com/holub/life/format/PatternFormatsTest.java` (20개, UI 없이 실행. 보드보다 큰 패턴은 놓지 못한 셀 수를 돌려주고, Load 화면에서 경고를 띄운다)

- 형식 선택: 확장자 대소문자 무시, 알 수 없는 확장자 거부, 기본 확장자 붙이기.
- RLE: 표준 글라이더 텍스트 읽기, 모든 프리셋과 빈 줄·빈 패턴 왕복, 잘못된 파일 4종 거부.
- Plaintext: 주석·이름 처리, 왕복, 잘못된 문자 거부.
- 호환: 원본 `store()` 파일을 `LifeFileFormat`이 읽고, `LifeFileFormat`이 쓴 파일을 원본 `load()`가 읽는다. 허용하지 않은 클래스가 든 직렬화 파일은 거부한다.
- Prototype: `create()`가 독립 복제본을 돌려준다(회전 후에도 원형 유지), 회전 4번·뒤집기 2번은 원래 모양, Gosper gun은 36셀(36×9).
- 배치: 기존 셀을 끄지 않음, 보드 밖은 건너뜀, 가운데 배치, 글라이더가 4세대 뒤 (1, 1)칸 이동.

결과: `./gradlew test` 전체 통과(기존 `BoardModelTest`·`ResidentTest` 포함).

참고: Windows에서 임시 폴더 경로가 길면 Gradle이 `Unable to establish loopback connection`으로 실패한다. `TEMP`/`TMP`를 `C:\tmp9` 같은 짧은 경로로 지정하면 된다.

### 5. 관련 자료와 남은 일

- 커밋·PR 링크: 아직 없음 (로컬 브랜치 `feature/e-pattern-formats`, 커밋 전)
- 스크린샷·UML: 아직 없음
- 미완료
  - GUI에서 Load/Store 대화상자와 Patterns 메뉴를 직접 확인하지 못했다.
  - 편집 모드(D)가 생기면 실행 중에는 패턴 배치와 Load를 막을지 정해야 한다. 지금은 `beforeChange`로 시계를 정지한다.
  - Undo(C)와 연결할 때 패턴 배치도 이력에 남길지 정해야 한다.
  - 규칙(B)이 정해지면 RLE의 `rule` 헤더 처리.
