package com.boydti.fawe.object.extent;

import com.boydti.fawe.FaweCache;
import java.util.HashSet;
import java.util.List;

import com.boydti.fawe.config.BBC;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.RegionWrapper;
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

    public ProcessedWEExtent(final World world, final Thread thread, final FawePlayer<?> player, final HashSet<RegionWrapper> mask, final int max) {
        super(world);
        this.user = player;
        this.world = world.getName();
        this.max = max != -1 ? max : Integer.MAX_VALUE;
        this.mask = mask;
        this.thread = thread;
    }

    public void setMax(final int max) {
        this.max = max != -1 ? max : Integer.MAX_VALUE;
    }

    public void setParent(final Extent parent) {
        this.parent = parent;
    }

    @Override
    public Entity createEntity(final Location location, final BaseEntity entity) {
        if (this.Eblocked) {
            return null;
        }
        this.Ecount++;
        if (this.Ecount > Settings.MAX_ENTITIES) {
            this.Eblocked = true;
            MainUtil.sendAdmin(BBC.WORLDEDIT_DANGEROUS_WORLDEDIT.format(this.world + ": " + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ(), this.user));
        }
        if (WEManager.IMP.maskContains(this.mask, location.getBlockX(), location.getBlockZ())) {
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
    public BaseBiome getBiome(final Vector2D position) {
        if (!SetQueue.IMP.isChunkLoaded(this.world, position.getBlockX() >> 4, position.getBlockZ() >> 4)) {
            return EditSession.nullBiome;
        }
        synchronized (this.thread) {
            return super.getBiome(position);
        }
    }

    private BaseBlock lastBlock;
    private BlockVector lastVector;

    @Override
    public BaseBlock getLazyBlock(final Vector position) {
        if ((this.lastBlock != null) && this.lastVector.equals(position.toBlockVector())) {
            return this.lastBlock;
        }
        if (!SetQueue.IMP.isChunkLoaded(this.world, position.getBlockX() >> 4, position.getBlockZ() >> 4)) {
            try {
                this.lastVector = position.toBlockVector();
                return this.lastBlock = super.getBlock(position);
            } catch (final Throwable e) {
                return EditSession.nullBlock;
            }
        }
        synchronized (this.thread) {
            this.lastVector = position.toBlockVector();
            return this.lastBlock = super.getLazyBlock(position);
        }
    }

    @Override
    public List<? extends Entity> getEntities() {
        synchronized (this.thread) {
            return super.getEntities();
        }
    }

    @Override
    public List<? extends Entity> getEntities(final Region region) {
        synchronized (this.thread) {
            return super.getEntities(region);
        }
    }

    @Override
    public BaseBlock getBlock(final Vector position) {
        return this.getLazyBlock(position);
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
                if (this.BSblocked) {
                    return false;
                }
                this.BScount++;
                if (this.BScount > Settings.MAX_BLOCKSTATES) {
                    this.BSblocked = true;
                    MainUtil.sendAdmin(BBC.WORLDEDIT_DANGEROUS_WORLDEDIT.format(this.world + ": " + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ(), this.user));
                }
                final int x = location.getBlockX();
                final int z = location.getBlockZ();
                if (WEManager.IMP.maskContains(this.mask, x, z)) {
                    if (this.count++ > this.max) {
                        if (this.parent != null) {
                            WEManager.IMP.cancelEdit(this.parent);
                            this.parent = null;
                        }
                        return false;
                    }
                    if (block.hasNbtData()) {
                        SetQueue.IMP.addTask(this.world, x, location.getBlockY(), z, new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    ProcessedWEExtent.super.setBlock(location, block);
                                } catch (WorldEditException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                        return true;
                    }
                    SetQueue.IMP.setBlock(this.world, x, location.getBlockY(), z, id, FaweCache.hasData(id) ? (byte) block.getData() : 0);
                    return true;
                }
                return false;
            }
            default: {
                final int x = location.getBlockX();
                final int y = location.getBlockY();
                final int z = location.getBlockZ();
                if (WEManager.IMP.maskContains(this.mask, location.getBlockX(), location.getBlockZ())) {
                    if (this.count++ > this.max) {
                        WEManager.IMP.cancelEdit(this.parent);
                        this.parent = null;
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
                            SetQueue.IMP.setBlock(this.world, x, y, z, id);
                            return true;
                        }
                        default: {
                            SetQueue.IMP.setBlock(this.world, x, y, z, id, (byte) block.getData());
                            return true;
                        }
                    }
                }
                return false;
            }
        }
    }

    @Override
    public boolean setBiome(final Vector2D position, final BaseBiome biome) {
        if (WEManager.IMP.maskContains(this.mask, position.getBlockX(), position.getBlockZ())) {
            SetQueue.IMP.setBiome(this.world, position.getBlockX(), position.getBlockZ(), biome);
        }
        return false;
    }
}
