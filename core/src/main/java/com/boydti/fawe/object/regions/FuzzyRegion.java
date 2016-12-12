package com.boydti.fawe.object.regions;

import com.boydti.fawe.util.MathMan;
import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.RegionFunction;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.visitor.RecursiveVisitor;
import com.sk89q.worldedit.regions.AbstractRegion;
import com.sk89q.worldedit.regions.RegionOperationException;
import com.sk89q.worldedit.world.World;
import java.util.BitSet;
import java.util.Iterator;

public class FuzzyRegion extends AbstractRegion {

    private final Mask mask;
    private BitSet set = new BitSet();
    private boolean populated;
    private int minX, minY, minZ, maxX, maxY, maxZ;
    private int offsetX, offsetY, offsetZ;
    private Extent extent;
    private int count = 0;

    {
        minX = minY = minZ = Integer.MAX_VALUE;
        maxX = maxY = maxZ = Integer.MIN_VALUE;
    }

    public FuzzyRegion(World world, Extent editSession, Mask mask) {
        super(world);
        this.extent = editSession;
        this.mask = mask;
    }

    public Mask getMask() {
        return mask;
    }

    private static int pair(int x, int y, int z) {
        byte b1 = (byte) y;
        byte b2 = (byte) (x);
        byte b3 = (byte) (z);
        int x16 = (x >> 8) & 0x7;
        int z16 = (z >> 8) & 0x7;
        byte b4 = MathMan.pair8(x16, z16);
        return ((b1 & 0xFF)
             + ((b2 & 0xFF) << 8)
             + ((b3 & 0xFF) << 16)
             + ((b4 & 0x7F) << 24))
             ;
    }

    @Override
    public int getArea() {
        return set.cardinality();
    }

    public void select(int x, int y, int z) {
        RecursiveVisitor visitor = new RecursiveVisitor(mask, new RegionFunction() {
            @Override
            public boolean apply(Vector position) throws WorldEditException {
                return true;
            }
        }) {
            @Override
            public boolean isVisited(Node node) {
                return contains(node.getX(), node.getY(), node.getZ());
            }

            @Override
            public void addVisited(Node node, int depth) {
                try {
                    set(node.getX(), node.getY(), node.getZ());
                } catch (RegionOperationException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void cleanVisited(int layer) {
                // Do nothing
            }
        };
        visitor.visit(new Vector(x, y, z));
        Operations.completeBlindly(visitor);
    }

    @Override
    public Iterator<BlockVector> iterator() {
        return new Iterator<BlockVector>() {

            private int index = -1;
            private BlockVector pos = new BlockVector(0, 0, 0);

            @Override
            public boolean hasNext() {
                index = set.nextSetBit(index + 1);
                return index != -1;
            }

            @Override
            public BlockVector next() {
                int b1 = ((byte) index) & 0xFF;
                int b2 = ((byte) (index >> 8)) & 0xFF;
                int b3 = ((byte)(index >> 16)) & 0xFF;
                byte b4 = (byte) (index >> 24);
                pos.x = offsetX + (((b2 + ((MathMan.unpair8x(b4)) << 8)) << 21) >> 21);
                pos.y = offsetY + b1;
                pos.z = offsetZ + (((b3 + ((MathMan.unpair8y(b4)) << 8)) << 21) >> 21);
                return pos;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public void set(int x, int y, int z) throws RegionOperationException{
        if (populated) {
            if (++count > 1048576) {
                throw new RegionOperationException("Selection is too large! (1048576 blocks)");
            }
            x -= offsetX;
            y -= offsetY;
            z -= offsetZ;
        } else {
            offsetX = x;
            offsetZ = z;
            x = 0;
            z = 0;
            populated = true;
        }
        set.set(pair(x, y, z), true);
        if (x >= 1024 || x <= -1024 || z >= 1024 || z <= -1024) {
            throw new RegionOperationException("Selection is too large! (1024 blocks wide)");
        }
        if (x > maxX) {
            maxX = x;
        }
        if (x < minX) {
            minX = x;
        }
        if (z > maxZ) {
            maxZ = z;
        }
        if (z < minZ) {
            minZ = z;
        }
        if (y > maxY) {
            maxY = y;
        }
        if (y < minY) {
            minY = y;
        }
    }

    public boolean contains(int x, int y, int z) {
        try {
            return set.get(pair(x - offsetX, y - offsetY, z - offsetZ));
        } catch (IndexOutOfBoundsException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Vector getMinimumPoint() {
        return new Vector(minX, minY, minZ);
    }

    @Override
    public Vector getMaximumPoint() {
        return new Vector(maxX, maxY, maxZ);
    }

    @Override
    public void expand(Vector... changes) throws RegionOperationException {
        throw new RegionOperationException("Selection is too large!");
    }

    @Override
    public void contract(Vector... changes) throws RegionOperationException {
        throw new RegionOperationException("Selection is too large!");
    }

    @Override
    public boolean contains(Vector position) {
        return contains((int) position.x, (int) position.y, (int) position.z);
    }

    @Override
    public void shift(Vector change) throws RegionOperationException {
        offsetX += change.getBlockX();
        offsetY += change.getBlockY();
        offsetZ += change.getBlockZ();
        minX += change.getBlockX();
        minY += change.getBlockY();
        minZ += change.getBlockZ();
        maxX += change.getBlockX();
        maxY += change.getBlockY();
        maxZ += change.getBlockZ();
    }

    public void setExtent(EditSession extent) {
        this.extent = extent;
    }
}
