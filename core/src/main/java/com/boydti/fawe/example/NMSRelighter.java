package com.boydti.fawe.example;

import com.boydti.fawe.FaweCache;
import com.boydti.fawe.object.FaweChunk;
import com.boydti.fawe.object.FaweQueue;
import com.boydti.fawe.util.MathMan;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NMSRelighter {
    private final NMSMappedFaweQueue queue;
    private final HashMap<Long, RelightSkyEntry> skyToRelight;
    private final HashMap<Long, RelightBlockEntry> blocksToRelight;
    private final int maxY;
    private volatile boolean relighting = false;

    private static final int DISPATCH_SIZE = 64;

    public NMSRelighter(NMSMappedFaweQueue queue) {
        this.queue = queue;
        skyToRelight = new HashMap<>();
        blocksToRelight = new HashMap<>();
        this.maxY = queue.getWEWorld().getMaxY();
    }

    public boolean addChunk(int cx, int cz, boolean[] fix) {
        long pair = MathMan.pairInt(cx, cz);
        if (skyToRelight.containsKey(pair)) {
            return false;
        }
        skyToRelight.put(pair, new RelightSkyEntry(cx, cz, fix));
        return true;
    }

    public void removeLighting() {
        for (Map.Entry<Long, RelightSkyEntry> entry : skyToRelight.entrySet()) {
            RelightSkyEntry chunk = entry.getValue();
            queue.ensureChunkLoaded(chunk.x, chunk.z);
            Object sections = queue.getCachedSections(queue.getWorld(), chunk.x, chunk.z);
            queue.removeLighting(sections, FaweQueue.RelightMode.ALL, queue.hasSky());
        }
    }

    public void addBlock(int x, int y, int z) {
        if (y < 1) {
            return;
        }
        int cx = x >> 4;
        int cz = z >> 4;
        long pair = MathMan.pairInt(cx, cz);
        RelightBlockEntry current = blocksToRelight.get(pair);
        if (current == null) {
            current = new RelightBlockEntry(pair);
            blocksToRelight.put(pair, current);
        }
        current.addBlock(x, y, z);
    }

    public void smoothBlockLight(int emit, int x, int y, int z, int rx, int ry, int rz) {
        int opacity = queue.getOpacity(rx, ry, rz);
        if (opacity >= emit) {
            return;
        }
        int emitAdjacent = queue.getEmmittedLight(rx, ry, rz);
        if (emit - emitAdjacent > 1) {
            queue.setBlockLight(rx, ry, rz, emit - 1);
            addBlock(rx, ry, rz);
        }
    }

    public void fixLightingSafe(boolean sky) {
        if (relighting) {
            return;
        }
        relighting = true;
        try {
            if (sky) {
                fixSkyLighting();
            }
            fixBlockLighting();
            sendChunks();
        } catch (Throwable e) {
            e.printStackTrace();
        }
        relighting = false;
    }

    public void fixBlockLighting() {
        while (!blocksToRelight.isEmpty()) {
            RelightBlockEntry current = blocksToRelight.entrySet().iterator().next().getValue();
            int bx = current.getX() << 4;
            int bz = current.getZ() << 4;
            while (!current.blocks.isEmpty()) {
                short coord = current.blocks.pollFirst();
                byte layer = MathMan.unpairShortX(coord);
                int y = MathMan.unpairShortY(coord) & 0xFF;
                int x = MathMan.unpair16x(layer);
                int z = MathMan.unpair16y(layer);
                int xx = bx + x;
                int zz = bz + z;
                int emit = queue.getEmmittedLight(xx, y, zz);
                if (emit < 1) {
                    continue;
                }
                smoothBlockLight(emit, xx, y, zz, xx - 1, y, zz);
                smoothBlockLight(emit, xx, y, zz, xx + 1, y, zz);
                smoothBlockLight(emit, xx, y, zz, xx, y, zz - 1);
                smoothBlockLight(emit, xx, y, zz, xx, y, zz + 1);
                if (y > 0) {
                    smoothBlockLight(emit, xx, y, zz, xx, y - 1, zz);
                }
                if (y < maxY) {
                    smoothBlockLight(emit, xx, y, zz, xx, y + 1, zz);
                }
            }
            blocksToRelight.remove(current.coord);
        }
    }

    public void sendChunks() {
        for (Map.Entry<Long, RelightSkyEntry> entry : skyToRelight.entrySet()) {
            RelightSkyEntry chunk = entry.getValue();
            CharFaweChunk fc = (CharFaweChunk) queue.getFaweChunk(chunk.x, chunk.z);
            int mask = 0;
            for (int y = 0; y < chunk.fix.length; y++) {
                if (chunk.fix[y]) {
                    mask += 1 << y;
                }
            }
            fc.setBitMask(mask);
            queue.sendChunk(fc);
        }

    }

    private boolean isTransparent(int x, int y, int z) {
        return queue.getOpacity(x, y, z) < 15;
    }

    public void lightBlock(int x, int y, int z, int brightness) {
        queue.setBlockLight(x, y, z, Math.max(15, brightness + 1));
        if (isTransparent(x - 1, y, z)) { queue.setBlockLight(x - 1, y, z, brightness); addBlock(x - 1, y, z); }
        if (isTransparent(x + 1, y, z)) { queue.setBlockLight(x + 1, y, z, brightness); addBlock(x + 1, y, z); }
        if (isTransparent(x, y, z - 1)) { queue.setBlockLight(x, y, z - 1, brightness); addBlock(x, y, z - 1); }
        if (isTransparent(x, y, z + 1)) { queue.setBlockLight(x, y, z + 1, brightness); addBlock(x, y, z + 1); }
        if (y > 0 && isTransparent(x, y - 1, z)) { queue.setBlockLight(x, y - 1, z, brightness); addBlock(x, y - 1, z); }
        if (y < maxY && isTransparent(x, y + 1, z)) { queue.setBlockLight(x, y + 1, z, brightness); addBlock(x, y + 1, z); }
    }

    public void fixSkyLighting() {
        // Order chunks
        ArrayList<RelightSkyEntry> chunksList = new ArrayList<>(skyToRelight.values());
        Collections.sort(chunksList);
        int size = chunksList.size();
        if (size > DISPATCH_SIZE) {
            int amount = (size + DISPATCH_SIZE - 1) / DISPATCH_SIZE;
            for (int i = 0; i < amount; i++) {
                int start = i * DISPATCH_SIZE;
                int end = Math.min(size, start + DISPATCH_SIZE);
                List<RelightSkyEntry> sub = chunksList.subList(start, end);
                fixSkyLighting(sub);
            }
        } else {
            fixSkyLighting(chunksList);
        }
    }

    private void fixSkyLighting(List<RelightSkyEntry> sorted) {
        RelightSkyEntry[] chunks = sorted.toArray(new RelightSkyEntry[sorted.size()]);
        byte[] cacheX = FaweCache.CACHE_X[0];
        byte[] cacheZ = FaweCache.CACHE_Z[0];
        for (int y = FaweChunk.HEIGHT - 1; y > 0; y--) {
            for (RelightSkyEntry chunk : chunks) { // Propogate skylight
                int layer = y >> 4;
                if (!chunk.fix[layer])continue;
                int bx = chunk.x << 4;
                int bz = chunk.z << 4;
                byte[] mask = chunk.mask;
                queue.ensureChunkLoaded(chunk.x, chunk.z);
                Object sections = queue.getCachedSections(queue.getWorld(), chunk.x, chunk.z);
                if (sections == null)continue;
                Object section = queue.getCachedSection(sections, layer);
                if (section == null)continue;
                chunk.smooth = false;
                for (int j = 0; j <= maxY; j++) {
                    int x = cacheX[j];
                    int z = cacheZ[j];
                    byte value = mask[j];
                    byte pair = (byte) queue.getOpacityBrightnessPair(section, x, y, z);
                    int opacity = MathMan.unpair16x(pair);
                    int brightness = MathMan.unpair16y(pair);
                    if (brightness > 1 &&  (brightness != 15 || opacity != 15)) {
                        lightBlock(bx + x, y, bz + z, brightness);
                    }
                    if (opacity > 1 && opacity >= value) {
                        mask[j] = 0;
                        queue.setSkyLight(section, x, y, z, 0);
                        continue;
                    }
                    switch (value) {
                        case 0:
                            if (opacity > 1) {
                                queue.setSkyLight(section, x, y, z, 0);
                                continue;
                            }
                            break;
                        case 2:
                        case 4:
                        case 6:
                        case 8:
                        case 10:
                        case 12:
                        case 14:
//                            if (opacity == 0) {
//                                mask[j] = --value;
//                            } else {
//                                mask[j] = (byte) Math.max(0, value - opacity);
//                            }
//                            queue.setSkyLight(section, x, y, z, value);
//                            continue;
                        case 1:
                        case 3:
                        case 5:
                        case 7:
                        case 9:
                        case 11:
                        case 13:
                            if (opacity <= 1) {
                                mask[j] = --value;
                            } else {
                                mask[j] = value = (byte) Math.max(0, value - opacity);
                            }
                            break;
                        case 15:
                            if (opacity > 1) {
                                value -= opacity;
                                mask[j] = value;
                            }
                            queue.setSkyLight(section, x, y, z, value);
                            continue;
                    }
                    chunk.smooth = true;
                    queue.setSkyLight(section, x, y, z, value);
                }
            }
            for (RelightSkyEntry chunk : chunks) { // Smooth forwards
                if (chunk.smooth) {
                    smoothSkyLight(chunk, y, true);
                }
            }
            for (int i = chunks.length - 1; i>= 0; i--) { // Smooth backwards
                RelightSkyEntry chunk = chunks[i];
                if (chunk.smooth) {
                    smoothSkyLight(chunk, y, false);
                }
            }
        }

    }

    public void smoothSkyLight(RelightSkyEntry chunk, int y, boolean direction) {
        byte[] mask = chunk.mask;
        int bx = chunk.x << 4;
        int bz = chunk.z << 4;
        queue.ensureChunkLoaded(chunk.x, chunk.z);
        Object sections = queue.getCachedSections(queue.getWorld(), chunk.x, chunk.z);
        if (sections == null) return;
        Object section = queue.getCachedSection(sections, y >> 4);
        if (section == null) return;
        if (direction) {
            for (int j = 0; j <= maxY; j++) {
                int x = j & 15;
                int z = j >> 4;
                if (mask[j] >= 14 || (mask[j] == 0 && queue.getOpacity(section, x, y, z) > 1)) {
                    continue;
                }
                byte value = mask[j];
                if ((value = (byte) Math.max(queue.getSkyLight(bx + x - 1, y, bz + z) - 1, value)) >= 14);
                else if ((value = (byte) Math.max(queue.getSkyLight(bx + x, y, bz + z - 1) - 1, value)) >= 14);
                if (value > mask[j]) queue.setSkyLight(section, x, y, z, mask[j] = value);
            }
        } else {
            for (int j = maxY; j >= 0; j--) {
                int x = j & 15;
                int z = j >> 4;
                if (mask[j] >= 14 || (mask[j] == 0 && queue.getOpacity(section, x, y, z) > 1)) {
                    continue;
                }
                byte value = mask[j];
                if ((value = (byte) Math.max(queue.getSkyLight(bx + x + 1, y, bz + z) - 1, value)) >= 14);
                else if ((value = (byte) Math.max(queue.getSkyLight(bx + x, y, bz + z + 1) - 1, value)) >= 14);
                if (value > mask[j]) queue.setSkyLight(section, x, y, z, mask[j] = value);
            }
        }
    }

    public boolean isUnlit(byte[] array) {
        for (byte val : array) {
            if (val != 0) {
                return false;
            }
        }
        return true;
    }

    private class RelightBlockEntry {
        public long coord;
        public ArrayDeque<Short> blocks;

        public RelightBlockEntry(long pair) {
            this.coord = pair;
            this.blocks = new ArrayDeque<>(1);
        }

        public void addBlock(int x, int y, int z) {
            byte layer = MathMan.pair16(x & 15, z & 15);
            short coord = MathMan.pairByte(layer, y);
            blocks.add(coord);
        }

        public int getX() {
            return MathMan.unpairIntX(coord);
        }

        public int getZ() {
            return MathMan.unpairIntY(coord);
        }
    }

    private class RelightSkyEntry implements Comparable {
        public final int x;
        public final int z;
        public final byte[] mask;
        public final boolean[] fix;
        public boolean smooth;

        public RelightSkyEntry(int x, int z, boolean[] fix) {
            this.x = x;
            this.z = z;
            byte[] array = new byte[maxY + 1];
            Arrays.fill(array, (byte) 15);
            this.mask = array;
            if (fix == null) {
                this.fix = new boolean[(maxY  + 1) >> 4];
                Arrays.fill(this.fix, true);
            } else {
                this.fix = fix;
            }
        }

        @Override
        public int compareTo(Object o) {
            RelightSkyEntry other = (RelightSkyEntry) o;
            if (other.x < x) {
                return -1;
            }
            if (other.x > x) {
                return 1;
            }
            if (other.z < z) {
                return -1;
            }
            if (other.z > z) {
                return 1;
            }
            return 0;
        }
    }
}
