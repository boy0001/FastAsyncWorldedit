package com.boydti.fawe.bukkit.wrapper;

import com.boydti.fawe.FaweCache;
import com.boydti.fawe.bukkit.wrapper.state.AsyncSign;
import com.boydti.fawe.object.FaweQueue;
import com.sk89q.worldedit.blocks.BlockID;
import java.util.Collection;
import java.util.List;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.PistonMoveReaction;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;

public class AsyncBlock implements Block {

    public int z;
    public int y;
    public int x;
    public final FaweQueue queue;
    public final AsyncWorld world;

    public AsyncBlock(AsyncWorld world, FaweQueue queue, int x, int y, int z) {
        this.world = world;
        this.queue = queue;
        this.x = x;
        this.y = Math.max(0, Math.min(255, y));
        this.z = z;
    }

    @Override
    public byte getData() {
        return (byte) (queue.getCachedCombinedId4Data(x, y, z, 0) & 0xF);
    }

    @Override
    public Block getRelative(int modX, int modY, int modZ) {
        return new AsyncBlock(world, queue, x + modX, y + modY, z + modZ);
    }

    @Override
    public Block getRelative(BlockFace face) {
        return this.getRelative(face.getModX(), face.getModY(), face.getModZ());
    }

    @Override
    public Block getRelative(BlockFace face, int distance) {
        return this.getRelative(face.getModX() * distance, face.getModY() * distance, face.getModZ() * distance);
    }

    @Override
    public Material getType() {
        return Material.getMaterial(queue.getCachedCombinedId4Data(x, y, z, 0) >> 4);
    }

    @Override
    public int getTypeId() {
        return queue.getCachedCombinedId4Data(x, y, z, 0) >> 4;
    }

    @Override
    public byte getLightLevel() {
        return (byte) queue.getLight(x, y, z);
    }

    @Override
    public byte getLightFromSky() {
        return (byte) queue.getSkyLight(x, y, z);
    }

    @Override
    public byte getLightFromBlocks() {
        return (byte) queue.getEmmittedLight(x, y, z);
    }

    @Override
    public World getWorld() {
        return world;
    }

    @Override
    public int getX() {
        return x;
    }

    @Override
    public int getY() {
        return y;
    }

    @Override
    public int getZ() {
        return z;
    }

    @Override
    public Location getLocation() {
        return new Location(world, x, y, z);
    }

    @Override
    public Location getLocation(Location loc) {
        if(loc != null) {
            loc.setWorld(this.getWorld());
            loc.setX((double)this.x);
            loc.setY((double)this.y);
            loc.setZ((double)this.z);
        }
        return loc;
    }

    @Override
    public Chunk getChunk() {
        return world.getChunkAt(x >> 4, z >> 4);
    }

    @Override
    public void setData(byte data) {
        setTypeIdAndData(getTypeId(), data, true);
    }

    @Override
    public void setData(byte data, boolean applyPhysics) {
        setTypeIdAndData(getTypeId(), data, applyPhysics);
    }

    @Override
    public void setType(Material type) {
        setTypeIdAndData(type.getId(), (byte) 0, true);
    }

    @Override
    public void setType(Material type, boolean applyPhysics) {
        setTypeIdAndData(type.getId(), (byte) 0, applyPhysics);
    }

    @Override
    public boolean setTypeId(int type) {
        return setTypeIdAndData(type, (byte) 0, true);
    }

    @Override
    public boolean setTypeId(int type, boolean applyPhysics) {
        return setTypeIdAndData(type, (byte) 0, applyPhysics);
    }

    @Override
    public boolean setTypeIdAndData(int type, byte data, boolean applyPhysics) {
        return queue.setBlock(x, y, z, (short) type, data);
    }

    @Override
    public BlockFace getFace(Block block) {
        BlockFace[] directions = BlockFace.values();
        for(int i = 0; i < directions.length; ++i) {
            BlockFace face = directions[i];
            if(this.getX() + face.getModX() == block.getX() && this.getY() + face.getModY() == block.getY() && this.getZ() + face.getModZ() == block.getZ()) {
                return face;
            }
        }
        return null;
    }

    @Override
    public BlockState getState() {
        int combined = queue.getCombinedId4Data(x, y, z, 0);
        switch (FaweCache.getId(combined)) {
            case BlockID.SIGN_POST:
            case BlockID.WALL_SIGN:
                return new AsyncSign(this, combined);
        }
        return new AsyncBlockState(this);
    }

    @Override
    public Biome getBiome() {
        return world.getAdapter().getBiome(queue.getBiomeId(x, z));
    }

    @Override
    public void setBiome(Biome bio) {
        int id = world.getAdapter().getBiomeId(bio);
        queue.setBiome(x, z, FaweCache.getBiome(id));
    }

    @Override
    public boolean isBlockPowered() {
        return false;
    }

    @Override
    public boolean isBlockIndirectlyPowered() {
        return false;
    }

    @Override
    public boolean isBlockFacePowered(BlockFace face) {
        return false;
    }

    @Override
    public boolean isBlockFaceIndirectlyPowered(BlockFace face) {
        return false;
    }

    @Override
    public int getBlockPower(BlockFace face) {
        return 0;
    }

    @Override
    public int getBlockPower() {
        return 0;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public boolean isLiquid() {
        return FaweCache.isLiquid(getTypeId());
    }

    @Override
    public double getTemperature() {
        return 0;
    }

    @Override
    public double getHumidity() {
        return 0;
    }

    @Override
    public PistonMoveReaction getPistonMoveReaction() {
        return null;
    }

    @Override
    public boolean breakNaturally() {
        return false;
    }

    @Override
    public boolean breakNaturally(ItemStack tool) {
        return false;
    }

    @Override
    public Collection<ItemStack> getDrops() {
        return null;
    }

    @Override
    public Collection<ItemStack> getDrops(ItemStack tool) {
        return null;
    }

    @Override
    public void setMetadata(String metadataKey, MetadataValue newMetadataValue) {

    }

    @Override
    public List<MetadataValue> getMetadata(String metadataKey) {
        return null;
    }

    @Override
    public boolean hasMetadata(String metadataKey) {
        return false;
    }

    @Override
    public void removeMetadata(String metadataKey, Plugin owningPlugin) {

    }

    public void setPosition(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
}
