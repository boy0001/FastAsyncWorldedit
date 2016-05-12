package com.boydti.fawe.object.extent;

import com.boydti.fawe.FaweCache;
import com.boydti.fawe.util.FaweQueue;
import com.boydti.fawe.util.MainUtil;
import com.boydti.fawe.util.ReflectionUtils;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.DoubleTag;
import com.sk89q.jnbt.ListTag;
import com.sk89q.jnbt.StringTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.EditSession;
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

public class FastWorldEditExtent extends AbstractDelegateExtent {

    private final FaweQueue queue;

    public FastWorldEditExtent(final World world, FaweQueue queue) {
        super(world);
        this.queue = queue;
    }

    public FaweQueue getQueue() {
        return queue;
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
    public BaseBiome getBiome(final Vector2D position) {
        if (!queue.isChunkLoaded(position.getBlockX() >> 4, position.getBlockZ() >> 4)) {
            return EditSession.nullBiome;
        }
        return super.getBiome(position);
    }

    private BaseBlock lastBlock;
    private BlockVector lastVector;

    @Override
    public BaseBlock getLazyBlock(final Vector position) {
        if ((this.lastBlock != null) && this.lastVector.equals(position.toBlockVector())) {
            return this.lastBlock;
        }
        if (!queue.isChunkLoaded(position.getBlockX() >> 4, position.getBlockZ() >> 4)) {
            try {
                this.lastVector = position.toBlockVector();
                return this.lastBlock = super.getBlock(position);
            } catch (final Throwable e) {
                return EditSession.nullBlock;
            }
        }
        this.lastVector = position.toBlockVector();
        return this.lastBlock = super.getBlock(position);
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
    public boolean setBlock(final Vector location, final BaseBlock block) throws WorldEditException {
        final short id = (short) block.getId();
        final int x = location.getBlockX();
        final int y = location.getBlockY();
        final int z = location.getBlockZ();
        switch (id) {
            case 63:
            case 68:
                if (block.hasNbtData() && !MainUtil.isValidSign(block.getNbtData())) {
                    queue.setBlock(x, y, z, id, FaweCache.hasData(id) ? (byte) block.getData() : 0);
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
            case 119:
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
                    CompoundTag nbt = block.getNbtData();
                    MainUtil.setPosition(nbt, x, y, z);
                    queue.setTile(x, y, z, nbt);
                }
                queue.setBlock(x, y, z, id, (byte) block.getData());
                return true;
            }
            default: {
                queue.setBlock(x, y, z, id, (byte) block.getData());
                return true;
            }
        }
    }
}
