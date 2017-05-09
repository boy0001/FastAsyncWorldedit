package com.boydti.fawe.object.extent;

import com.boydti.fawe.config.BBC;
import com.boydti.fawe.object.FaweLimit;
import com.boydti.fawe.object.RegionWrapper;
import com.boydti.fawe.util.WEManager;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.Vector2D;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.biome.BaseBiome;
import java.util.Collection;
import javax.annotation.Nullable;

public abstract class FaweRegionExtent extends ResettableExtent {
    private final FaweLimit limit;

    /**
     * Create a new instance.
     *
     * @param extent the extent
     */
    public FaweRegionExtent(Extent extent, FaweLimit limit) {
        super(extent);
        this.limit = limit;
    }

    public abstract boolean contains(int x, int y, int z);

    public abstract boolean contains(int x, int z);

    public abstract Collection<RegionWrapper> getRegions();

    public boolean isGlobal() {
        for (RegionWrapper region : getRegions()) {
            if (region.isGlobal()) {
                return true;
            }
        }
        return false;
    }

    public final boolean contains(Vector p) {
        return contains(p.getBlockX(), p.getBlockY(), p.getBlockZ());
    }

    public final boolean contains(Vector2D p) {
        return contains(p.getBlockX(), p.getBlockZ());
    }

    @Override
    public boolean setBlock(Vector location, BaseBlock block) throws WorldEditException {
        if (!contains(location)) {
            if (!limit.MAX_FAILS()) {
                WEManager.IMP.cancelEditSafe(this, BBC.WORLDEDIT_CANCEL_REASON_MAX_FAILS);
            }
            return false;
        }
        return super.setBlock(location, block);
    }

    @Override
    public boolean setBlock(int x, int y, int z, BaseBlock block) throws WorldEditException {
        if (!contains(x, y, z)) {
            if (!limit.MAX_FAILS()) {
                WEManager.IMP.cancelEditSafe(this, BBC.WORLDEDIT_CANCEL_REASON_MAX_FAILS);
            }
            return false;
        }
        return super.setBlock(x, y, z, block);
    }

    @Override
    public boolean setBiome(Vector2D position, BaseBiome biome) {
        if (!contains(position)) {
            if (!limit.MAX_FAILS()) {
                WEManager.IMP.cancelEditSafe(this, BBC.WORLDEDIT_CANCEL_REASON_MAX_FAILS);
            }
            return false;
        }
        return super.setBiome(position, biome);
    }

    @Override
    public BaseBiome getBiome(Vector2D position) {
        if (!contains(position)) {
            if (!limit.MAX_FAILS()) {
                WEManager.IMP.cancelEditSafe(this, BBC.WORLDEDIT_CANCEL_REASON_MAX_FAILS);
            }
            return EditSession.nullBiome;
        }
        return super.getBiome(position);
    }

    @Override
    public BaseBlock getBlock(Vector position) {
        if (!contains(position)) {
            if (!limit.MAX_FAILS()) {
                WEManager.IMP.cancelEditSafe(this, BBC.WORLDEDIT_CANCEL_REASON_MAX_FAILS);
            }
            return EditSession.nullBlock;
        }
        return super.getBlock(position);
    }

    @Override
    public BaseBlock getLazyBlock(Vector position) {
        if (!contains(position)) {
            if (!limit.MAX_FAILS()) {
                WEManager.IMP.cancelEditSafe(this, BBC.WORLDEDIT_CANCEL_REASON_MAX_FAILS);
            }
            return EditSession.nullBlock;
        }
        return super.getLazyBlock(position);
    }

    @Override
    public BaseBlock getLazyBlock(int x, int y, int z) {
        if (!contains(x, y, z)) {
            if (!limit.MAX_FAILS()) {
                WEManager.IMP.cancelEditSafe(this, BBC.WORLDEDIT_CANCEL_REASON_MAX_FAILS);
            }
            return EditSession.nullBlock;
        }
        return super.getLazyBlock(x, y, z);
    }

    @Override
    public int getBlockLight(int x, int y, int z) {
        if (!contains(x, y, z)) {
            if (!limit.MAX_FAILS()) {
                WEManager.IMP.cancelEditSafe(this, BBC.WORLDEDIT_CANCEL_REASON_MAX_FAILS);
            }
            return 0;
        }
        return super.getBlockLight(x, y, z);
    }

    @Override
    public int getBrightness(int x, int y, int z) {
        if (!contains(x, y, z)) {
            if (!limit.MAX_FAILS()) {
                WEManager.IMP.cancelEditSafe(this, BBC.WORLDEDIT_CANCEL_REASON_MAX_FAILS);
            }
            return 0;
        }
        return super.getBrightness(x, y, z);
    }

    @Override
    public int getLight(int x, int y, int z) {
        if (!contains(x, y, z)) {
            if (!limit.MAX_FAILS()) {
                WEManager.IMP.cancelEditSafe(this, BBC.WORLDEDIT_CANCEL_REASON_MAX_FAILS);
            }
            return 0;
        }
        return super.getLight(x, y, z);
    }

    @Override
    public int getOpacity(int x, int y, int z) {
        if (!contains(x, y, z)) {
            if (!limit.MAX_FAILS()) {
                WEManager.IMP.cancelEditSafe(this, BBC.WORLDEDIT_CANCEL_REASON_MAX_FAILS);
            }
            return 0;
        }
        return super.getOpacity(x, y, z);
    }

    @Override
    public int getSkyLight(int x, int y, int z) {
        if (!contains(x, y, z)) {
            if (!limit.MAX_FAILS()) {
                WEManager.IMP.cancelEditSafe(this, BBC.WORLDEDIT_CANCEL_REASON_MAX_FAILS);
            }
            return 0;
        }
        return super.getSkyLight(x, y, z);
    }

    @Nullable
    @Override
    public Entity createEntity(Location location, BaseEntity entity) {
        if (!contains(location.getBlockX(), location.getBlockY(), location.getBlockZ())) {
            if (!limit.MAX_FAILS()) {
                WEManager.IMP.cancelEditSafe(this, BBC.WORLDEDIT_CANCEL_REASON_MAX_FAILS);
            }
            return null;
        }
        return super.createEntity(location, entity);
    }
}
