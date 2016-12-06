package com.boydti.fawe.object.pattern;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.mask.ExistingBlockMask;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.pattern.AbstractPattern;
import com.sk89q.worldedit.regions.Region;


import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A pattern that reads from {@link Clipboard}.
 */
public class FullClipboardPattern extends AbstractPattern {
    private final Extent extent;
    private final Clipboard clipboard;
    private final BaseBlock block;

    private final Vector mutable = new Vector();

    /**
     * Create a new clipboard pattern.
     *
     * @param clipboard the clipboard
     */
    public FullClipboardPattern(Extent extent, Clipboard clipboard) {
        checkNotNull(clipboard);
        this.clipboard = clipboard;
        this.extent = extent;
        Vector origin = clipboard.getOrigin();
        block = clipboard.getBlock(origin);
    }

    @Override
    public BaseBlock apply(Vector to) {
        Region region = clipboard.getRegion();
        ForwardExtentCopy copy = new ForwardExtentCopy(clipboard, clipboard.getRegion(), clipboard.getOrigin(), extent, to);
        copy.setSourceMask(new ExistingBlockMask(clipboard));
        Operations.completeBlindly(copy);
        return block;
    }
}