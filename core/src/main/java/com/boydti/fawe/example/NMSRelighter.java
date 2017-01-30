package com.boydti.fawe.example;

import com.boydti.fawe.FaweCache;
import com.boydti.fawe.object.FaweChunk;
import com.boydti.fawe.object.FaweQueue;
import com.boydti.fawe.object.IntegerTrio;
import com.boydti.fawe.util.MathMan;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public class NMSRelighter implements Relighter{
    private final NMSMappedFaweQueue queue;

    private final Map<Long, RelightSkyEntry> skyToRelight;
    private final Map<Long, Map<Integer, Object>> lightQueue;
    private final Object present = new Object();
    private final Map<Long, Integer> chunksToSend;

    private final int maxY;
    private volatile boolean relighting = false;

    public final IntegerTrio mutableBlockPos = new IntegerTrio();

    private static final int DISPATCH_SIZE = 64;

    public NMSRelighter(NMSMappedFaweQueue queue) {
        this.queue = queue;
        this.skyToRelight = new Long2ObjectOpenHashMap<>();
        this.lightQueue = new Long2ObjectOpenHashMap<>();
        this.chunksToSend = new Long2ObjectOpenHashMap<>();
        this.maxY = queue.getMaxY();
    }

    @Override
    public boolean isEmpty() {
        return skyToRelight.isEmpty() &&  lightQueue.isEmpty();
    }

    public synchronized boolean addChunk(int cx, int cz, byte[] fix, int bitmask) {
        long pair = MathMan.pairInt(cx, cz);
        RelightSkyEntry toPut = new RelightSkyEntry(cx, cz, fix, bitmask);
        RelightSkyEntry existing = skyToRelight.put(pair, toPut);
        if (existing != null) {
            toPut.bitmask |= existing.bitmask;
            if (fix != null) {
                for (int i = 0; i < fix.length; i++) {
                    toPut.fix[i] &= existing.fix[i];
                }
            }
        }
        return true;
    }

    public synchronized void removeLighting() {
        Iterator<Map.Entry<Long, RelightSkyEntry>> iter = skyToRelight.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<Long, RelightSkyEntry> entry = iter.next();
            RelightSkyEntry chunk = entry.getValue();
            long pair = entry.getKey();
            Integer existing = chunksToSend.get(pair);
            chunksToSend.put(pair, chunk.bitmask | (existing != null ? existing : 0));
            queue.ensureChunkLoaded(chunk.x, chunk.z);
            Object sections = queue.getCachedSections(queue.getWorld(), chunk.x, chunk.z);
            queue.removeLighting(sections, FaweQueue.RelightMode.ALL, queue.hasSky());
            iter.remove();
        }
    }

    public synchronized void updateBlockLight(Map<Long, Map<Integer, Object>> map) {
        int size = map.size();
        if (size == 0) {
            return;
        }
        Queue<IntegerTrio> lightPropagationQueue = new ArrayDeque<>();
        Queue<Object[]> lightRemovalQueue = new ArrayDeque<>();
        Map<IntegerTrio, Object> visited = new HashMap<>();
        Map<IntegerTrio, Object> removalVisited = new HashMap<>();

        Iterator<Map.Entry<Long, Map<Integer, Object>>> iter = map.entrySet().iterator();
        while (iter.hasNext() && size-- > 0) {
            Map.Entry<Long, Map<Integer, Object>> entry = iter.next();
            long index = entry.getKey();
            Map<Integer, Object> blocks = entry.getValue();
            int chunkX = MathMan.unpairIntX(index);
            int chunkZ = MathMan.unpairIntY(index);
            int bx = chunkX << 4;
            int bz = chunkZ << 4;
            for (int blockHash : blocks.keySet()) {
                int x = (blockHash >> 12 & 0xF) + bx;
                int y = (blockHash & 0xFF);
                int z = (blockHash >> 8 & 0xF) + bz;
                int lcx = x & 0xF;
                int lcz = z & 0xF;
                int oldLevel = queue.getEmmittedLight(x, y, z);
                int newLevel = queue.getBrightness(x, y, z);
                if (oldLevel != newLevel) {
                    queue.setBlockLight(x, y, z, newLevel);
                    IntegerTrio node = new IntegerTrio(x, y, z);
                    if (newLevel < oldLevel) {
                        removalVisited.put(node, present);
                        lightRemovalQueue.add(new Object[]{node, oldLevel});
                    } else {
                        visited.put(node, present);
                        lightPropagationQueue.add(node);
                    }
                }
            }
            iter.remove();
        }

        while (!lightRemovalQueue.isEmpty()) {
            Object[] val = lightRemovalQueue.poll();
            IntegerTrio node = (IntegerTrio) val[0];
            int lightLevel = (int) val[1];

            this.computeRemoveBlockLight(node.x - 1, node.y, node.z, lightLevel, lightRemovalQueue, lightPropagationQueue, removalVisited, visited);
            this.computeRemoveBlockLight(node.x + 1, node.y, node.z, lightLevel, lightRemovalQueue, lightPropagationQueue, removalVisited, visited);
            if (node.y > 0) {
                this.computeRemoveBlockLight(node.x, node.y - 1, node.z, lightLevel, lightRemovalQueue, lightPropagationQueue, removalVisited, visited);
            }
            if (node.y < 255) {
                this.computeRemoveBlockLight(node.x, node.y + 1, node.z, lightLevel, lightRemovalQueue, lightPropagationQueue, removalVisited, visited);
            }
            this.computeRemoveBlockLight(node.x, node.y, node.z - 1, lightLevel, lightRemovalQueue, lightPropagationQueue, removalVisited, visited);
            this.computeRemoveBlockLight(node.x, node.y, node.z + 1, lightLevel, lightRemovalQueue, lightPropagationQueue, removalVisited, visited);
        }

        while (!lightPropagationQueue.isEmpty()) {
            IntegerTrio node = lightPropagationQueue.poll();
            int lightLevel = queue.getEmmittedLight(node.x, node.y, node.z);
            if (lightLevel > 1) {
                this.computeSpreadBlockLight(node.x - 1, node.y, node.z, lightLevel, lightPropagationQueue, visited);
                this.computeSpreadBlockLight(node.x + 1, node.y, node.z, lightLevel, lightPropagationQueue, visited);
                if (node.y > 0) {
                    this.computeSpreadBlockLight(node.x, node.y - 1, node.z, lightLevel, lightPropagationQueue, visited);
                }
                if (node.y < 255) {
                    this.computeSpreadBlockLight(node.x, node.y + 1, node.z, lightLevel, lightPropagationQueue, visited);
                }
                this.computeSpreadBlockLight(node.x, node.y, node.z - 1, lightLevel, lightPropagationQueue, visited);
                this.computeSpreadBlockLight(node.x, node.y, node.z + 1, lightLevel, lightPropagationQueue, visited);
            }
        }
    }

    private void computeRemoveBlockLight(int x, int y, int z, int currentLight, Queue<Object[]> queue, Queue<IntegerTrio> spreadQueue, Map<IntegerTrio, Object> visited,
         Map<IntegerTrio, Object> spreadVisited) {
        int current = this.queue.getEmmittedLight(x, y, z);
        if (current != 0 && current < currentLight) {
            this.queue.setBlockLight(x, y, z, 0);
            if (current > 1) {
                if (!visited.containsKey(mutableBlockPos)) {
                    IntegerTrio index = new IntegerTrio(x, y, z);
                    visited.put(index, present);
                    queue.add(new Object[] { index, current });
                }
            }
        } else if (current >= currentLight) {
            mutableBlockPos.set(x, y, z);
            if (!spreadVisited.containsKey(mutableBlockPos)) {
                IntegerTrio index = new IntegerTrio(x, y, z);
                spreadVisited.put(index, present);
                spreadQueue.add(index);
            }
        }
    }

    private void computeSpreadBlockLight(int x, int y, int z, int currentLight, Queue<IntegerTrio> queue, Map<IntegerTrio, Object> visited) {
        currentLight = currentLight - Math.max(1, this.queue.getOpacity(x, y, z));
        if (currentLight > 0) {
            int current = this.queue.getEmmittedLight(x, y, z);
            if (current < currentLight) {
                this.queue.setBlockLight(x, y, z, currentLight);
                mutableBlockPos.set(x, y, z);
                if (!visited.containsKey(mutableBlockPos)) {
                    visited.put(new IntegerTrio(x, y, z), present);
                    if (currentLight > 1) {
                        queue.add(new IntegerTrio(x, y, z));
                    }
                }
            }
        }
    }

    public void addLightUpdate(int x, int y, int z) {
        long index = MathMan.pairInt((int) x >> 4, (int) z >> 4);
        Map<Integer, Object> currentMap = lightQueue.get(index);
        if (currentMap == null) {
            currentMap = new Int2ObjectOpenHashMap<>();
            synchronized (lightQueue) {
                this.lightQueue.put(index, currentMap);
            }
        }
        currentMap.put((int) MathMan.tripleBlockCoord(x, y, z), present);
    }

    public synchronized void fixLightingSafe(boolean sky) {
        try {
            if (sky) {
                fixSkyLighting();
            }
            fixBlockLighting();
            sendChunks();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public void fixBlockLighting() {
        updateBlockLight(this.lightQueue);
    }

    public synchronized void sendChunks() {
        Iterator<Map.Entry<Long, Integer>> iter = chunksToSend.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<Long, Integer> entry = iter.next();
            long pair = entry.getKey();
            int bitMask = entry.getValue();
            int x = MathMan.unpairIntX(pair);
            int z = MathMan.unpairIntY(pair);
            queue.sendChunk(x, z, bitMask);
            iter.remove();
        }
    }

    private boolean isTransparent(int x, int y, int z) {
        return queue.getOpacity(x, y, z) < 15;
    }

    public synchronized void fixSkyLighting() {
        // Order chunks
        ArrayList<RelightSkyEntry> chunksList = new ArrayList<>(skyToRelight.size());
        Iterator<Map.Entry<Long, RelightSkyEntry>> iter = skyToRelight.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<Long, RelightSkyEntry> entry = iter.next();
            chunksToSend.put(entry.getKey(), entry.getValue().bitmask);
            chunksList.add(entry.getValue());
            iter.remove();
        }
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

    public void fill(byte[] mask, int chunkX, int y, int chunkZ, byte reason) {
        if (y >= FaweChunk.HEIGHT) {
            Arrays.fill(mask, (byte) 15);
            return;
        }
        switch (reason) {
            case SkipReason.SOLID: {
                Arrays.fill(mask, (byte) 0);
                return;
            }
            case SkipReason.AIR: {
                int bx = chunkX << 4;
                int bz = chunkZ << 4;
                int index = 0;
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        mask[index++] = (byte) queue.getSkyLight(bx + x, y, bz + z);
                    }
                }
            }
        }
    }

    private void fixSkyLighting(List<RelightSkyEntry> sorted) {
        RelightSkyEntry[] chunks = sorted.toArray(new RelightSkyEntry[sorted.size()]);
        byte[] cacheX = FaweCache.CACHE_X[0];
        byte[] cacheZ = FaweCache.CACHE_Z[0];
        for (int y = FaweChunk.HEIGHT - 1; y > 0; y--) {
            for (RelightSkyEntry chunk : chunks) { // Propogate skylight
                int layer = y >> 4;
                byte[] mask = chunk.mask;
                if (chunk.fix[layer] != SkipReason.NONE) {
                    if ((y & 15) == 0 && layer != 0 && chunk.fix[layer - 1] == SkipReason.NONE) {
                        fill(mask, chunk.x, y, chunk.z, chunk.fix[layer]);
                    }
                    continue;
                }
                int bx = chunk.x << 4;
                int bz = chunk.z << 4;
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
                        addLightUpdate(bx + x, y, bz + z);
                    }
                    switch (value) {
                        case 0:
                            if (opacity > 1) {
                                queue.setSkyLight(section, x, y, z, 0);
                                continue;
                            }
                            break;
                        case 1:
                        case 2:
                        case 3:
                        case 4:
                        case 5:
                        case 6:
                        case 7:
                        case 8:
                        case 9:
                        case 10:
                        case 11:
                        case 12:
                        case 13:
                        case 14:
                            if (opacity >= value) {
                                mask[j] = 0;
                                queue.setSkyLight(section, x, y, z, 0);
                                continue;
                            }
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
            for (int j = 0; j < 256; j++) {
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
            for (int j = 255; j >= 0; j--) {
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

        public RelightBlockEntry(long pair) {
            this.coord = pair;
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
        public final byte[] fix;
        public int bitmask;
        public boolean smooth;

        public RelightSkyEntry(int x, int z, byte[] fix, int bitmask) {
            this.x = x;
            this.z = z;
            byte[] array = new byte[256];
            Arrays.fill(array, (byte) 15);
            this.mask = array;
            this.bitmask = bitmask;
            if (fix == null) {
                this.fix = new byte[(maxY  + 1) >> 4];
                Arrays.fill(this.fix, SkipReason.NONE);
            } else {
                this.fix = fix;
            }
        }

        @Override
        public String toString() {
            return x + "," + z;
        }

        @Override
        public int compareTo(Object o) {
            RelightSkyEntry other = (RelightSkyEntry) o;
            if (other.x < x) {
                return 1;
            }
            if (other.x > x) {
                return -1;
            }
            if (other.z < z) {
                return 1;
            }
            if (other.z > z) {
                return -1;
            }
            return 0;
        }
    }
}
