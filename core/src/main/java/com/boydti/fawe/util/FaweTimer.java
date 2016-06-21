package com.boydti.fawe.util;

public class FaweTimer implements Runnable {

    private long tick = 0;

    private final double[] history = new double[] {20d,20d,20d,20d,20d,20d,20d,20d,20d,20d,20d,20d,20d,20d,20d,20d,20d,20d,20d,20d};
    private int historyIndex = 0;
    private long lastPoll = System.nanoTime();
    private final long tickInterval = 50;

    @Override
    public void run() {
        final long startTime = System.nanoTime();
        final long currentTime = System.currentTimeMillis();
        long timeSpent = (startTime - lastPoll) / 1000;
        if (timeSpent == 0)
        {
            timeSpent = 1;
        }
        tick++;
        double tps = tickInterval * 1000000.0 / timeSpent;
        history[historyIndex++] = tps;
        if (historyIndex >= history.length) {
            historyIndex = 0;
        }
        lastPoll = startTime;
    }

    private long lastGetTPSTick = 0;
    private double lastGetTPSValue = 20d;

    public double getTPS() {
        if (tick == lastGetTPSTick) {
            return lastGetTPSValue;
        }
        double total = 0;
        for (double tps : history) {
            total += tps;
        }
        lastGetTPSValue = total / history.length;
        lastGetTPSTick = tick;
        return lastGetTPSValue;
    }
}
