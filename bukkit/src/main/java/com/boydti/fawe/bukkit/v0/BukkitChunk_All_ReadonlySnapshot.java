package com.boydti.fawe.bukkit.v0;

import com.boydti.fawe.FaweCache;
import com.boydti.fawe.object.FaweChunk;
import com.boydti.fawe.util.MathMan;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.bukkit.adapter.BukkitImplAdapter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.ChunkSnapshot;
import org.bukkit.block.Biome;

public class BukkitChunk_All_ReadonlySnapshot extends FaweChunk {
    private final ChunkSnapshot snapshot;
    private final boolean hasBiomes;
    private Set<CompoundTag> entities = new HashSet<>();
    private Map<Short, CompoundTag> tiles = new HashMap<>();

    public BukkitChunk_All_ReadonlySnapshot(BukkitQueue_All parent, ChunkSnapshot snapshot, boolean biomes) {
        super(parent, snapshot.getX(), snapshot.getZ());
        this.snapshot = snapshot;
        this.hasBiomes = biomes;
    }

    public void setTiles(Map<Short, CompoundTag> tiles) {
        this.tiles = tiles;
    }

    public void setEntities(Set<CompoundTag> entities) {
        this.entities = entities;
    }

    @Override
    public BukkitQueue_All getParent() {
        return (BukkitQueue_All) super.getParent();
    }

    @Override
    public int getBitMask() {
        return Character.MAX_VALUE;
    }

    @Override
    public int getBlockCombinedId(int x, int y, int z) {
        int id = snapshot.getBlockTypeId(x, y, z);
        return FaweCache.getCombined(id, FaweCache.hasData(id) ? snapshot.getBlockData(x, y, z) : 0);
    }

    @Override
    public byte[] getBiomeArray() {
        if (!hasBiomes) return null;
        BukkitImplAdapter adapter = getParent().getAdapter();
        byte[] biomes = new byte[256];
        int index = 0;
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++, index++) {
                Biome biome = snapshot.getBiome(x, z);
                biomes[index] = (byte) adapter.getBiomeId(biome);
            }
        }
        return biomes;
    }

    @Override
    public Object getChunk() {
        return snapshot;
    }

    @Override
    public void setTile(int x, int y, int z, CompoundTag tile) {
        tiles.put(MathMan.tripleBlockCoord(x, y, z), tile);
    }

    @Override
    public void setEntity(CompoundTag entity) {
        entities.add(entity);
    }

    @Override
    public void removeEntity(UUID uuid) {
        throw new UnsupportedOperationException("Read only");
    }

    @Override
    public void setBlock(int x, int y, int z, int id, int data) {
        throw new UnsupportedOperationException("Read only");
    }

    @Override
    public Set<CompoundTag> getEntities() {
        return entities;
    }

    @Override
    public Set<UUID> getEntityRemoves() {
        throw new UnsupportedOperationException("Read only");
    }

    @Override
    public Map<Short, CompoundTag> getTiles() {
        return tiles;
    }

    @Override
    public CompoundTag getTile(int x, int y, int z) {
        if (tiles == null) return null;
        short pair = MathMan.tripleBlockCoord(x, y, z);
        return tiles.get(pair);
    }

    @Override
    public void setBiome(int x, int z, byte biome) {
        throw new UnsupportedOperationException("Read only");
    }

    @Override
    public FaweChunk copy(boolean shallow) {
        return null;
    }

    @Override
    public FaweChunk call() {
        return null;
    }
}
