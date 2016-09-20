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
    private final Vector size;
    private final Vector min;

    /**
     * Create a new clipboard pattern.
     *
     * @param clipboard the clipboard
     */
    public ClipboardPattern(Clipboard clipboard) {
        checkNotNull(clipboard);
        this.clipboard = clipboard;
        this.size = clipboard.getMaximumPoint().subtract(clipboard.getMinimumPoint()).add(1, 1, 1);
        this.min = clipboard.getMinimumPoint();
    }

    private Vector mutable = new Vector();

    @Override
    public BaseBlock apply(Vector position) {
        int xp = Math.abs(position.getBlockX()) % size.getBlockX();
        int yp = Math.abs(position.getBlockY()) % size.getBlockY();
        int zp = Math.abs(position.getBlockZ()) % size.getBlockZ();
        mutable.x = min.x + xp;
        mutable.y = min.y + yp;
        mutable.z = min.z + zp;
        return clipboard.getBlock(mutable);
    }

    public static Class<?> inject() {
        return ClipboardPattern.class;
    }
}