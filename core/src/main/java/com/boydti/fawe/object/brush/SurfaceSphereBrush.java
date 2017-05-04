package com.boydti.fawe.object.brush;

import com.boydti.fawe.FaweCache;
import com.boydti.fawe.object.mask.AdjacentAnyMask;
import com.boydti.fawe.object.mask.RadiusMask;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.command.tool.brush.Brush;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.Masks;
import com.sk89q.worldedit.function.mask.SolidBlockMask;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.function.visitor.BreadthFirstSearch;
import com.sk89q.worldedit.function.visitor.RecursiveVisitor;
import java.util.Arrays;

public class SurfaceSphereBrush implements Brush {
    @Override
    public void build(EditSession editSession, Vector position, Pattern pattern, double size) throws MaxChangedBlocksException {
        Mask mask = editSession.getMask();
        if (mask == null) {
            mask = Masks.alwaysTrue();
        }
        AdjacentAnyMask adjacent = new AdjacentAnyMask(editSession, Arrays.asList(new BaseBlock(0)));
        for (int id = 0; id < 256; id++) {
            if (FaweCache.canPassThrough(id, 0)) {
                adjacent.add(new BaseBlock(id, -1));
            }
        }
        final SolidBlockMask solid = new SolidBlockMask(editSession);
        final RadiusMask radius = new RadiusMask(0, (int) size);
        RecursiveVisitor visitor = new RecursiveVisitor(vector -> solid.test(vector) && radius.test(vector) && adjacent.test(vector), vector -> editSession.setBlock(vector, pattern));
        visitor.visit(position);
        visitor.setDirections(Arrays.asList(BreadthFirstSearch.DIAGONAL_DIRECTIONS));
        Operations.completeBlindly(visitor);
    }
}