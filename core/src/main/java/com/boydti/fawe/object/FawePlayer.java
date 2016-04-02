package com.boydti.fawe.object;

import java.util.HashSet;
import java.util.UUID;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.util.WEManager;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.regions.RegionSelector;
import com.sk89q.worldedit.regions.selector.CuboidRegionSelector;
import java.util.concurrent.ConcurrentHashMap;

public abstract class FawePlayer<T> {

    public final T parent;
    private LocalSession session;

    /**
     * The metadata map.
     */
    private ConcurrentHashMap<String, Object> meta;

    public static <T> FawePlayer<T> wrap(final Object obj) {
        return Fawe.imp().wrap(obj);
    }

    public FawePlayer(final T parent) {
        this.parent = parent;
        Fawe.get().register(this);
    }

    public abstract String getName();

    public abstract UUID getUUID();

    public abstract boolean hasPermission(final String perm);

    public abstract void setPermission(final String perm, final boolean flag);

    public abstract void sendMessage(final String message);

    public abstract void executeCommand(final String substring);

    public abstract FaweLocation getLocation();

    public abstract Player getPlayer();

    public Region getSelection() {
        try {
            return this.getSession().getSelection(this.getPlayer().getWorld());
        } catch (final IncompleteRegionException e) {
            return null;
        }
    }

    public LocalSession getSession() {
        return (this.session != null || this.getPlayer() == null) ? this.session : Fawe.get().getWorldEdit().getSession(this.getPlayer());
    }

    public HashSet<RegionWrapper> getCurrentRegions() {
        return WEManager.IMP.getMask(this);
    }

    public void setSelection(final RegionWrapper region) {
        final Player player = this.getPlayer();
        final RegionSelector selector = new CuboidRegionSelector(player.getWorld(), region.getBottomVector(), region.getTopVector());
        this.getSession().setRegionSelector(player.getWorld(), selector);
    }

    public RegionWrapper getLargestRegion() {
        int area = 0;
        RegionWrapper max = null;
        for (final RegionWrapper region : this.getCurrentRegions()) {
            final int tmp = (region.maxX - region.minX) * (region.maxZ - region.minZ);
            if (tmp > area) {
                area = tmp;
                max = region;
            }
        }
        return max;
    }

    @Override
    public String toString() {
        return this.getName();
    }

    public boolean hasWorldEditBypass() {
        return this.hasPermission("fawe.bypass");
    }

    /**
     * Set some session only metadata for the player
     * @param key
     * @param value
     */
    public void setMeta(String key, Object value) {
        if (this.meta == null) {
            this.meta = new ConcurrentHashMap<>();
        }
        this.meta.put(key, value);
    }

    /**
     * Get the metadata for a key.
     * @param <T>
     * @param key
     * @return
     */
    public <T> T getMeta(String key) {
        if (this.meta != null) {
            return (T) this.meta.get(key);
        }
        return null;
    }

    public <T> T getMeta(String key, T def) {
        if (this.meta != null) {
            T value = (T) this.meta.get(key);
            return value == null ? def : value;
        }
        return def;
    }

    /**
     * Delete the metadata for a key.
     *  - metadata is session only
     *  - deleting other plugin's metadata may cause issues
     * @param key
     */
    public Object deleteMeta(String key) {
        return this.meta == null ? null : this.meta.remove(key);
    }

    public void unregister() {
        Fawe.get().unregister(getName());
    }
}
