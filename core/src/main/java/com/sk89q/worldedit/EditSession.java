/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweCache;
import com.boydti.fawe.config.BBC;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.EditSessionWrapper;
import com.boydti.fawe.object.FaweLimit;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.NullChangeSet;
import com.boydti.fawe.object.RegionWrapper;
import com.boydti.fawe.object.RunnableVal;
import com.boydti.fawe.object.changeset.DiskStorageHistory;
import com.boydti.fawe.object.changeset.FaweChangeSet;
import com.boydti.fawe.object.changeset.MemoryOptimizedHistory;
import com.boydti.fawe.object.exception.FaweException;
import com.boydti.fawe.object.extent.FastWorldEditExtent;
import com.boydti.fawe.object.extent.FaweExtent;
import com.boydti.fawe.object.extent.NullExtent;
import com.boydti.fawe.object.extent.ProcessedWEExtent;
import com.boydti.fawe.object.extent.SafeExtentWrapper;
import com.boydti.fawe.util.ExtentWrapper;
import com.boydti.fawe.util.FaweQueue;
import com.boydti.fawe.util.MemUtil;
import com.boydti.fawe.util.Perm;
import com.boydti.fawe.util.SetQueue;
import com.boydti.fawe.util.TaskManager;
import com.boydti.fawe.util.WEManager;
import com.boydti.fawe.wrappers.WorldWrapper;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.blocks.BlockType;
import com.sk89q.worldedit.command.tool.BrushTool;
import com.sk89q.worldedit.command.tool.Tool;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.event.extent.EditSessionEvent;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extent.ChangeSetExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.MaskingExtent;
import com.sk89q.worldedit.extent.buffer.ForgetfulExtentBuffer;
import com.sk89q.worldedit.extent.inventory.BlockBag;
import com.sk89q.worldedit.extent.world.SurvivalModeExtent;
import com.sk89q.worldedit.function.GroundFunction;
import com.sk89q.worldedit.function.RegionMaskingFilter;
import com.sk89q.worldedit.function.block.BlockReplace;
import com.sk89q.worldedit.function.block.Naturalizer;
import com.sk89q.worldedit.function.generator.GardenPatchGenerator;
import com.sk89q.worldedit.function.mask.BlockMask;
import com.sk89q.worldedit.function.mask.BoundedHeightMask;
import com.sk89q.worldedit.function.mask.ExistingBlockMask;
import com.sk89q.worldedit.function.mask.FuzzyBlockMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.MaskIntersection;
import com.sk89q.worldedit.function.mask.MaskUnion;
import com.sk89q.worldedit.function.mask.Masks;
import com.sk89q.worldedit.function.mask.NoiseFilter2D;
import com.sk89q.worldedit.function.mask.RegionMask;
import com.sk89q.worldedit.function.operation.ChangeSetExecutor;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.OperationQueue;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.pattern.BlockPattern;
import com.sk89q.worldedit.function.pattern.Patterns;
import com.sk89q.worldedit.function.util.RegionOffset;
import com.sk89q.worldedit.function.visitor.DownwardVisitor;
import com.sk89q.worldedit.function.visitor.LayerVisitor;
import com.sk89q.worldedit.function.visitor.NonRisingVisitor;
import com.sk89q.worldedit.function.visitor.RecursiveVisitor;
import com.sk89q.worldedit.function.visitor.RegionVisitor;
import com.sk89q.worldedit.history.UndoContext;
import com.sk89q.worldedit.history.change.BlockChange;
import com.sk89q.worldedit.history.changeset.ChangeSet;
import com.sk89q.worldedit.internal.expression.Expression;
import com.sk89q.worldedit.internal.expression.ExpressionException;
import com.sk89q.worldedit.internal.expression.runtime.RValue;
import com.sk89q.worldedit.math.interpolation.Interpolation;
import com.sk89q.worldedit.math.interpolation.KochanekBartelsInterpolation;
import com.sk89q.worldedit.math.interpolation.Node;
import com.sk89q.worldedit.math.noise.RandomNoise;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.patterns.Pattern;
import com.sk89q.worldedit.patterns.SingleBlockPattern;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.EllipsoidRegion;
import com.sk89q.worldedit.regions.FlatRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.regions.Regions;
import com.sk89q.worldedit.regions.shape.ArbitraryBiomeShape;
import com.sk89q.worldedit.regions.shape.ArbitraryShape;
import com.sk89q.worldedit.regions.shape.RegionShape;
import com.sk89q.worldedit.regions.shape.WorldEditExpressionEnvironment;
import com.sk89q.worldedit.util.Countable;
import com.sk89q.worldedit.util.TreeGenerator;
import com.sk89q.worldedit.util.eventbus.EventBus;
import com.sk89q.worldedit.world.AbstractWorld;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.biome.BaseBiome;
import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;


import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.sk89q.worldedit.regions.Regions.asFlatRegion;
import static com.sk89q.worldedit.regions.Regions.maximumBlockY;
import static com.sk89q.worldedit.regions.Regions.minimumBlockY;

/**
 * An {@link Extent} that handles history, {@link BlockBag}s, change limits,
 * block re-ordering, and much more. Most operations in WorldEdit use this class.
 *
 * <p>Most of the actual functionality is implemented with a number of other
 * {@link Extent}s that are chained together. For example, history is logged
 * using the {@link ChangeSetExtent}.</p>
 */
public class EditSession implements Extent {

    private final Logger log = Logger.getLogger(EditSession.class.getCanonicalName());

    /**
     * Used by {@link #setBlock(Vector, BaseBlock, Stage)} to
     * determine which {@link Extent}s should be bypassed.
     */
    public enum Stage {
        BEFORE_HISTORY, BEFORE_REORDER, BEFORE_CHANGE
    }

    public World world;
    public Actor actor;
    public FaweChangeSet changeSet;
    public EditSessionWrapper wrapper;
    public FaweExtent faweExtent;
    public MaskingExtent maskingExtent;
    public Extent bypassReorderHistory;
    public Extent bypassHistory;
    public Extent bypassNone;
    public SurvivalModeExtent lazySurvivalExtent;
    public boolean fastmode;
    public Mask oldMask;
    public FaweLimit limit = FaweLimit.MAX.copy();
    public FaweQueue queue;

    public static BaseBiome nullBiome = new BaseBiome(0);
    public static BaseBlock nullBlock = FaweCache.CACHE_BLOCK[0];

    /**
     * Create a new instance.
     *
     * @param world a world
     * @param maxBlocks the maximum number of blocks that can be changed, or -1 to use no limit
     * @deprecated use {@link WorldEdit#getEditSessionFactory()} to create {@link EditSession}s
     */
    @Deprecated
    public EditSession(final LocalWorld world, final int maxBlocks) {
        this(world, maxBlocks, null);
    }

    /**
     * Create a new instance.
     *
     * @param world a world
     * @param maxBlocks the maximum number of blocks that can be changed, or -1 to use no limit
     * @param blockBag the block bag to set, or null to use none
     * @deprecated use {@link WorldEdit#getEditSessionFactory()} to create {@link EditSession}s
     */
    @Deprecated
    public EditSession(final LocalWorld world, final int maxBlocks, @Nullable final BlockBag blockBag) {
        this(WorldEdit.getInstance().getEventBus(), world, maxBlocks, blockBag, new EditSessionEvent(world, null, maxBlocks, null));
    }

    private int changes = 0;
    private int maxBlocks;
    private BlockBag blockBag;

    /**
     * Construct the object with a maximum number of blocks and a block bag.
     *
     * @param eventBus the event bus
     * @param world the world
     * @param maxBlocks the maximum number of blocks that can be changed, or -1 to use no limit
     * @param blockBag an optional {@link BlockBag} to use, otherwise null
     * @param event the event to call with the extent
     */
    public EditSession(final EventBus eventBus, World world, final int maxBlocks, @Nullable final BlockBag blockBag, final EditSessionEvent event) {
        checkNotNull(eventBus);
        checkArgument(maxBlocks >= -1, "maxBlocks >= -1 required");
        checkNotNull(event);
        this.actor = event.getActor();
        // Wrap world
        this.blockBag = blockBag;
        this.maxBlocks = maxBlocks;
        // Invalid; return null extent
        if (world == null) {
            FaweExtent extent = faweExtent = new NullExtent(world, BBC.WORLDEDIT_CANCEL_REASON_MAX_FAILS);
            this.bypassReorderHistory = extent;
            this.bypassHistory = extent;
            this.bypassNone = extent;
            this.changeSet = new NullChangeSet();
            this.wrapper = Fawe.imp().getEditSessionWrapper(this);
            return;
        }
        // Set the world of the event to the actual world (workaround for CoreProtect)
        try {
            Class<? extends EditSessionEvent> eventClass = event.getClass();
            Field fieldWorld = eventClass.getDeclaredField("world");
            fieldWorld.setAccessible(true);
            if (world instanceof WorldWrapper) {
                fieldWorld.set(event, ((WorldWrapper) world).getParent());
            } else {
                fieldWorld.set(event, world);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        this.world = (world = new WorldWrapper((AbstractWorld) world));
        this.wrapper = Fawe.imp().getEditSessionWrapper(this);
        // Not a player; bypass history
        if ((actor == null) || !actor.isPlayer()) {
            this.queue = SetQueue.IMP.getNewQueue(Fawe.imp().getWorldName(world), true, true);
            queue.addEditSession(this);
            Extent extent = (this.faweExtent = new FastWorldEditExtent(world, queue));
            // Everything bypasses
            extent = this.wrapExtent(extent, eventBus, event, Stage.BEFORE_CHANGE);
            extent = this.wrapExtent(extent, eventBus, event, Stage.BEFORE_REORDER);
            extent = this.wrapExtent(extent, eventBus, event, Stage.BEFORE_HISTORY);
            this.bypassReorderHistory = extent;
            this.bypassHistory = extent;
            this.bypassNone = extent;
            this.changeSet = new NullChangeSet();
            return;
        }
        this.changeSet = Settings.STORE_HISTORY_ON_DISK ? new DiskStorageHistory(world, actor.getUniqueId()) : new MemoryOptimizedHistory(actor);
        Extent extent;
        final FawePlayer fp = FawePlayer.wrap(actor);
        final LocalSession session = fp.getSession();
        this.fastmode = session.hasFastMode();
        if (fp.hasWorldEditBypass()) {
            this.queue = SetQueue.IMP.getNewQueue(Fawe.imp().getWorldName(world), true, true);
            queue.addEditSession(this);
            // Bypass skips processing and area restrictions
            extent = (this.faweExtent = new FastWorldEditExtent(world, queue));
            if (this.hasFastMode()) {
                // Fastmode skips history and memory checks
                extent = this.wrapExtent(extent, eventBus, event, Stage.BEFORE_CHANGE);
                extent = this.wrapExtent(extent, eventBus, event, Stage.BEFORE_REORDER);
                extent = this.wrapExtent(extent, eventBus, event, Stage.BEFORE_HISTORY);
                this.bypassReorderHistory = extent;
                this.bypassHistory = extent;
                this.bypassNone = extent;
                return;
            }
        } else {
            this.queue = SetQueue.IMP.getNewQueue(Fawe.imp().getWorldName(world), false, true);
            queue.addEditSession(this);
            this.limit = fp.getLimit();
            final HashSet<RegionWrapper> mask = WEManager.IMP.getMask(fp);
            if (mask.size() == 0) {
                // No allowed area; return null extent
                extent = faweExtent = new NullExtent(world, BBC.WORLDEDIT_CANCEL_REASON_MAX_FAILS);
                this.bypassReorderHistory = extent;
                this.bypassHistory = extent;
                this.bypassNone = extent;
                return;
            }
            // Process the WorldEdit action
            ProcessedWEExtent processed = (ProcessedWEExtent) (this.faweExtent = new ProcessedWEExtent(world, fp, mask, limit, queue));
            extent = processed;
            if (this.hasFastMode()) {
                // Fastmode skips history, masking, and memory checks
                extent = this.wrapExtent(extent, eventBus, event, Stage.BEFORE_CHANGE);
                extent = this.wrapExtent(extent, eventBus, event, Stage.BEFORE_REORDER);
                extent = this.wrapExtent(extent, eventBus, event, Stage.BEFORE_HISTORY);
                extent = new ExtentWrapper(extent);
                // Set the parent. This allows efficient cancelling
                processed.setParent(extent);
                this.bypassReorderHistory = extent;
                this.bypassHistory = extent;
                this.bypassNone = extent;
                return;
            } else {
                if (MemUtil.isMemoryLimited()) {
                    fp.sendMessage(BBC.WORLDEDIT_CANCEL_REASON.format(BBC.WORLDEDIT_CANCEL_REASON_LOW_MEMORY.s()));
                    if (Perm.hasPermission(fp, "worldedit.fast")) {
                        BBC.WORLDEDIT_OOM_ADMIN.send(fp);
                    }

                    // Memory limit reached; return null extent
                    extent = faweExtent = new NullExtent(world, BBC.WORLDEDIT_CANCEL_REASON_MAX_FAILS);
                    this.bypassReorderHistory = extent;
                    this.bypassHistory = extent;
                    this.bypassNone = extent;
                    return;
                }
            }
            // Perform memory checks after reorder
            extent = new SafeExtentWrapper(fp, extent);
            processed.setParent(extent);
        }
        // Include history, masking and memory checking.
        Extent wrapped;
        extent = wrapped = this.wrapExtent(extent, eventBus, event, Stage.BEFORE_CHANGE);
        extent = this.wrapExtent(extent, eventBus, event, Stage.BEFORE_REORDER);
        extent = this.wrapper.getHistoryExtent(this, limit, extent, this.changeSet, queue, fp);
        final Player skp = (Player) actor;
        final int item = skp.getItemInHand();
        boolean hasMask = session.getMask() != null;
        if ((item != 0) && (!hasMask)) {
            try {
                final Tool tool = session.getTool(item);
                if ((tool != null) && (tool instanceof BrushTool)) {
                    hasMask = ((BrushTool) tool).getMask() != null;
                }
            } catch (final Exception e) {}
        }
        if (hasMask) {
            extent = this.maskingExtent = new MaskingExtent(extent, Masks.alwaysTrue());
        }

        extent = this.wrapExtent(extent, eventBus, event, Stage.BEFORE_HISTORY);
        extent = new SafeExtentWrapper(fp, extent);
        this.bypassReorderHistory = wrapped;
        this.bypassHistory = wrapped;
        this.bypassNone = extent;
        return;
    }

    public void debug(String message) {
        if (actor != null && message != null && message.length() > 0) {
            actor.print(BBC.PREFIX.s() + " " + message);
        }
    }

    public FaweQueue getQueue() {
        return queue;
    }

    private Extent wrapExtent(final Extent extent, final EventBus eventBus, EditSessionEvent event, final Stage stage) {
        event = event.clone(stage);
        event.setExtent(extent);
        eventBus.post(event);
        final Extent toReturn = event.getExtent();
        if (toReturn != extent) {
            String className = toReturn.getClass().getSimpleName().toLowerCase();
            if (className.contains("coreprotect")) {
                if (Settings.EXTENT_DEBUG) {
                    Fawe.debug("&cUnsafe extent detected: " + toReturn.getClass().getCanonicalName() + " !");
                    Fawe.debug("&8 - &7Use BlocksHub instead");
                    Fawe.debug("&8 - &7Or use FAWE rollback");
                    Fawe.debug("&8 - &7Change `extent.debug: false` to hide this message");
                }
                return extent;
            }
            for (String allowed : Settings.ALLOWED_3RDPARTY_EXTENTS) {
                if (className.contains(allowed.toLowerCase())) {
                    return toReturn;
                }
            }
            if (Settings.EXTENT_DEBUG) {
                Fawe.debug("&cPotentially inefficient WorldEdit extent: " + toReturn.getClass().getSimpleName());
                Fawe.debug("&8 - &7For area restrictions, it is recommended to use the FaweAPI");
                Fawe.debug("&8 - &7To allow this plugin add it to the FAWE `allowed-plugins` list");
            }
        }
        return extent;
    }

    /**
     * Get the world.
     *
     * @return the world
     */
    public World getWorld() {
        return this.world;
    }

    /**
     * Get the underlying {@link ChangeSet}.
     *
     * @return the change set
     */
    public ChangeSet getChangeSet() {
        return this.changeSet;
    }

    public void setChangeSet(FaweChangeSet set) {
        changes++;
        this.changeSet = set;
    }

    /**
     * Get the maximum number of blocks that can be changed. -1 will be returned
     * if it the limit disabled.
     *
     * @return the limit (&gt;= 0) or -1 for no limit
     */
    public int getBlockChangeLimit() {
        return this.maxBlocks;
    }

    /**
     * Set the maximum number of blocks that can be changed.
     *
     * @param limit the limit (&gt;= 0) or -1 for no limit
     */
    public void setBlockChangeLimit(final int limit) {
        // Nothing
    }

    /**
     * Returns queue status.
     *
     * @return whether the queue is enabled
     */
    public boolean isQueueEnabled() {
        return false;
    }

    /**
     * Queue certain types of block for better reproduction of those blocks.
     */
    public void enableQueue() {
    }

    /**
     * Disable the queue. This will flush the queue.
     */
    public void disableQueue() {
        if (this.isQueueEnabled()) {
            this.flushQueue();
        }
    }

    /**
     * Get the mask.
     *
     * @return mask, may be null
     */
    public Mask getMask() {
        return this.oldMask;
    }

    /**
     * Set a mask.
     *
     * @param mask mask or null
     */
    public void setMask(final Mask mask) {
        if (this.maskingExtent == null) {
            return;
        }
        this.oldMask = mask;
        if (mask == null) {
            this.maskingExtent.setMask(Masks.alwaysTrue());
        } else {
            this.maskingExtent.setMask(mask);
        }
    }

    public FaweExtent getFaweExtent() {
        return this.faweExtent;
    }

    /**
     * Set the mask.
     *
     * @param mask the mask
     * @deprecated Use {@link #setMask(Mask)}
     */
    @Deprecated
    public void setMask(final com.sk89q.worldedit.masks.Mask mask) {
        if (mask == null) {
            this.setMask((Mask) null);
        } else {
            this.setMask(Masks.wrap(mask));
        }
    }

    /**
     * Get the {@link SurvivalModeExtent}.
     *
     * @return the survival simulation extent
     */
    public SurvivalModeExtent getSurvivalExtent() {
        if (this.lazySurvivalExtent == null) {
            this.lazySurvivalExtent = new SurvivalModeExtent(bypassReorderHistory, world);
        }
        return lazySurvivalExtent;
    }

    /**
     * Set whether fast mode is enabled.
     *
     * <p>Fast mode may skip lighting checks or adjacent block
     * notification.</p>
     *
     * @param enabled true to enable
     */
    public void setFastMode(final boolean enabled) {
        this.fastmode = enabled;
    }

    /**
     * Return fast mode status.
     *
     * <p>Fast mode may skip lighting checks or adjacent block
     * notification.</p>
     *
     * @return true if enabled
     */
    public boolean hasFastMode() {
        return this.fastmode;
    }

    /**
     * Get the {@link BlockBag} is used.
     *
     * @return a block bag or null
     */
    public BlockBag getBlockBag() {
        return this.blockBag;
    }

    /**
     * Set a {@link BlockBag} to use.
     *
     * @param blockBag the block bag to set, or null to use none
     */
    public void setBlockBag(final BlockBag blockBag) {
        this.blockBag = blockBag;
    }

    /**
     * Gets the list of missing blocks and clears the list for the next
     * operation.
     *
     * @return a map of missing blocks
     */
    public Map<Integer, Integer> popMissingBlocks() {
        return new HashMap<>();
    }

    /**
     * Get the number of blocks changed, including repeated block changes.
     *
     * <p>This number may not be accurate.</p>
     *
     * @return the number of block changes
     */
    public int getBlockChangeCount() {
        return this.changes;
    }

    @Override
    public BaseBiome getBiome(final Vector2D position) {
        return this.bypassNone.getBiome(position);
    }

    @Override
    public boolean setBiome(final Vector2D position, final BaseBiome biome) {
        this.changes++;
        return this.bypassNone.setBiome(position, biome);
    }

    @Override
    public BaseBlock getLazyBlock(final Vector position) {
        return getLazyBlock(position.getBlockX(), position.getBlockY(), position.getBlockZ());
    }

    public BaseBlock getLazyBlock(int x, int y, int z) {
        if (limit != null && limit.MAX_CHECKS-- < 0) {
            throw new FaweException(BBC.WORLDEDIT_CANCEL_REASON_MAX_CHECKS);
        }
        int combinedId4Data = queue.getCombinedId4DataDebug(x, y, z, 0, this);
        if (!FaweCache.hasNBT(combinedId4Data >> 4)) {
            return FaweCache.CACHE_BLOCK[combinedId4Data];
        }
        try {
            BaseBlock block = this.world.getBlock(new Vector(x, y, z));
            return block;
        } catch (Throwable e) {
            e.printStackTrace();
            return FaweCache.CACHE_BLOCK[combinedId4Data];
        }
    }

    @Override
    public BaseBlock getBlock(final Vector position) {
        return this.getLazyBlock(position);
    }

    /**
     * Get a block type at the given position.
     *
     * @param position the position
     * @return the block type
     * @deprecated Use {@link #getLazyBlock(Vector)} or {@link #getBlock(Vector)}
     */
    @Deprecated
    public int getBlockType(final Vector position) {
        if (limit != null && limit.MAX_CHECKS-- < 0) {
            throw new FaweException(BBC.WORLDEDIT_CANCEL_REASON_MAX_CHECKS);
        }
        int combinedId4Data = queue.getCombinedId4DataDebug(position.getBlockX(), position.getBlockY(), position.getBlockZ(), 0, this);
        return combinedId4Data >> 4;
    }

    /**
     * Get a block data at the given position.
     *
     * @param position the position
     * @return the block data
     * @deprecated Use {@link #getLazyBlock(Vector)} or {@link #getBlock(Vector)}
     */
    @Deprecated
    public int getBlockData(final Vector position) {
        if (limit != null && limit.MAX_CHECKS-- < 0) {
            throw new FaweException(BBC.WORLDEDIT_CANCEL_REASON_MAX_CHECKS);
        }
        int combinedId4Data = queue.getCombinedId4DataDebug(position.getBlockX(), position.getBlockY(), position.getBlockZ(), 0, this);
        return combinedId4Data & 0xF;
    }

    /**
     * Gets the block type at a position.
     *
     * @param position the position
     * @return a block
     * @deprecated Use {@link #getBlock(Vector)}
     */
    @Deprecated
    public BaseBlock rawGetBlock(final Vector position) {
        return this.getLazyBlock(position);
    }

    /**
     * Returns the highest solid 'terrain' block which can occur naturally.
     *
     * @param x the X coordinate
     * @param z the Z cooridnate
     * @param minY minimal height
     * @param maxY maximal height
     * @return height of highest block found or 'minY'
     */
    public int getHighestTerrainBlock(final int x, final int z, final int minY, final int maxY) {
        return this.getHighestTerrainBlock(x, z, minY, maxY, false);
    }

    /**
     * Returns the highest solid 'terrain' block which can occur naturally.
     *
     * @param x the X coordinate
     * @param z the Z coordinate
     * @param minY minimal height
     * @param maxY maximal height
     * @param naturalOnly look at natural blocks or all blocks
     * @return height of highest block found or 'minY'
     */
    public int getHighestTerrainBlock(final int x, final int z, final int minY, final int maxY, final boolean naturalOnly) {
        return this.wrapper.getHighestTerrainBlock(x, z, minY, maxY, naturalOnly);
    }

    /**
     * Set a block, bypassing both history and block re-ordering.
     *
     * @param position the position to set the block at
     * @param block the block
     * @param stage the level
     * @return whether the block changed
     * @throws WorldEditException thrown on a set error
     */
    public boolean setBlock(final Vector position, final BaseBlock block, final Stage stage) throws WorldEditException {
        this.changes++;
        switch (stage) {
            case BEFORE_HISTORY:
                return this.bypassNone.setBlock(position, block);
            case BEFORE_CHANGE:
                return this.bypassHistory.setBlock(position, block);
            case BEFORE_REORDER:
                return this.bypassReorderHistory.setBlock(position, block);
        }

        throw new RuntimeException("New enum entry added that is unhandled here");
    }

    /**
     * Set a block, bypassing both history and block re-ordering.
     *
     * @param position the position to set the block at
     * @param block the block
     * @return whether the block changed
     */
    public boolean rawSetBlock(final Vector position, final BaseBlock block) {
        this.changes++;
        try {
            return this.bypassHistory.setBlock(position, block);
        } catch (final WorldEditException e) {
            throw new RuntimeException("Unexpected exception", e);
        }
    }

    /**
     * Set a block, bypassing history but still utilizing block re-ordering.
     *
     * @param position the position to set the block at
     * @param block the block
     * @return whether the block changed
     */
    public boolean smartSetBlock(final Vector position, final BaseBlock block) {
        this.changes++;
        try {
            return this.bypassReorderHistory.setBlock(position, block);
        } catch (final WorldEditException e) {
            throw new RuntimeException("Unexpected exception", e);
        }
    }

    @Override
    public boolean setBlock(final Vector position, final BaseBlock block) throws MaxChangedBlocksException {
        this.changes++;
        try {
            return this.bypassNone.setBlock(position, block);
        } catch (final MaxChangedBlocksException e) {
            throw e;
        } catch (final WorldEditException e) {
            throw new RuntimeException("Unexpected exception", e);
        }
    }

    /**
     * Sets the block at a position, subject to both history and block re-ordering.
     *
     * @param position the position
     * @param pattern a pattern to use
     * @return Whether the block changed -- not entirely dependable
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    @SuppressWarnings("deprecation")
    public boolean setBlock(final Vector position, final Pattern pattern) throws MaxChangedBlocksException {
        return this.setBlock(position, pattern.next(position));
    }

    /**
     * Set blocks that are in a set of positions and return the number of times
     * that the block set calls returned true.
     *
     * @param vset a set of positions
     * @param pattern the pattern
     * @return the number of changed blocks
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    @SuppressWarnings("deprecation")
    private int setBlocks(final Set<Vector> vset, final Pattern pattern) throws MaxChangedBlocksException {
        for (final Vector v : vset) {
            changes += this.setBlock(v, pattern) ? 1 : 0;
        }
        return changes;
    }

    /**
     * Set a block (only if a previous block was not there) if {@link Math#random()}
     * returns a number less than the given probability.
     *
     * @param position the position
     * @param block the block
     * @param probability a probability between 0 and 1, inclusive
     * @return whether a block was changed
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public boolean setChanceBlockIfAir(final Vector position, final BaseBlock block, final double probability) throws MaxChangedBlocksException {
        return (FaweCache.RANDOM.random(65536) <= (probability * 65536)) && this.setBlockIfAir(position, block);
    }

    /**
     * Set a block only if there's no block already there.
     *
     * @param position the position
     * @param block the block to set
     * @return if block was changed
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     * @deprecated Use your own method
     */
    @Deprecated
    public boolean setBlockIfAir(final Vector position, final BaseBlock block) throws MaxChangedBlocksException {
        return this.getBlock(position).isAir() && this.setBlock(position, block);
    }

    @Override
    @Nullable
    public Entity createEntity(final com.sk89q.worldedit.util.Location location, final BaseEntity entity) {
        return this.bypassNone.createEntity(location, entity);
    }

    /**
     * Insert a contrived block change into the history.
     *
     * @param position the position
     * @param existing the previous block at that position
     * @param block the new block
     * @deprecated Get the change set with {@link #getChangeSet()} and add the change with that
     */
    @Deprecated
    public void rememberChange(final Vector position, final BaseBlock existing, final BaseBlock block) {
        this.changeSet.add(new BlockChange(position.toBlockVector(), existing, block));
    }

    /**
     * Restores all blocks to their initial state.
     *
     * @param editSession a new {@link EditSession} to perform the undo in
     */
    public void undo(final EditSession editSession) {
        final UndoContext context = new UndoContext();
        context.setExtent(editSession.bypassHistory);
        Operations.completeSmart(ChangeSetExecutor.createUndo(this.changeSet, context), new Runnable() {
            @Override
            public void run() {
                editSession.flushQueue();
            }
        }, true);
        editSession.changes = changes;
    }

    /**
     * Sets to new state.
     *
     * @param editSession a new {@link EditSession} to perform the redo in
     */
    public void redo(final EditSession editSession) {
        final UndoContext context = new UndoContext();
        context.setExtent(editSession.bypassHistory);
        Operations.completeSmart(ChangeSetExecutor.createRedo(this.changeSet, context), new Runnable() {
            @Override
            public void run() {
                editSession.flushQueue();
            }
        }, true);
        editSession.changes = changes;
    }

    /**
     * Get the number of changed blocks.
     *
     * @return the number of changes
     */
    public int size() {
        return this.getBlockChangeCount();
    }

    @Override
    public Vector getMinimumPoint() {
        return this.getWorld().getMinimumPoint();
    }

    @Override
    public Vector getMaximumPoint() {
        return this.getWorld().getMaximumPoint();
    }

    @Override
    public List<? extends Entity> getEntities(final Region region) {
        return this.bypassNone.getEntities(region);
    }

    @Override
    public List<? extends Entity> getEntities() {
        return this.bypassNone.getEntities();
    }

    /**
     * Finish off the queue.
     */
    public void flushQueue() {
        Operations.completeBlindly(EditSession.this.commit());
        if (queue != null) {
            queue.enqueue();
        }
    }

    @Override
    public @Nullable Operation commit() {
        return this.bypassNone.commit();
    }

    /**
     * Count the number of blocks of a given list of types in a region.
     *
     * @param region the region
     * @param searchIDs a list of IDs to search
     * @return the number of found blocks
     */
    public int countBlock(final Region region, final Set<Integer> searchIDs) {
        final boolean[] ids = new boolean[256];
        for (final int id : searchIDs) {
            if ((id < 256) && (id > 0)) {
                ids[id] = true;
            }
        }
        return this.countBlock(region, ids);
    }

    public int countBlock(final Region region, final boolean[] ids) {
        int i = 0;
        for (final Vector pt : region) {
            final int id = this.getBlockType(pt);
            if (ids[id]) {
                i++;
            }
        }
        return i;
    }

    /**
     * Count the number of blocks of a list of types in a region.
     *
     * @param region the region
     * @param searchBlocks the list of blocks to search
     * @return the number of blocks that matched the pattern
     */
    public int countBlocks(final Region region, final Set<BaseBlock> searchBlocks) {
        final boolean[] ids = new boolean[256];
        for (final BaseBlock block : searchBlocks) {
            final int id = block.getId();
            if ((id < 256) && (id > 0)) {
                ids[id] = true;
            }
        }
        return this.countBlock(region, ids);
    }

    /**
     * Fills an area recursively in the X/Z directions.
     *
     * @param origin the location to start from
     * @param block the block to fill with
     * @param radius the radius of the spherical area to fill
     * @param depth the maximum depth, starting from the origin
     * @param recursive whether a breadth-first search should be performed
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    @SuppressWarnings("deprecation")
    public int fillXZ(final Vector origin, final BaseBlock block, final double radius, final int depth, final boolean recursive) throws MaxChangedBlocksException {
        return this.fillXZ(origin, new SingleBlockPattern(block), radius, depth, recursive);
    }

    /**
     * Fills an area recursively in the X/Z directions.
     *
     * @param origin the origin to start the fill from
     * @param pattern the pattern to fill with
     * @param radius the radius of the spherical area to fill, with 0 as the smallest radius
     * @param depth the maximum depth, starting from the origin, with 1 as the smallest depth
     * @param recursive whether a breadth-first search should be performed
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    @SuppressWarnings("deprecation")
    public int fillXZ(final Vector origin, final Pattern pattern, final double radius, final int depth, final boolean recursive) throws MaxChangedBlocksException {
        checkNotNull(origin);
        checkNotNull(pattern);
        checkArgument(radius >= 0, "radius >= 0");
        checkArgument(depth >= 1, "depth >= 1");

        final MaskIntersection mask = new MaskIntersection(new RegionMask(new EllipsoidRegion(null, origin, new Vector(radius, radius, radius))), new BoundedHeightMask(Math.max(
        (origin.getBlockY() - depth) + 1, 0), Math.min(EditSession.this.getWorld().getMaxY(), origin.getBlockY())), Masks.negate(new ExistingBlockMask(EditSession.this)));

        // Want to replace blocks
        final BlockReplace replace = new BlockReplace(EditSession.this, Patterns.wrap(pattern));

        // Pick how we're going to visit blocks
        RecursiveVisitor visitor;
        if (recursive) {
            visitor = new RecursiveVisitor(mask, replace);
        } else {
            visitor = new DownwardVisitor(mask, replace, origin.getBlockY());
        }

        // Start at the origin
        visitor.visit(origin);

        // Execute
        Operations.completeSmart(visitor, new Runnable() {
            @Override
            public void run() {
                EditSession.this.flushQueue();
            }
        }, true);
        return this.changes = visitor.getAffected();
    }

    /**
     * Remove a cuboid above the given position with a given apothem and a given height.
     *
     * @param position base position
     * @param apothem an apothem of the cuboid (on the XZ plane), where the minimum is 1
     * @param height the height of the cuboid, where the minimum is 1
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    @SuppressWarnings("deprecation")
    public int removeAbove(final Vector position, final int apothem, final int height) throws MaxChangedBlocksException {
        checkNotNull(position);
        checkArgument(apothem >= 1, "apothem >= 1");
        checkArgument(height >= 1, "height >= 1");

        final Region region = new CuboidRegion(this.getWorld(), // Causes clamping of Y range
        position.add(-apothem + 1, 0, -apothem + 1), position.add(apothem - 1, height - 1, apothem - 1));
        final Pattern pattern = new SingleBlockPattern(new BaseBlock(BlockID.AIR));
        return this.setBlocks(region, pattern);
    }

    /**
     * Remove a cuboid below the given position with a given apothem and a given height.
     *
     * @param position base position
     * @param apothem an apothem of the cuboid (on the XZ plane), where the minimum is 1
     * @param height the height of the cuboid, where the minimum is 1
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    @SuppressWarnings("deprecation")
    public int removeBelow(final Vector position, final int apothem, final int height) throws MaxChangedBlocksException {
        checkNotNull(position);
        checkArgument(apothem >= 1, "apothem >= 1");
        checkArgument(height >= 1, "height >= 1");

        final Region region = new CuboidRegion(this.getWorld(), // Causes clamping of Y range
        position.add(-apothem + 1, 0, -apothem + 1), position.add(apothem - 1, -height + 1, apothem - 1));
        final Pattern pattern = new SingleBlockPattern(new BaseBlock(BlockID.AIR));
        return this.setBlocks(region, pattern);
    }

    /**
     * Remove blocks of a certain type nearby a given position.
     *
     * @param position center position of cuboid
     * @param blockType the block type to match
     * @param apothem an apothem of the cuboid, where the minimum is 1
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    @SuppressWarnings("deprecation")
    public int removeNear(final Vector position, final int blockType, final int apothem) throws MaxChangedBlocksException {
        checkNotNull(position);
        checkArgument(apothem >= 1, "apothem >= 1");

        final Mask mask = new FuzzyBlockMask(this, new BaseBlock(blockType, -1));
        final Vector adjustment = new Vector(1, 1, 1).multiply(apothem - 1);
        final Region region = new CuboidRegion(this.getWorld(), // Causes clamping of Y range
        position.add(adjustment.multiply(-1)), position.add(adjustment));
        final Pattern pattern = new SingleBlockPattern(new BaseBlock(BlockID.AIR));
        return this.replaceBlocks(region, mask, pattern);
    }

    /**
     * Sets all the blocks inside a region to a given block type.
     *
     * @param region the region
     * @param block the block
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    @SuppressWarnings("deprecation")
    public int setBlocks(final Region region, final BaseBlock block) throws MaxChangedBlocksException {
        checkNotNull(region);
        checkNotNull(block);
        Iterator<BlockVector> iter = region.iterator();
        try {
            while (iter.hasNext()) {
                this.bypassNone.setBlock(iter.next(), block);
            }
        } catch (final MaxChangedBlocksException e) {
            throw e;
        } catch (final WorldEditException e) {
            throw new RuntimeException("Unexpected exception", e);
        }
        return changes;
    }

    /**
     * Sets all the blocks inside a region to a given pattern.
     *
     * @param region the region
     * @param pattern the pattern that provides the replacement block
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    @SuppressWarnings("deprecation")
    public int setBlocks(final Region region, final Pattern pattern) throws MaxChangedBlocksException {
        checkNotNull(region);
        checkNotNull(pattern);
        final BlockReplace replace = new BlockReplace(EditSession.this, Patterns.wrap(pattern));
        final RegionVisitor visitor = new RegionVisitor(region, replace);
        Operations.completeSmart(visitor, new Runnable() {
            @Override
            public void run() {
                EditSession.this.flushQueue();
            }
        }, true);
        return this.changes = visitor.getAffected();
    }

    /**
     * Replaces all the blocks matching a given filter, within a given region, to a block
     * returned by a given pattern.
     *
     * @param region the region to replace the blocks within
     * @param filter a list of block types to match, or null to use {@link com.sk89q.worldedit.masks.ExistingBlockMask}
     * @param replacement the replacement block
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    @SuppressWarnings("deprecation")
    public int replaceBlocks(final Region region, final Set<BaseBlock> filter, final BaseBlock replacement) throws MaxChangedBlocksException {
        return this.replaceBlocks(region, filter, new SingleBlockPattern(replacement));
    }

    /**
     * Replaces all the blocks matching a given filter, within a given region, to a block
     * returned by a given pattern.
     *
     * @param region the region to replace the blocks within
     * @param filter a list of block types to match, or null to use {@link com.sk89q.worldedit.masks.ExistingBlockMask}
     * @param pattern the pattern that provides the new blocks
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    @SuppressWarnings("deprecation")
    public int replaceBlocks(final Region region, final Set<BaseBlock> filter, final Pattern pattern) throws MaxChangedBlocksException {
        final Mask mask = filter == null ? new ExistingBlockMask(this) : new FuzzyBlockMask(this, filter);
        return this.replaceBlocks(region, mask, pattern);
    }

    /**
     * Replaces all the blocks matching a given mask, within a given region, to a block
     * returned by a given pattern.
     *
     * @param region the region to replace the blocks within
     * @param mask the mask that blocks must match
     * @param pattern the pattern that provides the new blocks
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    @SuppressWarnings("deprecation")
    public int replaceBlocks(final Region region, final Mask mask, final Pattern pattern) throws MaxChangedBlocksException {
        checkNotNull(region);
        checkNotNull(mask);
        checkNotNull(pattern);
        final BlockReplace replace = new BlockReplace(EditSession.this, Patterns.wrap(pattern));
        final RegionMaskingFilter filter = new RegionMaskingFilter(mask, replace);
        final RegionVisitor visitor = new RegionVisitor(region, filter);
        Operations.completeSmart(visitor, new Runnable() {
            @Override
            public void run() {
                EditSession.this.flushQueue();
            }
        }, true);
        return this.changes = visitor.getAffected();
    }

    /**
     * Sets the blocks at the center of the given region to the given pattern.
     * If the center sits between two blocks on a certain axis, then two blocks
     * will be placed to mark the center.
     *
     * @param region the region to find the center of
     * @param pattern the replacement pattern
     * @return the number of blocks placed
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    @SuppressWarnings("deprecation")
    public int center(final Region region, final Pattern pattern) throws MaxChangedBlocksException {
        checkNotNull(region);
        checkNotNull(pattern);

        final Vector center = region.getCenter();
        final Region centerRegion = new CuboidRegion(this.getWorld(), // Causes clamping of Y range
                center.floor(), center.ceil());
        return this.setBlocks(centerRegion, pattern);
    }

    /**
     * Make the faces of the given region as if it was a {@link CuboidRegion}.
     *
     * @param region the region
     * @param block the block to place
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    @SuppressWarnings("deprecation")
    public int makeCuboidFaces(final Region region, final BaseBlock block) throws MaxChangedBlocksException {
        return this.makeCuboidFaces(region, new SingleBlockPattern(block));
    }

    /**
     * Make the faces of the given region as if it was a {@link CuboidRegion}.
     *
     * @param region the region
     * @param pattern the pattern to place
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    @SuppressWarnings("deprecation")
    public int makeCuboidFaces(final Region region, final Pattern pattern) throws MaxChangedBlocksException {
        checkNotNull(region);
        checkNotNull(pattern);

        final CuboidRegion cuboid = CuboidRegion.makeCuboid(region);
        final Region faces = cuboid.getFaces();
        return this.setBlocks(faces, pattern);
    }

    /**
     * Make the faces of the given region. The method by which the faces are found
     * may be inefficient, because there may not be an efficient implementation supported
     * for that specific shape.
     *
     * @param region the region
     * @param pattern the pattern to place
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    @SuppressWarnings("deprecation")
    public int makeFaces(final Region region, final Pattern pattern) throws MaxChangedBlocksException {
        checkNotNull(region);
        checkNotNull(pattern);

        if (region instanceof CuboidRegion) {
            return this.makeCuboidFaces(region, pattern);
        } else {
            return new RegionShape(region).generate(this, pattern, true);
        }
    }

    /**
     * Make the walls (all faces but those parallel to the X-Z plane) of the given region
     * as if it was a {@link CuboidRegion}.
     *
     * @param region the region
     * @param block the block to place
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    @SuppressWarnings("deprecation")
    public int makeCuboidWalls(final Region region, final BaseBlock block) throws MaxChangedBlocksException {
        return this.makeCuboidWalls(region, new SingleBlockPattern(block));
    }

    /**
     * Make the walls (all faces but those parallel to the X-Z plane) of the given region
     * as if it was a {@link CuboidRegion}.
     *
     * @param region the region
     * @param pattern the pattern to place
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    @SuppressWarnings("deprecation")
    public int makeCuboidWalls(final Region region, final Pattern pattern) throws MaxChangedBlocksException {
        checkNotNull(region);
        checkNotNull(pattern);

        final CuboidRegion cuboid = CuboidRegion.makeCuboid(region);
        final Region faces = cuboid.getWalls();
        return this.setBlocks(faces, pattern);
    }

    /**
     * Make the walls of the given region. The method by which the walls are found
     * may be inefficient, because there may not be an efficient implementation supported
     * for that specific shape.
     *
     * @param region the region
     * @param pattern the pattern to place
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    @SuppressWarnings("deprecation")
    public int makeWalls(final Region region, final Pattern pattern) throws MaxChangedBlocksException {
        checkNotNull(region);
        checkNotNull(pattern);

        if (region instanceof CuboidRegion) {
            return this.makeCuboidWalls(region, pattern);
        } else {
            final int minY = region.getMinimumPoint().getBlockY();
            final int maxY = region.getMaximumPoint().getBlockY();
            final ArbitraryShape shape = new RegionShape(region) {
                @Override
                protected BaseBlock getMaterial(final int x, final int y, final int z, final BaseBlock defaultMaterial) {
                    if ((y > maxY) || (y < minY)) {
                        // Put holes into the floor and ceiling by telling ArbitraryShape that the shape goes on outside the region
                        return defaultMaterial;
                    }

                    return super.getMaterial(x, y, z, defaultMaterial);
                }
            };
            return shape.generate(this, pattern, true);
        }
    }

    /**
     * Places a layer of blocks on top of ground blocks in the given region
     * (as if it were a cuboid).
     *
     * @param region the region
     * @param block the placed block
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    @SuppressWarnings("deprecation")
    public int overlayCuboidBlocks(final Region region, final BaseBlock block) throws MaxChangedBlocksException {
        checkNotNull(block);

        return this.overlayCuboidBlocks(region, new SingleBlockPattern(block));
    }

    /**
     * Places a layer of blocks on top of ground blocks in the given region
     * (as if it were a cuboid).
     *
     * @param region the region
     * @param pattern the placed block pattern
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    @SuppressWarnings("deprecation")
    public int overlayCuboidBlocks(final Region region, final Pattern pattern) throws MaxChangedBlocksException {
        checkNotNull(region);
        checkNotNull(pattern);
        final BlockReplace replace = new BlockReplace(EditSession.this, Patterns.wrap(pattern));
        final RegionOffset offset = new RegionOffset(new Vector(0, 1, 0), replace);
        final GroundFunction ground = new GroundFunction(new ExistingBlockMask(EditSession.this), offset);
        final LayerVisitor visitor = new LayerVisitor(asFlatRegion(region), minimumBlockY(region), maximumBlockY(region), ground);
        Operations.completeSmart(visitor, new Runnable() {
            @Override
            public void run() {
                EditSession.this.flushQueue();
            }
        }, true);
        return this.changes = ground.getAffected();
    }

    /**
     * Turns the first 3 layers into dirt/grass and the bottom layers
     * into rock, like a natural Minecraft mountain.
     *
     * @param region the region to affect
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int naturalizeCuboidBlocks(final Region region) throws MaxChangedBlocksException {
        checkNotNull(region);
        final Naturalizer naturalizer = new Naturalizer(EditSession.this);
        final FlatRegion flatRegion = Regions.asFlatRegion(region);
        final LayerVisitor visitor = new LayerVisitor(flatRegion, minimumBlockY(region), maximumBlockY(region), naturalizer);
        Operations.completeSmart(visitor, new Runnable() {
            @Override
            public void run() {
                EditSession.this.flushQueue();
            }
        }, true);
        return this.changes = naturalizer.getAffected();
    }

    /**
     * Stack a cuboid region.
     *
     * @param region the region to stack
     * @param dir the direction to stack
     * @param count the number of times to stack
     * @param copyAir true to also copy air blocks
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int stackCuboidRegion(final Region region, final Vector dir, final int count, final boolean copyAir) throws MaxChangedBlocksException {
        checkNotNull(region);
        checkNotNull(dir);
        checkArgument(count >= 1, "count >= 1 required");
        final Vector size = region.getMaximumPoint().subtract(region.getMinimumPoint()).add(1, 1, 1);
        final Vector to = region.getMinimumPoint();
        final ForwardExtentCopy copy = new ForwardExtentCopy(EditSession.this, region, EditSession.this, to);
        copy.setRepetitions(count);
        copy.setTransform(new AffineTransform().translate(dir.multiply(size)));
        if (!copyAir) {
            copy.setSourceMask(new ExistingBlockMask(EditSession.this));
        }
        Operations.completeSmart(copy, new Runnable() {
            @Override
            public void run() {
                EditSession.this.flushQueue();
            }
        }, true);
        return this.changes = copy.getAffected();
    }

    /**
     * Move the blocks in a region a certain direction.
     *
     * @param region the region to move
     * @param dir the direction
     * @param distance the distance to move
     * @param copyAir true to copy air blocks
     * @param replacement the replacement block to fill in after moving, or null to use air
     * @return number of blocks moved
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int moveRegion(final Region region, final Vector dir, final int distance, final boolean copyAir, final BaseBlock replacement) throws MaxChangedBlocksException {
        checkNotNull(region);
        checkNotNull(dir);
        checkArgument(distance >= 1, "distance >= 1 required");
        final Vector to = region.getMinimumPoint();

        // Remove the original blocks
        final com.sk89q.worldedit.function.pattern.Pattern pattern = replacement != null ? new BlockPattern(replacement) : new BlockPattern(new BaseBlock(BlockID.AIR));
        final BlockReplace remove = new BlockReplace(EditSession.this, pattern);

        // Copy to a buffer so we don't destroy our original before we can copy all the blocks from it
        final ForgetfulExtentBuffer buffer = new ForgetfulExtentBuffer(EditSession.this, new RegionMask(region));
        final ForwardExtentCopy copy = new ForwardExtentCopy(EditSession.this, region, buffer, to);
        copy.setTransform(new AffineTransform().translate(dir.multiply(distance)));
        copy.setSourceFunction(remove); // Remove
        copy.setRemovingEntities(true);
        if (!copyAir) {
            copy.setSourceMask(new ExistingBlockMask(EditSession.this));
        }

        // Then we need to copy the buffer to the world
        final BlockReplace replace = new BlockReplace(EditSession.this, buffer);
        final RegionVisitor visitor = new RegionVisitor(buffer.asRegion(), replace);

        final OperationQueue operation = new OperationQueue(copy, visitor);
        Operations.completeSmart(operation, new Runnable() {
            @Override
            public void run() {
                EditSession.this.flushQueue();
            }
        }, true);
        return this.changes = copy.getAffected();
    }

    /**
     * Move the blocks in a region a certain direction.
     *
     * @param region the region to move
     * @param dir the direction
     * @param distance the distance to move
     * @param copyAir true to copy air blocks
     * @param replacement the replacement block to fill in after moving, or null to use air
     * @return number of blocks moved
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int moveCuboidRegion(final Region region, final Vector dir, final int distance, final boolean copyAir, final BaseBlock replacement) throws MaxChangedBlocksException {
        return this.moveRegion(region, dir, distance, copyAir, replacement);
    }

    /**
     * Drain nearby pools of water or lava.
     *
     * @param origin the origin to drain from, which will search a 3x3 area
     * @param radius the radius of the removal, where a value should be 0 or greater
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int drainArea(final Vector origin, final double radius) throws MaxChangedBlocksException {
        checkNotNull(origin);
        checkArgument(radius >= 0, "radius >= 0 required");
        final MaskIntersection mask = new MaskIntersection(new BoundedHeightMask(0, EditSession.this.getWorld().getMaxY()), new RegionMask(new EllipsoidRegion(null, origin, new Vector(radius,
        radius, radius))), EditSession.this.getWorld().createLiquidMask());

        final BlockReplace replace = new BlockReplace(EditSession.this, new BlockPattern(new BaseBlock(BlockID.AIR)));
        final RecursiveVisitor visitor = new RecursiveVisitor(mask, replace);

        // Around the origin in a 3x3 block
        for (final BlockVector position : CuboidRegion.fromCenter(origin, 1)) {
            if (mask.test(position)) {
                visitor.visit(position);
            }
        }

        Operations.completeSmart(visitor, new Runnable() {
            @Override
            public void run() {
                EditSession.this.flushQueue();
            }
        }, true);
        return this.changes = visitor.getAffected();
    }

    /**
     * Fix liquids so that they turn into stationary blocks and extend outward.
     *
     * @param origin the original position
     * @param radius the radius to fix
     * @param moving the block ID of the moving liquid
     * @param stationary the block ID of the stationary liquid
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int fixLiquid(final Vector origin, final double radius, final int moving, final int stationary) throws MaxChangedBlocksException {
        checkNotNull(origin);
        checkArgument(radius >= 0, "radius >= 0 required");

        // Our origins can only be liquids
        BlockMask liquidMask = new BlockMask(
                this,
                FaweCache.getBlock(moving, 0),
                FaweCache.getBlock(stationary, 0));

        // But we will also visit air blocks
        MaskIntersection blockMask =
                new MaskUnion(liquidMask,
                        new BlockMask(
                                this,
                                new BaseBlock(BlockID.AIR)));

        // There are boundaries that the routine needs to stay in
        MaskIntersection mask = new MaskIntersection(
                new BoundedHeightMask(0, Math.min(origin.getBlockY(), getWorld().getMaxY())),
                new RegionMask(new EllipsoidRegion(null, origin, new Vector(radius, radius, radius))),
                blockMask);

        BlockReplace replace = new BlockReplace(this, new BlockPattern(FaweCache.getBlock(stationary, 0)));
        NonRisingVisitor visitor = new NonRisingVisitor(mask, replace);

        // Around the origin in a 3x3 block
        for (BlockVector position : CuboidRegion.fromCenter(origin, 1)) {
            if (liquidMask.test(position)) {
                visitor.visit(position);
            }
        }

        Operations.completeSmart(visitor, new Runnable() {
            @Override
            public void run() {
                EditSession.this.flushQueue();
            }
        }, true);
        return visitor.getAffected();
    }

    /**
     * Makes a cylinder.
     *
     * @param pos Center of the cylinder
     * @param block The block pattern to use
     * @param radius The cylinder's radius
     * @param height The cylinder's up/down extent. If negative, extend downward.
     * @param filled If false, only a shell will be generated.
     * @return number of blocks changed
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int makeCylinder(final Vector pos, final Pattern block, final double radius, final int height, final boolean filled) throws MaxChangedBlocksException {
        return this.makeCylinder(pos, block, radius, radius, height, filled);
    }

    /**
     * Makes a cylinder.
     *
     * @param pos Center of the cylinder
     * @param block The block pattern to use
     * @param radiusX The cylinder's largest north/south extent
     * @param radiusZ The cylinder's largest east/west extent
     * @param height The cylinder's up/down extent. If negative, extend downward.
     * @param filled If false, only a shell will be generated.
     * @return number of blocks changed
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int makeCylinder(Vector pos, final Pattern block, double radiusX, double radiusZ, int height, final boolean filled) throws MaxChangedBlocksException {
        radiusX += 0.5;
        radiusZ += 0.5;

        if (height == 0) {
            return this.changes;
        } else if (height < 0) {
            height = -height;
            pos = pos.subtract(0, height, 0);
        }

        if (pos.getBlockY() < 0) {
            pos = pos.setY(0);
        } else if (((pos.getBlockY() + height) - 1) > this.world.getMaxY()) {
            height = (this.world.getMaxY() - pos.getBlockY()) + 1;
        }

        final double invRadiusX = 1 / radiusX;
        final double invRadiusZ = 1 / radiusZ;

        final int ceilRadiusX = (int) Math.ceil(radiusX);
        final int ceilRadiusZ = (int) Math.ceil(radiusZ);

        double nextXn = 0;
        forX: for (int x = 0; x <= ceilRadiusX; ++x) {
            final double xn = nextXn;
            nextXn = (x + 1) * invRadiusX;
            double nextZn = 0;
            forZ: for (int z = 0; z <= ceilRadiusZ; ++z) {
                final double zn = nextZn;
                nextZn = (z + 1) * invRadiusZ;

                final double distanceSq = this.lengthSq(xn, zn);
                if (distanceSq > 1) {
                    if (z == 0) {
                        break forX;
                    }
                    break forZ;
                }

                if (!filled) {
                    if ((this.lengthSq(nextXn, zn) <= 1) && (this.lengthSq(xn, nextZn) <= 1)) {
                        continue;
                    }
                }

                for (int y = 0; y < height; ++y) {
                    this.setBlock(pos.add(x, y, z), block);
                    this.setBlock(pos.add(-x, y, z), block);
                    this.setBlock(pos.add(x, y, -z), block);
                    this.setBlock(pos.add(-x, y, -z), block);
                }
            }
        }

        return this.changes;
    }

    /**
     * Makes a sphere.
     *
     * @param pos Center of the sphere or ellipsoid
     * @param block The block pattern to use
     * @param radius The sphere's radius
     * @param filled If false, only a shell will be generated.
     * @return number of blocks changed
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int makeSphere(final Vector pos, final Pattern block, final double radius, final boolean filled) throws MaxChangedBlocksException {
        return this.makeSphere(pos, block, radius, radius, radius, filled);
    }

    /**
     * Makes a sphere or ellipsoid.
     *
     * @param pos Center of the sphere or ellipsoid
     * @param block The block pattern to use
     * @param radiusX The sphere/ellipsoid's largest north/south extent
     * @param radiusY The sphere/ellipsoid's largest up/down extent
     * @param radiusZ The sphere/ellipsoid's largest east/west extent
     * @param filled If false, only a shell will be generated.
     * @return number of blocks changed
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int makeSphere(final Vector pos, final Pattern block, double radiusX, double radiusY, double radiusZ, final boolean filled) throws MaxChangedBlocksException {
        radiusX += 0.5;
        radiusY += 0.5;
        radiusZ += 0.5;

        final double invRadiusX = 1 / radiusX;
        final double invRadiusY = 1 / radiusY;
        final double invRadiusZ = 1 / radiusZ;

        final int ceilRadiusX = (int) Math.ceil(radiusX);
        final int ceilRadiusY = (int) Math.ceil(radiusY);
        final int ceilRadiusZ = (int) Math.ceil(radiusZ);

        double nextXn = 0;
        forX: for (int x = 0; x <= ceilRadiusX; ++x) {
            final double xn = nextXn;
            nextXn = (x + 1) * invRadiusX;
            double nextYn = 0;
            forY: for (int y = 0; y <= ceilRadiusY; ++y) {
                final double yn = nextYn;
                nextYn = (y + 1) * invRadiusY;
                double nextZn = 0;
                forZ: for (int z = 0; z <= ceilRadiusZ; ++z) {
                    final double zn = nextZn;
                    nextZn = (z + 1) * invRadiusZ;

                    final double distanceSq = this.lengthSq(xn, yn, zn);
                    if (distanceSq > 1) {
                        if (z == 0) {
                            if (y == 0) {
                                break forX;
                            }
                            break forY;
                        }
                        break forZ;
                    }

                    if (!filled) {
                        if ((this.lengthSq(nextXn, yn, zn) <= 1) && (this.lengthSq(xn, nextYn, zn) <= 1) && (this.lengthSq(xn, yn, nextZn) <= 1)) {
                            continue;
                        }
                    }

                    this.setBlock(pos.add(x, y, z), block);
                    this.setBlock(pos.add(-x, y, z), block);
                    this.setBlock(pos.add(x, -y, z), block);
                    this.setBlock(pos.add(x, y, -z), block);
                    this.setBlock(pos.add(-x, -y, z), block);
                    this.setBlock(pos.add(x, -y, -z), block);
                    this.setBlock(pos.add(-x, y, -z), block);
                    this.setBlock(pos.add(-x, -y, -z), block);
                }
            }
        }

        return changes;
    }

    /**
     * Makes a pyramid.
     *
     * @param position a position
     * @param block a block
     * @param size size of pyramid
     * @param filled true if filled
     * @return number of blocks changed
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int makePyramid(final Vector position, final Pattern block, int size, final boolean filled) throws MaxChangedBlocksException {
        final int height = size;

        for (int y = 0; y <= height; ++y) {
            size--;
            for (int x = 0; x <= size; ++x) {
                for (int z = 0; z <= size; ++z) {
                    if ((filled && (z <= size) && (x <= size)) || (z == size) || (x == size)) {
                        this.setBlock(position.add(x, y, z), block);
                        this.setBlock(position.add(-x, y, z), block);
                        this.setBlock(position.add(x, y, -z), block);
                        this.setBlock(position.add(-x, y, -z), block);
                    }
                }
            }
        }

        return changes;
    }

    /**
     * Thaw blocks in a radius.
     *
     * @param position the position
     * @param radius the radius
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int thaw(final Vector position, final double radius) throws MaxChangedBlocksException {
        final double radiusSq = radius * radius;

        final int ox = position.getBlockX();
        final int oy = position.getBlockY();
        final int oz = position.getBlockZ();

        final BaseBlock air = new BaseBlock(0);
        final BaseBlock water = new BaseBlock(BlockID.STATIONARY_WATER);

        final int ceilRadius = (int) Math.ceil(radius);
        for (int x = ox - ceilRadius; x <= (ox + ceilRadius); ++x) {
            for (int z = oz - ceilRadius; z <= (oz + ceilRadius); ++z) {
                if ((new Vector(x, oy, z)).distanceSq(position) > radiusSq) {
                    continue;
                }

                for (int y = this.world.getMaxY(); y >= 1; --y) {
                    final Vector pt = new Vector(x, y, z);
                    final int id = this.getBlockType(pt);

                    switch (id) {
                        case BlockID.ICE:
                            this.setBlock(pt, water);
                            break;

                        case BlockID.SNOW:
                            this.setBlock(pt, air);
                            break;

                        case BlockID.AIR:
                            continue;

                        default:
                            break;
                    }

                    break;
                }
            }
        }

        return changes;
    }

    /**
     * Make snow in a radius.
     *
     * @param position a position
     * @param radius a radius
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int simulateSnow(final Vector position, final double radius) throws MaxChangedBlocksException {
        
        final double radiusSq = radius * radius;

        final int ox = position.getBlockX();
        final int oy = position.getBlockY();
        final int oz = position.getBlockZ();

        final BaseBlock ice = new BaseBlock(BlockID.ICE);
        final BaseBlock snow = new BaseBlock(BlockID.SNOW);

        final int ceilRadius = (int) Math.ceil(radius);
        for (int x = ox - ceilRadius; x <= (ox + ceilRadius); ++x) {
            for (int z = oz - ceilRadius; z <= (oz + ceilRadius); ++z) {
                if ((new Vector(x, oy, z)).distanceSq(position) > radiusSq) {
                    continue;
                }

                for (int y = this.world.getMaxY(); y >= 1; --y) {
                    final Vector pt = new Vector(x, y, z);
                    final int id = this.getBlockType(pt);

                    if (id == BlockID.AIR) {
                        continue;
                    }

                    // Ice!
                    if ((id == BlockID.WATER) || (id == BlockID.STATIONARY_WATER)) {
                        this.setBlock(pt, ice);
                        break;
                    }

                    // Snow should not cover these blocks
                    if (BlockType.isTranslucent(id)) {
                        break;
                    }

                    // Too high?
                    if (y == this.world.getMaxY()) {
                        break;
                    }

                    // add snow cover
                    this.setBlock(pt.add(0, 1, 0), snow);
                    break;
                }
            }
        }

        return changes;
    }

    /**
     * Make dirt green.
     *
     * @param position a position
     * @param radius a radius
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     * @deprecated Use {@link #green(Vector, double, boolean)}.
     */
    @Deprecated
    public int green(final Vector position, final double radius) throws MaxChangedBlocksException {
        return this.green(position, radius, true);
    }

    /**
     * Make dirt green.
     *
     * @param position a position
     * @param radius a radius
     * @param onlyNormalDirt only affect normal dirt (data value 0)
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int green(final Vector position, final double radius, final boolean onlyNormalDirt) throws MaxChangedBlocksException {
        
        final double radiusSq = radius * radius;

        final int ox = position.getBlockX();
        final int oy = position.getBlockY();
        final int oz = position.getBlockZ();

        final BaseBlock grass = new BaseBlock(BlockID.GRASS);

        final int ceilRadius = (int) Math.ceil(radius);
        for (int x = ox - ceilRadius; x <= (ox + ceilRadius); ++x) {
            for (int z = oz - ceilRadius; z <= (oz + ceilRadius); ++z) {
                if ((new Vector(x, oy, z)).distanceSq(position) > radiusSq) {
                    continue;
                }

                loop: for (int y = this.world.getMaxY(); y >= 1; --y) {
                    final Vector pt = new Vector(x, y, z);
                    final int id = this.getBlockType(pt);
                    final int data = this.getBlockData(pt);

                    switch (id) {
                        case BlockID.DIRT:
                            if (onlyNormalDirt && (data != 0)) {
                                break loop;
                            }
                            this.setBlock(pt, grass);
                            break loop;

                        case BlockID.WATER:
                        case BlockID.STATIONARY_WATER:
                        case BlockID.LAVA:
                        case BlockID.STATIONARY_LAVA:
                            // break on liquids...
                            break loop;

                        default:
                            // ...and all non-passable blocks
                            if (!BlockType.canPassThrough(id, data)) {
                                break loop;
                            }
                    }
                }
            }
        }

        return changes;
    }

    /**
     * Makes pumpkin patches randomly in an area around the given position.
     *
     * @param position the base position
     * @param apothem the apothem of the (square) area
     * @return number of patches created
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int makePumpkinPatches(final Vector position, final int apothem) throws MaxChangedBlocksException {
        // We want to generate pumpkins
        final GardenPatchGenerator generator = new GardenPatchGenerator(EditSession.this);
        generator.setPlant(GardenPatchGenerator.getPumpkinPattern());

        // In a region of the given radius
        final FlatRegion region = new CuboidRegion(EditSession.this.getWorld(), // Causes clamping of Y range
        position.add(-apothem, -5, -apothem), position.add(apothem, 10, apothem));
        final double density = 0.02;

        final GroundFunction ground = new GroundFunction(new ExistingBlockMask(EditSession.this), generator);
        final LayerVisitor visitor = new LayerVisitor(region, minimumBlockY(region), maximumBlockY(region), ground);
        visitor.setMask(new NoiseFilter2D(new RandomNoise(), density));
        Operations.completeSmart(visitor, new Runnable() {
            @Override
            public void run() {
                EditSession.this.flushQueue();
            }
        }, true);
        return this.changes = ground.getAffected();
    }

    /**
     * Makes a forest.
     *
     * @param basePosition a position
     * @param size a size
     * @param density between 0 and 1, inclusive
     * @param treeGenerator the tree genreator
     * @return number of trees created
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int makeForest(final Vector basePosition, final int size, final double density, final TreeGenerator treeGenerator) {
        final ArrayDeque<Vector> trees = new ArrayDeque<>();
        for (int x = basePosition.getBlockX() - size; x <= (basePosition.getBlockX() + size); ++x) {
            for (int z = basePosition.getBlockZ() - size; z <= (basePosition.getBlockZ() + size); ++z) {
                // Don't want to be in the ground
                if (!this.getLazyBlock(x, basePosition.getBlockY(), z).isAir()) {
                    continue;
                }
                // The gods don't want a tree here
                if (FaweCache.RANDOM.random(65536) >= (density * 65536)) {
                    continue;
                } // def 0.05
                trees.add(new Vector(x, 0, z));
            }
        }
        if (trees.size() > 0) {
            TaskManager.IMP.objectTask(trees, new RunnableVal<Vector>() {
                @Override
                public void run(Vector vector) {
                    try {
                        for (int y = basePosition.getBlockY(); y >= (basePosition.getBlockY() - 10); --y) {
                            vector = new Vector(vector.getX(), y, vector.getZ());
                            final int t = getBlock(vector).getType();
                            if ((t == BlockID.GRASS) || (t == BlockID.DIRT)) {
                                treeGenerator.generate(EditSession.this, new Vector(vector.getX(), y + 1, vector.getZ()));
                                break;
                            } else if (t == BlockID.SNOW) {
                                setBlock(vector, nullBlock);
                            } else if (t != BlockID.AIR) { // Trees won't grow on this!
                                break;
                            }
                        }
                    } catch (MaxChangedBlocksException ignore) {
                    }
                }
            }, new Runnable() {
                @Override
                public void run() {
                    queue.enqueue();
                }
            });
        }
        return this.changes = trees.size();
    }

    /**
     * Get the block distribution inside a region.
     *
     * @param region a region
     * @return the results
     */
    public List<Countable<Integer>> getBlockDistribution(final Region region) {
        final List<Countable<Integer>> distribution = new ArrayList<Countable<Integer>>();
        final Map<Integer, Countable<Integer>> map = new HashMap<Integer, Countable<Integer>>();

        if (region instanceof CuboidRegion) {
            // Doing this for speed
            final Vector min = region.getMinimumPoint();
            final Vector max = region.getMaximumPoint();

            final int minX = min.getBlockX();
            final int minY = min.getBlockY();
            final int minZ = min.getBlockZ();
            final int maxX = max.getBlockX();
            final int maxY = max.getBlockY();
            final int maxZ = max.getBlockZ();

            for (int x = minX; x <= maxX; ++x) {
                for (int y = minY; y <= maxY; ++y) {
                    for (int z = minZ; z <= maxZ; ++z) {
                        final Vector pt = new Vector(x, y, z);

                        final int id = this.getBlockType(pt);

                        if (map.containsKey(id)) {
                            map.get(id).increment();
                        } else {
                            final Countable<Integer> c = new Countable<Integer>(id, 1);
                            map.put(id, c);
                            distribution.add(c);
                        }
                    }
                }
            }
        } else {
            for (final Vector pt : region) {
                final int id = this.getBlockType(pt);

                if (map.containsKey(id)) {
                    map.get(id).increment();
                } else {
                    final Countable<Integer> c = new Countable<Integer>(id, 1);
                    map.put(id, c);
                }
            }
        }

        Collections.sort(distribution);
        // Collections.reverse(distribution);

        return distribution;
    }

    /**
     * Get the block distribution (with data values) inside a region.
     *
     * @param region a region
     * @return the results
     */
    public List<Countable<BaseBlock>> getBlockDistributionWithData(final Region region) {
        final List<Countable<BaseBlock>> distribution = new ArrayList<Countable<BaseBlock>>();
        final Map<BaseBlock, Countable<BaseBlock>> map = new HashMap<BaseBlock, Countable<BaseBlock>>();

        if (region instanceof CuboidRegion) {
            // Doing this for speed
            final Vector min = region.getMinimumPoint();
            final Vector max = region.getMaximumPoint();

            final int minX = min.getBlockX();
            final int minY = min.getBlockY();
            final int minZ = min.getBlockZ();
            final int maxX = max.getBlockX();
            final int maxY = max.getBlockY();
            final int maxZ = max.getBlockZ();

            for (int x = minX; x <= maxX; ++x) {
                for (int y = minY; y <= maxY; ++y) {
                    for (int z = minZ; z <= maxZ; ++z) {
                        final Vector pt = new Vector(x, y, z);

                        final BaseBlock blk = FaweCache.getBlock(this.getBlockType(pt), this.getBlockData(pt));

                        if (map.containsKey(blk)) {
                            map.get(blk).increment();
                        } else {
                            final Countable<BaseBlock> c = new Countable<BaseBlock>(blk, 1);
                            map.put(blk, c);
                            distribution.add(c);
                        }
                    }
                }
            }
        } else {
            for (final Vector pt : region) {
                final BaseBlock blk = FaweCache.getBlock(this.getBlockType(pt), this.getBlockData(pt));

                if (map.containsKey(blk)) {
                    map.get(blk).increment();
                } else {
                    final Countable<BaseBlock> c = new Countable<BaseBlock>(blk, 1);
                    map.put(blk, c);
                }
            }
        }

        Collections.sort(distribution);
        // Collections.reverse(distribution);

        return distribution;
    }

    public int makeShape(final Region region, final Vector zero, final Vector unit, final Pattern pattern, final String expressionString, final boolean hollow) throws ExpressionException,
    MaxChangedBlocksException {
        final Expression expression = Expression.compile(expressionString, "x", "y", "z", "type", "data");
        expression.optimize();

        final RValue typeVariable = expression.getVariable("type", false);
        final RValue dataVariable = expression.getVariable("data", false);

        final WorldEditExpressionEnvironment environment = new WorldEditExpressionEnvironment(this, unit, zero);
        expression.setEnvironment(environment);

        final ArbitraryShape shape = new ArbitraryShape(region) {
            @Override
            protected BaseBlock getMaterial(final int x, final int y, final int z, final BaseBlock defaultMaterial) {
                final Vector current = new Vector(x, y, z);
                environment.setCurrentBlock(current);
                final Vector scaled = current.subtract(zero).divide(unit);

                try {
                    if (expression.evaluate(scaled.getX(), scaled.getY(), scaled.getZ(), defaultMaterial.getType(), defaultMaterial.getData()) <= 0) {
                        return null;
                    }

                    return FaweCache.getBlock((int) typeVariable.getValue(), (int) dataVariable.getValue());
                } catch (final Exception e) {
                    EditSession.this.log.log(Level.WARNING, "Failed to create shape", e);
                    return null;
                }
            }
        };

        return shape.generate(this, pattern, hollow);
    }

    public int deformRegion(final Region region, final Vector zero, final Vector unit, final String expressionString) throws ExpressionException, MaxChangedBlocksException {
        final Expression expression = Expression.compile(expressionString, "x", "y", "z");
        expression.optimize();
        final RValue x = expression.getVariable("x", false);
        final RValue y = expression.getVariable("y", false);
        final RValue z = expression.getVariable("z", false);
        final WorldEditExpressionEnvironment environment = new WorldEditExpressionEnvironment(this, unit, zero);
        expression.setEnvironment(environment);
        
        for (BlockVector position : region) {
            // offset, scale
            final Vector scaled = position.subtract(zero).divide(unit);
            // transform
            expression.evaluate(scaled.getX(), scaled.getY(), scaled.getZ());
            final BlockVector sourcePosition = environment.toWorld(x.getValue(), y.getValue(), z.getValue());
            // read block from world
            BaseBlock material = FaweCache.CACHE_BLOCK[this.queue.getCombinedId4DataDebug(sourcePosition.getBlockX(), sourcePosition.getBlockY(), sourcePosition.getBlockZ(), 0, this)];
            // queue operation
            this.setBlock(position, material);
        }
        return changes;
    }

    /**
     * Hollows out the region (Semi-well-defined for non-cuboid selections).
     *
     * @param region the region to hollow out.
     * @param thickness the thickness of the shell to leave (manhattan distance)
     * @param pattern The block pattern to use
     *
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int hollowOutRegion(final Region region, final int thickness, final Pattern pattern) throws MaxChangedBlocksException {
        

        final Set<BlockVector> outside = new HashSet<BlockVector>();

        final Vector min = region.getMinimumPoint();
        final Vector max = region.getMaximumPoint();

        final int minX = min.getBlockX();
        final int minY = min.getBlockY();
        final int minZ = min.getBlockZ();
        final int maxX = max.getBlockX();
        final int maxY = max.getBlockY();
        final int maxZ = max.getBlockZ();

        for (int x = minX; x <= maxX; ++x) {
            for (int y = minY; y <= maxY; ++y) {
                this.recurseHollow(region, new BlockVector(x, y, minZ), outside);
                this.recurseHollow(region, new BlockVector(x, y, maxZ), outside);
            }
        }

        for (int y = minY; y <= maxY; ++y) {
            for (int z = minZ; z <= maxZ; ++z) {
                this.recurseHollow(region, new BlockVector(minX, y, z), outside);
                this.recurseHollow(region, new BlockVector(maxX, y, z), outside);
            }
        }

        for (int z = minZ; z <= maxZ; ++z) {
            for (int x = minX; x <= maxX; ++x) {
                this.recurseHollow(region, new BlockVector(x, minY, z), outside);
                this.recurseHollow(region, new BlockVector(x, maxY, z), outside);
            }
        }

        for (int i = 1; i < thickness; ++i) {
            final Set<BlockVector> newOutside = new HashSet<BlockVector>();
            outer: for (final BlockVector position : region) {
                for (final Vector recurseDirection : this.recurseDirections) {
                    final BlockVector neighbor = position.add(recurseDirection).toBlockVector();

                    if (outside.contains(neighbor)) {
                        newOutside.add(position);
                        continue outer;
                    }
                }
            }
            outside.addAll(newOutside);
        }

        outer: for (final BlockVector position : region) {
            for (final Vector recurseDirection : this.recurseDirections) {
                final BlockVector neighbor = position.add(recurseDirection).toBlockVector();

                if (outside.contains(neighbor)) {
                    continue outer;
                }
            }
            this.setBlock(position, pattern.next(position));
        }

        return changes;
    }

    /**
     * Draws a line (out of blocks) between two vectors.
     *
     * @param pattern The block pattern used to draw the line.
     * @param pos1 One of the points that define the line.
     * @param pos2 The other point that defines the line.
     * @param radius The radius (thickness) of the line.
     * @param filled If false, only a shell will be generated.
     *
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int drawLine(final Pattern pattern, final Vector pos1, final Vector pos2, final double radius, final boolean filled) throws MaxChangedBlocksException {

        Set<Vector> vset = new HashSet<Vector>();
        boolean notdrawn = true;

        final int x1 = pos1.getBlockX(), y1 = pos1.getBlockY(), z1 = pos1.getBlockZ();
        final int x2 = pos2.getBlockX(), y2 = pos2.getBlockY(), z2 = pos2.getBlockZ();
        int tipx = x1, tipy = y1, tipz = z1;
        final int dx = Math.abs(x2 - x1), dy = Math.abs(y2 - y1), dz = Math.abs(z2 - z1);

        if ((dx + dy + dz) == 0) {
            vset.add(new Vector(tipx, tipy, tipz));
            notdrawn = false;
        }

        if ((Math.max(Math.max(dx, dy), dz) == dx) && notdrawn) {
            for (int domstep = 0; domstep <= dx; domstep++) {
                tipx = x1 + (domstep * ((x2 - x1) > 0 ? 1 : -1));
                tipy = (int) Math.round(y1 + (((domstep * ((double) dy)) / (dx)) * ((y2 - y1) > 0 ? 1 : -1)));
                tipz = (int) Math.round(z1 + (((domstep * ((double) dz)) / (dx)) * ((z2 - z1) > 0 ? 1 : -1)));

                vset.add(new Vector(tipx, tipy, tipz));
            }
            notdrawn = false;
        }

        if ((Math.max(Math.max(dx, dy), dz) == dy) && notdrawn) {
            for (int domstep = 0; domstep <= dy; domstep++) {
                tipy = y1 + (domstep * ((y2 - y1) > 0 ? 1 : -1));
                tipx = (int) Math.round(x1 + (((domstep * ((double) dx)) / (dy)) * ((x2 - x1) > 0 ? 1 : -1)));
                tipz = (int) Math.round(z1 + (((domstep * ((double) dz)) / (dy)) * ((z2 - z1) > 0 ? 1 : -1)));

                vset.add(new Vector(tipx, tipy, tipz));
            }
            notdrawn = false;
        }

        if ((Math.max(Math.max(dx, dy), dz) == dz) && notdrawn) {
            for (int domstep = 0; domstep <= dz; domstep++) {
                tipz = z1 + (domstep * ((z2 - z1) > 0 ? 1 : -1));
                tipy = (int) Math.round(y1 + (((domstep * ((double) dy)) / (dz)) * ((y2 - y1) > 0 ? 1 : -1)));
                tipx = (int) Math.round(x1 + (((domstep * ((double) dx)) / (dz)) * ((x2 - x1) > 0 ? 1 : -1)));

                vset.add(new Vector(tipx, tipy, tipz));
            }
        }

        vset = this.getBallooned(vset, radius);
        if (!filled) {
            vset = this.getHollowed(vset);
        }
        return this.setBlocks(vset, pattern);
    }

    /**
     * Draws a spline (out of blocks) between specified vectors.
     *
     * @param pattern The block pattern used to draw the spline.
     * @param nodevectors The list of vectors to draw through.
     * @param tension The tension of every node.
     * @param bias The bias of every node.
     * @param continuity The continuity of every node.
     * @param quality The quality of the spline. Must be greater than 0.
     * @param radius The radius (thickness) of the spline.
     * @param filled If false, only a shell will be generated.
     *
     * @return number of blocks affected
     * @throws MaxChangedBlocksException thrown if too many blocks are changed
     */
    public int drawSpline(final Pattern pattern, final List<Vector> nodevectors, final double tension, final double bias, final double continuity, final double quality, final double radius,
    final boolean filled) throws MaxChangedBlocksException {

        Set<Vector> vset = new HashSet<Vector>();
        final List<Node> nodes = new ArrayList<Node>(nodevectors.size());

        final Interpolation interpol = new KochanekBartelsInterpolation();

        for (final Vector nodevector : nodevectors) {
            final Node n = new Node(nodevector);
            n.setTension(tension);
            n.setBias(bias);
            n.setContinuity(continuity);
            nodes.add(n);
        }

        interpol.setNodes(nodes);
        final double splinelength = interpol.arcLength(0, 1);
        for (double loop = 0; loop <= 1; loop += 1D / splinelength / quality) {
            final Vector tipv = interpol.getPosition(loop);
            final int tipx = (int) Math.round(tipv.getX());
            final int tipy = (int) Math.round(tipv.getY());
            final int tipz = (int) Math.round(tipv.getZ());

            vset.add(new Vector(tipx, tipy, tipz));
        }

        vset = this.getBallooned(vset, radius);
        if (!filled) {
            vset = this.getHollowed(vset);
        }
        return this.setBlocks(vset, pattern);
    }

    private double hypot(final double... pars) {
        double sum = 0;
        for (final double d : pars) {
            sum += Math.pow(d, 2);
        }
        return Math.sqrt(sum);
    }

    private Set<Vector> getBallooned(final Set<Vector> vset, final double radius) {
        final Set<Vector> returnset = new HashSet<Vector>();
        final int ceilrad = (int) Math.ceil(radius);

        for (final Vector v : vset) {
            final int tipx = v.getBlockX(), tipy = v.getBlockY(), tipz = v.getBlockZ();

            for (int loopx = tipx - ceilrad; loopx <= (tipx + ceilrad); loopx++) {
                for (int loopy = tipy - ceilrad; loopy <= (tipy + ceilrad); loopy++) {
                    for (int loopz = tipz - ceilrad; loopz <= (tipz + ceilrad); loopz++) {
                        if (this.hypot(loopx - tipx, loopy - tipy, loopz - tipz) <= radius) {
                            returnset.add(new Vector(loopx, loopy, loopz));
                        }
                    }
                }
            }
        }
        return returnset;
    }

    private Set<Vector> getHollowed(final Set<Vector> vset) {
        final Set<Vector> returnset = new HashSet<Vector>();
        for (final Vector v : vset) {
            final double x = v.getX(), y = v.getY(), z = v.getZ();
            if (!(vset.contains(new Vector(x + 1, y, z))
            && vset.contains(new Vector(x - 1, y, z))
            && vset.contains(new Vector(x, y + 1, z))
            && vset.contains(new Vector(x, y - 1, z))
            && vset.contains(new Vector(x, y, z + 1)) && vset.contains(new Vector(x, y, z - 1)))) {
                returnset.add(v);
            }
        }
        return returnset;
    }

    private void recurseHollow(final Region region, final BlockVector origin, final Set<BlockVector> outside) {
        final LinkedList<BlockVector> queue = new LinkedList<BlockVector>();
        queue.addLast(origin);

        while (!queue.isEmpty()) {
            final BlockVector current = queue.removeFirst();
            if (!BlockType.canPassThrough(this.getBlockType(current), this.getBlockData(current))) {
                continue;
            }

            if (!outside.add(current)) {
                continue;
            }

            if (!region.contains(current)) {
                continue;
            }

            for (final Vector recurseDirection : this.recurseDirections) {
                queue.addLast(current.add(recurseDirection).toBlockVector());
            }
        } // while
    }

    public int makeBiomeShape(final Region region, final Vector zero, final Vector unit, final BaseBiome biomeType, final String expressionString, final boolean hollow) throws ExpressionException,
    MaxChangedBlocksException {
        final Vector2D zero2D = zero.toVector2D();
        final Vector2D unit2D = unit.toVector2D();

        final Expression expression = Expression.compile(expressionString, "x", "z");
        expression.optimize();

        final EditSession editSession = this;
        final WorldEditExpressionEnvironment environment = new WorldEditExpressionEnvironment(editSession, unit, zero);
        expression.setEnvironment(environment);

        final ArbitraryBiomeShape shape = new ArbitraryBiomeShape(region) {
            @Override
            protected BaseBiome getBiome(final int x, final int z, final BaseBiome defaultBiomeType) {
                final Vector2D current = new Vector2D(x, z);
                environment.setCurrentBlock(current.toVector(0));
                final Vector2D scaled = current.subtract(zero2D).divide(unit2D);

                try {
                    if (expression.evaluate(scaled.getX(), scaled.getZ()) <= 0) {
                        return null;
                    }

                    return defaultBiomeType;
                } catch (final Exception e) {
                    EditSession.this.log.log(Level.WARNING, "Failed to create shape", e);
                    return null;
                }
            }
        };

        return shape.generate(this, biomeType, hollow);
    }

    private final Vector[] recurseDirections = {
    PlayerDirection.NORTH.vector(),
    PlayerDirection.EAST.vector(),
    PlayerDirection.SOUTH.vector(),
    PlayerDirection.WEST.vector(),
    PlayerDirection.UP.vector(),
    PlayerDirection.DOWN.vector(), };

    private double lengthSq(final double x, final double y, final double z) {
        return (x * x) + (y * y) + (z * z);
    }

    private double lengthSq(final double x, final double z) {
        return (x * x) + (z * z);
    }

    public static Class<?> inject() {
        return EditSession.class;
    }
}
