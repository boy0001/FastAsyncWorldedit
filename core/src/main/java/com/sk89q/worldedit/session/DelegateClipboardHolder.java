package com.sk89q.worldedit.session;

import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.math.transform.Transform;
import com.sk89q.worldedit.world.registry.WorldData;

public class DelegateClipboardHolder extends ClipboardHolder {
    private final ClipboardHolder parent;

    public DelegateClipboardHolder(ClipboardHolder holder) {
        super(holder.getClipboard(), holder.getWorldData());
        this.parent = holder;
    }

    @Override
    public WorldData getWorldData() {
        return parent.getWorldData();
    }

    @Override
    public Clipboard getClipboard() {
        return parent.getClipboard();
    }

    @Override
    public void setTransform(Transform transform) {
        parent.setTransform(transform);
    }

    @Override
    public Transform getTransform() {
        return parent.getTransform();
    }

    @Override
    public PasteBuilder createPaste(Extent targetExtent, WorldData targetWorldData) {
        return parent.createPaste(targetExtent, targetWorldData);
    }

    @Override
    public void close() {
        parent.close();
    }
}
