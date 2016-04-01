package com.boydti.fawe.object.changeset;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweCache;
import com.boydti.fawe.util.MainUtil;
import com.boydti.fawe.util.ReflectionUtils;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.IntTag;
import com.sk89q.jnbt.NBTInputStream;
import com.sk89q.jnbt.NBTOutputStream;
import com.sk89q.jnbt.NamedTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.history.change.BlockChange;
import com.sk89q.worldedit.history.change.Change;
import com.sk89q.worldedit.history.change.EntityCreate;
import com.sk89q.worldedit.history.change.EntityRemove;
import com.sk89q.worldedit.history.changeset.ChangeSet;

/**
 * Store the change on disk
 *  - High disk usage
 *  - Moderate CPU usage
 *  - Minimal memory usage
 *  - Slow
 */
public class DiskStorageHistory implements ChangeSet, FlushableChangeSet {
    
    private final File bdFile;
    private final File nbtfFile;
    private final File nbttFile;
    private final File entfFile;
    private final File enttFile;
    
    /*
     * Block data
     * 
     * [header]
     * {int origin x, int origin z}
     * 
     * [contents]...
     * { short rel x, short rel z, unsigned byte y, short combinedFrom, short combinedTo }
     */
    private GZIPOutputStream osBD;

    // NBT From
    private NBTOutputStream osNBTF;
    private GZIPOutputStream osNBTFG;
    private AtomicInteger osNBTFI;

    // NBT To
    private NBTOutputStream osNBTT;
    private GZIPOutputStream osNBTTG;
    private AtomicInteger osNBTTI;
    
    // Entity From
    private NBTOutputStream osENTF;
    private GZIPOutputStream osENTFG;
    private AtomicInteger osENTFI;
    
    // Entity To
    private NBTOutputStream osENTT;
    private GZIPOutputStream osENTTG;
    private AtomicInteger osENTTI;

    private int ox;
    private int oz;

    private final AtomicInteger size;
    
    public DiskStorageHistory(String name) {
        size = new AtomicInteger();
        String base = "history" + File.separator + name;
        File folder = new File(Fawe.imp().getDirectory(), base);
        int i;
        for (i = 0;; i++) {
            File test = new File(folder, i + ".bd");
            if (!test.exists()) {
                base += File.separator + i;
                nbtfFile = new File(Fawe.imp().getDirectory(), base + ".nbtf");
                nbttFile = new File(Fawe.imp().getDirectory(), base + ".nbtt");
                entfFile = new File(Fawe.imp().getDirectory(), base + ".entf");
                enttFile = new File(Fawe.imp().getDirectory(), base + ".entt");
                bdFile = new File(Fawe.imp().getDirectory(), base + ".bd");
                break;
            }
        }
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
    
    @Override
    public void flush() {
        try {
            if (osBD != null) {
                osBD.flush();
                osBD.close();
                osBD = null;
            }
            if (osNBTF != null) {
                osNBTFG.flush();
                osNBTF.close();
                osNBTF = null;
                osNBTFG = null;
            }
            if (osNBTT != null) {
                osNBTTG.flush();
                osNBTT.close();
                osNBTT = null;
                osNBTTG = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void add(EntityCreate change) {
        
    }
    
    public void add(EntityRemove change) {

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
            int combinedFrom = (FaweCache.hasData(idfrom) ? ((idfrom << 4) + from.getData()) : (idfrom << 4));
            CompoundTag nbtFrom = FaweCache.hasNBT(idfrom) ? from.getNbtData() : null;
    
            int idTo = to.getId();
            int combinedTo = (FaweCache.hasData(idTo) ? ((idTo << 4) + to.getData()) : (idTo << 4));
            CompoundTag nbtTo = FaweCache.hasNBT(idTo) ? to.getNbtData() : null;
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
                
            if (nbtFrom != null && MainUtil.isValidTag(nbtFrom)) {
                Map<String, Tag> value = ReflectionUtils.getMap(nbtFrom.getValue());
                value.put("x", new IntTag(x));
                value.put("y", new IntTag(y));
                value.put("z", new IntTag(z));
                NBTOutputStream nbtos = getNBTFOS(x, y, z);
                nbtos.writeNamedTag(osNBTFI.getAndIncrement() + "", nbtFrom);
            }
            
            if (nbtTo != null && MainUtil.isValidTag(nbtTo)) {
                Map<String, Tag> value = ReflectionUtils.getMap(nbtTo.getValue());
                value.put("x", new IntTag(x));
                value.put("y", new IntTag(y));
                value.put("z", new IntTag(z));
                NBTOutputStream nbtos = getNBTTOS(x, y, z);
                nbtos.writeNamedTag(osNBTTI.getAndIncrement() + "", nbtTo);
            }
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
        osBD.write((byte) (ox >> 24));
        osBD.write((byte) (ox >> 16));
        osBD.write((byte) (ox >> 8));
        osBD.write((byte) (ox));
        osBD.write((byte) (oz >> 24));
        osBD.write((byte) (oz >> 16));
        osBD.write((byte) (oz >> 8));
        osBD.write((byte) (oz));
        return osBD;
    }
    
    private NBTOutputStream getNBTFOS(int x, int y, int z) throws IOException {
        if (osNBTF != null) {
            return osNBTF;
        }
        nbtfFile.getParentFile().mkdirs();
        nbtfFile.createNewFile();
        osNBTFG = new GZIPOutputStream(new FileOutputStream(nbtfFile), true);
        osNBTF = new NBTOutputStream(osNBTFG);
        osNBTFI = new AtomicInteger();
        return osNBTF;
    }
    
    private NBTOutputStream getNBTTOS(int x, int y, int z) throws IOException {
        if (osNBTT != null) {
            return osNBTT;
        }
        nbttFile.getParentFile().mkdirs();
        nbttFile.createNewFile();
        osNBTTG = new GZIPOutputStream(new FileOutputStream(nbttFile), true);
        osNBTT = new NBTOutputStream(osNBTTG);
        osNBTTI = new AtomicInteger();
        return osNBTT;
    }

    private NBTOutputStream getENTFOS(int x, int y, int z) throws IOException {
        if (osNBTF != null) {
            return osNBTF;
        }
        entfFile.getParentFile().mkdirs();
        entfFile.createNewFile();
        osENTFG = new GZIPOutputStream(new FileOutputStream(entfFile), true);
        osENTF = new NBTOutputStream(osENTFG);
        osENTFI = new AtomicInteger();
        return osENTF;
    }
    
    private NBTOutputStream getENTTOS(int x, int y, int z) throws IOException {
        if (osENTT != null) {
            return osENTT;
        }
        enttFile.getParentFile().mkdirs();
        enttFile.createNewFile();
        osENTTG = new GZIPOutputStream(new FileOutputStream(enttFile), true);
        osENTT = new NBTOutputStream(osENTTG);
        osENTTI = new AtomicInteger();
        return osENTT;
    }

    @SuppressWarnings("resource")
    public Iterator<Change> getIterator(final boolean dir) {
        flush();
        try {
            if (bdFile.exists()) {
                if (osNBTF != null) {
                    NBTInputStream os = new NBTInputStream(new GZIPInputStream(new FileInputStream(nbtfFile)));
                    NamedTag tag = os.readNamedTag();
                }
                final NBTInputStream nbtf = osNBTF != null ? new NBTInputStream(new GZIPInputStream(new FileInputStream(nbtfFile))) : null;
                final NBTInputStream nbtt = osNBTT != null ? new NBTInputStream(new GZIPInputStream(new FileInputStream(nbttFile))) : null;

                final GZIPInputStream gis = new GZIPInputStream(new FileInputStream(bdFile));
                gis.skip(8);
                return new Iterator<Change>() {

                    private CompoundTag lastFrom = read(nbtf);
                    private CompoundTag lastTo = read(nbtt);
                    private Change last = read();
                    
                    public CompoundTag read(NBTInputStream stream) {
                        if (stream != null) {
                            try {
                                NamedTag nt = stream.readNamedTag();
                                return nt != null ? ((CompoundTag) nt.getTag()) : null;
                            } catch (IOException e) {}
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
                                    lastFrom = read(nbtf);
                                }
                            }
                            int to1 = gis.read();
                            int to2 = gis.read();
                            BaseBlock to = new BaseBlock(((to2 << 4) + (to1 >> 4)), (to1 & 0xf));
                            if (lastTo != null && FaweCache.hasNBT(to.getId())) {
                                Map<String, Tag> t = lastTo.getValue();
                                if (((IntTag) t.get("x")).getValue() == x && ((IntTag) t.get("z")).getValue() == z && ((IntTag) t.get("y")).getValue() == y) {
                                    to.setNbtData(lastTo);
                                    lastTo = read(nbtt);
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
                            if (nbtf != null) {
                                nbtf.close();
                            }
                            if (nbtt != null) {
                                nbtt.close();
                            }
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
