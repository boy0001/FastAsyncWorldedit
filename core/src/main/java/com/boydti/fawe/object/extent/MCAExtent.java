package com.boydti.fawe.object.extent;

import com.boydti.fawe.jnbt.anvil.MCAFile;
import com.boydti.fawe.object.FaweQueue;
import com.boydti.fawe.util.MathMan;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.Vector2D;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.biome.BaseBiome;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

public class MCAExtent extends AbstractDelegateExtent {
    private final FaweQueue queue;
    private final File folder;

    private Map<Long, MCAFile> regions;

    public MCAExtent(World world, FaweQueue queue) {
        super(world);
        this.queue = queue;
        this.folder = new File(queue.getSaveFolder(), "regions");
        this.regions = new HashMap<>();
    }

    public FaweQueue getQueue() {
        return queue;
    }

    private int lastX = Integer.MAX_VALUE;
    private int lastZ = Integer.MAX_VALUE;
    private MCAFile lastMCA;

    private MCAFile getMCA(int x, int y, int z) {
        int MCAX = x >> 9;
        int MCAZ = z >> 9;
        if (MCAX == lastX && MCAZ == lastZ) {
            return lastMCA;
        }
        lastX = MCAX;
        lastZ = MCAZ;
        long pair = MathMan.pairInt(lastX, lastZ);
        lastMCA = regions.get(pair);
        if (lastMCA == null) {
//            lastMCA = new MCAFile(folder, lastX, lastZ);
            // TODO
            regions.put(pair, lastMCA);
        }
        return lastMCA;
    }

    @Override
    public BaseBlock getBlock(Vector position) {
        // TODO get block from MCA
        return null;
    }

    @Override
    public BaseBlock getLazyBlock(Vector position) {
        // TODO set block in MCA
        return null;
    }

    @Override
    public boolean setBlock(Vector location, BaseBlock block) throws WorldEditException {
        // TODO set block in MCA
        return false;
    }

    @Override
    @Nullable
    public Entity createEntity(Location location, BaseEntity entity) {
        // TODO add entity to MCA
        return null;
    }

    @Override
    public List<? extends Entity> getEntities() {
        // TODO get entities from MCA
        return null;
    }

    @Override
    public List<? extends Entity> getEntities(Region region) {
        // TODO get entities from MCA
        return null;
    }

    @Override
    public BaseBiome getBiome(Vector2D position) {
        // TODO get biome from MCA
        return null;
    }

    @Override
    public boolean setBiome(Vector2D position, BaseBiome biome) {
        return false;
    }

    @Override
    public Vector getMinimumPoint() {
        return super.getMinimumPoint();
    }

    @Override
    public Vector getMaximumPoint() {
        return super.getMaximumPoint();
    }

    protected Operation commitBefore() {
        // Save MCA file if modified
        return null;
    }
}
