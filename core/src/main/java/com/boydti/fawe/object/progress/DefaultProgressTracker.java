package com.boydti.fawe.object.progress;

import com.boydti.fawe.config.BBC;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.RunnableVal2;
import com.boydti.fawe.util.FaweQueue;
import com.boydti.fawe.util.StringMan;
import com.boydti.fawe.util.TaskManager;

public class DefaultProgressTracker extends RunnableVal2<FaweQueue.ProgressType, Integer> {

    private final FawePlayer player;
    private final long start;

    public DefaultProgressTracker(FawePlayer player) {
        this.start = System.currentTimeMillis();
        this.player = player;
    }

    private int amountQueue = 0;
    private int amountDispatch = 0;
    private long lastTick = 0;

    @Override
    public void run(FaweQueue.ProgressType type, Integer amount) {
        switch (type) {
            case DISPATCH:
                amountDispatch = amount;
                break;
            case QUEUE:
                amountQueue = amount;
                break;
            case DONE:
                if (amountDispatch > 64) {
                    done();
                }
                return;
        }
        if (amountQueue > 64 || amountDispatch > 64) {
            send();
        }
    }

    private void done() {
        TaskManager.IMP.task(new Runnable() {
            @Override
            public void run() {
                final long time = System.currentTimeMillis() - start;
                player.sendTitle("", BBC.PROGRESS_DONE.format(time / 1000d));
                TaskManager.IMP.later(new Runnable() {
                    @Override
                    public void run() {
                        player.resetTitle();
                    }
                }, 60);
            }
        });
    }

    public void send() {
        TaskManager.IMP.task(new Runnable() {
            @Override
            public void run() {
                long currentTick = System.currentTimeMillis() / 50;
                if (currentTick > lastTick + Settings.DISPLAY_PROGRESS_INTERVAL) {
                    lastTick = currentTick;
                    String queue = StringMan.padRight("" + amountQueue, 3);
                    String dispatch = StringMan.padRight("" + amountDispatch, 3);
                    player.sendTitle("", BBC.PROGRESS_MESSAGE.format(queue, dispatch));
                }
            }
        });
    }
}
