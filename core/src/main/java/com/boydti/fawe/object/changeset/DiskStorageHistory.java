package com.boydti.fawe.object.changeset;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweCache;
import com.boydti.fawe.config.BBC;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.IntegerPair;
import com.boydti.fawe.object.RegionWrapper;
import com.boydti.fawe.util.MainUtil;
import com.boydti.fawe.util.ReflectionUtils;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.IntTag;
import com.sk89q.jnbt.NBTInputStream;
import com.sk89q.jnbt.NBTOutputStream;
import com.sk89q.jnbt.NamedTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.EditSessionFactory;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.history.change.BlockChange;
import com.sk89q.worldedit.history.change.Change;
import com.sk89q.worldedit.history.change.EntityCreate;
import com.sk89q.worldedit.history.change.EntityRemove;
import com.sk89q.worldedit.history.changeset.ChangeSet;
import com.sk89q.worldedit.world.World;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4InputStream;
import net.jpountz.lz4.LZ4OutputStream;

/**
 * Store the change on disk
 *  - High disk usage
 *  - Moderate CPU usage
 *  - Minimal memory usage
 *  - Slow
 */
public class DiskStorageHistory implements ChangeSet, FaweChangeSet {

    private UUID uuid;
    private File bdFile;
    private File nbtfFile;
    private File nbttFile;
    private File entfFile;
    private File enttFile;
    
    /*
     * Block data
     * 
     * [header]
     * {int origin x, int origin z}
     * 
     * [contents]...
     * { short rel x, short rel z, unsigned byte y, short combinedFrom, short combinedTo }
     */
    private OutputStream osBD;

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

    private AtomicInteger size = new AtomicInteger();
    private World world;

    public void deleteFiles() {
        bdFile.delete();
        nbtfFile.delete();
        nbttFile.delete();
        entfFile.delete();
        enttFile.delete();
    }

    public DiskStorageHistory(World world, UUID uuid) {
        size = new AtomicInteger();
        String base = "history" + File.separator + world.getName() + File.separator + uuid;
        File folder = new File(Fawe.imp().getDirectory(), base);
        int max = 0;
        if (folder.exists()) {
            for (File file : folder.listFiles()) {
                String name = file.getName().split("\\.")[0];
                if (name.matches("\\d+")) {
                    int index = Integer.parseInt(name);
                    if (index > max) {
                        max = index;
                    }
                }
            }
        }
        init(world, uuid, ++max);
    }

    public DiskStorageHistory(World world, UUID uuid, int index) {
        init(world, uuid, index);
    }

    public void init(World world, UUID uuid, int i) {
        this.uuid = uuid;
        this.world = world;
        String base = "history" + File.separator + world.getName() + File.separator + uuid;
        base += File.separator + i;
        nbtfFile = new File(Fawe.imp().getDirectory(), base + ".nbtf");
        nbttFile = new File(Fawe.imp().getDirectory(), base + ".nbtt");
        entfFile = new File(Fawe.imp().getDirectory(), base + ".entf");
        enttFile = new File(Fawe.imp().getDirectory(), base + ".entt");
        bdFile = new File(Fawe.imp().getDirectory(), base + ".bd");
    }

    public UUID getUUID() {
        return uuid;
    }

    public File getBDFile() {
        return bdFile;
    }

    public EditSession toEditSession(Player player) {
        EditSessionFactory factory = WorldEdit.getInstance().getEditSessionFactory();
        EditSession edit = factory.getEditSession(world, -1, null, player);
        edit.setChangeSet(this);
        return edit;
    }
    
    @Override
    public void add(Change change) {
        size.incrementAndGet();
        if ((change instanceof BlockChange)) {
            add((BlockChange) change);
        } else {
            Fawe.debug(BBC.PREFIX.s() + "Does not support " + change + " yet! (Please bug Empire92)");
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

    @Override
    public void add(int x, int y, int z, int combinedFrom, int combinedTo) {
        try {
            OutputStream stream = getBAOS(x, y, z);
            //x
            x-=ox;
            stream.write((x) & 0xff);
            stream.write(((x) >> 8) & 0xff);
            //z
            z-=oz;
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
            try {
                Map<String, Tag> value = ReflectionUtils.getMap(nbtTo.getValue());
                value.put("x", new IntTag(x));
                value.put("y", new IntTag(y));
                value.put("z", new IntTag(z));
                NBTOutputStream nbtos = getNBTTOS(x, y, z);
                nbtos.writeNamedTag(osNBTTI.getAndIncrement() + "", nbtTo);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void add(Vector loc, BaseBlock from, BaseBlock to) {
        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();
        try {
            int idfrom = from.getId();
            int combinedFrom = (FaweCache.hasData(idfrom) ? ((idfrom << 4) + from.getData()) : (idfrom << 4));
            CompoundTag nbtFrom = FaweCache.hasNBT(idfrom) ? from.getNbtData() : null;

            if (nbtFrom != null && MainUtil.isValidTag(nbtFrom)) {
                Map<String, Tag> value = ReflectionUtils.getMap(nbtFrom.getValue());
                value.put("x", new IntTag(x));
                value.put("y", new IntTag(y));
                value.put("z", new IntTag(z));
                NBTOutputStream nbtos = getNBTFOS(x, y, z);
                nbtos.writeNamedTag(osNBTFI.getAndIncrement() + "", nbtFrom);
            }
            add(x, y, z, combinedFrom, to);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void add(EntityCreate change) {
        // TODO
    }
    
    public void add(EntityRemove change) {
        // TODO
    }

    public void add(BlockChange change) {
        try {
            BlockVector loc = change.getPosition();
            BaseBlock from = change.getPrevious();
            BaseBlock to = change.getCurrent();
            add(loc, from, to);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private OutputStream getBAOS(int x, int y, int z) throws IOException {
        if (osBD != null) {
            return osBD;
        }
        bdFile.getParentFile().mkdirs();
        bdFile.createNewFile();
        FileOutputStream stream = new FileOutputStream(bdFile);
        LZ4Factory factory = LZ4Factory.fastestInstance();
        LZ4Compressor compressor = factory.fastCompressor();
        osBD = new LZ4OutputStream(stream, Settings.BUFFER_SIZE, factory.fastCompressor());
        if (Settings.COMPRESSION_LEVEL > 0) {
            osBD = new LZ4OutputStream(osBD, Settings.BUFFER_SIZE, factory.highCompressor());
        }
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


    private DiskStorageSummary summary;

    public DiskStorageSummary summarize(RegionWrapper requiredRegion, boolean shallow) {
        if (summary != null) {
            return summary;
        }
        if (bdFile.exists()) {
            if ((ox != 0 || oz != 0) && !requiredRegion.isIn(ox, oz)) {
                return summary = new DiskStorageSummary(ox, oz);
            }
            try (FileInputStream fis = new FileInputStream(bdFile)) {
                LZ4Factory factory = LZ4Factory.fastestInstance();
                LZ4Compressor compressor = factory.fastCompressor();
                final LZ4InputStream gis;
                if (Settings.COMPRESSION_LEVEL > 0) {
                    gis = new LZ4InputStream(new LZ4InputStream(fis));
                } else {
                    gis = new LZ4InputStream(fis);
                }
                ox = ((gis.read() << 24) + (gis.read() << 16) + (gis.read() << 8) + (gis.read() << 0));
                oz = ((gis.read() << 24) + (gis.read() << 16) + (gis.read() << 8) + (gis.read() << 0));
                summary = new DiskStorageSummary(ox, oz);
                if (!requiredRegion.isIn(ox, oz)) {
                    fis.close();
                    gis.close();
                    return summary;
                }
                byte[] buffer = new byte[9];
                int i = 0;
                while (!shallow || gis.hasBytesAvailableInDecompressedBuffer(9)) {
                    if (gis.read(buffer) == -1) {
                        fis.close();
                        gis.close();
                        return summary;
                    }
                    int x = ((byte) buffer[0] & 0xFF) + ((byte) buffer[1] << 8) + ox;
                    int z = ((byte) buffer[2] & 0xFF) + ((byte) buffer[3] << 8) + oz;
                    int combined1 = buffer[7];
                    int combined2 = buffer[8];
                    summary.add(x, z, ((combined2 << 4) + (combined1 >> 4)));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return summary;
    }

    public IntegerPair readHeader() {
        if (ox == 0 && oz == 0 && bdFile.exists()) {
            try {
                FileInputStream fis = new FileInputStream(bdFile);
                LZ4Factory factory = LZ4Factory.fastestInstance();
                LZ4Compressor compressor = factory.fastCompressor();
                final InputStream gis;
                if (Settings.COMPRESSION_LEVEL > 0) {
                    gis = new LZ4InputStream(new LZ4InputStream(fis));
                } else {
                    gis = new LZ4InputStream(fis);
                }
                ox = ((gis.read() << 24) + (gis.read() << 16) + (gis.read() << 8) + (gis.read() << 0));
                oz = ((gis.read() << 24) + (gis.read() << 16) + (gis.read() << 8) + (gis.read() << 0));
                fis.close();
                gis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return new IntegerPair(ox, oz);
    }

    @SuppressWarnings("resource")
    public Iterator<Change> getIterator(final boolean dir) {
        flush();
        try {
            if (bdFile.exists()) {
                final NBTInputStream nbtf = nbtfFile.exists() ? new NBTInputStream(new GZIPInputStream(new FileInputStream(nbtfFile))) : null;
                final NBTInputStream nbtt = nbttFile.exists() ? new NBTInputStream(new GZIPInputStream(new FileInputStream(nbttFile))) : null;

                FileInputStream fis = new FileInputStream(bdFile);
                LZ4Factory factory = LZ4Factory.fastestInstance();
                LZ4Compressor compressor = factory.fastCompressor();
                final InputStream gis;
                if (Settings.COMPRESSION_LEVEL > 0) {
                    gis = new LZ4InputStream(new LZ4InputStream(fis));
                } else {
                    gis = new LZ4InputStream(fis);
                }
                ox = ((gis.read() << 24) + (gis.read() << 16) + (gis.read() << 8) + (gis.read() << 0));
                oz = ((gis.read() << 24) + (gis.read() << 16) + (gis.read() << 8) + (gis.read() << 0));
                return new Iterator<Change>() {

                    private CompoundTag lastFrom = read(nbtf);
                    private CompoundTag lastTo = read(nbtt);
                    private Change last = read();
                    
                    public CompoundTag read(NBTInputStream stream) {
                        if (stream != null) {
                            try {
                                NamedTag nt = stream.readNamedTag();
                                return nt != null ? ((CompoundTag) nt.getTag()) : null;
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        return null;
                    }

                    public Change read() {
                        try {
                            int x = ((byte) gis.read() & 0xFF) + ((byte) gis.read() << 8) + ox;
                            int z = ((byte) gis.read() & 0xFF) + ((byte) gis.read() << 8) + oz;
                            int y = gis.read() & 0xff;
                            int from1 = gis.read();
                            int from2 = gis.read();
                            BaseBlock from = FaweCache.getBlock(((from2 << 4) + (from1 >> 4)), (from1 & 0xf));
                            if (lastFrom != null && FaweCache.hasNBT(from.getId())) {
                                Map<String, Tag> t = lastFrom.getValue();
                                if (((IntTag) t.get("x")).getValue() == x && ((IntTag) t.get("z")).getValue() == z && ((IntTag) t.get("y")).getValue() == y) {
                                    from = new BaseBlock(from.getId(), from.getData());
                                    from.setNbtData(lastFrom);
                                    lastFrom = read(nbtf);
                                }
                            }
                            int to1 = gis.read();
                            int to2 = gis.read();
                            BaseBlock to = FaweCache.getBlock(((to2 << 4) + (to1 >> 4)), (to1 & 0xf));
                            if (lastTo != null && FaweCache.hasNBT(to.getId())) {
                                Map<String, Tag> t = lastTo.getValue();
                                if (((IntTag) t.get("x")).getValue() == x && ((IntTag) t.get("z")).getValue() == z && ((IntTag) t.get("y")).getValue() == y) {
                                    to = new BaseBlock(to.getId(), to.getData());
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
                        throw new IllegalArgumentException("CANNOT REMOVE");
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

    public static class DiskStorageSummary {

        private final int z;
        private final int x;
        public int[] blocks;

        public int minX;
        public int minZ;

        public int maxX;
        public int maxZ;

        public DiskStorageSummary(int x, int z) {
            blocks = new int[256];
            this.x = x;
            this.z = z;
            minX = x;
            maxX = x;
            minZ = z;
            maxZ = z;
        }

        public void add(int x, int z, int id) {
            blocks[id]++;
            if (x < minX) {
                minX = x;
            } else if (x > maxX) {
                maxX = x;
            }
            if (z < minZ) {
                minZ = z;
            } else if (z > maxZ) {
                maxZ = z;
            }
        }

        public HashMap<Integer, Integer> getBlocks() {
            HashMap<Integer, Integer> map = new HashMap<>();
            for (int i = 0; i < blocks.length; i++) {
                if (blocks[i] != 0) {
                    map.put(i, blocks[i]);
                }
            }
            return map;
        }

        public Map<Integer, Double> getPercents() {
            HashMap<Integer, Integer> map = getBlocks();
            int count = getSize();
            HashMap<Integer, Double> newMap = new HashMap<Integer, Double>();
            for (Map.Entry<Integer, Integer> entry : map.entrySet()) {
                int id = entry.getKey();
                int changes = entry.getValue();
                double percent = ((changes * 1000l) / count) / 10d;
                newMap.put(id, percent);
            }
            return newMap;
        }

        public int getSize() {
            int count = 0;
            for (int i = 0; i < blocks.length; i++) {
                count += blocks[i];
            }
            return count;
        }
    }
}
