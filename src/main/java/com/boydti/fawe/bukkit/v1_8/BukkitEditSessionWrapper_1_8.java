package com.boydti.fawe.bukkit.v1_8;

import static com.boydti.fawe.util.ReflectionUtils.getRefClass;

import org.bukkit.Bukkit;

import com.boydti.fawe.bukkit.v0.BukkitEditSessionWrapper_0;
import com.boydti.fawe.util.ReflectionUtils.RefClass;
import com.boydti.fawe.util.ReflectionUtils.RefField;
import com.boydti.fawe.util.ReflectionUtils.RefMethod;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BlockType;

public class BukkitEditSessionWrapper_1_8 extends BukkitEditSessionWrapper_0 {
    
    private final RefClass classCraftWorld = getRefClass("{cb}.CraftWorld");
    private final RefClass classChunk = getRefClass("{nms}.Chunk");
    private final RefClass classWorld = getRefClass("{nms}.World");
    
    private RefMethod worldGetHandle;
    private RefMethod methodGetChunkAt;
    private RefField heightMap;
    private Object nmsWorld;
    
    private int lastXMin;
    private int lastZMin;
    private Object lastChunk;
    
    public BukkitEditSessionWrapper_1_8(final EditSession session) {
        super(session);
        try {
            worldGetHandle = classCraftWorld.getMethod("getHandle");
            methodGetChunkAt = classWorld.getMethod("getChunkAt", int.class, int.class);
            heightMap = classChunk.getField("heightMap");
            nmsWorld = worldGetHandle.of(Bukkit.getWorld(session.getWorld().getName())).call();
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }
    
    @Override
    public int getHighestTerrainBlock(final int x, final int z, final int minY, final int maxY, final boolean naturalOnly) {
        final int bx = x >> 4;
        final int bz = z >> 4;
        int[] heights;
        if ((lastChunk == null) || (bx != lastXMin) || (bz != lastZMin)) {
            lastXMin = bx;
            lastZMin = bz;
            lastChunk = methodGetChunkAt.of(nmsWorld).call(bx, bz);
        }
        if (lastChunk != null) {
            heights = (int[]) heightMap.of(lastChunk).get();
            final int lx = x & 15;
            final int lz = z & 15;
            final int height = heights[((lz << 4) | lx)];
            if ((height <= maxY) && (height >= minY)) {
                final Vector pt = new Vector(x, height, z);
                final int id = session.getBlockType(pt);
                if (naturalOnly ? BlockType.isNaturalTerrainBlock(id, 0) : !BlockType.canPassThrough(id, 0)) {
                    return height;
                }
            }
        }
        for (int y = maxY; y >= minY; --y) {
            final Vector pt = new Vector(x, y, z);
            final int id = session.getBlockType(pt);
            int data;
            switch (id) {
                case 0: {
                    continue;
                }
                case 2:
                case 4:
                case 13:
                case 14:
                case 15:
                case 20:
                case 21:
                case 22:
                case 25:
                case 30:
                case 32:
                case 37:
                case 39:
                case 40:
                case 41:
                case 42:
                case 45:
                case 46:
                case 47:
                case 48:
                case 49:
                case 51:
                case 52:
                case 54:
                case 56:
                case 57:
                case 58:
                case 60:
                case 61:
                case 62:
                case 7:
                case 8:
                case 9:
                case 10:
                case 11:
                case 73:
                case 74:
                case 78:
                case 79:
                case 80:
                case 81:
                case 82:
                case 83:
                case 84:
                case 85:
                case 87:
                case 88:
                case 101:
                case 102:
                case 103:
                case 110:
                case 112:
                case 113:
                case 117:
                case 121:
                case 122:
                case 123:
                case 124:
                case 129:
                case 133:
                case 138:
                case 137:
                case 140:
                case 165:
                case 166:
                case 169:
                case 170:
                case 172:
                case 173:
                case 174:
                case 176:
                case 177:
                case 181:
                case 182:
                case 188:
                case 189:
                case 190:
                case 191:
                case 192:
                    return y;
                default:
                    data = 0;
            }
            if (naturalOnly ? BlockType.isNaturalTerrainBlock(id, data) : !BlockType.canPassThrough(id, data)) {
                return y;
            }
        }
        return minY;
    }
    
}
