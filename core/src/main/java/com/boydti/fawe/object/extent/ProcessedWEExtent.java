package com.boydti.fawe.object.extent;

import com.boydti.fawe.FaweCache;
import com.boydti.fawe.config.BBC;
import com.boydti.fawe.object.FaweLimit;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.RegionWrapper;
import com.boydti.fawe.util.FaweQueue;
import com.boydti.fawe.util.MainUtil;
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
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.biome.BaseBiome;
import java.util.HashSet;
import java.util.List;

public class ProcessedWEExtent extends FaweExtent {
    private final FaweQueue queue;
    private final FaweLimit limit;
    private Extent parent;

    private final FawePlayer<?> user;
    private final HashSet<RegionWrapper> mask;

    public ProcessedWEExtent(final World world, final FawePlayer<?> player, final HashSet<RegionWrapper> mask, FaweLimit limit, FaweQueue queue) {
        super(world);
        this.user = player;
        this.queue = queue;
        this.mask = mask;
        this.limit = limit;
    }

    public void setParent(final Extent parent) {
        this.parent = parent;
    }

    @Override
    public Entity createEntity(final Location location, final BaseEntity entity) {
        if (limit.MAX_ENTITIES-- < 0 || entity == null) {
            return null;
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
        if (!queue.isChunkLoaded(position.getBlockX() >> 4, position.getBlockZ() >> 4)) {
            return EditSession.nullBiome;
        }
        return super.getBiome(position);
    }

    private BaseBlock lastBlock;
    private BlockVector lastVector;

    @Override
    public BaseBlock getLazyBlock(final Vector position) {
//        TODO get fast!
//        TODO caches base blocks

        if ((this.lastBlock != null) && this.lastVector.equals(position.toBlockVector())) {
            return this.lastBlock;
        }
        if (!queue.isChunkLoaded(position.getBlockX() >> 4, position.getBlockZ() >> 4)) {
            try {
                this.lastVector = position.toBlockVector();
                return this.lastBlock = super.getBlock(position);
            } catch (final Throwable e) {
                return EditSession.nullBlock;
            }
        }
        this.lastVector = position.toBlockVector();
        return super.getLazyBlock(position);
    }

    @Override
    public List<? extends Entity> getEntities() {
        return super.getEntities();
    }

    @Override
    public List<? extends Entity> getEntities(final Region region) {
        return super.getEntities(region);
    }

    @Override
    public BaseBlock getBlock(final Vector position) {
        return this.getLazyBlock(position);
    }

    @Override
    public boolean setBlock(final Vector location, final BaseBlock block) throws WorldEditException {
        final short id = (short) block.getType();
        boolean nbt = true;
        switch (id) {
            case 63:
            case 68:
                if (block.hasNbtData() && !MainUtil.isValidSign(block.getNbtData())) {
                    nbt = false;
                }
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
            case 119:
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
                if (limit.MAX_BLOCKSTATES-- < 0) {
                    if (this.parent != null) {
                        WEManager.IMP.cancelEdit(this.parent, BBC.WORLDEDIT_CANCEL_REASON_MAX_TILES);
                        this.parent = null;
                    }
                    return false;
                }
                final int x = location.getBlockX();
                final int z = location.getBlockZ();
                if (WEManager.IMP.maskContains(this.mask, x, z)) {
                    if (limit.MAX_CHANGES-- < 0) {
                        if (this.parent != null) {
                            WEManager.IMP.cancelEdit(this.parent, BBC.WORLDEDIT_CANCEL_REASON_MAX_CHANGES);
                            this.parent = null;
                        }
                        return false;
                    }
                    if (block.hasNbtData() && nbt) {
                        final Vector loc = new Vector(location.x, location.y, location.z);
                        queue.addTask(x >> 4, z >> 4, new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    ProcessedWEExtent.super.setBlock(loc, block);
                                } catch (WorldEditException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                        return true;
                    }
                    queue.setBlock(x, location.getBlockY(), z, id, FaweCache.hasData(id) ? (byte) block.getData() : 0);
                    return true;
                } else if (limit.MAX_FAILS-- < 0) {
                    if (this.parent != null) {
                        WEManager.IMP.cancelEdit(this.parent, BBC.WORLDEDIT_CANCEL_REASON_MAX_FAILS);
                        this.parent = null;
                    }
                }
                return false;
            }
            default: {
                final int x = location.getBlockX();
                final int y = location.getBlockY();
                final int z = location.getBlockZ();
                if (WEManager.IMP.maskContains(this.mask, location.getBlockX(), location.getBlockZ())) {
                    if (limit.MAX_CHANGES -- <0) {
                        WEManager.IMP.cancelEdit(this.parent, BBC.WORLDEDIT_CANCEL_REASON_MAX_CHANGES);
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
                        case 56:
                        case 57:
                        case 58:
                        case 60:
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
                        case 181:
                        case 182:
                        case 188:
                        case 189:
                        case 190:
                        case 191:
                        case 192: {
                            queue.setBlock(x, y, z, id, (byte) 0);
                            return true;
                        }
                        default: {
                            queue.setBlock(x, y, z, id, (byte) block.getData());
                            return true;
                        }
                    }
                } else if (limit.MAX_FAILS-- < 0) {
                    if (this.parent != null) {
                        WEManager.IMP.cancelEdit(this.parent, BBC.WORLDEDIT_CANCEL_REASON_MAX_FAILS);
                        this.parent = null;
                    }
                }
                return false;
            }
        }
    }

    @Override
    public boolean setBiome(final Vector2D position, final BaseBiome biome) {
        if (WEManager.IMP.maskContains(this.mask, position.getBlockX(), position.getBlockZ())) {
            queue.setBiome(position.getBlockX(), position.getBlockZ(), biome);
        }
        return false;
    }

    @Override
    public boolean contains(int x, int y, int z) {
        return WEManager.IMP.maskContains(this.mask, x, z);
    }
}
