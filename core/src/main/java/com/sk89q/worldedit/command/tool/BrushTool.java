package com.sk89q.worldedit.command.tool;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweCache;
import com.boydti.fawe.config.BBC;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.RunnableVal;
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
import com.boydti.fawe.object.mask.MaskedTargetBlock;
import com.boydti.fawe.object.pattern.PatternTraverser;
import com.boydti.fawe.util.EditSessionBuilder;
import com.boydti.fawe.util.MaskTraverser;
import com.boydti.fawe.util.TaskManager;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.worldedit.BlockWorldVector;
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
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extension.platform.Platform;
import com.sk89q.worldedit.extent.inventory.BlockBag;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.MaskIntersection;
import com.sk89q.worldedit.function.mask.SolidBlockMask;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.session.request.Request;
import com.sk89q.worldedit.util.Location;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.annotation.Nullable;


import static com.google.common.base.Preconditions.checkNotNull;

public class BrushTool implements DoubleActionTraceTool, ScrollTool, MovableTool, ResettableTool, Serializable {
//    TODO:
    // Serialize methods
    // serialize BrushSettings (primary and secondary only if different)
    // set transient values e.g. context

    public enum BrushAction {
        PRIMARY,
        SECONDARY
    }

    protected static int MAX_RANGE = 500;
    protected int range = -1;
    private VisualMode visualMode = VisualMode.NONE;
    private TargetMode targetMode = TargetMode.TARGET_BLOCK_RANGE;
    private Mask targetMask = null;

    private transient BrushSettings context = new BrushSettings();
    private transient BrushSettings primary = context;
    private transient BrushSettings secondary = context;

    private transient VisualExtent visualExtent;
    private transient Lock lock = new ReentrantLock();

    public BrushTool(String permission) {
        getContext().addPermission(permission);
    }

    public BrushTool() {
    }

    public static BrushTool fromString(Player player, LocalSession session, String json) throws CommandException, InputParseException {
        Gson gson = new Gson();
        Type type = new TypeToken<Map<String, Object>>() {
        }.getType();
        Map<String, Object> root = gson.fromJson(json, type);
        Map<String, Object> primary = (Map<String, Object>) root.get("primary");
        Map<String, Object> secondary = (Map<String, Object>) root.getOrDefault("secondary", primary);

        VisualMode visual = VisualMode.valueOf((String) root.getOrDefault("visual", "NONE"));
        TargetMode target = TargetMode.valueOf((String) root.getOrDefault("target", "TARGET_BLOCK_RANGE"));
        int range = (int) root.getOrDefault("range", -1);

        BrushTool tool = new BrushTool();
        tool.visualMode = visual;
        tool.targetMode = target;
        tool.range = range;

        BrushSettings primarySettings = BrushSettings.get(tool, player, session, primary);
        tool.setPrimary(primarySettings);
        if (primary != secondary) {
            BrushSettings secondarySettings = BrushSettings.get(tool, player, session, secondary);
            tool.setSecondary(secondarySettings);
        }

        return tool;
    }

    @Override
    public String toString() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("primary", primary.getSettings());
        if (primary != secondary) {
            map.put("secondary", secondary.getSettings());
        }
        if (visualMode != null) {
            map.put("visual", visualMode);
        }
        if (targetMode != TargetMode.TARGET_BLOCK_RANGE) {
            map.put("target", targetMode);
        }
        if (range != -1) {
            map.put("range", range);
        }
        return new Gson().toJson(map);
    }

    private void writeObject(java.io.ObjectOutputStream stream) throws IOException {
        stream.defaultWriteObject();
        stream.writeBoolean(primary == secondary);
        stream.writeObject(primary);
        if (primary != secondary) {
            stream.writeObject(secondary);
        }
    }

    private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
        lock = new ReentrantLock();
        boolean multi = stream.readBoolean();
        primary = (BrushSettings) stream.readObject();
        if (multi) {
            secondary = (BrushSettings) stream.readObject();
        } else {
            secondary = primary;
        }
        context = primary;
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
            return primary.canUse(player);
        }
        return primary.canUse(player) && secondary.canUse(player);
    }

    public ResettableExtent getTransform() {
        return getContext().getTransform();
    }

    public BrushSettings getPrimary() {
        return primary;
    }

    public BrushSettings getSecondary() {
        return secondary;
    }

    public BrushSettings getOffHand() {
        return context == primary ? secondary : primary;
    }

    public void setPrimary(BrushSettings primary) {
        checkNotNull(primary);
        if (this.secondary.getBrush() == null) this.secondary = primary;
        this.primary = primary;
        this.context = primary;
    }

    public void setSecondary(BrushSettings secondary) {
        checkNotNull(secondary);
        if (this.primary.getBrush() == null) this.primary = secondary;
        this.secondary = secondary;
        this.context = secondary;
    }

    public void setTransform(ResettableExtent transform) {
        getContext().setTransform(transform);
    }

    /**
     * Get the filter.
     *
     * @return the filter
     */
    public Mask getMask() {
        return getContext().getMask();
    }

    /**
     * Get the filter.
     *
     * @return the filter
     */
    public Mask getSourceMask() {
        return getContext().getSourceMask();
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
        this.getContext().setMask(filter);
    }

    /**
     * Set the block filter used for identifying blocks to replace.
     *
     * @param filter the filter to set
     */
    public void setSourceMask(Mask filter) {
        this.getContext().setSourceMask(filter);
    }

    /**
     * Set the brush.
     *
     * @param brush      tbe brush
     * @param permission the permission
     */
    @Deprecated
    public void setBrush(Brush brush, String permission) {
        setBrush(brush, permission, null);
    }

    @Deprecated
    public void setBrush(Brush brush, String permission, Player player) {
        if (player != null) clear(player);
        BrushSettings current = getContext();
        current.clearPerms();
        current.setBrush(brush);
        current.addPermission(permission);
    }

    /**
     * Get the current brush.
     *
     * @return the current brush
     */
    public Brush getBrush() {
        return getContext().getBrush();
    }

    /**
     * Set the material.
     *
     * @param material the material
     */
    public void setFill(@Nullable Pattern material) {
        this.getContext().setFill(material);
    }

    /**
     * Get the material.
     *
     * @return the material
     */
    @Nullable
    public Pattern getMaterial() {
        return getContext().getMaterial();
    }

    /**
     * Get the set brush size.
     *
     * @return a radius
     */
    public double getSize() {
        return getContext().getSize();
    }

    /**
     * Set the set brush size.
     *
     * @param radius a radius
     */
    public void setSize(double radius) {
        this.getContext().setSize(radius);
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
                return new MutableBlockVector(trace(editSession, player, getRange(), true));
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
                return new MutableBlockVector(trace(editSession, player, distance, true));
            }
            case TARGET_FACE_RANGE:
                return new MutableBlockVector(trace(editSession, player, getRange(), true));
            default:
                return null;
        }
    }

    private Vector trace(EditSession editSession, Player player, int range, boolean useLastBlock) {
        Mask mask = targetMask == null ? new SolidBlockMask(editSession) : targetMask;
        new MaskTraverser(mask).reset(editSession);
        MaskedTargetBlock tb = new MaskedTargetBlock(mask, player, range, 0.2);
        return TaskManager.IMP.sync(new RunnableVal<Vector>() {
            @Override
            public void run(Vector value) {
                BlockWorldVector result = tb.getMaskedTargetBlock(useLastBlock);
                this.value = result;
            }
        });
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
        Brush brush = current.getBrush();
        if (brush == null) return false;

        EditSession editSession = session.createEditSession(player);
        Vector target = getPosition(editSession, player);

        if (target == null) {
            editSession.cancel();
            BBC.NO_BLOCK.send(player);
            return true;
        }
        BlockBag bag = session.getBlockBag(player);
        Request.request().setEditSession(editSession);
        Mask mask = current.getMask();
        if (mask != null) {
            Mask existingMask = editSession.getMask();

            if (existingMask == null) {
                editSession.setMask(mask);
            } else if (existingMask instanceof MaskIntersection) {
                ((MaskIntersection) existingMask).add(mask);
            } else {
                MaskIntersection newMask = new MaskIntersection(existingMask);
                newMask.add(mask);
                editSession.setMask(newMask);
            }
        }
        Mask sourceMask = current.getSourceMask();
        if (sourceMask != null) {
            editSession.addSourceMask(sourceMask);
        }
        ResettableExtent transform = current.getTransform();
        if (transform != null) {
            editSession.addTransform(transform);
        }
        try {
            new PatternTraverser(current).reset(editSession);
            brush.build(editSession, target, current.getMaterial(), current.getSize());
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
        this.getContext().setScrollAction(scrollAction);
    }

    public void setTargetMode(TargetMode targetMode) {
        this.targetMode = targetMode != null ? targetMode : TargetMode.TARGET_BLOCK_RANGE;
    }

    public void setTargetMask(Mask mask) {
        this.targetMask = mask;
    }

    public void setVisualMode(VisualMode visualMode) {
        this.visualMode = visualMode != null ? visualMode : VisualMode.NONE;
    }

    public TargetMode getTargetMode() {
        return targetMode;
    }

    public Mask getTargetMask() {
        return targetMask;
    }

    public VisualMode getVisualMode() {
        return visualMode;
    }

    @Override
    public boolean increment(Player player, int amount) {
        BrushSettings current = getContext();
        ScrollAction tmp = current.getScrollAction();
        if (tmp != null) {
            tmp.setTool(this);
            if (tmp.increment(player, amount)) {
                if (visualMode != VisualMode.NONE) {
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
                    current.getBrush().build(editSession, position, current.getMaterial(), current.getSize());
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
