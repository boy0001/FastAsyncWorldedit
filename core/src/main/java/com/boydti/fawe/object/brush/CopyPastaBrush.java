package com.boydti.fawe.object.brush;

import com.boydti.fawe.config.BBC;
import com.boydti.fawe.object.clipboard.ResizableClipboardBuilder;
import com.boydti.fawe.object.exception.FaweException;
import com.boydti.fawe.object.function.NullRegionFunction;
import com.boydti.fawe.object.function.mask.AbstractDelegateMask;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.EmptyClipboardException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.Masks;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.function.visitor.RecursiveVisitor;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.ClipboardHolder;

public class CopyPastaBrush implements DoubleActionBrush {

    private final DoubleActionBrushTool tool;
    private final LocalSession session;
    private final Player player;

    public CopyPastaBrush(Player player, LocalSession session, DoubleActionBrushTool tool) {
        this.tool = tool;
        this.session = session;
        this.player = player;
    }

    @Override
    public void build(DoubleActionBrushTool.BrushAction action, final EditSession editSession, Vector position, Pattern pattern, double size) throws MaxChangedBlocksException {
        switch (action) {
            case SECONDARY: {
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
                player.print(BBC.getPrefix() + BBC.COMMAND_COPY.format(blocks));
                return;
            }
            case PRIMARY: {
                try {
                    ClipboardHolder holder = session.getClipboard();
                    Clipboard clipboard = holder.getClipboard();
                    Region region = clipboard.getRegion();
                    Vector centerOffset = region.getCenter().subtract(clipboard.getOrigin());
                    Operation operation = holder
                            .createPaste(editSession, editSession.getWorld().getWorldData())
                            .to(position.add(0, 1, 0))
                            .ignoreAirBlocks(true)
                            .build();
                    Operations.completeLegacy(operation);
                } catch (EmptyClipboardException e) {
                    player.print(BBC.getPrefix() + BBC.BRUSH_PASTE_NONE.s());
                }
            }
        }
    }
}
