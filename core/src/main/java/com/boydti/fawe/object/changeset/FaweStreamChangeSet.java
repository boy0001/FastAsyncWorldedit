package com.boydti.fawe.object.changeset;

import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.FaweOutputStream;
import com.boydti.fawe.object.change.MutableBlockChange;
import com.boydti.fawe.object.change.MutableEntityChange;
import com.boydti.fawe.object.change.MutableTileChange;
import com.boydti.fawe.util.MainUtil;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.NBTInputStream;
import com.sk89q.jnbt.NBTOutputStream;
import com.sk89q.worldedit.history.change.Change;
import com.sk89q.worldedit.world.World;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;

public abstract class FaweStreamChangeSet extends FaweChangeSet {

    public static final int MODE = 3;
    public static final int HEADER_SIZE = 9;

    private final int compression;

    public FaweStreamChangeSet(World world) {
        this(world, Settings.HISTORY.COMPRESSION_LEVEL);
    }

    public FaweStreamChangeSet(World world, int compression) {
        super(world);
        this.compression = compression;
    }

    public FaweOutputStream getCompressedOS(OutputStream os) throws IOException {
        return MainUtil.getCompressedOS(os, compression);
    }

    @Override
    public int size() {
        // Flush so we can accurately get the size
        flush();
        return blockSize;
    }

    public abstract int getCompressedSize();

    public abstract OutputStream getBlockOS(int x, int y, int z) throws IOException;
    public abstract NBTOutputStream getEntityCreateOS() throws IOException;
    public abstract NBTOutputStream getEntityRemoveOS() throws IOException;
    public abstract NBTOutputStream getTileCreateOS() throws IOException;
    public abstract NBTOutputStream getTileRemoveOS() throws IOException;

    public abstract InputStream getBlockIS() throws IOException;
    public abstract NBTInputStream getEntityCreateIS() throws IOException;
    public abstract NBTInputStream getEntityRemoveIS() throws IOException;
    public abstract NBTInputStream getTileCreateIS() throws IOException;
    public abstract NBTInputStream getTileRemoveIS() throws IOException;

    public int blockSize;
    public int entityCreateSize;
    public int entityRemoveSize;
    public int tileCreateSize;
    public int tileRemoveSize;

    private int originX;
    private int originZ;

    public void setOrigin(int x, int z) {
        originX = x;
        originZ = z;
    }

    public int getOriginX() {
        return originX;
    }

    public int getOriginZ() {
        return originZ;
    }

    public void add(int x, int y, int z, int combinedFrom, int combinedTo) {
        blockSize++;
        try {
            OutputStream stream = getBlockOS(x, y, z);
            //x
            x-=originX;
            stream.write((x) & 0xff);
            stream.write(((x) >> 8) & 0xff);
            //z
            z-=originZ;
            stream.write((z) & 0xff);
            stream.write(((z) >> 8) & 0xff);
            //y
            stream.write((byte) y);
            //from
            stream.write((combinedFrom) & 0xff);
            stream.write(((combinedFrom) >> 8) & 0xff);
            //to
            stream.write((combinedTo) & 0xff);
            stream.write(((combinedTo) >> 8) & 0xff);
        }
        catch (IOException e) {
            MainUtil.handleError(e);
        }
    }

    public void addTileCreate(CompoundTag tag) {
        if (tag == null) {
            return;
        }
        try {
            NBTOutputStream nbtos = getTileCreateOS();
            nbtos.writeNamedTag(tileCreateSize++ + "", tag);
        } catch (IOException e) {
            MainUtil.handleError(e);
        }
    }

    public void addTileRemove(CompoundTag tag) {
        if (tag == null) {
            return;
        }
        try {
            NBTOutputStream nbtos = getTileRemoveOS();
            nbtos.writeNamedTag(tileRemoveSize++ + "", tag);
        } catch (IOException e) {
            MainUtil.handleError(e);
        }
    }

    public void addEntityRemove(CompoundTag tag) {
        if (tag == null) {
            return;
        }
        try {
            NBTOutputStream nbtos = getEntityRemoveOS();
            nbtos.writeNamedTag(entityRemoveSize++ + "", tag);
        } catch (IOException e) {
            MainUtil.handleError(e);
        }
    }

    public void addEntityCreate(CompoundTag tag) {
        if (tag == null) {
            return;
        }
        try {
            NBTOutputStream nbtos = getEntityCreateOS();
            nbtos.writeNamedTag(entityCreateSize++ + "", tag);
        } catch (IOException e) {
            MainUtil.handleError(e);
        }
    }

    public Iterator<MutableBlockChange> getBlockIterator(final boolean dir) throws IOException {
        final InputStream is = getBlockIS();
        if (is == null) {
            return new ArrayList<MutableBlockChange>().iterator();
        }
        final MutableBlockChange change = new MutableBlockChange(0, 0, 0, (short) 0, (byte) 0);
        return new Iterator<MutableBlockChange>() {
            private MutableBlockChange last = read();
            public MutableBlockChange read() {
                try {
                    int read0 = is.read();
                    if (read0 == -1) {
                        return null;
                    }
                    int x = ((byte) read0 & 0xFF) + ((byte) is.read() << 8) + originX;
                    int z = ((byte) is.read() & 0xFF) + ((byte) is.read() << 8) + originZ;
                    int y = is.read() & 0xff;
                    change.x = x;
                    change.y = y;
                    change.z = z;
                    if (dir) {
                        is.skip(2);
                        int to1 = is.read();
                        int to2 = is.read();
                        change.id = (short) ((to2 << 4) + (to1 >> 4));
                        change.data = (byte) (to1 & 0xf);
                    } else {
                        int from1 = is.read();
                        int from2 = is.read();
                        is.skip(2);
                        change.id = (short) ((from2 << 4) + (from1 >> 4));
                        change.data = (byte) (from1 & 0xf);
                    }
                    return change;
                } catch (Exception ignoreEOF) {
                    MainUtil.handleError(ignoreEOF);
                }
                return null;
            }

            @Override
            public boolean hasNext() {
                if (last == null) {
                    last = read();
                }
                if (last != null) {
                    return true;
                }
                try {
                    is.close();
                } catch (IOException e) {
                    MainUtil.handleError(e);
                }
                return false;
            }

            @Override
            public MutableBlockChange next() {
                MutableBlockChange tmp = last;
                last = null;
                return tmp;
            }

            @Override
            public void remove() {
                throw new IllegalArgumentException("CANNOT REMOVE");
            }
        };
    }

    public Iterator<MutableEntityChange> getEntityIterator(final NBTInputStream is, final boolean create, final boolean dir) {
        if (is == null) {
            return new ArrayList<MutableEntityChange>().iterator();
        }
        final MutableEntityChange change = new MutableEntityChange(null, create);
        try {
            return new Iterator<MutableEntityChange>() {
                private CompoundTag last = read();

                public CompoundTag read() {
                    try {
                        return (CompoundTag) is.readNamedTag().getTag();
                    } catch (Exception ignoreEOS) {}
                    return null;
                }

                @Override
                public boolean hasNext() {
                    if (last == null) {
                        last = read();
                    }
                    if (last != null) {
                        return true;
                    }
                    try {
                        is.close();
                    } catch (IOException e) {
                        MainUtil.handleError(e);
                    }
                    return false;
                }

                @Override
                public MutableEntityChange next() {
                    change.tag = last;
                    last = null;
                    return change;
                }

                @Override
                public void remove() {
                    throw new IllegalArgumentException("CANNOT REMOVE");
                }
            };
        } catch (Exception e) {
            MainUtil.handleError(e);
            return null;
        }
    }

    public Iterator<MutableTileChange> getTileIterator(final NBTInputStream is, final boolean create, final boolean dir) {
        if (is == null) {
            return new ArrayList<MutableTileChange>().iterator();
        }
        final MutableTileChange change = new MutableTileChange(null, create);
        try {
            return new Iterator<MutableTileChange>() {
                private CompoundTag last = read();

                public CompoundTag read() {
                    try {
                        return (CompoundTag) is.readNamedTag().getTag();
                    } catch (Exception ignoreEOS) {}
                    return null;
                }

                @Override
                public boolean hasNext() {
                    if (last == null) {
                        last = read();
                    }
                    if (last != null) {
                        return true;
                    }
                    try {
                        is.close();
                    } catch (IOException e) {
                        MainUtil.handleError(e);
                    }
                    return false;
                }

                @Override
                public MutableTileChange next() {
                    change.tag = last;
                    last = null;
                    return change;
                }

                @Override
                public void remove() {
                    throw new IllegalArgumentException("CANNOT REMOVE");
                }
            };
        } catch (Exception e) {
            MainUtil.handleError(e);
            return null;
        }
    }

    public Iterator<Change> getIterator(final boolean dir) {
        flush();
        try {
            final Iterator<MutableTileChange> tileCreate = getTileIterator(getTileCreateIS(), true, dir);
            final Iterator<MutableTileChange> tileRemove = getTileIterator(getTileRemoveIS(), false, dir);

            final Iterator<MutableEntityChange> entityCreate = getEntityIterator(getEntityCreateIS(), true, dir);
            final Iterator<MutableEntityChange> entityRemove = getEntityIterator(getEntityRemoveIS(), false, dir);

            final Iterator<MutableBlockChange> blockChange = getBlockIterator(dir);

            return new Iterator<Change>() {
                Iterator<Change>[] iterators = new Iterator[]{tileCreate, tileRemove, entityCreate, entityRemove, blockChange};
                int i = 0;
                Iterator<Change> current = iterators[0];

                @Override
                public boolean hasNext() {
                    if (current.hasNext()) {
                        return true;
                    } else if (i >= iterators.length - 1) {
                        return false;
                    } else {
                        current = iterators[++i];
                    }
                    return hasNext();
                }

                @Override
                public void remove() {
                    current.remove();
                }

                @Override
                public Change next() {
                    return current.next();
                }
            };
        } catch (Exception e) {
            MainUtil.handleError(e);
        }
        return new ArrayList<Change>().iterator();
    }

    @Override
    public Iterator<Change> backwardIterator() {
        return getIterator(false);
    }

    @Override
    public Iterator<Change> forwardIterator() {
        return getIterator(true);
    }
}
