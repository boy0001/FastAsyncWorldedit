package com.boydti.fawe.object.brush;

import com.boydti.fawe.object.PseudoRandom;
import com.boydti.fawe.object.brush.heightmap.HeightMap;
import com.boydti.fawe.object.brush.heightmap.ScalableHeightMap;
import com.boydti.fawe.object.mask.AdjacentAnyMask;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.RegionFunction;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.RegionMask;
import com.sk89q.worldedit.function.mask.SolidBlockMask;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.function.visitor.RecursiveVisitor;
import com.sk89q.worldedit.regions.CuboidRegion;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;

public class StencilBrush extends HeightBrush {
    private final boolean onlyWhite;

    public StencilBrush(InputStream stream, int rotation, double yscale, boolean onlyWhite, Clipboard clipboard) {
        super(stream, rotation, yscale, false, clipboard);
        this.onlyWhite = onlyWhite;
    }

    @Override
    public void build(EditSession editSession, Vector position, Pattern pattern, double sizeDouble) throws MaxChangedBlocksException {
        final int cx = position.getBlockX();
        final int cy = position.getBlockY();
        final int cz = position.getBlockZ();
        int size = (int) sizeDouble;
        int maxY = editSession.getMaxY();
        int add;
        if (yscale < 0) {
            add = maxY;
        } else {
            add = 0;
        }
        double scale = (yscale / sizeDouble) * (maxY + 1);
        final HeightMap map = getHeightMap();
        map.setSize(size);
        int cutoff = onlyWhite ? maxY : 0;
        final SolidBlockMask solid = new SolidBlockMask(editSession);
        final AdjacentAnyMask adjacent = new AdjacentAnyMask(editSession, solid.getInverseBlocks());
        RegionMask region = new RegionMask(new CuboidRegion(position.subtract(size, size, size), position.add(size, size, size)));
        RecursiveVisitor visitor = new RecursiveVisitor(new Mask() {
            @Override
            public boolean test(Vector vector) {
                if (solid.test(vector) && region.test(vector)) {
                    int dx = vector.getBlockX() - cx;
                    int dy = vector.getBlockY() - cy;
                    int dz = vector.getBlockZ() - cz;
                    Vector dir = adjacent.direction(vector);
                    if (dir != null) {
                        if (dy != 0) {
                            if (dir.getBlockX() != 0) {
                                dx += dir.getBlockX() * dy;
                            } else if (dir.getBlockZ() != 0) {
                                dz += dir.getBlockZ() * dy;
                            }
                        }
                        double raise = map.getHeight(dx, dz);
                        int val = (int) Math.ceil(raise * scale) + add;
                        if (val < cutoff) {
                            return true;
                        }
                        if (val >= 255 || PseudoRandom.random.random(maxY) < val) {
                            editSession.setBlock(vector.getBlockX(), vector.getBlockY(), vector.getBlockZ(), pattern);
                        }
                        return true;
                    }
                }
                return false;
            }
        }, new RegionFunction() {
            @Override
            public boolean apply(Vector vector) throws WorldEditException {
                return true;
            }
        }, Integer.MAX_VALUE, editSession);
        visitor.setDirections(Arrays.asList(visitor.DIAGONAL_DIRECTIONS));
        visitor.visit(position);
        Operations.completeBlindly(visitor);

//        Mask mask = new ExistingBlockMask(editSession);
//        int maxY = editSession.getMaxY();
//        double scale = (yscale / sizeDouble) * (maxY + 1);
//        heightMap.setSize(size);
//        int cutoff = onlyWhite ? maxY : 0;
//
//        for (int x = -size; x <= size; x++) {
//            int xx = position.getBlockX() + x;
//            for (int z = -size; z <= size; z++) {
//                double raise;
//                switch (rotation) {
//                    default:raise = heightMap.getHeight(x, z); break;
//                    case 1: raise = heightMap.getHeight(z, x); break;
//                    case 2: raise = heightMap.getHeight(-x, -z); break;
//                    case 3: raise = heightMap.getHeight(-z, -x);break;
//                }
//                int val = (int) Math.ceil(raise * scale);
//                if (val <= cutoff) {
//                    continue;
//                }
//                if (val >= 255 || PseudoRandom.random.random(maxY) < val) {
//                    int zz = position.getBlockZ() + z;
//                    int y = editSession.getNearestSurfaceTerrainBlock(xx, zz, position.getBlockY(), 0, maxY);
//                    for (int i = 0; i < depth; i++) {
//                        editSession.setBlock(xx, y - i, zz, pattern);
//                    }
//                }
//            }
//        }
    }

    private void apply(double val) {

    }
}