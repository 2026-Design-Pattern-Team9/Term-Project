# Holub Life 팀 프로젝트

Holub Life를 기반으로 설계 패턴을 적용하고 기능을 확장하는 프로젝트다.

## 실행

```bash
./gradlew --gradle-user-home /private/tmp/holublife-team9-gradle run --no-daemon
```

## 테스트

```bash
./gradlew --gradle-user-home /private/tmp/holublife-team9-gradle test --no-daemon
```

현재 테스트에는 기존 Conway 규칙 회귀 테스트와 `BoardModel` 테스트, C 담당 세대 이력·주기 감지 테스트가 포함되어 있다.

## C 담당 기능 1차

`com.holub.life.history` 패키지는 `BoardModel.Snapshot`을 이용해 다음 기능을 제공한다.

- 최근 N개 세대의 보드 상태 보관
- 이전 세대 복원과 Undo 뒤 새 이력 분기
- 전체 칸 배치 비교를 통한 반복 주기 감지

상세한 기존 문제, 설계 결정, 테스트 결과는 [C 작업 기록](docs/worklogs/팀원C.md)을 참고한다.
