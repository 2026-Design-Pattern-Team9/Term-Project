package com.holub.life.control;

/**
 * Command 패턴.
 * 메뉴, 단축키처럼 입력 경로가 달라도 같은 동작 객체를 공유한다.
 * 실행 조작은 되돌릴 필요가 없어서 execute()만 둔다.
 */
@FunctionalInterface
public interface Command {
    void execute();
}
