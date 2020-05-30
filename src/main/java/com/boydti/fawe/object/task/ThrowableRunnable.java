package com.boydti.fawe.object.task;

import com.sk89q.worldedit.WorldEditException;

public interface ThrowableRunnable<T extends Throwable> {
    void run() throws T;
}
