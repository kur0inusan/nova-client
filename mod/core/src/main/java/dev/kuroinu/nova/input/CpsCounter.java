package dev.kuroinu.nova.input;

import dev.kuroinu.nova.event.EventBus;
import dev.kuroinu.nova.event.Events;

/** Counts in-game mouse clicks over a sliding one-second window. */
public final class CpsCounter {
    private static final int CAPACITY = 128;

    private final long[] left = new long[CAPACITY];
    private final long[] right = new long[CAPACITY];
    private int leftHead;
    private int rightHead;

    public CpsCounter(EventBus bus) {
        bus.subscribe(Events.MouseButton.class, new EventBus.Listener<Events.MouseButton>() {
            @Override
            public void handle(Events.MouseButton e) {
                if (e.pressed && !e.inScreen) {
                    record(e.button, System.currentTimeMillis());
                }
            }
        });
    }

    public synchronized void record(int button, long timeMs) {
        if (button == 0) {
            left[leftHead] = timeMs;
            leftHead = (leftHead + 1) % CAPACITY;
        } else if (button == 1) {
            right[rightHead] = timeMs;
            rightHead = (rightHead + 1) % CAPACITY;
        }
    }

    public int getLeft() {
        return count(left, System.currentTimeMillis());
    }

    public int getRight() {
        return count(right, System.currentTimeMillis());
    }

    synchronized int count(long[] clicks, long now) {
        int n = 0;
        for (long t : clicks) {
            if (t != 0 && now - t < 1000) n++;
        }
        return n;
    }

    int countLeftAt(long now) {
        return count(left, now);
    }
}
