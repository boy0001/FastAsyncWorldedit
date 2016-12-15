package com.boydti.fawe.object.schematic;

import com.boydti.fawe.object.clipboard.ReadOnlyClipboard;
import com.boydti.fawe.util.EditSessionBuilder;
import com.boydti.fawe.util.MaskTraverser;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.extent.transform.BlockTransformExtent;
import com.sk89q.worldedit.function.mask.ExistingBlockMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.transform.Transform;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.World;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import javax.annotation.Nullable;


import static com.google.common.base.Preconditions.checkNotNull;

public class Schematic {
    private final Clipboard clipboard;

    public Schematic(Clipboard clipboard) {
        checkNotNull(clipboard);
        this.clipboard = clipboard;
    }

    /**
     * Get the schematic for a region
     * @param region
     */
    public Schematic(Region region) {
        checkNotNull(region);
        checkNotNull(region.getWorld());
        EditSession session = new EditSessionBuilder(region.getWorld()).allowedRegionsEverywhere().autoQueue(false).build();
        this.clipboard = new BlockArrayClipboard(region, ReadOnlyClipboard.of(session, region));
    }

    public @Nullable Clipboard getClipboard() {
        return clipboard;
    }

    /**
     * Forwards to paste(world, to, true, true, null)
     * @param world
     * @param to
     * @return
     */
    public EditSession paste(World world, Vector to) {
        return paste(world, to, true, true, null);
    }

    public void save(File file, ClipboardFormat format) throws IOException {
        checkNotNull(file);
        checkNotNull(format);
        if (!file.exists()) {
            File parent = file.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }
            file.createNewFile();
        }
        save(new FileOutputStream(file), format);
    }

    /**
     * Save this schematic to a stream
     * @param stream
     * @param format
     * @throws IOException
     */
    public void save(OutputStream stream, ClipboardFormat format) throws IOException {
        checkNotNull(stream);
        checkNotNull(format);
        try (ClipboardWriter writer = format.getWriter(stream)) {
            writer.write(clipboard, clipboard.getRegion().getWorld().getWorldData());
        }
    }

    /**
     * Paste this schematic in a world
     * @param world
     * @param to
     * @param allowUndo
     * @param pasteAir
     * @param transform
     * @return
     */
    public EditSession paste(World world, Vector to, boolean allowUndo, boolean pasteAir, @Nullable Transform transform) {
        checkNotNull(world);
        checkNotNull(to);
        Region region = clipboard.getRegion();
        EditSessionBuilder builder = new EditSessionBuilder(world).autoQueue(true).checkMemory(false).allowedRegionsEverywhere().limitUnlimited();
        EditSession editSession;
        if (allowUndo) {
            editSession = builder.build();
        } else {
            editSession = builder.changeSetNull().fastmode(true).build();
        }
        Extent extent = clipboard;
        if (transform != null) {
            extent = new BlockTransformExtent(clipboard, transform, world.getWorldData().getBlockRegistry());
        }
        ForwardExtentCopy copy = new ForwardExtentCopy(extent, clipboard.getRegion(), clipboard.getOrigin(), editSession, to);
        if (transform != null) {
            copy.setTransform(transform);
        }
        Mask sourceMask = editSession.getSourceMask();
        if (sourceMask != null) {
            new MaskTraverser(sourceMask).reset(extent);
            copy.setSourceMask(sourceMask);
            editSession.setSourceMask(null);
        }
        if (!pasteAir) {
            copy.setSourceMask(new ExistingBlockMask(clipboard));
        }
        try {
            Operations.completeLegacy(copy);
        } catch (MaxChangedBlocksException e) {
            e.printStackTrace();
        }
        editSession.flushQueue();
        return editSession;
    }
}
