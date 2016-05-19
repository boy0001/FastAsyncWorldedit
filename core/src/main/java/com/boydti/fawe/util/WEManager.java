package com.boydti.fawe.util;

import com.boydti.fawe.config.BBC;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.FaweLocation;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.RegionWrapper;
import com.boydti.fawe.object.RunnableVal;
import com.boydti.fawe.object.exception.FaweException;
import com.boydti.fawe.object.extent.NullExtent;
import com.boydti.fawe.regions.FaweMask;
import com.boydti.fawe.regions.FaweMaskManager;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.HashSet;

public class WEManager {

    public final static WEManager IMP = new WEManager();

    public final ArrayDeque<FaweMaskManager> managers = new ArrayDeque<>();

    public void cancelEdit(Extent parent, BBC reason) throws WorldEditException {
        try {
            final Field field = AbstractDelegateExtent.class.getDeclaredField("extent");
            field.setAccessible(true);
            field.set(parent, new NullExtent((Extent) field.get(parent), reason));
        } catch (final Exception e) {
            MainUtil.handleError(e);
        }
        throw new FaweException(reason);
    }

    public boolean maskContains(final HashSet<RegionWrapper> mask, final int x, final int z) {
        for (final RegionWrapper region : mask) {
            if ((x >= region.minX) && (x <= region.maxX) && (z >= region.minZ) && (z <= region.maxZ)) {
                return true;
            }
        }
        return false;
    }

    public boolean maskContains(RegionWrapper[] mask, final int x, final int z) {
        switch (mask.length) {
            case 0:
                return false;
            case 1:
                return mask[0].isIn(x, z);
            default:
                for (final RegionWrapper region : mask) {
                    if (region.isIn(x, z)) {
                        return true;
                    }
                }
                return false;
        }
    }

    /**
     * Get a player's mask
     * @param player
     * @return
     */
    public HashSet<RegionWrapper> getMask(final FawePlayer<?> player) {
        return TaskManager.IMP.sync(new RunnableVal<HashSet<RegionWrapper>>() {
            @Override
            public void run(HashSet<RegionWrapper> value) {
                final HashSet<RegionWrapper> regions = new HashSet<>();
                if (player.hasPermission("fawe.bypass") || !Settings.REGION_RESTRICTIONS) {
                    regions.add(new RegionWrapper(Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE));
                    player.deleteMeta("lastmask");
                    this.value = regions;
                    return;
                }
                for (final FaweMaskManager manager : managers) {
                    if (player.hasPermission("fawe." + manager.getKey())) {
                        final FaweMask mask = manager.getMask(player);
                        if (mask != null) {
                            regions.addAll(mask.getRegions());
                        }
                    }
                }
                if (regions.size() == 0) {
                    HashSet<RegionWrapper> mask = player.<HashSet<RegionWrapper>>getMeta("lastmask");
                    if (mask != null) {
                        FaweLocation loc = player.getLocation();
                        for (RegionWrapper region : mask) {
                            if (region.isIn(loc.x, loc.z)) {
                                player.deleteMeta("lastmask");
                                this.value = regions;
                                return;
                            }
                        }
                        this.value = mask;
                        return;
                    }
                }
                player.setMeta("lastmask", regions);
                this.value = regions;
                return;
            }
        }, 1000);
    }

    public boolean intersects(final RegionWrapper region1, final RegionWrapper region2) {
        return (region1.minX <= region2.maxX) && (region1.maxX >= region2.minX) && (region1.minZ <= region2.maxZ) && (region1.maxZ >= region2.minZ);
    }

    public boolean regionContains(final RegionWrapper selection, final HashSet<RegionWrapper> mask) {
        for (final RegionWrapper region : mask) {
            if (this.intersects(region, selection)) {
                return true;
            }
        }
        return false;
    }

    public boolean delay(final FawePlayer<?> player, final String command) {
        final long start = System.currentTimeMillis();
        return this.delay(player, new Runnable() {
            @Override
            public void run() {
                try {
                    if ((System.currentTimeMillis() - start) > 1000) {
                        BBC.WORLDEDIT_RUN.send(FawePlayer.wrap(player));
                    }
                    TaskManager.IMP.task(new Runnable() {
                        @Override
                        public void run() {
                            final long start = System.currentTimeMillis();
                            player.executeCommand(command.substring(1));
                            TaskManager.IMP.later(new Runnable() {
                                @Override
                                public void run() {
                                    SetQueue.IMP.addTask(new Runnable() {
                                        @Override
                                        public void run() {
                                            if ((System.currentTimeMillis() - start) > 1000) {
                                                BBC.WORLDEDIT_COMPLETE.send(FawePlayer.wrap(player));
                                            }
                                        }
                                    });
                                }
                            }, 2);
                        }
                    });
                } catch (final Exception e) {
                    MainUtil.handleError(e);
                }
            }
        }, false, false);
    }

    public boolean delay(final FawePlayer<?> player, final Runnable whenDone, final boolean delayed, final boolean onlyDelayedExecution) {
        final boolean free = SetQueue.IMP.addTask(null);
        if (free) {
            if (delayed) {
                if (whenDone != null) {
                    whenDone.run();
                }
            } else {
                if ((whenDone != null) && !onlyDelayedExecution) {
                    whenDone.run();
                } else {
                    return false;
                }
            }
        } else {
            if (!delayed && (player != null)) {
                BBC.WORLDEDIT_DELAYED.send(player);
            }
            SetQueue.IMP.addTask(whenDone);
        }
        return true;
    }
}
