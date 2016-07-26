package com.boydti.fawe.example;

import com.boydti.fawe.FaweCache;
import com.boydti.fawe.util.MathMan;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

public class NMSRelighter {
    private final NMSMappedFaweQueue queue;
    private final HashMap<Long, RelightChunk> toRelight;

    public NMSRelighter(NMSMappedFaweQueue queue) {
        this.queue = queue;
        toRelight = new HashMap<>();
    }

    public boolean addChunk(int cx, int cz) {
        long pair = MathMan.pairInt(cx, cz);
        if (toRelight.containsKey(pair)) {
            return false;
        }
        toRelight.put(pair, new RelightChunk(cx, cz));
        return true;
    }

    public void fixBlockLighting() {
        // TODO
    }

    public void fixSkyLighting() {
        // Order chunks
        ArrayList<RelightChunk> chunksList = new ArrayList<>(toRelight.values());
        Collections.sort(chunksList);
        RelightChunk[] chunks = chunksList.toArray(new RelightChunk[chunksList.size()]);

        byte[] cacheX = FaweCache.CACHE_X[0];
        byte[] cacheZ = FaweCache.CACHE_Z[0];
        for (int y = 255; y >= 0; y--) {
            for (RelightChunk chunk : chunks) { // Propogate skylight
                byte[] mask = chunk.mask;
                Object sections = queue.getCachedSections(queue.getWorld(), chunk.x, chunk.z);
                if (sections == null) continue;
                Object section = queue.getCachedSection(sections, y >> 4);
                if (section == null) continue;
                chunk.smooth = false;
                for (int j = 0; j < 256; j++) {
                    int x = cacheX[j];
                    int z = cacheZ[j];
                    byte value = mask[j];
                    int opacity = queue.getOpacity(section, x, y, z);
                    if (opacity != 0 && opacity >= value) {
                        mask[j] = 0;
                        continue;
                    }
                    switch (value) {
                        case 0:
                            if (opacity != 0) {
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
                            if (opacity == 0) {
                                mask[j] = --value;
                            } else {
                                mask[j] = (byte) Math.max(0, value - opacity);
                            }
                            queue.setSkyLight(section, x, y, z, value);
                            continue;
                        case 1:
                        case 3:
                        case 5:
                        case 7:
                        case 9:
                        case 11:
                        case 13:
                            if (opacity == 0) {
                                mask[j] = --value;
                            } else {
                                mask[j] = value = (byte) Math.max(0, value - opacity);
                            }
                            break;
                        case 15:
                            if (opacity != 0) {
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
            for (RelightChunk chunk : chunks) { // Smooth forwards
                if (chunk.smooth) {
                    smooth(chunk, y, true);
                }
            }
            for (int i = chunks.length - 1; i>= 0; i--) { // Smooth backwards
                RelightChunk chunk = chunks[i];
                if (chunk.smooth) {
                    smooth(chunk, y, false);
                }
            }
        }

    }

    public void smooth(RelightChunk chunk, int y, boolean direction) {
        byte[] mask = chunk.mask;
        int bx = chunk.x << 4;
        int bz = chunk.z << 4;
        Object sections = queue.getCachedSections(queue.getWorld(), chunk.x, chunk.z);
        if (sections == null) return;
        Object section = queue.getCachedSection(sections, y >> 4);
        if (section == null) return;
        if (direction) {
            for (int j = 0; j < 256; j++) {
                int x = j & 15;
                int z = j >> 4;
                if (mask[j] >= 14 || (mask[j] == 0 && queue.getOpacity(section, x, y, z) > 0)) {
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
                if (mask[j] >= 14 || (mask[j] == 0 && queue.getOpacity(section, x, y, z) > 0)) {
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

    private class RelightChunk implements Comparable {
        public final int x;
        public final int z;
        public final byte[] mask;
        public boolean smooth;

        public RelightChunk(int x, int z) {
            this.x = x;
            this.z = z;
            byte[] array = new byte[256];
            Arrays.fill(array, (byte) 15);
            this.mask = array;
        }


        @Override
        public int compareTo(Object o) {
            RelightChunk other = (RelightChunk) o;
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
