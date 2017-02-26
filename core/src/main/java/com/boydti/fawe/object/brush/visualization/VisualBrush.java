package com.boydti.fawe.object.brush.visualization;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweCache;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.brush.DoubleActionBrush;
import com.boydti.fawe.object.brush.MovableBrush;
import com.boydti.fawe.object.brush.TargetMode;
import com.boydti.fawe.object.brush.scroll.ScrollableBrush;
import com.boydti.fawe.util.EditSessionBuilder;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.command.tool.BrushTool;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.util.Location;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public abstract class VisualBrush implements DoubleActionBrush, MovableBrush, ScrollableBrush {

    private Lock lock = new ReentrantLock();
    private final BrushTool tool;
    private VisualExtent visualExtent;
    private TargetMode mode;

    public VisualBrush(BrushTool tool) {
        this.tool = tool;
        this.mode = TargetMode.TARGET_POINT_RANGE;
    }

    public BrushTool getTool() {
        return tool;
    }

    public TargetMode getMode() {
        return mode;
    }

    public void setMode(TargetMode mode) {
        this.mode = mode;
    }

    @Override
    public abstract void build(BrushTool.BrushAction action, EditSession editSession, Vector position, Pattern pattern, double size) throws MaxChangedBlocksException;

    public Vector getPosition(EditSession editSession, Player player) {
        switch (mode) {
            case TARGET_BLOCK_RANGE:
                return player.getBlockTrace(tool.getRange(), false);
            case FOWARD_POINT_PITCH: {
                int d = 0;
                Location loc = player.getLocation();
                float pitch = loc.getPitch();
                pitch = 23 - (pitch / 4);
                d += (int) (Math.sin(Math.toRadians(pitch)) * 50);
                final Vector vector = loc.getDirection().setY(0).normalize().multiply(d);
                vector.add(loc.getX(), loc.getY(), loc.getZ()).toBlockVector();
                return vector;
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
                return player.getBlockTrace(distance, true);
            }
            case TARGET_POINT_RANGE:
                return player.getBlockTrace(tool.getRange(), true);
            default:
                return null;
        }
    }

    public void queueVisualization(FawePlayer player) {
        Fawe.get().getVisualQueue().queue(player);
    }

    /**
     * Visualize the brush action
     * @deprecated It is preferred to visualize only if a visualization is not in progress
     * @param action
     * @param player
     * @throws MaxChangedBlocksException
     */
    @Deprecated
    public synchronized void visualize(BrushTool.BrushAction action, Player player) throws MaxChangedBlocksException {
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
        editSession.setExtent(newVisualExtent);
        Vector position = getPosition(editSession, player);
        if (position != null) {
            build(BrushTool.BrushAction.PRIMARY, editSession, position, tool.getMaterial(), tool.getSize());
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
        return true;
    }

    @Override
    public boolean increment(int amount) {
        int max = WorldEdit.getInstance().getConfiguration().maxBrushRadius;
        double newSize = Math.max(0, Math.min(max, tool.getSize() + amount));
        tool.setSize(newSize);
        return true;
    }
}
