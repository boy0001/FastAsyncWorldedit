package com.sk89q.worldedit.event.extent;

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.event.Cancellable;
import com.sk89q.worldedit.event.Event;

public abstract class FaweEvent extends Event implements Cancellable {
    /**
     * Returns true if this event was called and not cancelled
     * @return !isCancelled
     */
    public boolean call() {
        WorldEdit.getInstance().getEventBus().post(this);
        return !this.isCancelled();
    }
}
