package com.holub.life.stats;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 최근 세대의 통계와 누적 통계를 보관하고 바뀔 때마다 구독자에게 알린다.
 * 알림에는 그 시점의 값을 묶은 불변 State를 넘기므로, 구독자가 읽는 도중 값이 바뀌지 않는다.
 *
 * <p>최근 세대 {@value #MAX_ENTRIES}개의 기록을 세대별로 남겨서, 되돌리기(Undo) 때 그 세대까지의
 * 값으로 되감을 수 있다. 오래된 기록은 버리지만 그 몫은 누적에 남는다.
 *
 * <p>반복 정보는 세대별로 저장하지 않고 마지막으로 받은 값 하나만 보여 준다. 값의 출처는 이력을
 * 가진 쪽이고, 여기서 따로 저장하면 이력이 새로 시작될 때 오래된 값이 남기 때문이다.
 */
public final class StatsModel {
    /** 되감을 수 있는 최대 세대 수. 이력 보관 개수(20)보다 넉넉하게 잡았다. */
    static final int MAX_ENTRIES = 100;

    private final List<StatsListener> listeners = new CopyOnWriteArrayList<>();

    private final List<Entry> entries = new ArrayList<>();
    private Totals base = Totals.EMPTY; // entries 앞에서 버려진 세대까지의 누적
    private RepeatInfo repeat;           // 마지막으로 받은 반복 정보. 없으면 null

    /** 세대 하나의 결과를 기록하고 누적에 더한 뒤 알린다. */
    public void record(GenerationStats stats) {
        record(stats, null);
    }

    /** 세대 결과와 함께 그 시점에 감지된 반복 정보(없으면 null)를 기록한다. */
    public void record(GenerationStats stats, RepeatInfo repeat) {
        Objects.requireNonNull(stats);
        State state;
        synchronized (this) {
            entries.add(new Entry(stats, totals().plus(stats)));
            this.repeat = repeat;
            if (entries.size() > MAX_ENTRIES) {
                base = entries.remove(0).totalsAfter();
            }
            state = state();
        }
        notifyListeners(state);
    }

    /** 최근 값과 누적을 모두 지운다. Clear, Load, 규칙 변경, 패턴 배치 뒤에 호출한다. */
    public void reset() {
        State state;
        synchronized (this) {
            entries.clear();
            base = Totals.EMPTY;
            repeat = null;
            state = state();
        }
        notifyListeners(state);
    }

    /**
     * 판이 세대 진행 말고 다른 이유로 바뀐 뒤(되돌리기, Clear, Load, 칸 편집) 현재 세대에 맞춘다.
     * 현재 세대보다 나중에 기록된 통계를 버리고, 반복 정보는 받은 값(없으면 null)으로 바꾼다.
     *
     * <p>0세대는 아직 아무 세대도 진행하지 않은 상태이므로 오래된 누적까지 모두 지운다.
     * 그 밖의 세대에서는 되돌리기로 도달할 수 있는 범위(최근 {@value #MAX_ENTRIES}세대 안)만
     * 정확하다. 보관 범위보다 더 오래된 세대는 이력(20개)이 되돌릴 수 없으므로 다루지 않는다.
     */
    public void rewindTo(long generation, RepeatInfo repeat) {
        State state;
        synchronized (this) {
            if (generation <= 0) {
                entries.clear();
                base = Totals.EMPTY;
            } else {
                entries.removeIf(e -> e.stats().generation() > generation);
            }
            this.repeat = repeat;
            state = state();
        }
        notifyListeners(state);
    }

    public synchronized State state() {
        if (entries.isEmpty()) {
            return new State(null, base, repeat);
        }
        Entry last = entries.get(entries.size() - 1);
        return new State(last.stats(), last.totalsAfter(), repeat);
    }

    private Totals totals() {
        return entries.isEmpty() ? base : entries.get(entries.size() - 1).totalsAfter();
    }

    public void addListener(StatsListener listener) {
        listeners.add(Objects.requireNonNull(listener));
    }

    public void removeListener(StatsListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners(State state) {
        for (StatsListener listener : listeners) {
            listener.statsChanged(state);
        }
    }

    private record Entry(GenerationStats stats, Totals totalsAfter) {
    }

    /** 알림 시점의 값. latest와 repeat는 없으면 null이다. */
    public record State(GenerationStats latest, Totals totals, RepeatInfo repeat) {
    }

    /** 기록된 모든 세대를 합한 값. */
    public record Totals(int generations, long survived, long births,
                         long underpopulation, long overpopulation, long otherDeaths) {
        static final Totals EMPTY = new Totals(0, 0, 0, 0, 0, 0);

        public long deaths() {
            return underpopulation + overpopulation + otherDeaths;
        }

        Totals plus(GenerationStats s) {
            return new Totals(generations + 1, survived + s.survived(), births + s.births(),
                    underpopulation + s.underpopulation(), overpopulation + s.overpopulation(),
                    otherDeaths + s.otherDeaths());
        }
    }
}
