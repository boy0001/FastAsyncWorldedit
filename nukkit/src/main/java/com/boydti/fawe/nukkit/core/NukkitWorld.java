package com.boydti.fawe.nukkit.core;

import cn.nukkit.blockentity.BlockEntity;
import cn.nukkit.blockentity.BlockEntityChest;
import cn.nukkit.entity.Entity;
import cn.nukkit.inventory.InventoryHolder;
import cn.nukkit.item.Item;
import cn.nukkit.item.ItemPickaxeDiamond;
import cn.nukkit.level.Level;
import cn.nukkit.math.Vector3;
import com.sk89q.worldedit.BlockVector2D;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalWorld;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.Vector2D;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.blocks.BaseItemStack;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.TreeGenerator;
import com.sk89q.worldedit.world.biome.BaseBiome;
import com.sk89q.worldedit.world.registry.LegacyWorldData;
import com.sk89q.worldedit.world.registry.WorldData;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.annotation.Nullable;


import static com.google.common.base.Preconditions.checkNotNull;

public class NukkitWorld extends LocalWorld {

    private static final Logger logger = WorldEdit.logger;

    private final WeakReference<Level> worldRef;

    /**
     * Construct the object.
     *
     * @param world the world
     */
    @SuppressWarnings("unchecked")
    public NukkitWorld(Level world) {
        this.worldRef = new WeakReference<Level>(world);
    }

    private Vector3 mutable = new Vector3(0, 0, 0);
    private Vector3 setMutable(Vector pt) {
        mutable.x = pt.getX();
        mutable.y = pt.getY();
        mutable.z = pt.getZ();
        return mutable;
    }

    @Override
    public List<com.sk89q.worldedit.entity.Entity> getEntities(Region region) {
        Level world = getLevel();
        cn.nukkit.entity.Entity[] ents = world.getEntities();
        List<com.sk89q.worldedit.entity.Entity> entities = new ArrayList<com.sk89q.worldedit.entity.Entity>();
        for (cn.nukkit.entity.Entity ent : ents) {
            if (region.contains(NukkitUtil.toVector(ent.getLocation()))) {
                entities.add(new NukkitEntity(ent));
            }
        }
        return entities;
    }

    @Override
    public List<com.sk89q.worldedit.entity.Entity> getEntities() {
        List<com.sk89q.worldedit.entity.Entity> list = new ArrayList<com.sk89q.worldedit.entity.Entity>();
        for (Entity entity : getLevel().getEntities()) {
            list.add(new NukkitEntity(entity));
        }
        return list;
    }

    @Nullable
    @Override
    public com.sk89q.worldedit.entity.Entity createEntity(com.sk89q.worldedit.util.Location location, BaseEntity entity) {
        return NukkitUtil.createEntity(getLevel(), location, entity);
    }

    /**
     * Get the world handle.
     *
     * @return the world
     */
    public Level getLevel() {
        return checkNotNull(worldRef.get(), "The world was unloaded and the reference is unavailable");
    }

    @Override
    public String getName() {
        return getLevel().getName();
    }

    @Override
    public int getBlockLightLevel(Vector pt) {
        return getLevel().getBlockLightAt(pt.getBlockX(), pt.getBlockY(), pt.getBlockZ());
    }

    @Override
    public boolean regenerate(Region region, EditSession editSession) {
        BaseBlock[] history = new BaseBlock[16 * 16 * (getMaxY() + 1)];

        for (Vector2D chunk : region.getChunks()) {
            Vector min = new Vector(chunk.getBlockX() * 16, 0, chunk.getBlockZ() * 16);

            // First save all the blocks inside
            for (int x = 0; x < 16; ++x) {
                for (int y = 0; y < (getMaxY() + 1); ++y) {
                    for (int z = 0; z < 16; ++z) {
                        Vector pt = min.add(x, y, z);
                        int index = y * 16 * 16 + z * 16 + x;
                        history[index] = editSession.getBlock(pt);
                    }
                }
            }

            try {
                getLevel().regenerateChunk(chunk.getBlockX(), chunk.getBlockZ());
            } catch (Throwable t) {
                logger.log(java.util.logging.Level.WARNING, "Chunk generation via Nukkit raised an error", t);
            }

            // Then restore
            for (int x = 0; x < 16; ++x) {
                for (int y = 0; y < (getMaxY() + 1); ++y) {
                    for (int z = 0; z < 16; ++z) {
                        Vector pt = min.add(x, y, z);
                        int index = y * 16 * 16 + z * 16 + x;

                        // We have to restore the block if it was outside
                        if (!region.contains(pt)) {
                            editSession.smartSetBlock(pt, history[index]);
                        } else { // Otherwise fool with history
                            editSession.rememberChange(pt, history[index],
                                    editSession.rawGetBlock(pt));
                        }
                    }
                }
            }
        }

        return true;
    }

    @Override
    public boolean clearContainerBlockContents(Vector pt) {
        BlockEntity block = getLevel().getBlockEntity(setMutable(pt));
        if (block == null) {
            return false;
        }
        if (block instanceof InventoryHolder) {
            if (block instanceof BlockEntityChest) {
                ((BlockEntityChest) block).getRealInventory().clearAll();
            } else {
                ((InventoryHolder) block).getInventory().clearAll();
            }
            return true;
        }
        return false;
    }

    @Override
    @Deprecated
    public boolean generateTree(EditSession editSession, Vector pt) {
        return generateTree(TreeGenerator.TreeType.TREE, editSession, pt);
    }

    @Override
    @Deprecated
    public boolean generateBigTree(EditSession editSession, Vector pt) {
        return generateTree(TreeGenerator.TreeType.BIG_TREE, editSession, pt);
    }

    @Override
    @Deprecated
    public boolean generateBirchTree(EditSession editSession, Vector pt) {
        return generateTree(TreeGenerator.TreeType.BIRCH, editSession, pt);
    }

    @Override
    @Deprecated
    public boolean generateRedwoodTree(EditSession editSession, Vector pt) {
        return generateTree(TreeGenerator.TreeType.REDWOOD, editSession, pt);
    }

    @Override
    @Deprecated
    public boolean generateTallRedwoodTree(EditSession editSession, Vector pt) {
        return generateTree(TreeGenerator.TreeType.TALL_REDWOOD, editSession, pt);
    }


    @Override
    public boolean generateTree(TreeGenerator.TreeType type, EditSession editSession, Vector pt) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void dropItem(Vector pt, BaseItemStack item) {
        Level world = getLevel();
        Item nukkitItem = new Item(item.getType(), item.getAmount(),
                item.getData());
        world.dropItem(NukkitUtil.toLocation(world, pt), nukkitItem);
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean isValidBlockType(int type) {
        Item item = Item.get(type);
        if (item == null) {
            return false;
        }
        return item != null && item.getId() < 256 && item.getBlock() != null;
    }

    @Override
    public void checkLoadedChunk(Vector pt) {
        Level world = getLevel();

        if (!world.isChunkLoaded(pt.getBlockX() >> 4, pt.getBlockZ() >> 4)) {
            world.loadChunk(pt.getBlockX() >> 4, pt.getBlockZ() >> 4);
        }
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        } else if ((other instanceof NukkitWorld)) {
            return ((NukkitWorld) other).getLevel().equals(getLevel());
        } else if (other instanceof com.sk89q.worldedit.world.World) {
            return ((com.sk89q.worldedit.world.World) other).getName().equals(getName());
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return getLevel().hashCode();
    }

    @Override
    public int getMaxY() {
        return 255;
    }

    @Override
    public Vector getMinimumPoint() {
        return new Vector(-30000000, 0, -30000000);
    }

    @Override
    public Vector getMaximumPoint() {
        return new Vector(30000000, 255, 30000000);
    }

    @Override
    public void fixAfterFastMode(Iterable<BlockVector2D> chunks) {

    }

    @Override
    public boolean playEffect(Vector position, int type, int data) {
        return false;
    }

    @Override
    public WorldData getWorldData() {
        return LegacyWorldData.getInstance();
    }

    @Override
    public void simulateBlockMine(Vector pt) {
        ItemPickaxeDiamond item = new ItemPickaxeDiamond(Integer.MAX_VALUE);
        getLevel().useBreakOn(setMutable(pt), item, null, true);
    }

    @Override
    public BaseBlock getBlock(Vector position) {
        return NukkitUtil.getBlock(getLevel(), position);
    }

    @Override
    public boolean setBlock(Vector position, BaseBlock block, boolean notifyAndLight) {
        return NukkitUtil.setBlock(getLevel(), position, block);
    }

    @SuppressWarnings("deprecation")
    @Override
    public BaseBlock getLazyBlock(Vector position) {
        return getBlock(position);
    }

    @Override
    public BaseBiome getBiome(Vector2D position) {
        int id = getLevel().getBiomeId(position.getBlockX(), position.getBlockZ());
        return new BaseBiome(id);
    }

    @Override
    public boolean setBiome(Vector2D position, BaseBiome biome) {
        getLevel().setBiomeId(position.getBlockX(), position.getBlockZ(), (byte) biome.getId());
        return true;
    }

}
