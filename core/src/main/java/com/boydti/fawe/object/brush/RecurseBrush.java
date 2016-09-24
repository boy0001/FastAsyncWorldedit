package com.boydti.fawe.object.brush;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.command.tool.BrushTool;
import com.sk89q.worldedit.command.tool.brush.Brush;
import com.sk89q.worldedit.function.block.BlockReplace;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.Masks;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.function.visitor.RecursiveVisitor;

public class RecurseBrush implements Brush {

    private final BrushTool tool;

    public RecurseBrush(BrushTool tool) {
        this.tool = tool;
    }

    @Override
    public void build(final EditSession editSession, final Vector position, Pattern to, double size) throws MaxChangedBlocksException {
        Mask mask = tool.getMask();
        if (mask == null) {
            mask = Masks.alwaysTrue();
        }
        final int radius = (int) size;
        BaseBlock block = editSession.getBlock(position);
        if (block.getId() == 0) {
            return;
        }
        final BlockReplace replace = new BlockReplace(editSession, to);
        editSession.setMask((Mask) null);
        RecursiveVisitor visitor = new RecursiveVisitor(mask, replace, radius);
        visitor.visit(position);
        Operations.completeBlindly(visitor);
    }
}
