package com.boydti.fawe.object.collection;

import com.boydti.fawe.util.MathMan;
import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.Vector;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

/**
 * The LocalBlockVectorSet is a Memory and CPU optimized set for storing BlockVectors which are all in a local region
 *  - All vectors must be in a 2048 * 2048 area centered around the first entry
 *  - This will use 8 bytes for every 64 BlockVectors (about 800x less than a HashSet)
 */
public class LocalBlockVectorSet implements Set<Vector> {
    private int offsetX, offsetZ;
    private final SparseBitSet set;
    private BlockVector mVec = new BlockVector(0, 0, 0);

    public LocalBlockVectorSet() {
        offsetX = offsetZ = Integer.MAX_VALUE;
        this.set = new SparseBitSet();
    }

    @Override
    public int size() {
        return set.cardinality();
    }

    @Override
    public boolean isEmpty() {
        return set.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        if (o instanceof Vector) {
            Vector v = (Vector) o;
            int x = v.getBlockX();
            int y = v.getBlockY();
            int z = v.getBlockZ();
            return set.get(MathMan.tripleSearchCoords(x - offsetX, y, z - offsetZ));
        }
        return false;
    }

    @Override
    public Iterator<Vector> iterator() {
        return new Iterator<Vector>() {
            int index = set.nextSetBit(0);
            int previous = -1;
            BlockVector mutable = new BlockVector(0, 0, 0);
            @Override
            public void remove() {
                set.clear(previous);
            }
            @Override
            public boolean hasNext() {
                return index != -1;
            }
            @Override
            public BlockVector next() {
                if (index != -1) {
                    int b1 = (index & 0xFF);
                    int b2 = ((byte) (index >> 8)) & 0x7F;
                    int b3 = ((byte)(index >> 15)) & 0xFF;
                    int b4 = ((byte) (index >> 23)) & 0xFF;
                    mutable.x = offsetX + (((b3 + ((MathMan.unpair8x(b2)) << 8)) << 21) >> 21);
                    mutable.y = b1;
                    mutable.z = offsetZ + (((b4 + ((MathMan.unpair8y(b2)) << 8)) << 21) >> 21);
                    previous = index;
                    index = set.nextSetBit(index + 1);
                    return mutable;
                }
                return null;
            }
        };
    }

    @Override
    public Object[] toArray() {
        return toArray(null);
    }

    @Override
    public <T> T[] toArray(T[] array) {
        int size = size();
        if (array == null || array.length < size) {
            array = (T[]) new BlockVector[size];
        }
        int index = 0;
        for (int i = 0; i < size; i++) {
            index = set.nextSetBit(index);
            int b1 = (index & 0xFF);
            int b2 = ((byte) (index >> 8)) & 0x7F;
            int b3 = ((byte)(index >> 15)) & 0xFF;
            int b4 = ((byte) (index >> 23)) & 0xFF;
            int x = offsetX + (((b3 + ((MathMan.unpair8x(b2)) << 8)) << 21) >> 21);
            int y = b1;
            int z = offsetZ + (((b4 + ((MathMan.unpair8y(b2)) << 8)) << 21) >> 21);
            array[i] = (T) new BlockVector(x, y, z);
            index++;
        }
        return array;
    }

    @Override
    public boolean add(Vector vector) {
        if (offsetX == Integer.MAX_VALUE) {
            offsetX = vector.getBlockX();
            offsetZ = vector.getBlockZ();
        }
        int relX = vector.getBlockX() - offsetX;
        int relZ = vector.getBlockZ() - offsetZ;
        if (relX > 1023 || relX < -1024 || relZ > 1023 || relZ < -1024) {
            throw new UnsupportedOperationException("LocalVectorSet can only contain vectors within 1024 blocks (cuboid) of the first entry. ");
        }
        int index = getIndex(vector);
        boolean value = set.get(index);
        set.set(index);
        return !value;
    }

    private int getIndex(Vector vector) {
        return MathMan.tripleSearchCoords(vector.getBlockX() - offsetX, vector.getBlockY(), vector.getBlockZ() - offsetZ);
    }

    @Override
    public boolean remove(Object o) {
        if (o instanceof Vector) {
            Vector v = (Vector) o;
            int relX = v.getBlockX() - offsetX;
            int relZ = v.getBlockZ() - offsetZ;
            if (relX > 1023 || relX < -1024 || relZ > 1023 || relZ < -1024) {
                return false;
            }
            int index = MathMan.tripleSearchCoords(relX, v.getBlockY(), relZ);
            boolean value = set.get(index);
            set.clear(index);
            return value;
        }
        return false;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        for (Object o : c) {
            if (!contains(o)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean addAll(Collection<? extends Vector> c) {
        boolean result = false;
        for (Vector v : c) {
            result |= add(v);
        }
        return result;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        boolean result = false;
        int size = size();
        int index = 0;
        for (int i = 0; i < size; i++) {
            index = set.nextSetBit(index);
            int b1 = (index & 0xFF);
            int b2 = ((byte) (index >> 8)) & 0x7F;
            int b3 = ((byte)(index >> 15)) & 0xFF;
            int b4 = ((byte) (index >> 23)) & 0xFF;
            mVec.x = offsetX + (((b3 + ((MathMan.unpair8x(b2)) << 8)) << 21) >> 21);
            mVec.y = b1;
            mVec.z = offsetZ + (((b4 + ((MathMan.unpair8y(b2)) << 8)) << 21) >> 21);
            if (!c.contains(mVec)) {
                result = true;
                set.clear(index);
            }
            index++;
        }
        return result;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        boolean result = false;
        for (Object o : c) {
            result |= remove(o);
        }
        return result;
    }

    @Override
    public void clear() {
        set.clear();
    }
}
