package com.boydti.fawe.util;

import com.boydti.fawe.config.BBC;
import com.boydti.fawe.object.FawePlayer;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;

public class SafeExtentWrapper extends AbstractDelegateExtent {
    private final FawePlayer<?> player;
    
    public SafeExtentWrapper(final FawePlayer<?> player, final Extent extent) {
        super(extent);
        this.player = player;
    }
    
    @Override
    public boolean setBlock(final Vector location, final BaseBlock block) throws WorldEditException {
        if (super.setBlock(location, block)) {
            if (MemUtil.isMemoryLimited()) {
                if (player != null) {
                    BBC.WORLDEDIT_OOM.send(player);
                    if (Perm.hasPermission(player, "worldedit.fast")) {
                        BBC.WORLDEDIT_OOM_ADMIN.send(player);
                    }
                }
                WEManager.IMP.cancelEdit(this);
                return false;
            }
            return true;
        }
        return false;
    }
}
