# Holub Life 팀 프로젝트

Allen Holub의 Life Game을 기반으로 보드 모델과 화면의 책임을 분리하고, 규칙 변경·세대 이력·실행 제어·파일 형식·통계 기능을 확장한 설계패턴 9팀 프로젝트다.

## 주요 기능과 담당

| 파트 | 담당 | 구현 내용 | 적용한 패턴과 설계 원칙 |
| --- | --- | --- | --- |
| A | 인지환 | 보드 편집·세대 계산·스냅샷·세대 완료 알림을 `BoardModel`로 분리 | MVC 책임 분리, Observer |
| B | 임대철 | Conway·HighLife·Seeds·Day & Night 규칙 선택, B/S 표기 직접 입력 | Strategy, 기존 Prototype 활용, 정적 팩토리 |
| C | 김승완 | 최근 세대 이력 보관, Undo, 같은 셀 배치의 반복 주기 감지 | Memento, Observer |
| D | 황대경 | 편집·실행·일시정지 상태 관리, 속도 변경, 메뉴·단축키 실행 제어 | State, Command, Observer |
| E | 최서영 | `.life`·`.cells`·`.rle` 불러오기·저장, 대표 패턴 배치 | Adapter, Factory, Prototype |
| F | 최은송 | 세대·생존 수·증감·밀도·반복 주기와 세대별·누적 통계 표시 | Observer, Adapter |

보드는 64×64이며 기본 규칙은 Conway다. 원본의 셀 편집, 한 세대 진행, 속도별 자동 실행, 정지, 초기화, `.life` 파일 읽기·쓰기를 지원한다.

## 개발 환경

- JDK 17 이상. `java`가 설치되어 있고 `JAVA_HOME`이 사용할 JDK를 가리켜야 한다.
- Gradle Wrapper 9.8.2와 JUnit 5.11.4를 사용한다. Gradle은 별도로 설치하지 않아도 된다.
- 처음 빌드할 때 Gradle과 Maven Central 의존성을 내려받을 인터넷 연결이 필요하다.
- 앱 실행에는 Swing 창을 표시할 수 있는 데스크톱 환경이 필요하다.
- 아래 명령은 `gradlew`, `gradlew.bat`, `build.gradle`이 있는 저장소 루트에서 실행한다.

## 실행

Windows PowerShell에서 실행한다.

```powershell
.\gradlew.bat run
```

macOS·Linux에서 실행한다.

```bash
./gradlew run
```

### 주요 조작

| 입력 | 동작 |
| --- | --- |
| 보드의 칸 클릭 | 셀의 생존 상태 전환. 자동 실행 중이면 먼저 일시정지 |
| `Go` → `Tick (Single Step)` 또는 오른쪽 화살표 | 한 세대 진행. 자동 실행 중에는 무시 |
| `Go`의 속도 메뉴 | Agonizing·Slow·Medium·Fast 속도로 자동 실행 |
| `Go` → `Halt` | 자동 실행 일시정지 |
| 스페이스 | 실행·일시정지 전환 |
| `Go` → `Undo` 또는 Windows/Linux `Ctrl+Z`, macOS `Cmd+Z` | 저장된 이력에서 이전 세대 복원 |
| `Grid` → `Clear` | 실행을 멈추고 보드와 세대 이력 초기화 |
| `Grid` → `Load` / `Store` | 패턴 파일 불러오기·저장 |
| `Rule` | 사전 정의 규칙 선택 또는 `Custom...`에서 `B36/S23` 같은 B/S 표기 입력 |
| `Patterns` | Block, Blinker, Glider 등 대표 패턴을 보드 중앙에 배치 |

## 테스트

Windows PowerShell에서 실행한다.

```powershell
.\gradlew.bat test
```

macOS·Linux에서 실행한다.

```bash
./gradlew test
```

기존 Conway 규칙과 보드 동작, 규칙 교체, 이력·Undo·반복 감지, 실행 상태와 메뉴, 파일 형식 호환성, 통계 계산 및 연결을 JUnit으로 검사한다. 테스트는 Swing 창을 띄우지 않고 모델·제어·변환 로직을 검증한다.

- HTML 결과는 `build/reports/tests/test/index.html`에서 확인한다.
- 테스트별 XML 결과는 `build/test-results/test/`에 생성된다.
- 테스트를 다시 실행하려면 `test` 뒤에 `--rerun-tasks`를 붙인다.

## 실행 파일 빌드

Windows PowerShell에서 실행한다.

```powershell
.\gradlew.bat clean build
java -jar build/libs/holub-life-1.0.0.jar
```

macOS·Linux에서 실행한다.

```bash
./gradlew clean build
java -jar build/libs/holub-life-1.0.0.jar
```

- 실행 진입점은 `com.holub.life.Life`다.
- 컴파일된 클래스는 `build/classes/java/main/`, 실행 JAR는 `build/libs/holub-life-1.0.0.jar`에 생성된다.
- 리소스는 Gradle 빌드 시 JAR에 포함된다.

## 코드와 문서 위치

| 경로 | 내용 |
| --- | --- |
| `src/main/java/com/holub/life/` | 보드 모델, 셀·이웃 구조, 규칙, Swing 화면 |
| `src/main/java/com/holub/life/history/` | 세대 이력과 반복 주기 감지 |
| `src/main/java/com/holub/life/control/` | 실행 상태, 명령, 메뉴·단축키 |
| `src/main/java/com/holub/life/format/` | 파일 형식 읽기·쓰기와 대표 패턴 |
| `src/main/java/com/holub/life/stats/` | 통계 계산·보관·화면 연결 |
| `src/main/resources/` | 실행에 필요한 리소스 |
| `src/test/java/` | JUnit 테스트 |
| `docs/worklogs/` | 파트별 변경 내용, 설계 결정, 테스트 기록 |
| `docs/original/` | 원본 프로젝트 안내 |

파트별 구현과 설계 결정은 다음 문서를 참고한다.

- [A · 보드 모델과 화면 분리](docs/worklogs/팀원A.md)
- [B · 규칙 바꾸기](docs/worklogs/팀원B.md)
- [C · 세대 이력과 반복 주기 감지](docs/worklogs/팀원C.md)
- [D · 실행 상태 관리와 실행 조작](docs/worklogs/팀원D.md)
- [E · 파일 형식 지원과 대표 패턴 배치](docs/worklogs/팀원E.md)
- [F · 세대 통계와 상태 표시](docs/worklogs/팀원F.md)

## 사용 시 참고

- Undo와 반복 감지는 메모리에 보관한 이력 범위에서 동작한다. 반복 감지는 셀 위치까지 같은 배치를 비교한다.
- 통계는 진행이 완료된 세대를 기준으로 계산한다. 셀 편집 직후 생존 수 등은 다음 세대가 진행될 때 갱신된다.
- `B0` 규칙은 지원하지 않는다.
- `.life`는 보드 좌표를 보존한다. `.cells`·`.rle`은 패턴 모양을 저장하고, 불러올 때 보드 중앙에 배치한다.
- RLE의 규칙 헤더가 지원되는 규칙이면 불러올 때 적용한다. 지원하지 않는 표기는 경고하고 현재 규칙을 유지한다.

## 프로젝트 주소와 원본

- 팀 저장소는 [2026-Design-Pattern-Team9/Term-Project](https://github.com/2026-Design-Pattern-Team9/Term-Project)다.
- 기반 코드는 [Allen Holub의 Life Game](https://holub.com/software/life/index.html)이다.
