package com.boydti.fawe.object.changeset;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.FaweInputStream;
import com.boydti.fawe.object.IntegerPair;
import com.boydti.fawe.object.RegionWrapper;
import com.boydti.fawe.util.MainUtil;
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

    // NBT To
    private NBTOutputStream osNBTT;

    // Entity Create From
    private NBTOutputStream osENTCF;

    // Entity Create To
    private NBTOutputStream osENTCT;

    private int index;

    public void deleteFiles() {
        bdFile.delete();
        nbtfFile.delete();
        nbttFile.delete();
        entfFile.delete();
        enttFile.delete();
    }

    public DiskStorageHistory(World world, UUID uuid) {
        super(world);
        String base = "history" + File.separator + Fawe.imp().getWorldName(world) + File.separator + uuid;
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
        init(uuid, ++max);
    }

    public DiskStorageHistory(World world, UUID uuid, int index) {
        super(world);
        init(uuid, index);
    }

    private void init(UUID uuid, int i) {
        this.uuid = uuid;
        this.index = i;
        String base = "history" + File.separator + Fawe.imp().getWorldName(getWorld()) + File.separator + uuid;
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

    public int getIndex() {
        return index;
    }

    @Override
    public boolean flush() {
        super.flush();
        boolean flushed = osBD != null || osNBTF != null || osNBTT != null && osENTCF != null || osENTCT != null;
        try {
            if (osBD != null) {
                osBD.close();
                osBD = null;
            }
            if (osNBTF != null) {
                osNBTF.close();
                osNBTF = null;
            }
            if (osNBTT != null) {
                osNBTT.close();
                osNBTT = null;
            }
            if (osENTCF != null) {
                osENTCF.close();
                osENTCF = null;
            }
            if (osENTCT != null) {
                osENTCT.close();
                osENTCT = null;
            }
        } catch (Exception e) {
            MainUtil.handleError(e);
        }
        return flushed;
    }

    @Override
    public int getCompressedSize() {
        return bdFile.exists() ? (int) bdFile.length() : 0;
    }

    @Override
    public long getSizeInMemory() {
        return 80;
    }

    @Override
    public OutputStream getBlockOS(int x, int y, int z) throws IOException {
        if (osBD != null) {
            return osBD;
        }
        writeHeader(x, y, z);
        return osBD;
    }

    public void writeHeader(int x, int y, int z) throws IOException {
        bdFile.getParentFile().mkdirs();
        bdFile.createNewFile();
        osBD = getCompressedOS(new FileOutputStream(bdFile));
        // Mode
        osBD.write((byte) MODE);
        // Origin
        setOrigin(x, z);
        osBD.write((byte) (x >> 24));
        osBD.write((byte) (x >> 16));
        osBD.write((byte) (x >> 8));
        osBD.write((byte) (x));
        osBD.write((byte) (z >> 24));
        osBD.write((byte) (z >> 16));
        osBD.write((byte) (z >> 8));
        osBD.write((byte) (z));
    }

    @Override
    public NBTOutputStream getEntityCreateOS() throws IOException {
        if (osENTCT != null) {
            return osENTCT;
        }
        enttFile.getParentFile().mkdirs();
        enttFile.createNewFile();
        osENTCT = new NBTOutputStream(getCompressedOS(new FileOutputStream(enttFile)));
        return osENTCT;
    }

    @Override
    public NBTOutputStream getEntityRemoveOS() throws IOException {
        if (osENTCF != null) {
            return osENTCF;
        }
        entfFile.getParentFile().mkdirs();
        entfFile.createNewFile();
        osENTCF = new NBTOutputStream(getCompressedOS(new FileOutputStream(entfFile)));
        return osENTCF;
    }

    @Override
    public NBTOutputStream getTileCreateOS() throws IOException {
        if (osNBTT != null) {
            return osNBTT;
        }
        nbttFile.getParentFile().mkdirs();
        nbttFile.createNewFile();
        osNBTT = new NBTOutputStream(getCompressedOS(new FileOutputStream(nbttFile)));
        return osNBTT;
    }

    @Override
    public NBTOutputStream getTileRemoveOS() throws IOException {
        if (osNBTF != null) {
            return osNBTF;
        }
        nbtfFile.getParentFile().mkdirs();
        nbtfFile.createNewFile();
        osNBTF = new NBTOutputStream(getCompressedOS(new FileOutputStream(nbtfFile)));
        return osNBTF;
    }

    @Override
    public InputStream getBlockIS() throws IOException {
        if (!bdFile.exists()) {
            return null;
        }
        InputStream is = MainUtil.getCompressedIS(new FileInputStream(bdFile));
        // skip mode
        is.skip(1);
        // origin
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
        return new NBTInputStream(MainUtil.getCompressedIS(new FileInputStream(enttFile)));
    }

    @Override
    public NBTInputStream getEntityRemoveIS() throws IOException {
        if (!entfFile.exists()) {
            return null;
        }
        return new NBTInputStream(MainUtil.getCompressedIS(new FileInputStream(entfFile)));
    }

    @Override
    public NBTInputStream getTileCreateIS() throws IOException {
        if (!nbttFile.exists()) {
            return null;
        }
        return new NBTInputStream(MainUtil.getCompressedIS(new FileInputStream(nbttFile)));
    }

    @Override
    public NBTInputStream getTileRemoveIS() throws IOException {
        if (!nbtfFile.exists()) {
            return null;
        }
        return new NBTInputStream(MainUtil.getCompressedIS(new FileInputStream(nbtfFile)));
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
                    FaweInputStream gis = MainUtil.getCompressedIS(fis);
                    // skip mode
                    gis.skip(1);
                    // origin
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
                    int amount = (Settings.HISTORY.BUFFER_SIZE - HEADER_SIZE) / 9;
                    while (!shallow && ++i < amount) {
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
                    MainUtil.handleError(e);
                }
            }
        return summary;
    }

    public IntegerPair readHeader() {
        int ox = getOriginX();
        int oz = getOriginZ();
        if (ox == 0 && oz == 0 && bdFile.exists()) {
            try (FileInputStream fis = new FileInputStream(bdFile)) {
                final InputStream gis = MainUtil.getCompressedIS(fis);
                // skip mode
                gis.skip(1);
                // origin
                ox = ((gis.read() << 24) + (gis.read() << 16) + (gis.read() << 8) + (gis.read() << 0));
                oz = ((gis.read() << 24) + (gis.read() << 16) + (gis.read() << 8) + (gis.read() << 0));
                setOrigin(ox, oz);
                fis.close();
                gis.close();
            } catch (IOException e) {
                MainUtil.handleError(e);
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
