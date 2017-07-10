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

    private FaweQueue queue;
    private final int maxY;

    public FastWorldEditExtent(final World world, FaweQueue queue) {
        super(world);
        this.queue = queue;
        this.maxY = world.getMaxY();
    }

    public FaweQueue getQueue() {
        return queue;
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
        return super.getEntities();
    }

    @Override
    public List<? extends Entity> getEntities(final Region region) {
        return super.getEntities(region);
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
            case 63:
                // Fix for signs
                CompoundTag nbt = block.getNbtData();
                return queue.setBlock(x, y, z, id, block.getData(), nbt != null && !MainUtil.isValidSign(nbt) ? null : nbt);
            case 65:
            case 68:
            case 54:
            case 146:
            case 61:
                // Fix for default block rotation
                byte data = (byte) block.getData();
                if (data == 0) {
                    data = 2;
                }
                return queue.setBlock(x, y, z, id, data, block.getNbtData());
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
                // Tile
                return queue.setBlock(x, y, z, id, block.getData(), block.getNbtData());
            case 0:
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
            case 11:
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
            case 188:
            case 189:
            case 190:
            case 191:
            case 192:
                // No data
                return queue.setBlock(x, y, z, id);
            default: {
                return queue.setBlock(x, y, z, id, block.getData());
            }
        }
    }
}
