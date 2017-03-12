package com.boydti.fawe.object.clipboard;

import com.google.common.io.ByteSource;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.world.registry.WorldData;
import java.util.UUID;

public class RandomClipboardHolder extends LazyClipboardHolder {
    public RandomClipboardHolder(ByteSource source, ClipboardFormat format, WorldData worldData, UUID uuid) {
        super(source, format, worldData, uuid);
    }
}
