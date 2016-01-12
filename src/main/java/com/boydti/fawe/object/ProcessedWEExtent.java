package com.boydti.fawe.object;

import java.util.HashSet;
import java.util.List;

import com.boydti.fawe.config.BBC;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.util.MainUtil;
import com.boydti.fawe.util.SetQueue;
import com.boydti.fawe.util.TaskManager;
import com.boydti.fawe.util.WEManager;
import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.Vector2D;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.biome.BaseBiome;

public class ProcessedWEExtent extends AbstractDelegateExtent {
    private Extent parent;
    
    private boolean BSblocked = false;
    private boolean Eblocked = false;
    private int BScount = 0;
    private int Ecount = 0;
    private int count = 0;

    private int max;
    private final FawePlayer<?> user;
    private final String world;
    private final HashSet<RegionWrapper> mask;
    private final Thread thread;
    
    public ProcessedWEExtent(World world, Thread thread, FawePlayer<?> player, HashSet<RegionWrapper> mask, int max) {
        super(world);
        this.user = player;
        this.world = world.getName();
        this.max = max != -1 ? max : Integer.MAX_VALUE;
        this.mask = mask;
        this.thread = thread;
    }
    
    public void setMax(int max) {
        this.max = max != -1 ? max : Integer.MAX_VALUE;
    }

    public void setParent(Extent parent) {
        this.parent = parent;
    }
    
    @Override
    public Entity createEntity(final Location location, final BaseEntity entity) {
        if (Eblocked) {
            return null;
        }
        Ecount++;
        if (Ecount > Settings.MAX_ENTITIES) {
            Eblocked = true;
            MainUtil.sendAdmin(BBC.WORLDEDIT_DANGEROUS_WORLDEDIT.format(world + ": " + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ(), user));
        }
        if (WEManager.IMP.maskContains(mask, location.getBlockX(), location.getBlockZ())) {
            TaskManager.IMP.task(new Runnable() {
                @Override
                public void run() {
                    ProcessedWEExtent.super.createEntity(location, entity);
                }
            });
        }
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
            return lastBlock = super.getLazyBlock(position);
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
    public boolean setBlock(final Vector location, final BaseBlock block) throws WorldEditException {
        final short id = (short) block.getType();
        switch (id) {
            case 54:
            case 130:
            case 142:
            case 27:
            case 137:
            case 52:
            case 154:
            case 84:
            case 25:
            case 144:
            case 138:
            case 176:
            case 177:
            case 63:
            case 119:
            case 68:
            case 323:
            case 117:
            case 116:
            case 28:
            case 66:
            case 157:
            case 61:
            case 62:
            case 140:
            case 146:
            case 149:
            case 150:
            case 158:
            case 23:
            case 123:
            case 124:
            case 29:
            case 33:
            case 151:
            case 178: {
                if (BSblocked) {
                    return false;
                }
                BScount++;
                if (BScount > Settings.MAX_BLOCKSTATES) {
                    BSblocked = true;
                    MainUtil.sendAdmin(BBC.WORLDEDIT_DANGEROUS_WORLDEDIT.format(world + ": " + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ(), user));
                }
                final int x = location.getBlockX();
                final int z = location.getBlockZ();
                if (WEManager.IMP.maskContains(mask, x, z)) {
                    if (count++ > max) {
                        if (parent != null) {
                            WEManager.IMP.cancelEdit(parent);
                            parent = null;
                        }
                        return false;
                    }
                    SetQueue.IMP.setBlock(world, x, location.getBlockY(), z, id, (byte) block.getData());
                }
                break;
            }
            default: {
                final int x = location.getBlockX();
                final int y = location.getBlockY();
                final int z = location.getBlockZ();
                if (WEManager.IMP.maskContains(mask, location.getBlockX(), location.getBlockZ())) {
                    if (count++ > max) {
                        WEManager.IMP.cancelEdit(parent);
                        parent = null;
                        return false;
                    }
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
                        case 54:
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
                            break;
                        }
                        default: {
                            SetQueue.IMP.setBlock(world, x, y, z, id, (byte) block.getData());
                            break;
                        }
                    }
                    return true;
                }
            }
            
        }
        return false;
    }
    
    @Override
    public boolean setBiome(final Vector2D position, final BaseBiome biome) {
        if (WEManager.IMP.maskContains(mask, position.getBlockX(), position.getBlockZ())) {
            SetQueue.IMP.setBiome(world, position.getBlockX(), position.getBlockZ(), biome);
        }
        return false;
    }
}
