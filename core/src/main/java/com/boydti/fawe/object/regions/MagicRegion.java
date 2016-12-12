package com.boydti.fawe.object.regions;

import com.boydti.fawe.util.EditSessionBuilder;
import com.boydti.fawe.util.MathMan;
import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.regions.AbstractRegion;
import com.sk89q.worldedit.regions.RegionOperationException;
import com.sk89q.worldedit.world.World;
import java.util.BitSet;
import java.util.Iterator;

public class MagicRegion extends AbstractRegion {

    private BitSet set = new BitSet();
    private int minX, minY, minZ, maxX, maxY, maxZ;
    private int offsetX, offsetY, offsetZ;

    {
        minX = minY = minZ = Integer.MAX_VALUE;
        maxX = maxY = maxZ = Integer.MIN_VALUE;
    }

    public MagicRegion(World world) {
        super(world);
    }

    private static int pair(int x, int y, int z) {
        byte b1 = (byte) y;
        byte b2 = (byte) (x);
        byte b3 = (byte) (z);
        int x16 = (x >> 8) & 0xF;
        int z16 = (z >> 8) & 0xF;
        byte b4 = MathMan.pair16(x16, z16);
        return (b1 & 0xFF)
             + ((b2 & 0xFF) << 8)
             + ((b3 & 0xFF) << 16)
             + ((b4 & 0xFF) << 24)
             ;
    }

    public void select(int x, int y, int z) {
        EditSession editSession = new EditSessionBuilder(getWorld())
                .limitUnlimited()
                .changeSetNull()
                .fastmode(true)
                .allowedRegionsEverywhere()
                .checkMemory(false)
                .autoQueue(false)
                .build();

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
                int b1 = index & 0xFF;
                int b2 = (index >> 8) & 0xFF;
                int b3 = (index >> 16) & 0xFF;
                byte b4 = (byte) ((index >> 24) & 0xFF);
                pos.x = offsetX + (((b2 & 0xFF) + ((MathMan.unpair16x(b4)) << 8)) << 20) >> 20;
                pos.y = offsetY + b1;
                pos.z = offsetZ + (((b3) + ((MathMan.unpair16y(b4)) << 8)) << 20) >> 20;
                return pos;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }


    public void set(int x, int y, int z) throws RegionOperationException{
        x -= offsetX;
        y -= offsetY;
        z -= offsetZ;
        set.set(pair(x, y, z), true);
        if (x >= 2048 || x <= -2048 || z >= 2048 || z <= -2048) {
            throw new RegionOperationException("Selection is too large!");
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
        return set.get(pair(x - offsetX, y - offsetY, z - offsetZ));
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
}
