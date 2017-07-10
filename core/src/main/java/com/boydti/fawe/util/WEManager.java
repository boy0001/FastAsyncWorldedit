package com.boydti.fawe.util;

import com.boydti.fawe.config.BBC;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.FaweLocation;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.RegionWrapper;
import com.boydti.fawe.object.exception.FaweException;
import com.boydti.fawe.object.extent.NullExtent;
import com.boydti.fawe.regions.FaweMask;
import com.boydti.fawe.regions.FaweMaskManager;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class WEManager {

    public final static WEManager IMP = new WEManager();

    public final ArrayDeque<FaweMaskManager> managers = new ArrayDeque<>();

    public void cancelEditSafe(Extent parent, BBC reason) throws FaweException {
        try {
            final Field field = AbstractDelegateExtent.class.getDeclaredField("extent");
            field.setAccessible(true);
            Object currentExtent = field.get(parent);
            if (!(currentExtent instanceof NullExtent)) {
                field.set(parent, new NullExtent((Extent) field.get(parent), reason));
            }
        } catch (final Exception e) {
            MainUtil.handleError(e);
        }
        throw new FaweException(reason);
    }

    public void cancelEdit(Extent parent, BBC reason) throws WorldEditException {
        cancelEditSafe(parent, reason);
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

    @Deprecated
    public RegionWrapper[] getMask(final FawePlayer<?> player) {
        return getMask(player, FaweMaskManager.MaskType.MEMBER);
    }

    public boolean isIn(int x, int y, int z, Collection<RegionWrapper> regions) {
        for (RegionWrapper region : regions) {
            if (region.isIn(x, y, z)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get a player's mask
     *
     * @param player
     * @return
     */
    public RegionWrapper[] getMask(final FawePlayer<?> player, FaweMaskManager.MaskType type) {
        if (player.hasPermission("fawe.bypass") || !Settings.IMP.REGION_RESTRICTIONS) {
            return new RegionWrapper[]{RegionWrapper.GLOBAL()};
        }
        FaweLocation loc = player.getLocation();
        String world = loc.world;
        if (!world.equals(player.getMeta("lastMaskWorld"))) {
            player.deleteMeta("lastMaskWorld");
            player.deleteMeta("lastMask");
        }
        player.setMeta("lastMaskWorld", world);
        Set<FaweMask> masks = player.getMeta("lastMask");
        HashSet<RegionWrapper> regions = new HashSet<>();
        if (masks == null) {
            masks = new HashSet<>();
        } else {
            boolean removed = false;
            if (masks.isEmpty()) {
                removed = true;
            } else {
                for (FaweMask mask : masks) {
                    HashSet<RegionWrapper> curRegions = mask.getRegions();
                    for (RegionWrapper region : curRegions) {
                        if (!isIn(loc.x, loc.y, loc.z, curRegions)) {
                            removed = true;
                            break;
                        }
                    }
                    if (!mask.isValid(player, type)) {
                        removed = true;
                        break;
                    }
                    regions.addAll(curRegions);
                }
            }
            if (removed) {
                masks.clear();
                regions.clear();
            } else {
                return regions.toArray(new RegionWrapper[regions.size()]);
            }
        }
        for (final FaweMaskManager manager : managers) {
            if (player.hasPermission("fawe." + manager.getKey())) {
                try {
                    final FaweMask fm = manager.getMask(player);
                    if (fm != null) {
                        HashSet<RegionWrapper> cur = fm.getRegions();
                        regions.addAll(cur);
                        masks.add(fm);
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        }
        if (!masks.isEmpty()) {
            player.setMeta("lastMask", masks);
        } else {
            player.deleteMeta("lastMask");
        }
        return regions.toArray(new RegionWrapper[regions.size()]);
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
                                    SetQueue.IMP.addEmptyTask(new Runnable() {
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
        final boolean free = SetQueue.IMP.addEmptyTask(null);
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
            SetQueue.IMP.addEmptyTask(whenDone);
        }
        return true;
    }
}
