package com.boydti.fawe.object.brush;

import com.boydti.fawe.config.BBC;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.PseudoRandom;
import com.boydti.fawe.object.brush.visualization.VisualExtent;
import com.boydti.fawe.object.clipboard.ResizableClipboardBuilder;
import com.boydti.fawe.object.function.NullRegionFunction;
import com.boydti.fawe.object.function.mask.AbstractDelegateMask;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.command.tool.brush.Brush;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.Masks;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.function.visitor.RecursiveVisitor;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.ClipboardHolder;

public class CopyPastaBrush implements Brush, ResettableTool {

    private final LocalSession session;
    private final boolean randomRotate;

    public CopyPastaBrush(LocalSession session, boolean randomRotate) {
        session.setClipboard(null);
        this.session = session;
        this.randomRotate = randomRotate;
    }

    @Override
    public boolean reset() {
        session.setClipboard(null);
        return true;
    }

    @Override
    public void build(final EditSession editSession, Vector position, Pattern pattern, double size) throws MaxChangedBlocksException {
        FawePlayer fp = editSession.getPlayer();
        ClipboardHolder clipboard = session.getExistingClipboard();
        if (clipboard == null) {
            if (editSession.getExtent() instanceof VisualExtent) {
                return;
            }
            Mask mask = editSession.getMask();
            if (mask == null) {
                mask = Masks.alwaysTrue();
            }
            final ResizableClipboardBuilder builder = new ResizableClipboardBuilder(editSession.getWorld());
            final int size2 = (int) (size * size);
            final int minY = position.getBlockY();
            mask = new AbstractDelegateMask(mask) {
                @Override
                public boolean test(Vector vector) {
                    if (super.test(vector) && vector.getBlockY() >= minY) {
                        BaseBlock block = editSession.getLazyBlock(vector);
                        if (block != EditSession.nullBlock) {
                            builder.add(vector, EditSession.nullBlock, block);
                            return true;
                        }
                    }
                    return false;
                }
            };
            // Add origin
            mask.test(position);
            RecursiveVisitor visitor = new RecursiveVisitor(mask, new NullRegionFunction(), (int) size, editSession);
            visitor.visit(position);
            Operations.completeBlindly(visitor);
            // Build the clipboard
            Clipboard newClipboard = builder.build();
            newClipboard.setOrigin(position);
            ClipboardHolder holder = new ClipboardHolder(newClipboard, editSession.getWorld().getWorldData());
            session.setClipboard(holder);
            int blocks = builder.size();
            BBC.COMMAND_COPY.send(fp, blocks);
            return;
        } else {
            if (randomRotate) {
                int rotate = 90 * PseudoRandom.random.nextInt(4);
                clipboard.setTransform(rotate != 0 ? new AffineTransform().rotateY(rotate) : new AffineTransform());
            }
            Clipboard faweClip = clipboard.getClipboard();
            Region region = faweClip.getRegion();
            Vector centerOffset = region.getCenter().subtract(faweClip.getOrigin());
            Operation operation = clipboard
                    .createPaste(editSession, editSession.getWorld().getWorldData())
                    .to(position.add(0, 1, 0))
                    .ignoreAirBlocks(true)
                    .build();
            Operations.completeLegacy(operation);
        }
    }
}
