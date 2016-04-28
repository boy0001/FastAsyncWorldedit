package com.boydti.fawe.object.clipboard;

import com.boydti.fawe.FaweCache;
import com.boydti.fawe.object.IntegerTrio;
import com.boydti.fawe.object.RunnableVal2;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.Extent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class MemoryOptimizedClipboard extends FaweClipboard {
    protected int length;
    protected int height;
    protected int width;
    protected int area;

    // x,z,y+15>>4 | y&15
    private final byte[][] ids;
    private byte[][] datas;
    private final HashMap<IntegerTrio, CompoundTag> nbtMap;
    private final HashSet<ClipboardEntity> entities;

    public MemoryOptimizedClipboard(int width, int height, int length) {
        this.width = width;
        this.height = height;
        this.length = length;
        this.area = width * length;
        ids = new byte[width * length * ((height + 15) >>  4)][];
        nbtMap = new HashMap<>();
        entities = new HashSet<>();
    }

    private int ylast;
    private int ylasti;
    private int zlast;
    private int zlasti;

    @Override
    public BaseBlock getBlock(int x, int y, int z) {
        int i = x + ((ylast == y) ? ylasti : (ylasti = ((ylast = y) >> 4) * area)) + ((zlast == z) ? zlasti : (zlasti = (zlast = z) * width));
        byte[] idArray = ids[i];
        if (idArray == null) {
            return FaweCache.CACHE_BLOCK[0];
        }
        int y2 = y & 0xF;
        int id = idArray[y2] & 0xFF;
        BaseBlock block;
        if (!FaweCache.hasData(id) || datas == null) {
            block = FaweCache.CACHE_BLOCK[id << 4];
        } else {
            byte[] dataArray = datas[i];
            if (dataArray == null) {
                block = FaweCache.CACHE_BLOCK[id << 4];
            } else {
                block = FaweCache.CACHE_BLOCK[(id << 4) + dataArray[y2]];
            }
        }
        if (FaweCache.hasNBT(id)) {
            CompoundTag nbt = nbtMap.get(new IntegerTrio(x, y, z));
            if (nbt != null) {
                block = new BaseBlock(block.getId(), block.getData());
                block.setNbtData(nbt);
            }
        }
        return block;
    }

    @Override
    public void forEach(final RunnableVal2<Vector,BaseBlock> task, boolean air) {
        BlockVector pos = new BlockVector(0, 0, 0);
        int y1max = ((height + 15) >>  4);
        for (int x = 0; x < width; x++) {
            int i1 = x;
            for (int z = 0; z < length; z++) {
                int i2 = i1 + z * width;
                for (int y = 0; y < y1max; y++) {
                    int y1 = y << 4;
                    int i = i2 + y * area;
                    byte[] idArray = ids[i];
                    if (idArray == null) {
                        if (!air) {
                            continue;
                        }
                        for (int y2 = 0; y2 < 16; y2++) {
                            pos.x = x;
                            pos.z = z;
                            pos.y = y1 + y2;
                            task.run(pos, EditSession.nullBlock);
                        }
                        continue;
                    }
                    for (int y2 = 0; y2 < idArray.length; y2++) {
                        int id = idArray[y2] & 0xFF;
                        if (id == 0 && !air) {
                            continue;
                        }
                        pos.x = x;
                        pos.z = z;
                        pos.y = y1 + y2;
                        BaseBlock block;
                        if (!FaweCache.hasData(id) || datas == null) {
                            block = FaweCache.CACHE_BLOCK[id << 4];
                        } else {
                            byte[] dataArray = datas[i];
                            if (dataArray == null) {
                                block = FaweCache.CACHE_BLOCK[id << 4];
                            } else {
                                block = FaweCache.CACHE_BLOCK[(id << 4) + dataArray[y2]];
                            }
                        }
                        if (FaweCache.hasNBT(id)) {
                            CompoundTag nbt = nbtMap.get(new IntegerTrio((int) pos.x, (int) pos.y, (int) pos.z));
                            if (nbt != null) {
                                block = new BaseBlock(block.getId(), block.getData());
                                block.setNbtData(nbt);
                            }
                        }
                        task.run(pos, block);
                    }
                }
            }
        }
    }

    @Override
    public boolean setBlock(int x, int y, int z, BaseBlock block) {
        final int id = block.getId();
        switch (id) {
            case 0: {
                int i = x + ((ylast == y) ? ylasti : (ylasti = ((ylast = y) >> 4) * area)) + ((zlast == z) ? zlasti : (zlasti = (zlast = z) * width));
                byte[] idArray = ids[i];
                if (idArray != null) {
                    int y2 = y & 0xF;
                    idArray[y2] = 0;
                }
                return true;
            }
            case 54:
            case 130:
            case 142:
            case 27:
            case 137:
            case 52:
            case 154:
            case 84:
            case 25:
            case 144:
            case 138:
            case 176:
            case 177:
            case 63:
            case 119:
            case 68:
            case 323:
            case 117:
            case 116:
            case 28:
            case 66:
            case 157:
            case 61:
            case 62:
            case 140:
            case 146:
            case 149:
            case 150:
            case 158:
            case 23:
            case 123:
            case 124:
            case 29:
            case 33:
            case 151:
            case 178: {
                if (block.hasNbtData()) {
                    nbtMap.put(new IntegerTrio(x, y, z), block.getNbtData());
                }
                int i = x + ((ylast == y) ? ylasti : (ylasti = ((ylast = y) >> 4) * area)) + ((zlast == z) ? zlasti : (zlasti = (zlast = z) * width));
                int y2 = y & 0xF;
                byte[] idArray = ids[i];
                if (idArray == null) {
                    idArray = new byte[16];
                    ids[i] = idArray;
                }
                idArray[y2] = (byte) id;
                if (FaweCache.hasData(id)) {
                    int data = block.getData();
                    if (data == 0) {
                        return true;
                    }
                    if (datas == null) {
                        datas = new byte[area * ((height + 15) >> 4)][];
                    }
                    byte[] dataArray = datas[i];
                    if (dataArray == null) {
                        dataArray = datas[i] = new byte[16];
                    }
                    dataArray[y2] = (byte) data;
                }
                return true;
            }
            case 2:
            case 4:
            case 13:
            case 14:
            case 15:
            case 20:
            case 21:
            case 22:
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
            case 56:
            case 57:
            case 58:
            case 60:
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
            case 85:
            case 87:
            case 88:
            case 101:
            case 102:
            case 103:
            case 110:
            case 112:
            case 113:
            case 121:
            case 122:
            case 129:
            case 133:
            case 165:
            case 166:
            case 169:
            case 170:
            case 172:
            case 173:
            case 174:
            case 181:
            case 182:
            case 188:
            case 189:
            case 190:
            case 191:
            case 192: {
                int i = x + ((ylast == y) ? ylasti : (ylasti = ((ylast = y) >> 4) * area)) + ((zlast == z) ? zlasti : (zlasti = (zlast = z) * width));
                int y2 = y & 0xF;
                byte[] idArray = ids[i];
                if (idArray == null) {
                    idArray = new byte[16];
                    ids[i] = idArray;
                }
                idArray[y2] = (byte) id;
                return true;
            }
            default: {
                int i = x + ((ylast == y) ? ylasti : (ylasti = ((ylast = y) >> 4) * area)) + ((zlast == z) ? zlasti : (zlasti = (zlast = z) * width));
                int y2 = y & 0xF;
                byte[] idArray = ids[i];
                if (idArray == null) {
                    idArray = new byte[16];
                    ids[i] = idArray;
                }
                idArray[y2] = (byte) id;
                int data = block.getData();
                if (data == 0) {
                    return true;
                }
                if (datas == null) {
                    datas = new byte[area * ((height + 15) >> 4)][];
                }
                byte[] dataArray = datas[i];
                if (dataArray == null) {
                    dataArray = datas[i] = new byte[16];
                }
                dataArray[y2] = (byte) data;
                return true;
            }
        }
    }

    @Override
    public Entity createEntity(Extent world, double x, double y, double z, float yaw, float pitch, BaseEntity entity) {
        FaweClipboard.ClipboardEntity ret = new ClipboardEntity(world, x, y, z, yaw, pitch, entity);
        entities.add(ret);
        return ret;
    }

    @Override
    public List<? extends Entity> getEntities() {
        return new ArrayList<>(entities);
    }

    @Override
    public boolean remove(ClipboardEntity clipboardEntity) {
        return entities.remove(clipboardEntity);
    }
}
