package com.boydti.fawe.object.clipboard;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.regions.Region;

public class WorldCutClipboard extends WorldCopyClipboard {
    public WorldCutClipboard(EditSession editSession, Region region, boolean copyEntities, boolean copyBiome) {
        super(editSession, region, copyEntities, copyBiome);
    }

    public WorldCutClipboard(EditSession editSession, Region region) {
        super(editSession, region);
    }

    @Override
    public BaseBlock getBlock(int x, int y, int z) {
        int xx = mx + x;
        int yy = my + y;
        int zz = mz + z;
        BaseBlock block = editSession.getLazyBlock(xx, yy, zz);
        editSession.setBlock(xx, yy, zz, EditSession.nullBlock);
        return block;
    }

    public BaseBlock getBlockAbs(int x, int y, int z) {
        BaseBlock block = editSession.getLazyBlock(x, y, z);
        editSession.setBlock(x, y, z, EditSession.nullBlock);
        return block;
    }

    @Override
    public void forEach(BlockReader task, boolean air) {
        super.forEach(task, air);
        editSession.flushQueue();
    }
}