package com.boydti.fawe.object.regions;

import com.boydti.fawe.object.visitor.FuzzySearch;
import com.boydti.fawe.util.MathMan;
import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.operation.Operations;
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

    @Override
    public int getArea() {
        return set.cardinality();
    }

    public void select(int x, int y, int z) {
        FuzzySearch search = new FuzzySearch(this, extent, new Vector(x, y, z));
        Operations.completeBlindly(search);
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
                int b1 = (index & 0xFF);
                int b2 = ((byte) (index >> 8)) & 0x7F;
                int b3 = ((byte)(index >> 15)) & 0xFF;
                int b4 = ((byte) (index >> 23)) & 0xFF;
                pos.x = offsetX + (((b3 + ((MathMan.unpair8x(b2)) << 8)) << 21) >> 21);
                pos.y = offsetY + b1;
                pos.z = offsetZ + (((b4 + ((MathMan.unpair8y(b2)) << 8)) << 21) >> 21);
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
            if (++count > 16777216) {
                throw new RegionOperationException("Selection is too large! (16777216 blocks)");
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
        set.set(MathMan.tripleSearchCoords(x, y, z), true);
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
            return set.get(MathMan.tripleSearchCoords(x - offsetX, y - offsetY, z - offsetZ));
        } catch (IndexOutOfBoundsException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Vector getMinimumPoint() {
        return new Vector(minX + offsetX, minY + offsetY, minZ + offsetZ);
    }

    @Override
    public Vector getMaximumPoint() {
        return new Vector(maxX + offsetX, maxY + offsetY, maxZ + offsetZ);
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
        return contains(position.getBlockX(), position.getBlockY(), position.getBlockZ());
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
