package com.boydti.fawe.object.pattern;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.Vector2D;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.pattern.AbstractPattern;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.biome.BaseBiome;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

public class PatternExtent extends AbstractPattern implements Extent {
    private final Pattern pattern;
    private transient BaseBlock block;
    private transient Vector target = new Vector();

    public PatternExtent(Pattern pattern) {
        this.pattern = pattern;
    }

    private void readObject(java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        target = new Vector();
    }

    @Override
    public Vector getMinimumPoint() {
        return new Vector(Integer.MIN_VALUE, 0, Integer.MIN_VALUE);
    }

    @Override
    public Vector getMaximumPoint() {
        return new Vector(Integer.MAX_VALUE, 255, Integer.MAX_VALUE);
    }

    @Override
    public List<? extends Entity> getEntities(Region region) {
        return new ArrayList<>();
    }

    @Override
    public List<? extends Entity> getEntities() {
        return new ArrayList<>();
    }

    @Nullable
    @Override
    public Entity createEntity(Location location, BaseEntity entity) {
        return null;
    }

    @Override
    public BaseBlock getBlock(Vector position) {
        BaseBlock tmp = pattern.apply(position);
        if (position == target || (position.getX() == target.getX() && position.getY() == target.getY() && position.getZ() == target.getZ())) {
            block = tmp;
        } else {
            block = null;
        }
        return tmp;
    }

    public void setTarget(Vector vector) {
        this.target = vector;
    }

    public boolean getAndResetTarget(Extent extent, Vector set, Vector get) throws WorldEditException {
        BaseBlock result = block;
        if (result != null) {
            block = null;
            return extent.setBlock(set, result);
        } else {
            return pattern.apply(extent, set, target);
        }
    }

    public BaseBlock getAndResetTarget() {
        BaseBlock result = block;
        if (result != null) {
            block = null;
            return result;
        } else {
            return pattern.apply(target);
        }
    }

    @Override
    public BaseBlock getLazyBlock(Vector position) {
        return getBlock(position);
    }

    @Override
    public BaseBiome getBiome(Vector2D position) {
        return new BaseBiome(0);
    }

    @Override
    public boolean setBlock(Vector position, BaseBlock block) throws WorldEditException {
        return false;
    }

    @Override
    public boolean setBiome(Vector2D position, BaseBiome biome) {
        return false;
    }

    @Nullable
    @Override
    public Operation commit() {
        return null;
    }

    @Override
    public BaseBlock apply(Vector position) {
        return pattern.apply(position);
    }

    @Override
    public boolean apply(Extent extent, Vector set, Vector get) throws WorldEditException {
        return pattern.apply(extent, set, get);
    }
}
