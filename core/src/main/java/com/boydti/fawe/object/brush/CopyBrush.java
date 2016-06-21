package com.boydti.fawe.object.brush;

import com.boydti.fawe.config.BBC;
import com.boydti.fawe.object.clipboard.ResizableClipboardBuilder;
import com.boydti.fawe.object.exception.FaweException;
import com.boydti.fawe.object.function.NullRegionFunction;
import com.boydti.fawe.object.function.mask.AbstractDelegateMask;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.command.tool.BrushTool;
import com.sk89q.worldedit.command.tool.brush.Brush;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.Masks;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.function.visitor.RecursiveVisitor;
import com.sk89q.worldedit.session.ClipboardHolder;

public class CopyBrush implements Brush {

    private final BrushTool tool;
    private final LocalSession session;
    private final Player player;

    public CopyBrush(Player player, LocalSession session, BrushTool tool) {
        this.tool = tool;
        this.session = session;
        this.player = player;
    }

    @Override
    public void build(final EditSession editSession, Vector position, Pattern pattern, double size) throws MaxChangedBlocksException {
        Mask mask = tool.getMask();
        if (mask == null) {
            mask = Masks.alwaysTrue();
        }
        final ResizableClipboardBuilder builder = new ResizableClipboardBuilder(editSession.getWorld());
        final int size2 = (int) (size * size);
        final int minY = position.getBlockY();
        mask = new AbstractDelegateMask(mask) {
            private int visited = 0;
            @Override
            public boolean test(Vector vector) {
                if (super.test(vector) && vector.getBlockY() >= minY) {
                    BaseBlock block = editSession.getLazyBlock(vector);
                    if (block != EditSession.nullBlock) {
                        if (visited++ > size2) {
                            throw new FaweException(BBC.WORLDEDIT_CANCEL_REASON_MAX_CHECKS);
                        }
                        builder.add(vector, EditSession.nullBlock, block);
                        return true;
                    }
                }
                return false;
            }
        };
        // Add origin
        mask.test(position);

        RecursiveVisitor visitor = new RecursiveVisitor(mask, new NullRegionFunction());
        visitor.visit(position);
        Operations.completeBlindly(visitor);
        // Build the clipboard
        Clipboard clipboard = builder.build();
        clipboard.setOrigin(position);
        ClipboardHolder holder = new ClipboardHolder(clipboard, editSession.getWorld().getWorldData());
        session.setClipboard(holder);
        int blocks = builder.size();
        BBC.COMMAND_COPY.send(player, blocks);
    }
}
