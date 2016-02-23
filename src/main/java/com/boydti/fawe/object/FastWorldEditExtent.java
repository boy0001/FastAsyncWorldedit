package com.boydti.fawe.object;

import java.util.List;

import com.boydti.fawe.util.SetQueue;
import com.boydti.fawe.util.TaskManager;
import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.Vector2D;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.biome.BaseBiome;

public class FastWorldEditExtent extends AbstractDelegateExtent {
    
    private final String world;
    private final Thread thread;
    
    public FastWorldEditExtent(World world, Thread thread) {
        super(world);
        this.thread = thread;
        this.world = world.getName();
    }
    
    @Override
    public Entity createEntity(final Location location, final BaseEntity entity) {
        TaskManager.IMP.task(new Runnable() {
            @Override
            public void run() {
                FastWorldEditExtent.super.createEntity(location, entity);
            }
        });
        return null;
    }
    
    @Override
    public BaseBiome getBiome(Vector2D position) {
        if (!SetQueue.IMP.isChunkLoaded(world, position.getBlockX() >> 4, position.getBlockZ() >> 4)) {
            return EditSession.nullBiome;
        }
        synchronized (thread) {
            return super.getBiome(position);
        }
    }
    
    private BaseBlock lastBlock;
    private BlockVector lastVector;

    @Override
    public BaseBlock getLazyBlock(Vector position) {
        if (lastBlock != null && lastVector.equals(position.toBlockVector())) {
            return lastBlock;
        }
        if (!SetQueue.IMP.isChunkLoaded(world, position.getBlockX() >> 4, position.getBlockZ() >> 4)) {
            try {
                lastVector = position.toBlockVector();
                return lastBlock = super.getBlock(position);
            } catch (Throwable e) {
                return EditSession.nullBlock;
            }
        }
        synchronized (thread) {
            lastVector = position.toBlockVector();
            return lastBlock = super.getBlock(position);
        }
    }

    @Override
    public List<? extends Entity> getEntities() {
        synchronized (thread) {
            return super.getEntities();
        }
    }
    
    @Override
    public List<? extends Entity> getEntities(Region region) {
        synchronized (thread) {
            return super.getEntities(region);
        }
    }
    
    @Override
    public BaseBlock getBlock(Vector position) {
        return getLazyBlock(position);
    }
    
    @Override
    public boolean setBiome(Vector2D position, BaseBiome biome) {
        SetQueue.IMP.setBiome(world, position.getBlockX(), position.getBlockZ(), biome);
        return true;
    }
    
    @Override
    public boolean setBlock(Vector location, BaseBlock block) throws WorldEditException {
        short id = (short) block.getId();
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        switch (id) {
            case 0:
            case 2:
            case 4:
            case 13:
            case 14:
            case 15:
            case 20:
            case 21:
            case 22:
            case 25:
            case 30:
            case 32:
            case 37:
            case 39:
            case 40:
            case 41:
            case 42:
            case 45:
            case 46:
            case 47:
            case 48:
            case 49:
            case 51:
            case 52:
            case 55:
            case 56:
            case 57:
            case 58:
            case 60:
            case 61:
            case 62:
            case 7:
            case 8:
            case 9:
            case 10:
            case 11:
            case 73:
            case 74:
            case 78:
            case 79:
            case 80:
            case 81:
            case 82:
            case 83:
            case 84:
            case 85:
            case 87:
            case 88:
            case 101:
            case 102:
            case 103:
            case 110:
            case 112:
            case 113:
            case 117:
            case 121:
            case 122:
            case 123:
            case 124:
            case 129:
            case 133:
            case 138:
            case 137:
            case 140:
            case 165:
            case 166:
            case 169:
            case 170:
            case 172:
            case 173:
            case 174:
            case 176:
            case 177:
            case 181:
            case 182:
            case 188:
            case 189:
            case 190:
            case 191:
            case 192: {
                SetQueue.IMP.setBlock(world, x, y, z, id);
                return true;
            }
            default: {
                SetQueue.IMP.setBlock(world, x, y, z, id, (byte) block.getData());
                return true;
            }
        }
    }
    
}
