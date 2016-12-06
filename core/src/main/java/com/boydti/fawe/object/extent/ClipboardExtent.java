package com.boydti.fawe.object.extent;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.mask.ExistingBlockMask;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.regions.Region;


import static com.google.common.base.Preconditions.checkNotNull;

public class ClipboardExtent extends ResettableExtent {

    private final Clipboard clipboard;
    private final Vector origin;
    private final boolean ignoreAir;
    private Extent extent;

    private final Vector mutable = new Vector();

    public ClipboardExtent(Extent parent, Clipboard clipboard, boolean ignoreAir) {
        super(parent);
        checkNotNull(clipboard);
        this.extent = parent;
        this.clipboard = clipboard;
        this.origin = clipboard.getOrigin();
        this.ignoreAir = ignoreAir;
    }

    @Override
    public boolean setBlock(Vector to, BaseBlock block) throws WorldEditException {
        Region region = clipboard.getRegion();
        ForwardExtentCopy copy = new ForwardExtentCopy(clipboard, clipboard.getRegion(), clipboard.getOrigin(), extent, to);
        if (ignoreAir) {
            copy.setSourceMask(new ExistingBlockMask(clipboard));
        }
        Operations.completeLegacy(copy);
        return true;
    }

    @Override
    public boolean setBlock(int x, int y, int z, BaseBlock block) throws WorldEditException {
        mutable.x = x;
        mutable.y = y;
        mutable.z = z;
        return setBlock(mutable, block);
    }

    @Override
    public ResettableExtent setExtent(Extent extent) {
        this.extent = extent;
        return super.setExtent(extent);
    }
}
