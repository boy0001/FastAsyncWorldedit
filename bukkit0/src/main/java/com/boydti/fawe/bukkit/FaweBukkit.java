package com.boydti.fawe.bukkit;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweAPI;
import com.boydti.fawe.IFawe;
import com.boydti.fawe.bukkit.regions.FactionsFeature;
import com.boydti.fawe.bukkit.regions.FactionsOneFeature;
import com.boydti.fawe.bukkit.regions.FactionsUUIDFeature;
import com.boydti.fawe.bukkit.regions.GriefPreventionFeature;
import com.boydti.fawe.bukkit.regions.PlotMeFeature;
import com.boydti.fawe.bukkit.regions.PreciousStonesFeature;
import com.boydti.fawe.bukkit.regions.ResidenceFeature;
import com.boydti.fawe.bukkit.regions.TownyFeature;
import com.boydti.fawe.bukkit.regions.Worldguard;
import com.boydti.fawe.bukkit.v0.BukkitQueue_All;
import com.boydti.fawe.bukkit.v0.ChunkListener;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.EditSessionWrapper;
import com.boydti.fawe.object.FaweCommand;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.regions.FaweMaskManager;
import com.boydti.fawe.util.FaweQueue;
import com.boydti.fawe.util.ReflectionUtils;
import com.boydti.fawe.util.StringMan;
import com.boydti.fawe.util.TaskManager;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.world.World;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

public class FaweBukkit implements IFawe, Listener {

    private final ABukkitMain plugin;
    private VaultUtil vault;
    private WorldEditPlugin worldedit;

    public VaultUtil getVault() {
        return this.vault;
    }

    public WorldEditPlugin getWorldEditPlugin() {
        if (this.worldedit == null) {
            this.worldedit = (WorldEditPlugin) Bukkit.getPluginManager().getPlugin("WorldEdit");
        }
        return this.worldedit;
    }

    public FaweBukkit(ABukkitMain plugin) {
        this.plugin = plugin;
        try {
            Bukkit.getPluginManager().registerEvents(this, plugin);
            Fawe.set(this);
            if (Bukkit.getVersion().contains("git-Spigot") && FaweAPI.checkVersion(this.getVersion(), 1, 7, 10)) {
                debug("====== USE PAPER SPIGOT ======");
                debug("DOWNLOAD: https://tcpr.ca/downloads/paperspigot");
                debug("GUIDE: https://www.spigotmc.org/threads/21726/");
                debug(" - This is only a recommendation");
                debug("==============================");
            }

        } catch (final Throwable e) {
            e.printStackTrace();
            Bukkit.getServer().shutdown();
        }
        TaskManager.IMP.task(new Runnable() {
            @Override
            public void run() {
                new ChunkListener();
            }
        });
    }

    @Override
    public void debug(final String s) {
        Bukkit.getLogger().info(ChatColor.translateAlternateColorCodes('&', s));
    }

    @Override
    public File getDirectory() {
        return plugin.getDataFolder();
    }

    @Override
    public void setupCommand(final String label, final FaweCommand cmd) {
        plugin.getCommand(label).setExecutor(new BukkitCommand(cmd));
    }

    @Override
    public FawePlayer<Player> wrap(final Object obj) {
        if (obj.getClass() == String.class) {
            String name = (String) obj;
            FawePlayer existing = Fawe.get().getCachedPlayer(name);
            if (existing != null) {
                return existing;
            }
            return new BukkitPlayer(Bukkit.getPlayer(name));
        } else if (obj instanceof Player) {
            Player player = (Player) obj;
            FawePlayer existing = Fawe.get().getCachedPlayer(player.getName());
            return existing != null ? existing : new BukkitPlayer(player);
        } else {
            return null;
        }
    }

    @Override
    public void startMetrics() {
        Metrics metrics = new Metrics(plugin);
        metrics.start();
        debug("&6Metrics enabled.");
    }

    /**
     * Vault isn't required, but used for setting player permissions (WorldEdit bypass)
     * @return
     */
    @Override
    public void setupVault() {
        try {
            this.vault = new VaultUtil();
        } catch (final Throwable e) {
            this.debug("&cPlease install vault!");
        }
    }

    /**
     * The task manager handles sync/async tasks
     */
    @Override
    public TaskManager getTaskManager() {
        return new BukkitTaskMan(plugin);
    }

    private int[] version;

    @Override
    public int[] getVersion() {
        if (this.version == null) {
            try {
                this.version = new int[3];
                final String[] split = Bukkit.getBukkitVersion().split("-")[0].split("\\.");
                this.version[0] = Integer.parseInt(split[0]);
                this.version[1] = Integer.parseInt(split[1]);
                if (split.length == 3) {
                    this.version[2] = Integer.parseInt(split[2]);
                }
            } catch (final NumberFormatException e) {
                e.printStackTrace();
                Fawe.debug(StringMan.getString(Bukkit.getBukkitVersion()));
                Fawe.debug(StringMan.getString(Bukkit.getBukkitVersion().split("-")[0].split("\\.")));
                return new int[] { Integer.MAX_VALUE, 0, 0 };
            }
        }
        return this.version;
    }

    private boolean hasNMS = true;

    /**
     * The FaweQueue is a core part of block placement<br>
     *  - The queue returned here is used in the SetQueue class (SetQueue handles the implementation specific queue)<br>
     *  - Block changes are grouped by chunk (as it's more efficient for lighting/packet sending)<br>
     *  - The FaweQueue returned here will provide the wrapper around the chunk object (FaweChunk)<br>
     *  - When a block change is requested, the SetQueue will first check if the chunk exists in the queue, or it will create and add it<br>
     */
    @Override
    public FaweQueue getNewQueue(String world, boolean dontCareIfFast) {
        try {
            Field fieldDirtyCount = ReflectionUtils.getRefClass("{nms}.PlayerChunk").getField("dirtyCount").getRealField();
            fieldDirtyCount.setAccessible(true);
            int mod = fieldDirtyCount.getModifiers();
            if ((mod & Modifier.VOLATILE) == 0) {
                Field modifiersField = Field.class.getDeclaredField("modifiers");
                modifiersField.setAccessible(true);
                modifiersField.setInt(fieldDirtyCount, mod + Modifier.VOLATILE);
            }
        } catch (Throwable ignore) {}
        try {
            return plugin.getQueue(world);
        } catch (Throwable ignore) {}
        if (hasNMS) {
            debug("====== NO NMS BLOCK PLACER FOUND ======");
            debug("FAWE couldn't find a fast block placer");
            debug("Bukkit version: " + Bukkit.getVersion());
            debug("Supported NMS versions: 1.8, 1.9");
            debug("Fallback placer: " + BukkitQueue_All.class);
            debug("=======================================");
            hasNMS = false;
        }
        return new BukkitQueue_All(world);
    }

    public ABukkitMain getPlugin() {
        return plugin;
    }

    @Override
    public String getWorldName(World world) {
        return world.getName();
    }

    /**
     * The EditSessionWrapper should have the same functionality as the normal EditSessionWrapper but with some optimizations
     */
    @Override
    public EditSessionWrapper getEditSessionWrapper(final EditSession session) {
        return plugin.getEditSessionWrapper(session);
    }

    /**
     * A mask manager handles region restrictions e.g. PlotSquared plots / WorldGuard regions
     */
    @Override
    public Collection<FaweMaskManager> getMaskManagers() {
        final Plugin worldguardPlugin = Bukkit.getServer().getPluginManager().getPlugin("WorldGuard");
        final ArrayList<FaweMaskManager> managers = new ArrayList<>();
        if ((worldguardPlugin != null) && worldguardPlugin.isEnabled()) {
            try {
                managers.add(new Worldguard(worldguardPlugin, this));
                Fawe.debug("Plugin 'WorldGuard' found. Using it now.");
            } catch (final Throwable e) {
                e.printStackTrace();
            }
        }
        final Plugin plotmePlugin = Bukkit.getServer().getPluginManager().getPlugin("PlotMe");
        if ((plotmePlugin != null) && plotmePlugin.isEnabled()) {
            try {
                managers.add(new PlotMeFeature(plotmePlugin, this));
                Fawe.debug("Plugin 'PlotMe' found. Using it now.");
            } catch (final Throwable e) {
                e.printStackTrace();
            }
        }
        final Plugin townyPlugin = Bukkit.getServer().getPluginManager().getPlugin("Towny");
        if ((townyPlugin != null) && townyPlugin.isEnabled()) {
            try {
                managers.add(new TownyFeature(townyPlugin, this));
                Fawe.debug("Plugin 'Towny' found. Using it now.");
            } catch (final Throwable e) {
                e.printStackTrace();
            }
        }
        final Plugin factionsPlugin = Bukkit.getServer().getPluginManager().getPlugin("Factions");
        if ((factionsPlugin != null) && factionsPlugin.isEnabled()) {
            try {
                managers.add(new FactionsFeature(factionsPlugin, this));
                Fawe.debug("Plugin 'Factions' found. Using it now.");
            } catch (final Throwable e) {
                try {
                    managers.add(new FactionsUUIDFeature(factionsPlugin, this));
                    Fawe.debug("Plugin 'FactionsUUID' found. Using it now.");
                } catch (Throwable e2) {
                    try {
                        managers.add(new FactionsOneFeature(factionsPlugin, this));
                        Fawe.debug("Plugin 'FactionsUUID' found. Using it now.");
                    } catch (Throwable e3) {
                        e.printStackTrace();
                    }

                }
            }
        }
        final Plugin residencePlugin = Bukkit.getServer().getPluginManager().getPlugin("Residence");
        if ((residencePlugin != null) && residencePlugin.isEnabled()) {
            try {
                managers.add(new ResidenceFeature(residencePlugin, this));
                Fawe.debug("Plugin 'Residence' found. Using it now.");
            } catch (final Throwable e) {
                e.printStackTrace();
            }
        }
        final Plugin griefpreventionPlugin = Bukkit.getServer().getPluginManager().getPlugin("GriefPrevention");
        if ((griefpreventionPlugin != null) && griefpreventionPlugin.isEnabled()) {
            try {
                managers.add(new GriefPreventionFeature(griefpreventionPlugin, this));
                Fawe.debug("Plugin 'GriefPrevention' found. Using it now.");
            } catch (final Throwable e) {
                e.printStackTrace();
            }
        }
        final Plugin preciousstonesPlugin = Bukkit.getServer().getPluginManager().getPlugin("PreciousStones");
        if ((preciousstonesPlugin != null) && preciousstonesPlugin.isEnabled()) {
            try {
                managers.add(new PreciousStonesFeature(preciousstonesPlugin, this));
                Fawe.debug("Plugin 'PreciousStones' found. Using it now.");
            } catch (final Throwable e) {
                e.printStackTrace();
            }
        }
        return managers;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        FawePlayer fp = FawePlayer.wrap(player);
        fp.unregister();
        Fawe.get().unregister(event.getPlayer().getName());
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        FawePlayer fp = FawePlayer.wrap(player);
        if (Settings.STORE_HISTORY_ON_DISK) {
            fp.getSession().clearHistory();
            fp.loadSessionsFromDisk(fp.getWorld());
        }
    }

    @Override
    public String getPlatform() {
        return "bukkit";
    }

    @Override
    public UUID getUUID(String name) {
        return Bukkit.getOfflinePlayer(name).getUniqueId();
    }

    @Override
    public String getName(UUID uuid) {
        return Bukkit.getOfflinePlayer(uuid).getName();
    }
}
