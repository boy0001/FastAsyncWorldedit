package com.boydti.fawe.util;

/**
 * TPS and Lag Checker.
 */
public class Lag implements Runnable {
    /**
     * Ticks
     */
    public static final long[] T = new long[600];
    /**
     * Tick count
     */
    public static int TC = 0;
    /**
     * something :_:
     */
    @SuppressWarnings("unused")
    public static long LT = 0L;

    /**
     * Get the server TPS
     *
     * @return server tick per second
     */
    public static double getTPS() {
        return Math.round(getTPS(100)) > 20.0D ? 20.0D : Math.round(getTPS(100));
    }

    /**
     * Return the tick per second (measured in $ticks)
     *
     * @param ticks Ticks
     *
     * @return ticks per second
     */
    public static double getTPS(final int ticks) {
        if (TC < ticks) {
            return 20.0D;
        }
        final int t = (TC - 1 - ticks) % T.length;
        final long e = System.currentTimeMillis() - T[t];
        return ticks / (e / 1000.0D);
    }

    /**
     * Get number of ticks since
     *
     * @param tI Ticks <
     *
     * @return number of ticks since $tI
     */
    public static long getElapsed(final int tI) {
        final long t = T[tI % T.length];
        return System.currentTimeMillis() - t;
    }

    /**
     * Get lag percentage
     *
     * @return lag percentage
     */
    public static double getPercentage() {
        return Math.round((1.0D - (Lag.getTPS() / 20.0D)) * 100.0D);
    }

    /**
     * Get TPS percentage (of 20)
     *
     * @return TPS percentage
     */
    public static double getFullPercentage() {
        return getTPS() * 5.0D;
    }

    @Override
    public void run() {
        T[TC % T.length] = System.currentTimeMillis();
        TC++;
    }
}
