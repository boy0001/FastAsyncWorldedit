package com.boydti.fawe.object.

import com.boydti.fawe.example.NMSMappedFaweQueue;
import com.boydti.fawe.jnbt.MCRFile;
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

public class MCRExtent extends AbstractDelegateExtent {
    private final FaweQueue queue;
    private final File folder;

    private Map<Long, MCRFile> regions;

    public MCRExtent(World world, FaweQueue queue) {
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
    private MCRFile lastMCR;

    private MCRFile getMCR(int x, int y, int z) {
        int mcrX = x >> 9;
        int mcrZ = z >> 9;
        if (mcrX == lastX && mcrZ == lastZ) {
            return lastMCR;
        }
        lastX = mcrX;
        lastZ = mcrZ;
        long pair = MathMan.pairInt(lastX, lastZ);
        lastMCR = regions.get(pair);
        if (lastMCR == null) {
            lastMCR = new MCRFile(folder, lastX, lastZ);
            regions.put(pair, lastMCR);
        }
        return lastMCR;
    }

    @Override
    public BaseBlock getBlock(Vector position) {
        // TODO get block from MCR
        return null;
    }

    @Override
    public BaseBlock getLazyBlock(Vector position) {
        // TODO set block in MCR
        return null;
    }

    @Override
    public boolean setBlock(Vector location, BaseBlock block) throws WorldEditException {
        // TODO set block in MCR
        return false;
    }

    @Override
    @Nullable
    public Entity createEntity(Location location, BaseEntity entity) {
        // TODO add entity to MCR
        return null;
    }

    @Override
    public List<? extends Entity> getEntities() {
        // TODO get entities from MCR
        return null;
    }

    @Override
    public List<? extends Entity> getEntities(Region region) {
        // TODO get entities from MCR
        return null;
    }

    @Override
    public BaseBiome getBiome(Vector2D position) {
        // TODO get biome from MCR
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
        // Save MCR file if modified
        return null;
    }
}
