package com.sk89q.worldedit.extent;

import com.boydti.fawe.FaweCache;
import com.boydti.fawe.jnbt.anvil.generator.*;
import com.boydti.fawe.object.PseudoRandom;
import com.boydti.fawe.object.clipboard.WorldCopyClipboard;
import com.sk89q.worldedit.*;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.blocks.BlockType;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.biome.BaseBiome;
import com.sk89q.worldedit.world.registry.WorldData;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

public interface Extent extends InputExtent, OutputExtent {

    Vector getMinimumPoint();

    Vector getMaximumPoint();

    default List<? extends Entity> getEntities(Region region) {
        return new ArrayList<>();
    }

    default List<? extends Entity> getEntities() {
        return new ArrayList<>();
    }

    default
    @Nullable
    Entity createEntity(Location location, BaseEntity entity) {
        return null;
    }

    @Override
    default BaseBlock getLazyBlock(Vector position) {
        return getBlock(position);
    }

    default public boolean setBlock(int x, int y, int z, BaseBlock block) throws WorldEditException {
        return setBlock(MutableBlockVector.get(x, y, z), block);
    }

    @Nullable
    @Override
    default Operation commit() {
        return null;
    }

    default BaseBlock getLazyBlock(int x, int y, int z) {
        return getLazyBlock(MutableBlockVector.get(x, y, z));
    }

    default int getMaxY() {
        return 255;
    }

    default boolean setBiome(int x, int y, int z, BaseBiome biome) {
        return setBiome(MutableBlockVector2D.get(x, z), biome);
    }

    /**
     * Returns the highest solid 'terrain' block which can occur naturally.
     *
     * @param x    the X coordinate
     * @param z    the Z cooridnate
     * @param minY minimal height
     * @param maxY maximal height
     * @return height of highest block found or 'minY'
     */
    default int getHighestTerrainBlock(final int x, final int z, final int minY, final int maxY) {
        return this.getHighestTerrainBlock(x, z, minY, maxY, false);
    }

    /**
     * Returns the highest solid 'terrain' block which can occur naturally.
     *
     * @param x           the X coordinate
     * @param z           the Z coordinate
     * @param minY        minimal height
     * @param maxY        maximal height
     * @param naturalOnly look at natural blocks or all blocks
     * @return height of highest block found or 'minY'
     */
    default int getHighestTerrainBlock(final int x, final int z, int minY, int maxY, final boolean naturalOnly) {
        maxY = Math.min(maxY, Math.max(0, maxY));
        minY = Math.max(0, minY);
        if (naturalOnly) {
            for (int y = maxY; y >= minY; --y) {
                BaseBlock block = getLazyBlock(x, y, z);
                if (BlockType.isNaturalTerrainBlock(block.getId(), block.getData())) {
                    return y;
                }
            }
        } else {
            for (int y = maxY; y >= minY; --y) {
                BaseBlock block = getLazyBlock(x, y, z);
                if (!FaweCache.canPassThrough(block.getId(), block.getData())) {
                    return y;
                }
            }
        }
        return minY;
    }

    default public int getNearestSurfaceLayer(int x, int z, int y, int minY, int maxY) {
        int clearanceAbove = maxY - y;
        int clearanceBelow = y - minY;
        int clearance = Math.min(clearanceAbove, clearanceBelow);

        BaseBlock block = getLazyBlock(x, y, z);
        boolean state = FaweCache.isLiquidOrGas(block.getId());
        int data1 = block.getData();
        int data2 = block.getData();
        int offset = state ? 0 : 1;
        for (int d = 0; d <= clearance; d++) {
            int y1 = y + d;
            block = getLazyBlock(x, y1, z);
            if (FaweCache.isLiquidOrGas(block.getId()) != state) {
                return ((y1 - offset) << 3) - (7 - (state ? block.getData() : data1));
            }
            data1 = block.getData();
            int y2 = y - d;
            block = getLazyBlock(x, y2, z);
            if (FaweCache.isLiquidOrGas(block.getId()) != state) {
                return ((y2 + offset) << 3) - (7 - (state ? block.getData() : data2));
            }
            data2 = block.getData();
        }
        if (clearanceAbove != clearanceBelow) {
            if (clearanceAbove < clearanceBelow) {
                for (int layer = y - clearance - 1; layer >= minY; layer--) {
                    block = getLazyBlock(x, layer, z);
                    if (FaweCache.isLiquidOrGas(block.getId()) != state) {

//                        int blockHeight = (newHeight) >> 3;
//                        int layerHeight = (newHeight) & 0x7;

                        int data = (state ? block.getData() : data1);
                        return ((layer + offset) << 3) + 0;
                    }
                    data1 = block.getData();
                }
            } else {
                for (int layer = y + clearance + 1; layer <= maxY; layer++) {
                    block = getLazyBlock(x, layer, z);
                    if (FaweCache.isLiquidOrGas(block.getId()) != state) {
                        return ((layer - offset) << 3) - (7 - (state ? block.getData() : data2));
                    }
                    data2 = block.getData();
                }
            }
        }
        return (state ? minY : maxY) << 4;
    }

    public default int getNearestSurfaceTerrainBlock(int x, int z, int y, int minY, int maxY, boolean ignoreAir) {
        return getNearestSurfaceTerrainBlock(x, z, y, minY, maxY, minY, maxY, ignoreAir);
    }

    public default int getNearestSurfaceTerrainBlock(int x, int z, int y, int minY, int maxY) {
        return getNearestSurfaceTerrainBlock(x, z, y, minY, maxY, minY, maxY);
    }

    public default int getNearestSurfaceTerrainBlock(int x, int z, int y, int minY, int maxY, int failedMin, int failedMax) {
        return getNearestSurfaceTerrainBlock(x, z, y, minY, maxY, failedMin, failedMax, true);
    }

    public default int getNearestSurfaceTerrainBlock(int x, int z, int y, int minY, int maxY, int failedMin, int failedMax, boolean ignoreAir) {
        y = Math.max(minY, Math.min(maxY, y));
        int clearanceAbove = maxY - y;
        int clearanceBelow = y - minY;
        int clearance = Math.min(clearanceAbove, clearanceBelow);
        BaseBlock block = getLazyBlock(x, y, z);
        boolean state = FaweCache.canPassThrough(block.getId(), block.getData());
        int offset = state ? 0 : 1;
        for (int d = 0; d <= clearance; d++) {
            int y1 = y + d;
            block = getLazyBlock(x, y1, z);
            if (FaweCache.canPassThrough(block.getId(), block.getData()) != state && block != EditSession.nullBlock) return y1 - offset;
            int y2 = y - d;
            block = getLazyBlock(x, y2, z);
            if (FaweCache.canPassThrough(block.getId(), block.getData()) != state && block != EditSession.nullBlock) return y2 + offset;
        }
        if (clearanceAbove != clearanceBelow) {
            if (clearanceAbove < clearanceBelow) {
                for (int layer = y - clearance - 1; layer >= minY; layer--) {
                    block = getLazyBlock(x, layer, z);
                    if (FaweCache.canPassThrough(block.getId(), block.getData()) != state && block != EditSession.nullBlock) return layer + offset;
                }
            } else {
                for (int layer = y + clearance + 1; layer <= maxY; layer++) {
                    block = getLazyBlock(x, layer, z);
                    if (FaweCache.canPassThrough(block.getId(), block.getData()) != state && block != EditSession.nullBlock) return layer - offset;
                }
            }
        }
        int result = state ? failedMin : failedMax;
        if(result > 0 && !ignoreAir) {
            block = getLazyBlock(x, result, z);
            return block.isAir() ? -1 : result;
        }
        return result;
    }

    default public void addCaves(Region region) throws WorldEditException {
        generate(region, new CavesGen(8));
    }

    public default void generate(Region region, GenBase gen) throws WorldEditException {
        for (Vector2D chunkPos : region.getChunks()) {
            gen.generate(chunkPos, this);
        }
    }

    default public void addSchems(Region region, Mask mask, WorldData worldData, List<ClipboardHolder> clipboards, int rarity, boolean rotate) throws WorldEditException {
        spawnResource(region, new SchemGen(mask, this, worldData, clipboards, rotate), rarity, 1);
    }

    default public void spawnResource(Region region, Resource gen, int rarity, int frequency) throws WorldEditException {
        PseudoRandom random = new PseudoRandom();
        for (Vector2D chunkPos : region.getChunks()) {
            for (int i = 0; i < frequency; i++) {
                if (random.nextInt(100) > rarity) {
                    continue;
                }
                int x = (chunkPos.getBlockX() << 4) + random.nextInt(16);
                int z = (chunkPos.getBlockZ() << 4) + random.nextInt(16);
                gen.spawn(random, x, z);
            }
        }
    }

    default boolean contains(Vector pt) {
        Vector min = getMinimumPoint();
        Vector max = getMaximumPoint();
        return (pt.containedWithin(min, max));
    }

    default public void addOre(Region region, Mask mask, Pattern material, int size, int frequency, int rarity, int minY, int maxY) throws WorldEditException {
        spawnResource(region, new OreGen(this, mask, material, size, minY, maxY), rarity, frequency);
    }

    default public void addOres(Region region, Mask mask) throws WorldEditException {
        addOre(region, mask, FaweCache.getBlock(BlockID.DIRT, 0), 33, 10, 100, 0, 255);
        addOre(region, mask, FaweCache.getBlock(BlockID.GRAVEL, 0), 33, 8, 100, 0, 255);
        addOre(region, mask, FaweCache.getBlock(BlockID.STONE, 1), 33, 10, 100, 0, 79);
        addOre(region, mask, FaweCache.getBlock(BlockID.STONE, 3), 33, 10, 100, 0, 79);
        addOre(region, mask, FaweCache.getBlock(BlockID.STONE, 5), 33, 10, 100, 0, 79);
        addOre(region, mask, FaweCache.getBlock(BlockID.COAL_ORE, 0), 17, 20, 100, 0, 127);
        addOre(region, mask, FaweCache.getBlock(BlockID.IRON_ORE, 0), 9, 20, 100, 0, 63);
        addOre(region, mask, FaweCache.getBlock(BlockID.GOLD_ORE, 0), 9, 2, 100, 0, 31);
        addOre(region, mask, FaweCache.getBlock(BlockID.REDSTONE_ORE, 0), 8, 8, 100, 0, 15);
        addOre(region, mask, FaweCache.getBlock(BlockID.DIAMOND_ORE, 0), 8, 1, 100, 0, 15);
        addOre(region, mask, FaweCache.getBlock(BlockID.LAPIS_LAZULI_ORE, 0), 7, 1, 100, 0, 15);
        addOre(region, mask, FaweCache.getBlock(BlockID.EMERALD_ORE, 0), 5, 1, 100, 4, 31);
    }
}
