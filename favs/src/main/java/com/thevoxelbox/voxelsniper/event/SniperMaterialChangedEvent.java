//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.thevoxelbox.voxelsniper.event;

import com.boydti.fawe.Fawe;
import com.thevoxelbox.voxelsniper.Sniper;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.material.MaterialData;

public class SniperMaterialChangedEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final Sniper sniper;
    private final MaterialData originalMaterial;
    private final MaterialData newMaterial;
    private final String toolId;

    public SniperMaterialChangedEvent(Sniper sniper, String toolId, MaterialData originalMaterial, MaterialData newMaterial) {
        super(!Fawe.get().isMainThread());
        this.sniper = sniper;
        this.originalMaterial = originalMaterial;
        this.newMaterial = newMaterial;
        this.toolId = toolId;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public MaterialData getOriginalMaterial() {
        return this.originalMaterial;
    }

    public MaterialData getNewMaterial() {
        return this.newMaterial;
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
        return SniperMaterialChangedEvent.class;
    }
}
