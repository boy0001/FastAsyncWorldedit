package com.thevoxelbox.voxelsniper.event;

import com.boydti.fawe.Fawe;
import com.thevoxelbox.voxelsniper.Sniper;
import com.thevoxelbox.voxelsniper.brush.IBrush;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class SniperBrushChangedEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final Sniper sniper;
    private final IBrush originalBrush;
    private final IBrush newBrush;
    private final String toolId;

    public SniperBrushChangedEvent(Sniper sniper, String toolId, IBrush originalBrush, IBrush newBrush) {
        super(!Fawe.isMainThread());
        this.sniper = sniper;
        this.originalBrush = originalBrush;
        this.newBrush = newBrush;
        this.toolId = toolId;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public IBrush getOriginalBrush() {
        return this.originalBrush;
    }

    public IBrush getNewBrush() {
        return this.newBrush;
    }

    public Sniper getSniper() {
        return this.sniper;
    }

    public String getToolId() {
        return this.toolId;
    }

    public HandlerList getHandlers() {
        return handlers;
    }

    public static Class<?> inject() {
        return SniperBrushChangedEvent.class;
    }
}
