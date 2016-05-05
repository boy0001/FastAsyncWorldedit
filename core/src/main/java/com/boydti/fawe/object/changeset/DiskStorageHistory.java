package com.boydti.fawe.object.changeset;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.IntegerPair;
import com.boydti.fawe.object.RegionWrapper;
import com.sk89q.jnbt.NBTInputStream;
import com.sk89q.jnbt.NBTOutputStream;
import com.sk89q.worldedit.world.World;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPOutputStream;
import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4InputStream;

/**
 * Store the change on disk
 *  - High disk usage
 *  - Moderate CPU usage
 *  - Minimal memory usage
 *  - Slow
 */
public class DiskStorageHistory extends FaweStreamChangeSet {

    private UUID uuid;
    private File bdFile;
    private File nbtfFile;
    private File nbttFile;
    private File entfFile;
    private File enttFile;

    /**
     * Summary of this change (not accurate for larger edits)
     */
    private DiskStorageSummary summary;

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

    // Entity Create From
    private NBTOutputStream osENTCF;
    private GZIPOutputStream osENTCFG;

    // Entity Create To
    private NBTOutputStream osENTCT;
    private GZIPOutputStream osENTCTG;

    private World world;

    public void deleteFiles() {
        bdFile.delete();
        nbtfFile.delete();
        nbttFile.delete();
        entfFile.delete();
        enttFile.delete();
    }

    public DiskStorageHistory(World world, UUID uuid) {
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

    private void init(World world, UUID uuid, int i) {
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

    @Override
    public boolean flush() {
        boolean flushed = false;
        try {
            if (osBD != null) {
                flushed = true;
                osBD.flush();
                osBD.close();
                osBD = null;
            }
            if (osNBTF != null) {
                flushed = true;
                osNBTFG.flush();
                osNBTF.close();
                osNBTF = null;
                osNBTFG = null;
            }
            if (osNBTT != null) {
                flushed = true;
                osNBTTG.flush();
                osNBTT.close();
                osNBTT = null;
                osNBTTG = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return flushed;
    }

    @Override
    public int getCompressedSize() {
        return bdFile.exists() ? (int) bdFile.length() : 0;
    }

    @Override
    public OutputStream getBlockOS(int x, int y, int z) throws IOException {
        if (osBD != null) {
            return osBD;
        }
        bdFile.getParentFile().mkdirs();
        bdFile.createNewFile();
        osBD = getCompressedOS(new FileOutputStream(bdFile));
        setOrigin(x, z);
        osBD.write((byte) (x >> 24));
        osBD.write((byte) (x >> 16));
        osBD.write((byte) (x >> 8));
        osBD.write((byte) (x));
        osBD.write((byte) (z >> 24));
        osBD.write((byte) (z >> 16));
        osBD.write((byte) (z >> 8));
        osBD.write((byte) (z));
        return osBD;
    }

    @Override
    public NBTOutputStream getEntityCreateOS() throws IOException {
        if (osENTCT != null) {
            return osENTCT;
        }
        enttFile.getParentFile().mkdirs();
        enttFile.createNewFile();
        osENTCTG = new GZIPOutputStream(new FileOutputStream(enttFile), true);
        osENTCT = new NBTOutputStream(osENTCTG);
        return osENTCT;
    }

    @Override
    public NBTOutputStream getEntityRemoveOS() throws IOException {
        if (osENTCF != null) {
            return osENTCF;
        }
        entfFile.getParentFile().mkdirs();
        entfFile.createNewFile();
        osENTCFG = new GZIPOutputStream(new FileOutputStream(entfFile), true);
        osENTCF = new NBTOutputStream(osENTCFG);
        return osENTCF;
    }

    @Override
    public NBTOutputStream getTileCreateOS() throws IOException {
        if (osNBTT != null) {
            return osNBTT;
        }
        nbttFile.getParentFile().mkdirs();
        nbttFile.createNewFile();
        osNBTTG = new GZIPOutputStream(new FileOutputStream(nbttFile), true);
        osNBTT = new NBTOutputStream(osNBTTG);
        return osNBTT;
    }

    @Override
    public NBTOutputStream getTileRemoveOS() throws IOException {
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

    @Override
    public InputStream getBlockIS() throws IOException {
        if (!bdFile.exists()) {
            return null;
        }
        InputStream is = getCompressedIS(new FileInputStream(bdFile));
        int x = ((is.read() << 24) + (is.read() << 16) + (is.read() << 8) + (is.read() << 0));
        int z = ((is.read() << 24) + (is.read() << 16) + (is.read() << 8) + (is.read() << 0));
        setOrigin(x, z);
        return is;
    }

    @Override
    public NBTInputStream getEntityCreateIS() throws IOException {
        if (!enttFile.exists()) {
            return null;
        }
        return new NBTInputStream(getCompressedIS(new FileInputStream(enttFile)));
    }

    @Override
    public NBTInputStream getEntityRemoveIS() throws IOException {
        if (!entfFile.exists()) {
            return null;
        }
        return new NBTInputStream(getCompressedIS(new FileInputStream(entfFile)));
    }

    @Override
    public NBTInputStream getTileCreateIS() throws IOException {
        if (!nbttFile.exists()) {
            return null;
        }
        return new NBTInputStream(getCompressedIS(new FileInputStream(nbttFile)));
    }

    @Override
    public NBTInputStream getTileRemoveIS() throws IOException {
        if (!nbtfFile.exists()) {
            return null;
        }
        return new NBTInputStream(getCompressedIS(new FileInputStream(nbtfFile)));
    }

    public DiskStorageSummary summarize(RegionWrapper requiredRegion, boolean shallow) {
        if (summary != null) {
            return summary;
        }
        if (bdFile.exists()) {
            int ox = getOriginX();
            int oz = getOriginZ();
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
                setOrigin(ox, oz);
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
                    int combined1 = buffer[7] & 0xFF;
                    int combined2 = buffer[8] & 0xFF;
                    summary.add(x, z, ((combined2 << 4) + (combined1 >> 4)));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return summary;
    }

    public IntegerPair readHeader() {
        int ox = getOriginX();
        int oz = getOriginZ();
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
                setOrigin(ox, oz);
                fis.close();
                gis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return new IntegerPair(ox, oz);
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
