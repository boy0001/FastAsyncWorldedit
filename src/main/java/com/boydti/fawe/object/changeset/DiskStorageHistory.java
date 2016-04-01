package com.boydti.fawe.object.changeset;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweCache;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.NBTOutputStream;
import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.history.change.BlockChange;
import com.sk89q.worldedit.history.change.Change;
import com.sk89q.worldedit.history.changeset.ChangeSet;

/**
 * Store the change on disk
 *  - High disk usage
 *  - Moderate CPU usage
 *  - Minimal memory usage
 *  - Slow
 */
public class DiskStorageHistory implements ChangeSet {
    
    private final File bdFile;
    private final File nbtFile;
    private final File anyFile;
    
    private GZIPOutputStream osBD;
    private ObjectOutputStream osANY;
    private NBTOutputStream osNBT;
    
    private int ox;
    private int oz;

    private final AtomicInteger size;
    
    public DiskStorageHistory() {
        size = new AtomicInteger();
        UUID uuid = UUID.randomUUID();
        nbtFile = new File(Fawe.imp().getDirectory(), "history" + File.separator + uuid.toString() + ".nbt");
        bdFile = new File(Fawe.imp().getDirectory(), "history" + File.separator + uuid.toString() + ".bd");
        anyFile = new File(Fawe.imp().getDirectory(), "history" + File.separator + uuid.toString() + ".any");
    }
    
    @Override
    public void add(Change change) {
        size.incrementAndGet();
        if ((change instanceof BlockChange)) {
            add((BlockChange) change);
        } else {
            System.out.print("[FAWE] Does not support " + change + " yet! (Please bug Empire92)");
        }
    }
    
    public void flush() {
        try {
            if (osBD != null) {
                osBD.flush();
                osBD.close();
            }
            if (osANY != null) {
                osANY.flush();
                osANY.close();
            }
            if (osNBT != null) {
                osNBT.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void add(BlockChange change) {
        try {
        BlockVector loc = change.getPosition();
        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();

        BaseBlock from = change.getPrevious();
        BaseBlock to = change.getCurrent();

        int idfrom = from.getId();
        int combinedFrom = (byte) (FaweCache.hasData(idfrom) ? ((idfrom << 4) + from.getData()) : (idfrom << 4));
        CompoundTag nbtFrom = FaweCache.hasData(idfrom) ? from.getNbtData() : null;

        int idTo = to.getId();
        int combinedTo = (byte) (FaweCache.hasData(idTo) ? ((idTo << 4) + to.getData()) : (idTo << 4));
        CompoundTag nbtTo = FaweCache.hasData(idTo) ? to.getNbtData() : null;
        
            GZIPOutputStream stream = getBAOS(x, y, z);
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

        /*
         * [header]
         * [int x][int z] origin
         * [contents]
         * relative: short x,short z,unsigned byte y
         * from: char
         * to: char
         */
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private GZIPOutputStream getBAOS(int x, int y, int z) throws IOException {
        if (osBD != null) {
            return osBD;
        }
        bdFile.getParentFile().mkdirs();
        bdFile.createNewFile();
        osBD = new GZIPOutputStream(new FileOutputStream(bdFile), true);
        ox = x;
        oz = z;
        osBD.write((ox) & 0xff);
        osBD.write(((ox) >> 8) & 0xff);
        osBD.write((oz) & 0xff);
        osBD.write(((oz) >> 8) & 0xff);
        return osBD;
    }
    
    private NBTOutputStream getNBTOS(int x, int y, int z) throws IOException {
        if (osNBT != null) {
            return osNBT;
        }
        nbtFile.getParentFile().mkdirs();
        nbtFile.createNewFile();
        //        osNBT = new FileOutputStream(bdFile);
        // TODO FIXME
        return osNBT;
    }

    @SuppressWarnings("resource")
    public Iterator<Change> getIterator(final boolean dir) {
        flush();
        try {
            if (bdFile.exists()) {
                final GZIPInputStream gis = new GZIPInputStream(new FileInputStream(bdFile));
                gis.skip(4);
                return new Iterator<Change>() {
                    
                    private Change last = read();

                    public Change read() {
                        try {
                            int x = gis.read() + (gis.read() << 8) + ox;
                            int z = gis.read() + (gis.read() << 8) + oz;
                            int y = gis.read() & 0xff;
                            int from1 = gis.read();
                            int from2 = gis.read();
                            BaseBlock from = new BaseBlock(((from2 << 4) + (from1 >> 4)), (from1 & 0xf));
                            int to1 = gis.read();
                            int to2 = gis.read();
                            BaseBlock to = new BaseBlock(((to2 << 4) + (to1 >> 4)), (to1 & 0xf));
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
        flush();
        return size.get();
    }

}
