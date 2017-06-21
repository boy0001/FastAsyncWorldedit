package com.boydti.fawe.object.clipboard;

import com.boydti.fawe.object.PseudoRandom;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.math.transform.Transform;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.registry.WorldData;

public class MultiClipboardHolder extends ClipboardHolder {
    private final ClipboardHolder[] holders;

    private ClipboardHolder holder;

    public MultiClipboardHolder(WorldData worldData, ClipboardHolder... holders) {
        super(holders[0].getClipboard(), worldData);
        holder = holders[0];
        this.holders = holders;
    }


    @Override
    public Clipboard getClipboard() {
        holder = holders[PseudoRandom.random.nextInt(holders.length)];
        return holder.getClipboard();
    }

    @Override
    public Transform getTransform() {
        return holder.getTransform();
    }

    @Override
    public void setTransform(Transform transform) {
        holder.setTransform(transform);
    }

    @Override
    public void close() {
        for (ClipboardHolder holder : holders) {
            if (holder != null) holder.close();
        }
    }
}