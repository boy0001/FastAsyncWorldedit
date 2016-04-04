package com.boydti.fawe.object;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.changeset.DiskStorageHistory;
import com.boydti.fawe.util.WEManager;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.regions.RegionSelector;
import com.sk89q.worldedit.regions.selector.CuboidRegionSelector;
import com.sk89q.worldedit.world.World;
import java.io.File;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.UUID;
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
        if (getSession() == null || getPlayer() == null || session.getSize() != 0 || !Settings.STORE_HISTORY_ON_DISK) {
            return;
        }
        try {
            UUID uuid = getUUID();
            for (World world : WorldEdit.getInstance().getServer().getWorlds()) {
                ArrayDeque<Integer> editIds = new ArrayDeque<>();
                File folder = new File(Fawe.imp().getDirectory(), "history" + File.separator + world.getName() + File.separator + uuid);
                if (folder.isDirectory()) {
                    for (File file : folder.listFiles()) {
                        if (file.getName().endsWith(".bd")) {
                            int index = Integer.parseInt(file.getName().split("\\.")[0]);
                            editIds.add(index);
                        }
                    }
                }
                if (editIds.size() > 0) {
                    Fawe.debug("[FAWE] Indexing " + editIds.size() + " history objects for " + getName());
                    for (int index : editIds) {
                        DiskStorageHistory set = new DiskStorageHistory(world, uuid, index);
                        EditSession edit = set.toEditSession(getPlayer());
                        session.remember(edit);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Fawe.debug("Failed to load history for: " + getName());
        }
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
        return (this.session != null || this.getPlayer() == null) ? this.session : (session = Fawe.get().getWorldEdit().getSession(this.getPlayer()));
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
