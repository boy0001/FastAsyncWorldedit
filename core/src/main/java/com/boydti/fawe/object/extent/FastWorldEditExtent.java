package com.boydti.fawe.object.extent;

import com.boydti.fawe.FaweCache;
import com.boydti.fawe.object.FaweQueue;
import com.boydti.fawe.object.HasFaweQueue;
import com.boydti.fawe.util.MainUtil;
import com.boydti.fawe.util.ReflectionUtils;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.DoubleTag;
import com.sk89q.jnbt.ListTag;
import com.sk89q.jnbt.StringTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.Vector2D;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.biome.BaseBiome;
import java.util.List;
import java.util.Map;

public class FastWorldEditExtent extends AbstractDelegateExtent implements HasFaweQueue {

    private final World world;
    private FaweQueue queue;
    private final int maxY;

    public FastWorldEditExtent(final World world, FaweQueue queue) {
        super(queue);
        this.world = world;
        this.queue = queue;
        this.maxY = world.getMaxY();
    }

    public FaweQueue getQueue() {
        return queue;
    }

    @Override
    public int getMaxY() {
        return queue.getMaxY();
    }

    @Override
    public int getLight(int x, int y, int z) {
        return queue.getLight(x, y, z);
    }

    @Override
    public int getBlockLight(int x, int y, int z) {
        return queue.getEmmittedLight(x, y, z);
    }

    @Override
    public int getSkyLight(int x, int y, int z) {
        return queue.getSkyLight(x, y, z);
    }

    @Override
    public int getBrightness(int x, int y, int z) {
        return queue.getBrightness(x, y, z);
    }

    @Override
    public int getOpacity(int x, int y, int z) {
        return queue.getOpacity(x, y, z);
    }

    @Override
    public Entity createEntity(final Location loc, final BaseEntity entity) {
        if (entity != null) {
            CompoundTag tag = entity.getNbtData();
            Map<String, Tag> map = ReflectionUtils.getMap(tag.getValue());
            map.put("Id", new StringTag(entity.getTypeId()));
            ListTag pos = (ListTag) map.get("Pos");
            if (pos != null) {
                List<Tag> posList = ReflectionUtils.getList(pos.getValue());
                posList.set(0, new DoubleTag(loc.getX()));
                posList.set(1, new DoubleTag(loc.getY()));
                posList.set(2, new DoubleTag(loc.getZ()));
            }
            queue.setEntity(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), tag);
        }
        return null;
    }

    @Override
    public String toString() {
        return super.toString() + ":" + queue + "(" + getExtent() + ")";
    }

    @Override
    public BaseBiome getBiome(final Vector2D position) {
        return FaweCache.CACHE_BIOME[queue.getBiomeId(position.getBlockX(), position.getBlockZ())];
    }

    @Override
    public boolean setBlock(final Vector location, final BaseBlock block) throws WorldEditException {
        return setBlock(location.getBlockX(), location.getBlockY(), location.getBlockZ(), block);
    }

    @Override
    public BaseBlock getLazyBlock(Vector location) {
        return getLazyBlock(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    @Override
    public BaseBlock getLazyBlock(int x, int y, int z) {
        int combinedId4Data = queue.getCombinedId4Data(x, y, z, 0);
        int id = FaweCache.getId(combinedId4Data);
        if (!FaweCache.hasNBT(id)) {
            return FaweCache.CACHE_BLOCK[combinedId4Data];
        }
        try {
            CompoundTag tile = queue.getTileEntity(x, y, z);
            if (tile != null) {
                return new BaseBlock(id, FaweCache.getData(combinedId4Data), tile);
            } else {
                return FaweCache.CACHE_BLOCK[combinedId4Data];
            }
        } catch (Throwable e) {
            MainUtil.handleError(e);
            return FaweCache.CACHE_BLOCK[combinedId4Data];
        }
    }

    @Override
    public List<? extends Entity> getEntities() {
        return world.getEntities();
    }

    @Override
    public List<? extends Entity> getEntities(final Region region) {
        return world.getEntities(region);
    }

    @Override
    public BaseBlock getBlock(final Vector position) {
        return this.getLazyBlock(position);
    }

    @Override
    public boolean setBiome(final Vector2D position, final BaseBiome biome) {
        queue.setBiome(position.getBlockX(), position.getBlockZ(), biome);
        return true;
    }

    @Override
    public boolean setBlock(int x, int y, int z, final BaseBlock block) throws WorldEditException {
        int id = block.getId();
        switch (id) {
            case 63: {
                // Fix for signs
                CompoundTag nbt = block.getNbtData();
                return queue.setBlock(x, y, z, id, block.getData(), nbt != null && !MainUtil.isValidSign(nbt) ? null : nbt);
            }
            case 65: // Fix rotation
            case 68:
            case 54:
            case 146:
            case 61: {
                // Fix for default block rotation
                byte data = (byte) block.getData();
                if (data == 0) {
                    data = 2;
                }
                return queue.setBlock(x, y, z, id, data, block.getNbtData());
            }
            case 0: // No data
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
            case 7:
            case 73:
            case 74:
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
            case 172:
            case 173:
            case 174:
            case 190:
            case 191:
            case 192:
                // No data
                return queue.setBlock(x, y, z, id);
            case 1: // No tiles
            case 3:
            case 5:
            case 6:
            case 8:
            case 9:
            case 10:
            case 11:
            case 12:
            case 16:
            case 17:
            case 18:
            case 19:
            case 24:
            case 31:
            case 34:
            case 35:
            case 36:
            case 38:
            case 43:
            case 44:
            case 50:
            case 53:
            case 55:
            case 59:
            case 60:
            case 64:
            case 67:
            case 69:
            case 70:
            case 71:
            case 72:
            case 75:
            case 76:
            case 77:
            case 78:
            case 89:
            case 90:
            case 91:
            case 92:
            case 93:
            case 94:
            case 95:
            case 96:
            case 97:
            case 98:
            case 99:
            case 100:
            case 104:
            case 105:
            case 106:
            case 107:
            case 108:
            case 109:
            case 111:
            case 114:
            case 115:
            case 118:
            case 120:
            case 125:
            case 126:
            case 127:
            case 128:
            case 131:
            case 132:
            case 134:
            case 135:
            case 136:
            case 139:
            case 141:
            case 143:
            case 145:
            case 147:
            case 148:
            case 152:
            case 153:
            case 155:
            case 156:
            case 159:
            case 160:
            case 161:
            case 162:
            case 163:
            case 164:
            case 167:
            case 168:
            case 170:
            case 171:
            case 175:
            case 179:
            case 180:
            case 181:
            case 182:
            case 183:
            case 184:
            case 185:
            case 186:
            case 187:
            case 193:
            case 194:
            case 195:
            case 196:
            case 197:
            case 198:
            case 199:
            case 200:
            case 201:
            case 202:
            case 203:
            case 204:
            case 205:
            case 206:
            case 207:
            case 208:
            case 212:
            case 213:
            case 214:
            case 215:
            case 216:
            case 217:
            case 235:
            case 236:
            case 237:
            case 238:
            case 239:
            case 240:
            case 241:
            case 242:
            case 243:
            case 244:
            case 245:
            case 246:
            case 247:
            case 248:
            case 249:
            case 250:
            case 251:
            case 252: {
                return queue.setBlock(x, y, z, id, block.getData());
            }
            case 26: // Tiles
            case 218:
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
            case 119:
            case 323:
            case 117:
            case 116:
            case 28:
            case 66:
            case 157:
            case 62:
            case 140:
            case 149:
            case 150:
            case 158:
            case 23:
            case 123:
            case 124:
            case 29:
            case 33:
            case 151:
            case 178:
            case 188:
            case 189:
            case 209:
            case 210:
            case 211:
            case 255:
            case 219:
            case 220:
            case 221:
            case 222:
            case 223:
            case 224:
            case 225:
            case 226:
            case 227:
            case 228:
            case 229:
            case 230:
            case 231:
            case 232:
            case 233:
            case 234:
            default: {
                CompoundTag nbt = block.getNbtData();
                if (nbt != null) {
                    return queue.setBlock(x, y, z, id, block.getData(), nbt);
                } else {
                    return queue.setBlock(x, y, z, id, block.getData());
                }
            }
        }
    }
}
