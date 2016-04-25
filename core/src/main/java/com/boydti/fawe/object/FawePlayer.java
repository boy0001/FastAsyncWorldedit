package com.boydti.fawe.object;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweAPI;
import com.boydti.fawe.config.BBC;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.changeset.DiskStorageHistory;
import com.boydti.fawe.object.clipboard.DiskOptimizedClipboard;
import com.boydti.fawe.util.TaskManager;
import com.boydti.fawe.util.WEManager;
import com.boydti.fawe.wrappers.PlayerWrapper;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.EmptyClipboardException;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.regions.RegionSelector;
import com.sk89q.worldedit.regions.selector.CuboidRegionSelector;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.registry.WorldData;
import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public abstract class FawePlayer<T> {

    public final T parent;
    private LocalSession session;

    /**
     * The metadata map.
     */
    private volatile ConcurrentHashMap<String, Object> meta;

    /**
     * Wrap some object into a FawePlayer<br>
     *     - org.bukkit.entity.Player
     *     - org.spongepowered.api.entity.living.player
     *     - com.sk89q.worldedit.entity.Player
     *     - String (name)
     *     - UUID (player UUID)
     * @param obj
     * @param <V>
     * @return
     */
    public static <V> FawePlayer<V> wrap(final Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof Player) {
            Player actor = (Player) obj;
            if (obj.getClass().getSimpleName().equals("PlayerProxy")) {
                try {
                    Field fieldBasePlayer = actor.getClass().getDeclaredField("basePlayer");
                    fieldBasePlayer.setAccessible(true);
                    Player player = (Player) fieldBasePlayer.get(actor);
                    return wrap(player);
                } catch (Throwable e) {
                    e.printStackTrace();
                    return Fawe.imp().wrap(actor.getName());
                }
            } else if (obj instanceof PlayerWrapper){
                return wrap(((PlayerWrapper) obj).getParent());
            } else {
                try {
                    Field fieldPlayer = actor.getClass().getDeclaredField("player");
                    fieldPlayer.setAccessible(true);
                    return wrap(fieldPlayer.get(actor));
                } catch (Throwable e) {
                    e.printStackTrace();
                    return Fawe.imp().wrap(actor.getName());
                }
            }
        }
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
            String currentWorldName = getLocation().world;
            World world = getWorld();
            if (world != null) {
                if (world.getName().equals(currentWorldName)) {
                    getSession().clearHistory();
                    loadSessionsFromDisk(world);
                }
            }
            loadClipboardFromDisk();
        } catch (Exception e) {
            e.printStackTrace();
            Fawe.debug("Failed to load history for: " + getName());
        }
    }

    public void loadClipboardFromDisk() {
        try {
            File file = new File(Fawe.imp().getDirectory(), "clipboard" + File.separator + getUUID());
            if (file.exists()) {
                DiskOptimizedClipboard doc = new DiskOptimizedClipboard(file);
                Player player = getPlayer();
                LocalSession session = getSession();
                try {
                    if (session.getClipboard() != null) {
                        return;
                    }
                } catch (EmptyClipboardException e) {}
                if (player != null && session != null) {
                    sendMessage("&d" + BBC.PREFIX.s() + " " + BBC.LOADING_CLIPBOARD.s());
                    WorldData worldData = player.getWorld().getWorldData();
                    Clipboard clip = doc.toClipboard();
                    ClipboardHolder holder = new ClipboardHolder(clip, worldData);
                    getSession().setClipboard(holder);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Get the current World
     * @return
     */
    public World getWorld() {
        return FaweAPI.getWorld(getLocation().world);
    }

    /**
     * Load all the undo EditSession's from disk for a world <br>
     *     - Usually already called when a player joins or changes world
     * @param world
     */
    public void loadSessionsFromDisk(final World world) {
        if (world == null) {
            return;
        }
        final long start = System.currentTimeMillis();
        final UUID uuid = getUUID();
        final List<Integer> editIds = new ArrayList<>();
        final File folder = new File(Fawe.imp().getDirectory(), "history" + File.separator + world.getName() + File.separator + uuid);
        if (folder.isDirectory()) {
            for (File file : folder.listFiles()) {
                if (file.getName().endsWith(".bd")) {
                    int index = Integer.parseInt(file.getName().split("\\.")[0]);
                    editIds.add(index);
                }
            }
        }
        if (editIds.size() > 0) {
            sendMessage("&d" + BBC.PREFIX.s() + " " + BBC.INDEXING_HISTORY.format(editIds.size()));
            TaskManager.IMP.async(new Runnable() {
                @Override
                public void run() {
                    Collections.sort(editIds);
                    for (int i = editIds.size() - 1; i >= 0; i--) {
                        int index = editIds.get(i);
                        DiskStorageHistory set = new DiskStorageHistory(world, uuid, index);
                        EditSession edit = set.toEditSession(getPlayer());
                        if (world.equals(getWorld())) {
                            session.remember(edit, 0);
                        } else {
                            return;
                        }
                    }
                    sendMessage("&d" + BBC.PREFIX.s() + " " + BBC.INDEXING_COMPLETE.format((System.currentTimeMillis() - start) / 1000d));
                }
            });
        }
    }

    /**
     * Get the player's limit
     * @return
     */
    public FaweLimit getLimit() {
        return Settings.getLimit(this);
    }

    /**
     * Get the player's name
     * @return
     */
    public abstract String getName();

    /**
     * Get the player's UUID
     * @return
     */
    public abstract UUID getUUID();

    /**
     * Check the player's permission
     * @param perm
     * @return
     */
    public abstract boolean hasPermission(final String perm);

    /**
     * Set a permission (requires Vault)
     * @param perm
     * @param flag
     */
    public abstract void setPermission(final String perm, final boolean flag);

    /**
     * Send a message to the player
     * @param message
     */
    public abstract void sendMessage(final String message);

    /**
     * Have the player execute a command
     * @param substring
     */
    public abstract void executeCommand(final String substring);

    /**
     * Get the player's location
     * @return
     */
    public abstract FaweLocation getLocation();

    /**
     * Get the WorldEdit player object
     * @return
     */
    public abstract Player getPlayer();

    /**
     * Get the player's current selection (or null)
     * @return
     */
    public Region getSelection() {
        try {
            return this.getSession().getSelection(this.getPlayer().getWorld());
        } catch (final IncompleteRegionException e) {
            return null;
        }
    }

    /**
     * Get the player's current LocalSession
     * @return
     */
    public LocalSession getSession() {
        return (this.session != null || this.getPlayer() == null) ? this.session : (session = Fawe.get().getWorldEdit().getSession(this.getPlayer()));
    }

    /**
     * Get the player's current allowed WorldEdit regions
     * @return
     */
    public HashSet<RegionWrapper> getCurrentRegions() {
        return WEManager.IMP.getMask(this);
    }

    /**
     * Set the player's WorldEdit selection to the following CuboidRegion
     * @param region
     */
    public void setSelection(final RegionWrapper region) {
        final Player player = this.getPlayer();
        final RegionSelector selector = new CuboidRegionSelector(player.getWorld(), region.getBottomVector(), region.getTopVector());
        this.getSession().setRegionSelector(player.getWorld(), selector);
    }

    /**
     * Get the largest region in the player's allowed WorldEdit region
     * @return
     */
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

    /**
     * Check if the player has WorldEdit bypass enabled
     * @return
     */
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
     * @param <V>
     * @param key
     * @return
     */
    public <V> V getMeta(String key) {
        if (this.meta != null) {
            return (V) this.meta.get(key);
        }
        return null;
    }

    /**
     * Get the metadata for a specific key (or return the default provided)
     * @param key
     * @param def
     * @param <V>
     * @return
     */
    public <V> V getMeta(String key, V def) {
        if (this.meta != null) {
            V value = (V) this.meta.get(key);
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

    /**
     * Unregister this player (delets all metadata etc)
     *  - Usually called on logout
     */
    public void unregister() {
        getSession().setClipboard(null);
        getSession().clearHistory();
        WorldEdit.getInstance().removeSession(getPlayer());
        Fawe.get().unregister(getName());
    }
}
