package com.holub.life.control;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.holub.ui.MenuSite;

/**
 * Go 메뉴. 원본 Clock.createMenus()에서 옮겨 왔다.
 * 원본은 메뉴 6개가 리스너 하나를 공유하고 메뉴 이름의 첫 글자로 동작을 골랐다.
 * 여기서는 메뉴 항목마다 자기 Command를 연결한다.
 */
public final class GoMenu {
    public static final String MENU = "Go";

    /** 메뉴 이름과 연결된 동작. */
    public record Entry(String name, Command command) {
        public Entry {
            Objects.requireNonNull(name);
            Objects.requireNonNull(command);
        }
    }

    private final List<Entry> entries;

    public GoMenu(GameController game) {
        List<Entry> list = new ArrayList<>();
        // 원본과 같은 이름, 같은 순서
        list.add(new Entry("Halt", new PauseCommand(game)));
        list.add(new Entry("Tick (Single Step)", new StepCommand(game)));
        for (Speed speed : Speed.values()) {
            list.add(new Entry(speed.menuName(), new RunCommand(game, speed)));
        }
        // 새로 추가
        list.add(new Entry("Run/Pause (Space)", new TogglePlayCommand(game)));
        entries = List.copyOf(list);
    }

    /** 화면 없이 메뉴 구성을 확인할 수 있게 공개한다. */
    public List<Entry> entries() {
        return entries;
    }

    /** MenuSite.establish() 이후에 호출한다. */
    public void install() {
        for (Entry entry : entries) {
            MenuSite.addLine(this, MENU, entry.name(), event -> entry.command().execute());
        }
    }
}
