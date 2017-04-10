package com.sk89q.worldedit.command.tool;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweCache;
import com.boydti.fawe.config.BBC;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.brush.BrushSettings;
import com.boydti.fawe.object.brush.MovableTool;
import com.boydti.fawe.object.brush.ResettableTool;
import com.boydti.fawe.object.brush.TargetMode;
import com.boydti.fawe.object.brush.scroll.ScrollAction;
import com.boydti.fawe.object.brush.scroll.ScrollTool;
import com.boydti.fawe.object.brush.visualization.VisualChunk;
import com.boydti.fawe.object.brush.visualization.VisualExtent;
import com.boydti.fawe.object.brush.visualization.VisualMode;
import com.boydti.fawe.object.extent.ResettableExtent;
import com.boydti.fawe.object.pattern.PatternTraverser;
import com.boydti.fawe.util.EditSessionBuilder;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalConfiguration;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.MutableBlockVector;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.command.tool.brush.Brush;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extension.platform.Platform;
import com.sk89q.worldedit.extent.inventory.BlockBag;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.MaskIntersection;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.session.request.Request;
import com.sk89q.worldedit.util.Location;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.annotation.Nullable;


import static com.google.common.base.Preconditions.checkNotNull;

public class BrushTool implements DoubleActionTraceTool, ScrollTool, MovableTool, ResettableTool {

    public enum BrushAction {
        PRIMARY,
        SECONDARY
    }

    protected static int MAX_RANGE = 500;
    protected int range = -1;
    private VisualMode visualMode = VisualMode.NONE;
    private TargetMode targetMode = TargetMode.TARGET_BLOCK_RANGE;

    private transient BrushSettings context = new BrushSettings();
    private BrushSettings primary = context;
    private BrushSettings secondary = context;

    private transient VisualExtent visualExtent;
    private transient Lock lock = new ReentrantLock();

    /**
     * Construct the tool.
     *
     * @param permission the permission to check before use is allowed
     */
    public BrushTool(String permission) {
        checkNotNull(permission);
        this.getContext().permission = permission;
    }

    public BrushSettings getContext() {
        BrushSettings tmp = context;
        if (tmp == null) {
            context = tmp = primary;
        }
        return tmp;
    }

    public void setContext(BrushSettings context) {
        this.context = context;
    }

    @Override
    public boolean canUse(Actor player) {
        if (primary == secondary) {
            return player.hasPermission(getContext().permission);
        }
        return player.hasPermission(primary.permission) && player.hasPermission(secondary.permission);
    }

    public ResettableExtent getTransform() {
        return getContext().transform;
    }

    public BrushSettings getPrimary() {
        return primary;
    }

    public BrushSettings getSecondary() {
        return secondary;
    }

    public void setPrimary(BrushSettings primary) {
        checkNotNull(primary);
        this.primary = primary;
    }

    public void setSecondary(BrushSettings secondary) {
        checkNotNull(secondary);
        this.secondary = secondary;
    }

    public void setTransform(ResettableExtent transform) {
        this.getContext().transform = transform;
    }

    /**
     * Get the filter.
     *
     * @return the filter
     */
    public Mask getMask() {
        return getContext().mask;
    }

    /**
     * Get the filter.
     *
     * @return the filter
     */
    public Mask getSourceMask() {
        return getContext().sourceMask;
    }

    @Override
    public boolean reset() {
        Brush br = getBrush();
        if (br instanceof ResettableTool) {
            return ((ResettableTool) br).reset();
        }
        return false;
    }

    /**
     * Set the block filter used for identifying blocks to replace.
     *
     * @param filter the filter to set
     */
    public void setMask(Mask filter) {
        this.getContext().mask = filter;
    }

    /**
     * Set the block filter used for identifying blocks to replace.
     *
     * @param filter the filter to set
     */
    public void setSourceMask(Mask filter) {
        this.getContext().sourceMask = filter;
    }

    /**
     * Set the brush.
     *
     * @param brush tbe brush
     * @param permission the permission
     */
    public void setBrush(Brush brush, String permission) {
        setBrush(brush, permission, null);
    }

    public void setBrush(Brush brush, String permission, Player player) {
        if (player != null) clear(player);
        BrushSettings current = getContext();
        current.brush = brush;
        current.permission = permission;
    }

    /**
     * Get the current brush.
     *
     * @return the current brush
     */
    public Brush getBrush() {
        return getContext().brush;
    }

    /**
     * Set the material.
     *
     * @param material the material
     */
    public void setFill(@Nullable Pattern material) {
        this.getContext().material = material;
    }

    /**
     * Get the material.
     *
     * @return the material
     */
    @Nullable public Pattern getMaterial() {
        return getContext().material;
    }

    /**
     * Get the set brush size.
     *
     * @return a radius
     */
    public double getSize() {
        return getContext().size;
    }

    /**
     * Set the set brush size.
     *
     * @param radius a radius
     */
    public void setSize(double radius) {
        this.getContext().size = radius;
    }

    /**
     * Get the set brush range.
     *
     * @return the range of the brush in blocks
     */
    public int getRange() {
        return (range < 0) ? MAX_RANGE : Math.min(range, MAX_RANGE);
    }

    /**
     * Set the set brush range.
     *
     * @param range the range of the brush in blocks
     */
    public void setRange(int range) {
        this.range = range;
    }

    public Vector getPosition(EditSession editSession, Player player) {
        switch (targetMode) {
            case TARGET_BLOCK_RANGE:
                return new MutableBlockVector(player.getBlockTrace(getRange(), true));
            case FOWARD_POINT_PITCH: {
                int d = 0;
                Location loc = player.getLocation();
                float pitch = loc.getPitch();
                pitch = 23 - (pitch / 4);
                d += (int) (Math.sin(Math.toRadians(pitch)) * 50);
                final Vector vector = loc.getDirection().setY(0).normalize().multiply(d);
                vector.add(loc.getX(), loc.getY(), loc.getZ()).toBlockVector();
                return new MutableBlockVector(vector);
            }
            case TARGET_POINT_HEIGHT: {
                Location loc = player.getLocation();
                final int height = loc.getBlockY();
                final int x = loc.getBlockX();
                final int z = loc.getBlockZ();
                int y;
                for (y = height; y > 0; y--) {
                    BaseBlock block = editSession.getBlock(x, y, z);
                    if (!FaweCache.isLiquidOrGas(block.getId())) {
                        break;
                    }
                }
                final int distance = (height - y) + 8;
                return new MutableBlockVector(player.getBlockTrace(distance, true));
            }
            case TARGET_FACE_RANGE:
                return new MutableBlockVector(player.getBlockTraceFace(getRange(), true));
            default:
                return null;
        }
    }

    public boolean act(BrushAction action, Platform server, LocalConfiguration config, Player player, LocalSession session) {
        switch (action) {
            case PRIMARY:
                setContext(primary);
                break;
            case SECONDARY:
                setContext(secondary);
                break;
        }

        BrushSettings current = getContext();

        EditSession editSession = session.createEditSession(player);
        Vector target = getPosition(editSession, player);

        if (target == null) {
            editSession.cancel();
            BBC.NO_BLOCK.send(player);
            return true;
        }
        BlockBag bag = session.getBlockBag(player);
        Request.request().setEditSession(editSession);
        if (current.mask != null) {
            Mask existingMask = editSession.getMask();

            if (existingMask == null) {
                editSession.setMask(current.mask);
            } else if (existingMask instanceof MaskIntersection) {
                ((MaskIntersection) existingMask).add(current.mask);
            } else {
                MaskIntersection newMask = new MaskIntersection(existingMask);
                newMask.add(current.mask);
                editSession.setMask(newMask);
            }
        }
        if (current.sourceMask != null) {
            editSession.addSourceMask(current.sourceMask);
        }
        if (current.transform != null) {
            editSession.addTransform(current.transform);
        }
        try {
            new PatternTraverser(current).reset(editSession);
            current.brush.build(editSession, target, current.material, current.size);
        } catch (MaxChangedBlocksException e) {
            player.printError("Max blocks change limit reached."); // Never happens
        } finally {
            if (bag != null) {
                bag.flushChanges();
            }
            session.remember(editSession);
            Request.reset();
        }
        return true;
    }

    @Override
    public boolean actPrimary(Platform server, LocalConfiguration config, Player player, LocalSession session) {
        return act(BrushAction.PRIMARY, server, config, player, session);
    }

    @Override
    public boolean actSecondary(Platform server, LocalConfiguration config, Player player, LocalSession session) {
        return act(BrushAction.SECONDARY, server, config, player, session);
    }

    public static Class<?> inject() {
        return BrushTool.class;
    }

    public void setScrollAction(ScrollAction scrollAction) {
        this.getContext().scrollAction = scrollAction;
    }

    public void setTargetMode(TargetMode targetMode) {
        this.targetMode = targetMode != null ? targetMode : TargetMode.TARGET_BLOCK_RANGE;
    }

    public void setVisualMode(VisualMode visualMode) {
        this.visualMode = visualMode != null ? visualMode : VisualMode.NONE;
    }

    public TargetMode getTargetMode() {
        return targetMode;
    }

    public VisualMode getVisualMode() {
        return visualMode;
    }

    @Override
    public boolean increment(Player player, int amount) {
        BrushSettings current = getContext();
        ScrollAction tmp = current.scrollAction;
        if (tmp != null) {
            tmp.setTool(this);
            if (tmp.increment(player, amount)) {
                if  (visualMode != VisualMode.NONE) {
                    try {
                        queueVisualization(FawePlayer.wrap(player));
                    } catch (Throwable e) {
                        WorldEdit.getInstance().getPlatformManager().handleThrowable(e, player);
                    }
                }
                return true;
            }
        }
        if (visualMode != VisualMode.NONE) {
            clear(player);
        }
        return false;
    }

    public void queueVisualization(FawePlayer player) {
        Fawe.get().getVisualQueue().queue(player);
    }

    @Deprecated
    public synchronized void visualize(BrushTool.BrushAction action, Player player) throws MaxChangedBlocksException {
        VisualMode mode = getVisualMode();
        if (mode == VisualMode.NONE) {
            return;
        }
        FawePlayer<Object> fp = FawePlayer.wrap(player);
        EditSession editSession = new EditSessionBuilder(player.getWorld())
                .player(fp)
                .allowedRegionsEverywhere()
                .autoQueue(false)
                .blockBag(null)
                .changeSetNull()
                .combineStages(false)
                .build();
        VisualExtent newVisualExtent = new VisualExtent(editSession.getExtent(), editSession.getQueue());
        Vector position = getPosition(editSession, player);
        if (position != null) {
            editSession.setExtent(newVisualExtent);
            switch (mode) {
                case POINT: {
                    editSession.setBlockFast(position, FaweCache.getBlock(VisualChunk.VISUALIZE_BLOCK, 0));
                    break;
                }
                case OUTLINE: {
                    BrushSettings current = getContext();
                    new PatternTraverser(current).reset(editSession);
                    current.brush.build(editSession, position, current.material, current.size);
                    break;
                }
            }
        }
        if (visualExtent != null) {
            // clear old data
            visualExtent.clear(newVisualExtent, fp);
        }
        visualExtent = newVisualExtent;
        newVisualExtent.visualize(fp);
    }

    public void clear(Player player) {
        FawePlayer<Object> fp = FawePlayer.wrap(player);
        Fawe.get().getVisualQueue().dequeue(fp);
        if (visualExtent != null) {
            visualExtent.clear(null, fp);
        }
    }

    @Override
    public boolean move(Player player) {
        if (visualMode != VisualMode.NONE) {
            queueVisualization(FawePlayer.wrap(player));
            return true;
        }
        return false;
    }
}
