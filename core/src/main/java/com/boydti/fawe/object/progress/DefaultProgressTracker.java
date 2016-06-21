package com.boydti.fawe.object.progress;

import com.boydti.fawe.config.BBC;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.RunnableVal2;
import com.boydti.fawe.object.FaweQueue;
import com.boydti.fawe.util.StringMan;
import com.boydti.fawe.util.TaskManager;

/**
 * The default progress tracker uses titles
 */
public class DefaultProgressTracker extends RunnableVal2<FaweQueue.ProgressType, Integer> {

    private final FawePlayer player;
    private final long start;

    public DefaultProgressTracker(FawePlayer player) {
        this.start = System.currentTimeMillis();
        this.player = player;
    }

    // Number of times a chunk was queued
    private int totalQueue = 0;
    // Current size of the queue
    private int amountQueue = 0;
    // Number of chunks dispatched
    private int amountDispatch = 0;

    @Override
    public void run(FaweQueue.ProgressType type, Integer amount) {
        switch (type) {
            case DISPATCH:
                amountDispatch = amount;
                break;
            case QUEUE:
                totalQueue++;
                amountQueue = amount;
                break;
            case DONE:
                if (totalQueue > 64) {
                    done();
                }
                return;
        }
        // Only send a message after 64 chunks (i.e. ignore smaller edits)
        if (totalQueue > 64) {
            send();
        }
    }

    private final void done() {
        TaskManager.IMP.task(new Runnable() {
            @Override
            public void run() {
                doneTask();
            }
        });
    }

    private long lastTick = 0;

    private final void send() {
        // Avoid duplicates
        long currentTick = System.currentTimeMillis() / 50;
        if (currentTick > lastTick + Settings.QUEUE.PROGRESS.INTERVAL) {
            lastTick = currentTick;
            TaskManager.IMP.task(new Runnable() { // Run on main thread
                @Override
                public void run() {
                    sendTask();
                }
            });
        }
    }

    public void doneTask() {
        final long time = System.currentTimeMillis() - start;
        player.sendTitle("", BBC.PROGRESS_DONE.format(time / 1000d));
        TaskManager.IMP.later(new Runnable() { // Run on main thread
            @Override
            public void run() {
                doneTask();
            }
        }, 60);
    }

    public void sendTask() {
        String queue = StringMan.padRight("" + amountQueue, 3);
        String dispatch = StringMan.padRight("" + amountDispatch, 3);
        player.sendTitle("", BBC.PROGRESS_MESSAGE.format(queue, dispatch));
    }
}
