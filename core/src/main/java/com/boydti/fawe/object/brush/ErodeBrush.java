package com.boydti.fawe.object.brush;

import com.boydti.fawe.FaweCache;
import com.boydti.fawe.object.PseudoRandom;
import com.boydti.fawe.object.RunnableVal2;
import com.boydti.fawe.object.clipboard.CPUOptimizedClipboard;
import com.boydti.fawe.object.clipboard.FaweClipboard;
import com.boydti.fawe.object.clipboard.OffsetFaweClipboard;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.command.tool.BrushTool;
import com.sk89q.worldedit.function.pattern.Pattern;
import java.util.Arrays;

public class ErodeBrush implements DoubleActionBrush {

    private PseudoRandom rand = new PseudoRandom();

    private static final Vector[] FACES_TO_CHECK = {new Vector(0, 0, 1), new Vector(0, 0, -1), new Vector(0, 1, 0), new Vector(0, -1, 0), new Vector(1, 0, 0), new Vector(-1, 0, 0)};
    
    @Override
    public void build(BrushTool.BrushAction action, EditSession editSession, Vector position, Pattern pattern, double size) throws MaxChangedBlocksException {
        switch (action) {
            case PRIMARY: {
                int erodeFaces = 2;
                int erodeRec = 1;
                int fillFaces = 5;
                int fillRec = 1;
                this.erosion(editSession, erodeFaces, erodeRec, fillFaces, fillRec, position, size);
                break;
            }
            case SECONDARY: {
                int erodeFaces = 6;
                int erodeRec = 0;
                int fillFaces = 1;
                int fillRec = 1;
                this.erosion(editSession, erodeFaces, erodeRec, fillFaces, fillRec, position, size);
                break;
            }
        }
    }

    protected void erosion(final EditSession es, int erodeFaces, int erodeRec, int fillFaces, int fillRec, Vector target, double size) {
        int brushSize = (int) size + 1;
        int brushSizeSquared = (int) (size * size);
        int dimension = brushSize * 2 + 1;
        FaweClipboard buffer1 = new OffsetFaweClipboard(new CPUOptimizedClipboard(dimension, dimension, dimension), brushSize);
        FaweClipboard buffer2 = new OffsetFaweClipboard(new CPUOptimizedClipboard(dimension, dimension, dimension), brushSize);

        final int bx = target.getBlockX();
        final int by = target.getBlockY();
        final int bz = target.getBlockZ();

        for (int x = -brushSize; x <= brushSize; x++) {
            int x0 = x + bx;
            for (int y = -brushSize; y <= brushSize; y++) {
                int y0 = y + by;
                for (int z = -brushSize; z <= brushSize; z++) {
                    int z0 = z + bz;
                    BaseBlock state = es.getBlock(x0, y0, z0);
                    buffer1.setBlock(x, y, z, state);
                    buffer2.setBlock(x, y, z, state);
                }
            }
        }

        int swap = 0;
        for (int i = 0; i < erodeRec; ++i) {
            erosionIteration(brushSize, brushSizeSquared, erodeFaces, swap % 2 == 0 ? buffer1 : buffer2, swap % 2 == 1 ? buffer1 : buffer2);
            swap++;
        }

        for (int i = 0; i < fillRec; ++i) {
            fillIteration(brushSize, brushSizeSquared, fillFaces, swap % 2 == 0 ? buffer1 : buffer2, swap % 2 == 1 ? buffer1 : buffer2);
            swap++;
        }
        FaweClipboard finalBuffer = swap % 2 == 0 ? buffer1 : buffer2;

        finalBuffer.forEach(new RunnableVal2<Vector, BaseBlock>() {
            @Override
            public void run(Vector pos, BaseBlock block) {
                es.setBlock(pos.getBlockX() + bx, pos.getBlockY() + by, pos.getBlockZ() + bz, block);
            }
        }, true);
    }

    private void fillIteration(int brushSize, int brushSizeSquared, int fillFaces, FaweClipboard current, FaweClipboard target) {
        int[] frequency = null;
        for (int x = -brushSize; x <= brushSize; x++) {
            int x2 = x * x;
            for (int z = -brushSize; z <= brushSize; z++) {
                int x2y2 = x2 + z * z;
                for (int y = -brushSize; y <= brushSize; y++) {
                    int cube = x2y2 + y * y;
                    target.setBlock(x, y, z, current.getBlock(x, y, z));
                    if (cube >= brushSizeSquared) {
                        continue;
                    }
                    BaseBlock state = current.getBlock(x, y, z);
                    if (!FaweCache.isLiquidOrGas(state.getId())) {
                        continue;
                    }
                    int total = 0;
                    int highest = 1;
                    BaseBlock highestState = state;
                    if (frequency == null) {
                        frequency = new int[4096];
                    } else {
                        Arrays.fill(frequency, 0);
                    }
                    for (Vector offs : FACES_TO_CHECK) {
                        BaseBlock next = current.getBlock(x + offs.getBlockX(), y + offs.getBlockY(), z + offs.getBlockZ());
                        if (FaweCache.isLiquidOrGas(next.getId())) {
                            continue;
                        }
                        total++;
                        int count = ++frequency[next.getType()];
                        if (count >= highest) {
                            highest = count;
                            highestState = next;
                        }
                    }
                    if (total >= fillFaces) {
                        target.setBlock(x, y, z, highestState);
                    }
                }
            }
        }
    }

    private void erosionIteration(int brushSize, int brushSizeSquared, int erodeFaces, FaweClipboard current, FaweClipboard target) {
        int[] frequency = null;
        for (int x = -brushSize; x <= brushSize; x++) {
            int x2 = x * x;
            for (int z = -brushSize; z <= brushSize; z++) {
                int x2y2 = x2 + z * z;
                for (int y = -brushSize; y <= brushSize; y++) {
                    int cube = x2y2 + y * y;
                    target.setBlock(x, y, z, current.getBlock(x, y, z));
                    if (cube >= brushSizeSquared) {
                        continue;
                    }
                    BaseBlock state = current.getBlock(x, y, z);
                    if (FaweCache.isLiquidOrGas(state.getId())) {
                        continue;
                    }
                    int total = 0;
                    int highest = 1;
                    int highestState = state.getType();
                    if (frequency == null) {
                        frequency = new int[4096];
                    } else {
                        Arrays.fill(frequency, 0);
                    }
                    for (Vector offs : FACES_TO_CHECK) {
                        BaseBlock next = current.getBlock(x + offs.getBlockX(), y + offs.getBlockY(), z + offs.getBlockZ());
                        if (!FaweCache.isLiquidOrGas(next.getId())) {
                            continue;
                        }
                        total++;
                        int count = ++frequency[next.getType()];
                        if (count > highest) {
                            highest = count;
                            highestState = next.getType();
                        }
                    }
                    if (total >= erodeFaces) {
                        target.setBlock(x, y, z, FaweCache.CACHE_BLOCK[highestState << 4]);
                    }
                }
            }
        }
    }
}
