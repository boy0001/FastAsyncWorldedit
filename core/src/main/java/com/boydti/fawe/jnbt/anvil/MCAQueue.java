package com.boydti.fawe.jnbt.anvil;

import com.boydti.fawe.example.CharFaweChunk;
import com.boydti.fawe.example.NMSMappedFaweQueue;
import com.boydti.fawe.example.NullFaweChunk;
import com.boydti.fawe.object.FaweChunk;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.FaweQueue;
import com.boydti.fawe.object.RegionWrapper;
import com.boydti.fawe.object.RunnableVal2;
import com.boydti.fawe.object.RunnableVal4;
import com.boydti.fawe.util.MainUtil;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.world.biome.BaseBiome;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

public class MCAQueue extends NMSMappedFaweQueue<FaweQueue, FaweChunk, FaweChunk, FaweChunk> {

    private FaweQueue parent;
    private NMSMappedFaweQueue parentNMS;
    private final boolean hasSky;
    private final File saveFolder;
    private final ThreadLocal<MutableMCABackedBaseBlock> blockStore = new ThreadLocal<MutableMCABackedBaseBlock>() {
        @Override
        protected MutableMCABackedBaseBlock initialValue() {
            return new MutableMCABackedBaseBlock();
        }
    };

    public MCAQueue(FaweQueue parent) {
        super(parent.getWEWorld(), new MCAQueueMap());
        this.parent = parent;
        if (parent instanceof NMSMappedFaweQueue) {
            parentNMS = (NMSMappedFaweQueue) parent;
        }
        ((MCAQueueMap) getFaweQueueMap()).setParentQueue(parent);
        hasSky = parent.hasSky();
        saveFolder = parent.getSaveFolder();
    }

    public MCAQueue(String world, File saveFolder, boolean hasSky) {
        super(world, new MCAQueueMap());
        ((MCAQueueMap) getFaweQueueMap()).setParentQueue(this);
        this.saveFolder = saveFolder;
        this.hasSky = hasSky;
    }

    @Override
    public FaweChunk loadChunk(FaweQueue faweQueue, int x, int z, boolean generate) {
        return getFaweChunk(x, z);
    }

    @Override
    public FaweChunk getSections(FaweChunk faweChunk) {
        return faweChunk;
    }

    @Override
    public FaweChunk getCachedChunk(FaweQueue faweQueue, int cx, int cz) {
        return getFaweChunk(cx, cz);
    }

    @Override
    public int getBiome(FaweChunk faweChunk, int x, int z) {
        if (faweChunk instanceof MCAChunk) {
            return ((MCAChunk) faweChunk).getBiomeArray()[((z & 0xF) << 4 | x & 0xF)];
        } else if (parent != null){
            return parent.getBiomeId(x, z);
        } else {
            return 0;
        }
    }

    public void pasteRegion(MCAQueue from, final RegionWrapper regionFrom, Vector offset) throws IOException {
        offset = new Vector((offset.getBlockX() >> 4) << 4, 0, (offset.getBlockZ() >> 4) << 4);
        int oX = offset.getBlockX();
        int oZ = offset.getBlockZ();
        int oY = offset.getBlockY();
        int oCX = oX >> 4;
        int oCZ = oZ >> 4;
        RegionWrapper regionTo = new RegionWrapper(regionFrom.minX + oX, regionFrom.maxX + oX, regionFrom.minZ + oZ, regionFrom.maxZ + oZ);
        File folder = getSaveFolder();
        final ForkJoinPool pool = new ForkJoinPool();
        int bMcaX = (regionTo.minX >> 9);
        int bMcaZ = (regionTo.minZ >> 9);
        int tMcaX = (regionTo.maxX >> 9);
        int tMcaZ = (regionTo.maxZ >> 9);
        for (int mcaZ = bMcaZ; mcaZ <= tMcaZ; mcaZ++) {
            for (int mcaX = bMcaX; mcaX <= tMcaX; mcaX++) {
                int bcx = Math.max(mcaX << 5, regionTo.minX >> 4);
                int bcz = Math.max(mcaZ << 5, regionTo.minZ >> 4);
                int tcx = Math.min((mcaX << 5) + 31, regionTo.maxX >> 4);
                int tcz = Math.min((mcaZ << 5) + 31, regionTo.maxZ >> 4);
                File file = new File(folder, "r." + mcaX + "." + mcaZ + ".mca");
                if (!file.exists()) {
                    file.createNewFile();
                }
                MCAFile mcaFile = new MCAFile(null, file);
                mcaFile.init();
                // If oX & 15 == 0 && oZ && 15 == 0
                if ((oX & 15) == 0 && (oZ & 15) == 0) {
                    if (oY == 0) {
                        if (regionFrom.minY == 0 && regionFrom.maxY == 255) {
                            for (int cz = bcz; cz <= tcz; cz++) {
                                for (int cx = bcx; cx <= tcx; cx++) {
                                    FaweChunk chunk = from.getFaweChunk(cx - oCX, cz - oCZ);
                                    if (!(chunk instanceof NullFaweChunk)) {
                                        MCAChunk mcaChunk = (MCAChunk) chunk;
                                        mcaChunk.setLoc(null, cx, cz);
                                        mcaChunk.setModified();
                                        mcaFile.setChunk(mcaChunk);
                                    }
                                }
                            }
                        } else {
                            for (int cz = bcz; cz <= tcz; cz++) {
                                for (int cx = bcx; cx <= tcx; cx++) {
                                    FaweChunk chunk = from.getFaweChunk(cx - oCX, cz - oCZ);
                                    if (!(chunk instanceof NullFaweChunk)) {
                                        MCAChunk mcaChunk = (MCAChunk) chunk;
                                        MCAChunk toChunk = mcaFile.getChunk(cx, cz);
                                        if (toChunk == null || (toChunk.getMinLayer() << 4 >= regionTo.minY && (toChunk.getMaxLayer() << 4) + 15 <= regionTo.maxY)) {
                                            mcaChunk.setLoc(null, cx, cz);
                                            mcaChunk.setModified();
                                            mcaFile.setChunk(mcaChunk);
                                        } else {

                                        }
                                    }
                                }
                            }
                        }
                    } else if ((oY & 15) == 0) {
                    } else {
                    }
                } else {
                }
                mcaFile.close(pool);
                from.clear();
            }
        }
        pool.awaitQuiescence(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
    }

    public <G, T extends MCAFilter<G>> T filterRegion(final T filter, final RegionWrapper region) {
        this.filterWorld(new MCAFilter<G>() {
            @Override
            public boolean appliesFile(int mcaX, int mcaZ) {
                return region.isInMCA(mcaX, mcaZ) && filter.appliesFile(mcaX, mcaZ);
            }

            @Override
            public MCAFile applyFile(MCAFile file) {
                return filter.applyFile(file);
            }

            @Override
            public boolean appliesChunk(int cx, int cz) {
                return region.isInChunk(cx, cz) && filter.appliesChunk(cx, cz);
            }

            @Override
            public G get() {
                return filter.get();
            }

            @Override
            public MCAChunk applyChunk(MCAChunk chunk, G value) {
                chunk = filter.applyChunk(chunk, value);
                if (chunk != null) {
                    final MutableMCABackedBaseBlock mutableBlock = blockStore.get();
                    mutableBlock.setChunk(chunk);
                    int bx = chunk.getX() << 4;
                    int bz = chunk.getZ() << 4;
                    int tx = bx + 15;
                    int tz = bz + 15;
                    bx = Math.max(bx, region.minX);
                    bz = Math.max(bz, region.minZ);
                    tx = Math.min(tx, region.maxX);
                    tz = Math.min(tz, region.maxZ);
                    int minLayer = region.minY >> 4;
                    int maxLayer = region.maxY >> 4;
                    for (int layer = minLayer; layer <= maxLayer; layer++) {
                        if (chunk.doesSectionExist(layer)) {
                            mutableBlock.setArrays(layer);
                            int yStart = layer << 4;
                            int yEnd = yStart + 15;
                            yStart = Math.max(yStart, region.minY);
                            yEnd = Math.min(yEnd, region.maxY);
                            for (int y = yStart, y0 = (yStart & 15); y <= yEnd; y++, y0++) {
                                int yIndex = ((y0) << 8);
                                mutableBlock.setY(y);
                                for (int z = bz, z0 = bz & 15; z <= tz; z++, z0++) {
                                    int zIndex = yIndex + ((z0) << 4);
                                    mutableBlock.setZ(z);
                                    for (int x = bx, x0 = bx & 15; x <= tx; x++, x0++) {
                                        int xIndex = zIndex + x0;
                                        mutableBlock.setX(x);
                                        mutableBlock.setIndex(xIndex);
                                        filter.applyBlock(x, y, z, mutableBlock, value);
                                    }
                                }
                            }
                        }
                    }
                }
                return null;
            }

            @Override
            public void finishChunk(MCAChunk chunk, G cache) {
                super.finishChunk(chunk, cache);
            }
        });
        return filter;
    }

    public <G, T extends MCAFilter<G>> T createRegion(final T filter, final RegionWrapper region) {
        int botMcaX = region.minX >> 9;
        int botMcaZ = region.minZ >> 9;
        int topMcaX = region.maxX >> 9;
        int topMcaZ = region.maxZ >> 9;
        for (int mcaX = botMcaX >> 9; mcaX <= topMcaX; mcaX++) {
            for (int mcaZ = botMcaZ >> 9; mcaZ <= topMcaZ; mcaZ++) {

            }
        }
        return filter;
    }

    public <G, T extends MCAFilter<G>> T filterWorld(final T filter) {
        File folder = getSaveFolder();
        final ForkJoinPool pool = new ForkJoinPool();
        MainUtil.traverse(folder.toPath(), new RunnableVal2<Path, BasicFileAttributes>() {
            @Override
            public void run(Path path, BasicFileAttributes attr) {
                try {
                    String name = path.getFileName().toString();
                    String[] split = name.split("\\.");
                    final int mcaX = Integer.parseInt(split[1]);
                    final int mcaZ = Integer.parseInt(split[2]);
                    if (filter.appliesFile(mcaX, mcaZ)) {
                        File file = path.toFile();
                        MCAFile mcaFile = new MCAFile(MCAQueue.this, file);
                        final MCAFile original = mcaFile;
                        final MCAFile finalFile = filter.applyFile(mcaFile);
                        if (finalFile != null && !finalFile.isDeleted()) {
                            finalFile.init();
                            // May not do anything, but seems to lead to smaller lag spikes
                            final int cbx = mcaX << 5;
                            final int cbz = mcaZ << 5;

                            finalFile.forEachSortedChunk(new RunnableVal4<Integer, Integer, Integer, Integer>() {
                                @Override
                                public void run(final Integer rcx, final Integer rcz, Integer offset, Integer size) {
                                    pool.submit(new Runnable() {
                                        @Override
                                        public void run() {
                                            try {
                                                int cx = cbx + rcx;
                                                int cz = cbz + rcz;
                                                if (filter.appliesChunk(cx, cz)) {
                                                    MCAChunk chunk = finalFile.getChunk(cx, cz);
                                                    try {
                                                        final G value = filter.get();
                                                        chunk = filter.applyChunk(chunk, value);
                                                        if (chunk != null) {
                                                            final MutableMCABackedBaseBlock mutableBlock = blockStore.get();
                                                            mutableBlock.setChunk(chunk);
                                                            int bx = cx << 4;
                                                            int bz = cz << 4;
                                                            for (int layer = 0; layer < chunk.ids.length; layer++) {
                                                                if (chunk.doesSectionExist(layer)) {
                                                                    mutableBlock.setArrays(layer);
                                                                    int yStart = layer << 4;
                                                                    int index = 0;
                                                                    for (int y = yStart; y < yStart + 16; y++) {
                                                                        mutableBlock.setY(y);
                                                                        for (int z = bz; z < bz + 16; z++) {
                                                                            mutableBlock.setZ(z);
                                                                            for (int x = bx; x < bx + 16; x++, index++) {
                                                                                mutableBlock.setX(x);
                                                                                mutableBlock.setIndex(index);
                                                                                filter.applyBlock(x, y, z, mutableBlock, value);
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                            filter.finishChunk(chunk, value);
                                                        }
                                                    } catch (Throwable e) {
                                                        e.printStackTrace();
                                                    }
                                                }
                                            } catch (Throwable e) {
                                                System.out.println("Failed to load: r." + mcaX + "." + mcaZ + ".mca -> (local) " + rcx + "," + rcz);
                                                e.printStackTrace();
                                            }
                                        }
                                    });
                                }
                            });
                            pool.awaitQuiescence(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
                            original.close(pool);
                            if (original != finalFile) finalFile.close(pool);
                        } else if (mcaFile.isDeleted()) {
                            try {
                                original.close(pool);
                                file.delete();
                            } catch (Throwable ignore) {
                                ignore.printStackTrace();
                            }
                        }
                    }
                } catch (Throwable ignore) {
                    ignore.printStackTrace();
                }
            }
        });
        pool.shutdown();
        try {
            pool.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return filter;
    }

    @Override
    public void relight(int x, int y, int z) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public void relightBlock(int x, int y, int z) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public void relightSky(int x, int y, int z) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public boolean regenerateChunk(FaweQueue faweQueue, int x, int z, BaseBiome biome, Long seed) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public CharFaweChunk getPrevious(CharFaweChunk fs, FaweChunk sections, Map<?, ?> tiles, Collection<?>[] entities, Set<UUID> createdEntities, boolean all) throws Exception {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    public FaweQueue getImpWorld() {
        return parent;
    }

    @Override
    public void setHeightMap(FaweChunk chunk, byte[] heightMap) {
        MCAChunk mca = (MCAChunk) chunk;
        if (mca != null) {
            int[] otherMap = mca.getHeightMapArray();
            for (int i = 0; i < heightMap.length; i++) {
                int newHeight = heightMap[i] & 0xFF;
                int currentHeight = otherMap[i];
                if (newHeight > currentHeight) {
                    otherMap[i] = newHeight;
                }
            }
        }
    }

    @Override
    public void setFullbright(FaweChunk sections) {
        if (sections.getClass() == MCAChunk.class) {
            ((MCAChunk) sections).setFullbright();
        } else if (parentNMS != null) {
            int cx = sections.getX();
            int cz = sections.getZ();
            parentNMS.ensureChunkLoaded(cx, cz);
            Object parentSections = parentNMS.getCachedSections(parentNMS.getWorld(), cx, cz);
            if (parentSections != null) {
                parentNMS.setFullbright(sections);
            }
        }
    }

    @Override
    public boolean removeLighting(FaweChunk sections, RelightMode mode, boolean hasSky) {
        if (mode != RelightMode.NONE) {
            if (sections.getClass() == MCAChunk.class) {
                ((MCAChunk) sections).removeLight();
            } else if (parentNMS != null) {
                int cx = sections.getX();
                int cz = sections.getZ();
                parentNMS.ensureChunkLoaded(cx, cz);
                Object parentSections = parentNMS.getCachedSections(parentNMS.getWorld(), cx, cz);
                if (parentSections != null) {
                    parentNMS.removeLighting(sections, mode, hasSky);
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public void setSkyLight(FaweChunk sections, int x, int y, int z, int value) {
        if (sections.getClass() == MCAChunk.class) {
            ((MCAChunk) sections).setSkyLight(x, y, z, value);
        } else if (parentNMS != null) {
            parentNMS.setSkyLight(x, y, z, value);
        }
    }

    @Override
    public void setBlockLight(FaweChunk sections, int x, int y, int z, int value) {
        if (sections.getClass() == MCAChunk.class) {
            ((MCAChunk) sections).setBlockLight(x, y, z, value);
        } else if (parentNMS != null) {
            parentNMS.setBlockLight(x, y, z, value);
        }
    }

    @Override
    public void refreshChunk(FaweChunk fs) {
        if (fs.getClass() != MCAChunk.class) {
            parentNMS.sendChunk(fs);
        }
    }

    @Override
    public void sendChunk(int x, int z, int bitMask) {
        if (parentNMS != null) {
            parentNMS.sendChunk(x, z, bitMask);
        }
    }

    @Override
    public CompoundTag getTileEntity(FaweChunk sections, int x, int y, int z) {
        if (sections.getClass() == MCAChunk.class) {
            return sections.getTile(x, y, z);
        } else {
            return parentNMS.getTileEntity(x, y, z);
        }
    }

    @Override
    public FaweChunk getFaweChunk(int cx, int cz) {
        return getFaweQueueMap().getFaweChunk(cx, cz);
    }

    @Override
    public File getSaveFolder() {
        return saveFolder;
    }

    @Override
    public boolean hasSky() {
        return hasSky;
    }

    @Override
    public MCAChunk getCachedSections(FaweQueue faweQueue, int cx, int cz) {
        return (MCAChunk) getFaweQueueMap().getFaweChunk(cx, cz);
    }

    @Override
    public FaweChunk getCachedSection(FaweChunk sections, int cy) {
        if (sections.getClass() == MCAChunk.class) {
            if (((MCAChunk) sections).doesSectionExist(cy)) {
                return sections;
            }
            return null;
        } else if (parentNMS != null) {
            return sections;
        }
        return null;
    }

    @Override
    public int getCombinedId4Data(FaweChunk sections, int x, int y, int z) {
        if (sections.getClass() == MCAChunk.class) {
            return sections.getBlockCombinedId(x, y, z);
        } else {
            return parentNMS.getCombinedId4Data(x, y, z);
        }
    }

    @Override
    public int getSkyLight(FaweChunk sections, int x, int y, int z) {
        if (sections.getClass() == MCAChunk.class) {
            return ((MCAChunk) sections).getSkyLight(x, y, z);
        } else {
            return parentNMS.getSkyLight(x, y, z);
        }
    }

    @Override
    public int getEmmittedLight(FaweChunk sections, int x, int y, int z) {
        if (sections.getClass() == MCAChunk.class) {
            return ((MCAChunk) sections).getBlockLight(x, y, z);
        } else {
            return parentNMS.getEmmittedLight(x, y, z);
        }
    }

    @Override
    public void startSet(boolean parallel) {
        if (parent != null) {
            parent.startSet(parallel);
        }
    }

    @Override
    public void endSet(boolean parallel) {
        if (parent != null) {
            parent.endSet(parallel);
        }
    }

    @Override
    public void sendBlockUpdate(FaweChunk chunk, FawePlayer... players) {
        if (parent != null) {
            parentNMS.sendBlockUpdate(chunk, players);
        }
    }
}
