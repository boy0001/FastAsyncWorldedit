package com.sk89q.worldedit.extent;

import com.boydti.fawe.FaweCache;
import com.boydti.fawe.jnbt.anvil.generator.CavesGen;
import com.boydti.fawe.jnbt.anvil.generator.GenBase;
import com.boydti.fawe.jnbt.anvil.generator.OreGen;
import com.boydti.fawe.jnbt.anvil.generator.Resource;
import com.boydti.fawe.jnbt.anvil.generator.SchemGen;
import com.boydti.fawe.object.PseudoRandom;
import com.sk89q.worldedit.MutableBlockVector;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.Vector2D;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.pattern.BlockPattern;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.util.Location;
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

    default @Nullable Entity createEntity(Location location, BaseEntity entity) {
        throw new UnsupportedOperationException(getClass() + " does not support entity creation!");
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
                        return ((layer + offset) << 3) - (7 - (state ? block.getData() : data1));
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
        return maxY << 4;
    }

    public default int getNearestSurfaceTerrainBlock(int x, int z, int y, int minY, int maxY) {
        int clearanceAbove = maxY - y;
        int clearanceBelow = y - minY;
        int clearance = Math.min(clearanceAbove, clearanceBelow);
        BaseBlock block = getLazyBlock(x, y, z);
        boolean state = FaweCache.canPassThrough(block.getId(), block.getData());
        int offset = state ? 0 : 1;
        for (int d = 0; d <= clearance; d++) {
            int y1 = y + d;
            block = getLazyBlock(x, y1, z);
            if (FaweCache.canPassThrough(block.getId(), block.getData()) != state) return y1 - offset;
            int y2 = y - d;
            block = getLazyBlock(x, y2, z);
            if (FaweCache.canPassThrough(block.getId(), block.getData()) != state) return y2 + offset;
        }
        if (clearanceAbove != clearanceBelow) {
            if (clearanceAbove < clearanceBelow) {
                for (int layer = y - clearance - 1; layer >= minY; layer--) {
                    block = getLazyBlock(x, layer, z);
                    if (FaweCache.canPassThrough(block.getId(), block.getData()) != state) return layer + offset;
                }
            } else {
                for (int layer = y + clearance + 1; layer <= maxY; layer++) {
                    block = getLazyBlock(x, layer, z);
                    if (FaweCache.canPassThrough(block.getId(), block.getData()) != state) return layer - offset;
                }
            }
        }
        return maxY;
    }


    default public void addCaves(Region region) throws WorldEditException {
        generate(region, new CavesGen(8));
    }

    public default void generate(Region region, GenBase gen) throws WorldEditException {
        for (Vector2D chunkPos : region.getChunks()) {
            gen.generate(chunkPos, this);
        }
    }

    default public void addSchems(Region region, Mask mask, WorldData worldData, ClipboardHolder[] clipboards, int rarity, boolean rotate) throws WorldEditException{
        spawnResource(region, new SchemGen(mask, this, worldData, clipboards, rotate), rarity, 1);
    }

    default public void spawnResource(Region region, Resource gen, int rarity, int frequency) throws WorldEditException {
        PseudoRandom random = new PseudoRandom();
        for (Vector2D chunkPos : region.getChunks()) {
            for (int i = 0; i < frequency; i++) {
                if (random.nextInt(101) > rarity) {
                    continue;
                }
                int x = (chunkPos.getBlockX() << 4) + PseudoRandom.random.nextInt(16);
                int z = (chunkPos.getBlockZ() << 4) + PseudoRandom.random.nextInt(16);
                gen.spawn(random, x, z);
            }
        }
    }

    default public void addOre(Region region, Mask mask, Pattern material, int size, int frequency, int rarity, int minY, int maxY) throws WorldEditException {
        spawnResource(region, new OreGen(this, mask, material, size, minY, maxY), rarity, frequency);
    }

    default public void addOres(Region region, Mask mask) throws WorldEditException {
        addOre(region, mask, new BlockPattern(BlockID.DIRT), 33, 10, 100, 0, 255);
        addOre(region, mask, new BlockPattern(BlockID.GRAVEL), 33, 8, 100, 0, 255);
        addOre(region, mask, new BlockPattern(BlockID.STONE, 1), 33, 10, 100, 0, 79);
        addOre(region, mask, new BlockPattern(BlockID.STONE, 3), 33, 10, 100, 0, 79);
        addOre(region, mask, new BlockPattern(BlockID.STONE, 5), 33, 10, 100, 0, 79);
        addOre(region, mask, new BlockPattern(BlockID.COAL_ORE), 17, 20, 100, 0, 127);
        addOre(region, mask, new BlockPattern(BlockID.IRON_ORE), 9, 20, 100, 0, 63);
        addOre(region, mask, new BlockPattern(BlockID.GOLD_ORE), 9, 2, 100, 0, 31);
        addOre(region, mask, new BlockPattern(BlockID.REDSTONE_ORE), 8, 8, 100, 0, 15);
        addOre(region, mask, new BlockPattern(BlockID.DIAMOND_ORE), 8, 1, 100, 0, 15);
        addOre(region, mask, new BlockPattern(BlockID.LAPIS_LAZULI_ORE), 7, 1, 100, 0, 15);
        addOre(region, mask, new BlockPattern(BlockID.EMERALD_ORE), 5, 1, 100, 4, 31);
    }
}
