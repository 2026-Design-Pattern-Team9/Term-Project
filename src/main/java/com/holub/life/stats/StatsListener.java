package com.holub.life.stats;

/** StatsModel의 값이 바뀌었을 때 알림을 받는다. 알림은 모델을 갱신한 스레드에서 온다. */
@FunctionalInterface
public interface StatsListener {
    void statsChanged(StatsModel.State state);
}
