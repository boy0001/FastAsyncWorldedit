package com.sk89q.worldedit.function.pattern;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.extent.clipboard.Clipboard;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A pattern that reads from {@link Clipboard}.
 */
public class ClipboardPattern extends AbstractPattern {

    private final Clipboard clipboard;
    private final int sx, sy, sz;
    private final Vector min;

    /**
     * Create a new clipboard pattern.
     *
     * @param clipboard the clipboard
     */
    public ClipboardPattern(Clipboard clipboard) {
        checkNotNull(clipboard);
        this.clipboard = clipboard;
        Vector size = clipboard.getMaximumPoint().subtract(clipboard.getMinimumPoint()).add(1, 1, 1);
        this.sx = size.getBlockX();
        this.sy = size.getBlockY();
        this.sz = size.getBlockZ();
        this.min = clipboard.getMinimumPoint();
    }

    private Vector mutable = new Vector();

    @Override
    public BaseBlock apply(Vector position) {
        int xp = position.getBlockX() % sx;
        int yp = position.getBlockY() % sy;
        int zp = position.getBlockZ() % sz;
        if (xp < 0) xp += sx;
        if (yp < 0) yp += sy;
        if (zp < 0) zp += sz;
        mutable.x = min.x + xp;
        mutable.y = min.y + yp;
        mutable.z = min.z + zp;
        return clipboard.getBlock(mutable);
    }

    public static Class<?> inject() {
        return ClipboardPattern.class;
    }
}