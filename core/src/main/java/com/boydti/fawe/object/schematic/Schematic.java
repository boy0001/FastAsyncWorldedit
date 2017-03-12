package com.boydti.fawe.object.schematic;

import com.boydti.fawe.object.HasFaweQueue;
import com.boydti.fawe.object.RunnableVal2;
import com.boydti.fawe.object.clipboard.ReadOnlyClipboard;
import com.boydti.fawe.util.EditSessionBuilder;
import com.boydti.fawe.util.MaskTraverser;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.extent.transform.BlockTransformExtent;
import com.sk89q.worldedit.function.RegionFunction;
import com.sk89q.worldedit.function.mask.ExistingBlockMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.visitor.RegionVisitor;
import com.sk89q.worldedit.math.transform.Transform;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.registry.WorldData;
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

    public void paste(Extent extent, WorldData worldData, Vector to, boolean pasteAir, Transform transform) {
        Region region = clipboard.getRegion();
        BlockTransformExtent source = new BlockTransformExtent(clipboard, transform, worldData.getBlockRegistry());
        ForwardExtentCopy copy = new ForwardExtentCopy(source, clipboard.getRegion(), clipboard.getOrigin(), extent, to);
        copy.setTransform(transform);
        if (!pasteAir) {
            copy.setSourceMask(new ExistingBlockMask(clipboard));
        }
        Operations.completeBlindly(copy);
    }

    public void paste(Extent extent, Vector to, boolean pasteAir) {
        Region region = clipboard.getRegion().clone();
        final int maxY = extent.getMaximumPoint().getBlockY();
        final Vector bot = clipboard.getMinimumPoint();
        final Vector origin = clipboard.getOrigin();
        // Optimize for BlockArrayClipboard
        if (clipboard instanceof BlockArrayClipboard && region instanceof CuboidRegion) {
            // To is relative to the world origin (player loc + small clipboard offset) (As the positions supplied are relative to the clipboard min)
            final int relx = to.getBlockX() + bot.getBlockX() - origin.getBlockX();
            final int rely = to.getBlockY() + bot.getBlockY() - origin.getBlockY();
            final int relz = to.getBlockZ() + bot.getBlockZ() - origin.getBlockZ();
            BlockArrayClipboard bac = (BlockArrayClipboard) clipboard;
            bac.IMP.forEach(new RunnableVal2<Vector, BaseBlock>() {
                @Override
                public void run(Vector mutable, BaseBlock block) {
                    mutable.mutX(mutable.getX() + relx);
                    mutable.mutY(mutable.getY() + rely);
                    mutable.mutZ(mutable.getZ() + relz);
                    if (mutable.getY() >= 0 && mutable.getY() <= maxY) {
                        try {
                            extent.setBlock(mutable, block);
                        } catch (WorldEditException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }, pasteAir);
        } else {
            // To must be relative to the clipboard origin ( player location - clipboard origin ) (as the locations supplied are relative to the world origin)
            final int relx = to.getBlockX() - origin.getBlockX();
            final int rely = to.getBlockY() - origin.getBlockY();
            final int relz = to.getBlockZ() - origin.getBlockZ();
            RegionVisitor visitor = new RegionVisitor(region, new RegionFunction() {
                @Override
                public boolean apply(Vector mutable) throws WorldEditException {
                    BaseBlock block = clipboard.getBlock(mutable);
                    if (block == EditSession.nullBlock && !pasteAir) {
                        return false;
                    }
                    mutable.mutX(mutable.getX() + relx);
                    mutable.mutY(mutable.getY() + rely);
                    mutable.mutZ(mutable.getZ() + relz);
                    if (mutable.getY() >= 0 && mutable.getY() <= maxY) {
                        return extent.setBlock(mutable, block);
                    }
                    return false;
                }
            }, (HasFaweQueue) (extent instanceof HasFaweQueue ? extent : null));
            Operations.completeBlindly(visitor);
        }
        // Entity offset is the paste location subtract the clipboard origin (entity's location is already relative to the world origin)
        final int entityOffsetX = to.getBlockX() - origin.getBlockX();
        final int entityOffsetY = to.getBlockY() - origin.getBlockY();
        final int entityOffsetZ = to.getBlockZ() - origin.getBlockZ();
        // entities
        for (Entity entity : clipboard.getEntities()) {
            Location pos = entity.getLocation();
            Location newPos = new Location(pos.getExtent(), pos.getX() + entityOffsetX, pos.getY() + entityOffsetY, pos.getZ() + entityOffsetZ, pos.getYaw(), pos.getPitch());
            extent.createEntity(newPos, entity.getState());
        }
    }
}
