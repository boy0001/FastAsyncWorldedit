package com.boydti.fawe.object.brush;

import com.boydti.fawe.config.BBC;
import com.boydti.fawe.object.extent.ResettableExtent;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalConfiguration;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.WorldVector;
import com.sk89q.worldedit.command.tool.DoubleActionTraceTool;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extension.platform.Platform;
import com.sk89q.worldedit.extent.inventory.BlockBag;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.MaskIntersection;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.session.request.Request;
import javax.annotation.Nullable;


import static com.google.common.base.Preconditions.checkNotNull;

public class DoubleActionBrushTool implements DoubleActionTraceTool {

    public enum BrushAction {
        PRIMARY,
        SECONDARY
    }

    protected static int MAX_RANGE = 500;
    protected int range = -1;
    private Mask mask = null;
    private Mask sourceMask = null;
    private ResettableExtent transform = null;
    private DoubleActionBrush brush = null;
    @Nullable
    private Pattern material;
    private double size = 1;
    private String permission;

    /**
     * Construct the tool.
     *
     * @param permission the permission to check before use is allowed
     */
    public DoubleActionBrushTool(String permission) {
        checkNotNull(permission);
        this.permission = permission;
    }

    @Override
    public boolean canUse(Actor player) {
        return player.hasPermission(permission);
    }

    public ResettableExtent getTransform() {
        return transform;
    }

    public void setTransform(ResettableExtent transform) {
        this.transform = transform;
    }

    /**
     * Get the filter.
     *
     * @return the filter
     */
    public Mask getMask() {
        return mask;
    }

    /**
     * Get the filter.
     *
     * @return the filter
     */
    public Mask getSourceMask() {
        return sourceMask;
    }

    /**
     * Set the block filter used for identifying blocks to replace.
     *
     * @param filter the filter to set
     */
    public void setMask(Mask filter) {
        this.mask = filter;
    }

    /**
     * Set the block filter used for identifying blocks to replace.
     *
     * @param filter the filter to set
     */
    public void setSourceMask(Mask filter) {
        this.sourceMask = filter;
    }

    /**
     * Set the brush.
     *
     * @param brush tbe brush
     * @param permission the permission
     */
    public void setBrush(DoubleActionBrush brush, String permission) {
        this.brush = brush;
        this.permission = permission;
    }

    /**
     * Get the current brush.
     *
     * @return the current brush
     */
    public DoubleActionBrush getBrush() {
        return brush;
    }

    /**
     * Set the material.
     *
     * @param material the material
     */
    public void setFill(@Nullable Pattern material) {
        this.material = material;
    }

    /**
     * Get the material.
     *
     * @return the material
     */
    @Nullable public Pattern getMaterial() {
        return material;
    }

    /**
     * Get the set brush size.
     *
     * @return a radius
     */
    public double getSize() {
        return size;
    }

    /**
     * Set the set brush size.
     *
     * @param radius a radius
     */
    public void setSize(double radius) {
        this.size = radius;
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

    public boolean act(BrushAction action, Platform server, LocalConfiguration config, Player player, LocalSession session) {
        WorldVector target = null;
        target = player.getBlockTrace(getRange(), true);

        if (target == null) {
            BBC.NO_BLOCK.send(player);
            return true;
        }

        BlockBag bag = session.getBlockBag(player);

        EditSession editSession = session.createEditSession(player);
        Request.request().setEditSession(editSession);
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
        if (sourceMask != null) {
            Mask existingMask = editSession.getSourceMask();

            if (existingMask == null) {
                editSession.setSourceMask(sourceMask);
            } else if (existingMask instanceof MaskIntersection) {
                ((MaskIntersection) existingMask).add(sourceMask);
            } else {
                MaskIntersection newMask = new MaskIntersection(existingMask);
                newMask.add(sourceMask);
                editSession.setSourceMask(newMask);
            }
        }
        if (transform != null) {
            editSession.addTransform(transform);
        }
        try {
            brush.build(action, editSession, target, material, size);
        } catch (MaxChangedBlocksException e) {
            player.printError("Max blocks change limit reached."); // Never happens
        } finally {
            if (bag != null) {
                bag.flushChanges();
            }
            session.remember(editSession);
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
}
