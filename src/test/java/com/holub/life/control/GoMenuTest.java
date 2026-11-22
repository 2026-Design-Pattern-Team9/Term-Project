package com.holub.life.control;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.holub.life.BoardModel;
import com.holub.life.control.GameState.Mode;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Clock에서 옮겨 온 Go 메뉴가 원본과 같은 이름, 같은 순서, 같은 동작을 갖는지 확인한다.
 * 메뉴 구성만 검사하므로 창(MenuSite)이 필요 없다.
 */
class GoMenuTest {
    private final BoardModel model = new BoardModel();
    private final RecordingTicker ticker = new RecordingTicker();
    private final GameController game = new GameController(model, ticker);
    private final GoMenu menu = new GoMenu(game);

    @Test
    void keepsTheOriginalMenuNamesInOrderAndAddsRunPause() {
        List<String> names = menu.entries().stream().map(GoMenu.Entry::name).toList();

        assertEquals(List.of("Halt", "Tick (Single Step)", "Agonizing", "Slow", "Medium", "Fast",
                "Run/Pause (Space)"), names);
    }

    /** 원본 Clock은 메뉴 이름 첫 글자로 A=500, S=150, M=70, F=30 ms를 골랐다. */
    @Test
    void speedMenusUseTheSameIntervalsAsTheOriginalClock() {
        for (String name : List.of("Agonizing", "Slow", "Medium", "Fast")) {
            command(name).execute();
        }
        assertEquals(List.of(500, 150, 70, 30), ticker.started);
    }

    @Test
    void haltAndTickMenusGoThroughTheCurrentState() {
        command("Medium").execute();
        command("Tick (Single Step)").execute();
        assertEquals(0, ticker.ticks, "실행 중 Tick은 무시");

        command("Halt").execute();
        assertEquals(Mode.PAUSED, game.state().mode());

        command("Tick (Single Step)").execute();
        assertEquals(1, ticker.ticks);
    }

    /** 첫 글자 분기에서는 'S'로 시작하는 새 메뉴가 Slow로 처리됐다. 이제 메뉴마다 동작이 따로 있다. */
    @Test
    void eachEntryHasItsOwnCommandInsteadOfSharingOneListener() {
        long distinct = menu.entries().stream().map(GoMenu.Entry::command).distinct().count();
        assertEquals(menu.entries().size(), distinct);
    }

    private Command command(String name) {
        return menu.entries().stream()
                .filter(entry -> entry.name().equals(name))
                .findFirst()
                .orElseThrow()
                .command();
    }

    private static final class RecordingTicker implements Ticker {
        final List<Integer> started = new ArrayList<>();
        int ticks;

        @Override
        public void startTicking(int millisecondsBetweenTicks) {
            started.add(millisecondsBetweenTicks);
        }

        @Override
        public void stop() {
        }

        @Override
        public void tick() {
            ticks++;
        }
    }
}
