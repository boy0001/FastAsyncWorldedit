package com.boydti.fawe.object.changeset;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweCache;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.util.MainUtil;
import com.boydti.fawe.util.ReflectionUtils;
import com.google.common.collect.Iterators;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.IntTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.history.change.BlockChange;
import com.sk89q.worldedit.history.change.Change;
import com.sk89q.worldedit.history.changeset.ChangeSet;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4InputStream;
import net.jpountz.lz4.LZ4OutputStream;

/**
 * ChangeSet optimized for low memory usage
 *  - No disk usage
 *  - High CPU usage
 *  - Low memory usage
 */
public class MemoryOptimizedHistory implements ChangeSet, FaweChangeSet {
    
    private ArrayDeque<CompoundTag> fromTags;
    private ArrayDeque<CompoundTag> toTags;
    
    private byte[] ids;
    private Object lock;
    private int decompressedLength;

    private ByteArrayOutputStream idsStream;
    private OutputStream idsStreamZip;

    private ArrayDeque<Change> entities;

    int ox;
    int oz;

    private int size;
    
    public MemoryOptimizedHistory() {

    }


    @Override
    public void add(int x, int y, int z, int combinedFrom, int combinedTo) {
        size++;
        try {
            OutputStream stream = getBAOS(x, y, z);
            //x
            stream.write((x - ox) & 0xff);
            stream.write(((x - ox) >> 8) & 0xff);
            //z
            stream.write((z - oz) & 0xff);
            stream.write(((z - oz) >> 8) & 0xff);
            //y
            stream.write((byte) y);
            //from
            stream.write((combinedFrom) & 0xff);
            stream.write(((combinedFrom) >> 8) & 0xff);
            //to
            stream.write((combinedTo) & 0xff);
            stream.write(((combinedTo) >> 8) & 0xff);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void add(int x, int y, int z, int combinedId4DataFrom, BaseBlock to) {
        int idTo = to.getId();
        int combinedTo = (FaweCache.hasData(idTo) ? ((idTo << 4) + to.getData()) : (idTo << 4));
        CompoundTag nbtTo = FaweCache.hasNBT(idTo) ? to.getNbtData() : null;
        add(x, y, z, combinedId4DataFrom, combinedTo);
        if (nbtTo != null && MainUtil.isValidTag(nbtTo)) {
            Map<String, Tag> value = ReflectionUtils.getMap(nbtTo.getValue());
            value.put("x", new IntTag(x));
            value.put("y", new IntTag(y));
            value.put("z", new IntTag(z));
            if (toTags == null) {
                toTags = new ArrayDeque<>();
            }
            toTags.add(nbtTo);
        }
    }

    @Override
    public void add(Vector loc, BaseBlock from, BaseBlock to) {
        try {
            int x = loc.getBlockX();
            int y = loc.getBlockY();
            int z = loc.getBlockZ();

            int idfrom = from.getId();
            int combinedFrom = (FaweCache.hasData(idfrom) ? ((idfrom << 4) + from.getData()) : (idfrom << 4));
            CompoundTag nbtFrom = FaweCache.hasNBT(idfrom) ? from.getNbtData() : null;
            if (nbtFrom != null && MainUtil.isValidTag(nbtFrom)) {
                Map<String, Tag> value = ReflectionUtils.getMap(nbtFrom.getValue());
                value.put("x", new IntTag(x));
                value.put("y", new IntTag(y));
                value.put("z", new IntTag(z));
                if (fromTags == null) {
                    fromTags = new ArrayDeque<>();
                }
                fromTags.add(nbtFrom);
            }
            add(x, y, z, combinedFrom, to);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void add(Change arg) {
        if ((arg instanceof BlockChange)) {
                BlockChange change = (BlockChange) arg;
                BlockVector loc = change.getPosition();
                BaseBlock from = change.getPrevious();
                BaseBlock to = change.getCurrent();
                add(loc, from, to);
        } else {
            if (entities == null) {
                entities = new ArrayDeque<>();
            }
            entities.add(arg);
        }
    }
    
    private OutputStream getBAOS(int x, int y, int z) throws IOException {
        if (idsStreamZip != null) {
            return idsStreamZip;
        }
        LZ4Factory factory = LZ4Factory.fastestInstance();
        idsStream = new ByteArrayOutputStream(Settings.BUFFER_SIZE);
        idsStreamZip = new LZ4OutputStream(idsStream, Settings.BUFFER_SIZE, factory.fastCompressor());
        if (Settings.COMPRESSION_LEVEL > 0) {
//            Deflater deflater = new Deflater(Math.min(9, Settings.COMPRESSION_LEVEL), true);
//            idsStreamZip = new DeflaterOutputStream(idsStreamZip, deflater, true);
            idsStreamZip = new LZ4OutputStream(idsStreamZip, Settings.BUFFER_SIZE, factory.highCompressor());
        }
        ox = x;
        oz = z;
        return idsStreamZip;
    }
    
    @SuppressWarnings("resource")
    public Iterator<Change> getIterator(final boolean dir) {
        flush();
        if (lock != null) {
            try {
                lock.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        try {
            Iterator<Change> idsIterator;
            Iterator<Change> entsIterator = entities != null ? entities.iterator() : new ArrayList().iterator();
            if (ids == null) {
                idsIterator = new ArrayList().iterator();
            } else {
                ByteArrayInputStream bais = new ByteArrayInputStream(ids);
                final InputStream gis;
                if (Settings.COMPRESSION_LEVEL > 0) {
                    gis = new LZ4InputStream(new LZ4InputStream(bais));
                } else {
                    gis = new LZ4InputStream(bais);
                }
                idsIterator = new Iterator<Change>() {
                    private final Iterator<CompoundTag> lastFromIter = fromTags != null ? fromTags.iterator() : null;
                    private final Iterator<CompoundTag> lastToIter = toTags != null ? toTags.iterator() : null;
                    private CompoundTag lastFrom = read(lastFromIter);
                    private CompoundTag lastTo = read(lastToIter);
                    private Change last = read();
                    
                    public CompoundTag read(Iterator<CompoundTag> tags) {
                        if (tags != null && tags.hasNext()) {
                            return tags.next();
                        }
                        return null;
                    }
                    
                    public Change read() {
                        try {
                            int x = gis.read() + (gis.read() << 8) + ox;
                            int z = gis.read() + (gis.read() << 8) + oz;
                            int y = gis.read() & 0xff;
                            int from1 = gis.read();
                            int from2 = gis.read();
                            BaseBlock from = new BaseBlock(((from2 << 4) + (from1 >> 4)), (from1 & 0xf));
                            if (lastFrom != null && FaweCache.hasNBT(from.getId())) {
                                Map<String, Tag> t = lastFrom.getValue();
                                if (((IntTag) t.get("x")).getValue() == x && ((IntTag) t.get("z")).getValue() == z && ((IntTag) t.get("y")).getValue() == y) {
                                    from.setNbtData(lastFrom);
                                    lastFrom = read(lastFromIter);
                                }
                            }
                            int to1 = gis.read();
                            int to2 = gis.read();
                            BaseBlock to = new BaseBlock(((to2 << 4) + (to1 >> 4)), (to1 & 0xf));
                            if (lastTo != null && FaweCache.hasNBT(to.getId())) {
                                Map<String, Tag> t = lastTo.getValue();
                                if (((IntTag) t.get("x")).getValue() == x && ((IntTag) t.get("z")).getValue() == z && ((IntTag) t.get("y")).getValue() == y) {
                                    to.setNbtData(lastTo);
                                    lastTo = read(lastToIter);
                                }
                            }
                            BlockVector position = new BlockVector(x, y, z);
                            return dir ? new BlockChange(position, to, from) : new BlockChange(position, from, to);
                        } catch (Exception e) {
                            return null;
                        }
                    }
                    
                    @Override
                    public boolean hasNext() {
                        if (last != null) {
                            return true;
                        }
                        try {
                            gis.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return false;
                    }
                    
                    @Override
                    public Change next() {
                        Change tmp = last;
                        last = read();
                        return tmp;
                    }
                    
                    @Override
                    public void remove() {
                        throw new IllegalArgumentException("CANNOT REMIVE");
                    }
                };
            }
            return Iterators.concat(idsIterator, entsIterator);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ArrayList<Change>().iterator();
    }

    @Override
    public Iterator<Change> backwardIterator() {
        return getIterator(false);
    }
    
    @Override
    public Iterator<Change> forwardIterator() {
        return getIterator(false);
    }
    
    @Override
    public int size() {
        return size;
    }
    
    @Override
    public void flush() {
        if (idsStreamZip != null) {
            try {
                idsStream.flush();
                idsStreamZip.flush();
                idsStreamZip.close();
                ids = idsStream.toByteArray();
                // Estimate
                int total = 0x18 * size;
                int ratio = total / ids.length;
                int saved = total - ids.length;
                if (ratio > 3) {
                    // TODO remove this debug message
                    Fawe.debug("[FAWE] History compressed. Saved ~ " + saved + "b (" + ratio + "x smaller)");
                }
                idsStream = null;
                idsStreamZip = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
}
