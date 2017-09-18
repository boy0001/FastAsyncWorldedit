package com.boydti.fawe.bukkit.wrapper;

import com.boydti.fawe.FaweCache;
import com.sk89q.jnbt.CompoundTag;
import java.util.List;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.material.MaterialData;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;

public class AsyncBlockState implements BlockState {

    private byte data;
    private short id;
    private CompoundTag nbt;
    private final AsyncBlock block;

    public AsyncBlockState(AsyncBlock block) {
        this(block, block.queue.getCombinedId4Data(block.x, block.y, block.z, 0));
    }

    public AsyncBlockState(AsyncBlock block, int combined) {
        this.block = block;
        this.id = (short) (combined >> 4);
        this.data = (byte) (combined & 0xF);
        if (FaweCache.hasNBT(id)) {
            this.nbt = block.queue.getTileEntity(block.x, block.y, block.z);
        } else {
            this.nbt = null;
        }
    }

    @Override
    public Block getBlock() {
        return block;
    }

    @Override
    public MaterialData getData() {
        return new MaterialData(id, data);
    }

    @Override
    public Material getType() {
        return Material.getMaterial(id);
    }

    @Override
    public int getTypeId() {
        return id;
    }

    @Override
    public byte getLightLevel() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public World getWorld() {
        return block.world;
    }

    @Override
    public int getX() {
        return block.x;
    }

    @Override
    public int getY() {
        return block.y;
    }

    @Override
    public int getZ() {
        return block.z;
    }

    @Override
    public Location getLocation() {
        return block.getLocation();
    }

    @Override
    public Location getLocation(Location loc) {
        return block.getLocation(loc);
    }

    @Override
    public Chunk getChunk() {
        return block.getChunk();
    }

    @Override
    public void setData(MaterialData data) {
        setTypeId(data.getItemTypeId());
        setRawData(data.getData());
    }

    @Override
    public void setType(Material type) {
        setTypeId(type.getId());
    }

    @Override
    public boolean setTypeId(int type) {
        if (id != type) {
            this.id = (short) type;
            return true;
        }
        return false;
    }

    @Override
    public boolean update() {
        return update(false);
    }

    @Override
    public boolean update(boolean force) {
        return update(force, true);
    }

    @Override
    public boolean update(boolean force, boolean applyPhysics) {
        boolean result = block.queue.setBlock(block.x, block.y, block.z, id, data);
        if (nbt != null) {
            block.queue.setTile(block.x, block.y, block.z, nbt);
        }
        return result;
    }

    public CompoundTag getNbtData() {
        return nbt;
    }

    public void setNbtData(CompoundTag nbt) {
        this.nbt = nbt;
    }

    @Override
    public byte getRawData() {
        return data;
    }

    @Override
    public void setRawData(byte data) {
        this.data = data;
    }

    @Override
    public boolean isPlaced() {
        return false;
    }

    @Override
    public void setMetadata(String key, MetadataValue value) {
        block.setMetadata(key, value);
    }

    @Override
    public List<MetadataValue> getMetadata(String key) {
        return block.getMetadata(key);
    }

    @Override
    public boolean hasMetadata(String key) {
        return block.hasMetadata(key);
    }

    @Override
    public void removeMetadata(String key, Plugin plugin) {
        block.removeMetadata(key, plugin);
    }
}
