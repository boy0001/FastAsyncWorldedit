package com.boydti.fawe.bukkit;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.IFawe;
import com.boydti.fawe.bukkit.chat.BukkitChatManager;
import com.boydti.fawe.bukkit.regions.FactionsFeature;
import com.boydti.fawe.bukkit.regions.FactionsOneFeature;
import com.boydti.fawe.bukkit.regions.FactionsUUIDFeature;
import com.boydti.fawe.bukkit.regions.GriefPreventionFeature;
import com.boydti.fawe.bukkit.regions.PlotMeFeature;
import com.boydti.fawe.bukkit.regions.PreciousStonesFeature;
import com.boydti.fawe.bukkit.regions.ResidenceFeature;
import com.boydti.fawe.bukkit.regions.TownyFeature;
import com.boydti.fawe.bukkit.regions.Worldguard;
import com.boydti.fawe.bukkit.v0.BukkitQueue_0;
import com.boydti.fawe.bukkit.v0.BukkitQueue_All;
import com.boydti.fawe.bukkit.v0.ChunkListener;
import com.boydti.fawe.bukkit.v1_10.BukkitQueue_1_10;
import com.boydti.fawe.bukkit.v1_11.BukkitQueue_1_11;
import com.boydti.fawe.bukkit.v1_12.BukkitQueue_1_12;
import com.boydti.fawe.bukkit.v1_12.NMSRegistryDumper;
import com.boydti.fawe.bukkit.v1_7.BukkitQueue17;
import com.boydti.fawe.bukkit.v1_8.BukkitQueue18R3;
import com.boydti.fawe.bukkit.v1_9.BukkitQueue_1_9_R1;
import com.boydti.fawe.config.BBC;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.FaweCommand;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.FaweQueue;
import com.boydti.fawe.regions.FaweMaskManager;
import com.boydti.fawe.util.MainUtil;
import com.boydti.fawe.util.ReflectionUtils;
import com.boydti.fawe.util.TaskManager;
import com.boydti.fawe.util.metrics.BStats;
import com.sk89q.bukkit.util.FallbackRegistrationListener;
import com.sk89q.worldedit.bukkit.BukkitPlayerBlockBag;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.bukkit.EditSessionBlockChangeDelegate;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.world.World;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.primesoft.blockshub.BlocksHubBukkit;

public class FaweBukkit implements IFawe, Listener {

    private final BukkitMain plugin;
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

    public FaweBukkit(BukkitMain plugin) {
        this.plugin = plugin;
        try {
            Fawe.set(this);
            setupInjector();
            try {
                com.sk89q.worldedit.bukkit.BukkitPlayer.inject(); // Fixes
                BukkitWorld.inject(); // Fixes
                BukkitPlayerBlockBag.inject(); // features
                FallbackRegistrationListener.inject(); // Fixes
            } catch (Throwable e) {
                debug("========= INJECTOR FAILED =========");
                e.printStackTrace();
                debug("===================================");
            }
            try {
                new BrushListener(plugin);
            } catch (Throwable e) {
                debug("====== BRUSH LISTENER FAILED ======");
                e.printStackTrace();
                debug("===================================");
            }
            if (Bukkit.getVersion().contains("git-Spigot")) {
                debug("====== USE PAPER ======");
                debug("DOWNLOAD: https://ci.destroystokyo.com/job/PaperSpigot/");
                debug("GUIDE: https://www.spigotmc.org/threads/21726/");
                debug(" - This is only a recommendation");
                debug("==============================");
            }
            if (Bukkit.getVersion().contains("git-Paper") && Settings.IMP.EXPERIMENTAL.DYNAMIC_CHUNK_RENDERING) {
                new RenderListener(plugin);
            }
            try {
                Fawe.get().setChatManager(new BukkitChatManager());
            } catch (Throwable ignore) {
                ignore.printStackTrace();
            }
        } catch (final Throwable e) {
            MainUtil.handleError(e);
            Bukkit.getServer().shutdown();
        }
        TaskManager.IMP.task(new Runnable() {
            @Override
            public void run() {
                Bukkit.getPluginManager().registerEvents(FaweBukkit.this, FaweBukkit.this.plugin);
                new ChunkListener();
            }
        });
    }

    @Override
    public int getPlayerCount() {
        return plugin.getServer().getOnlinePlayers().size();
    }

    @Override
    public boolean isOnlineMode() {
        return Bukkit.getOnlineMode();
    }

    @Override
    public String getPlatformVersion() {
        String bukkitVersion = Bukkit.getVersion();
        int index = bukkitVersion.indexOf("MC: ");
        return index == -1 ? bukkitVersion : bukkitVersion.substring(index + 4, bukkitVersion.length() - 1);
    }

    public void setupInjector() {
        Fawe.setupInjector();
        // Inject
        EditSessionBlockChangeDelegate.inject();
    }

    @Override
    public void debug(final String s) {
        ConsoleCommandSender console = Bukkit.getConsoleSender();
        if (console != null) {
            console.sendMessage(BBC.color(s));
        } else {
            Bukkit.getLogger().info(BBC.color(s));
        }
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
            Player player = Bukkit.getPlayer(name);
            return player != null ? new BukkitPlayer(player) : null;
        } else if (obj instanceof Player) {
            Player player = (Player) obj;
            FawePlayer existing = Fawe.get().getCachedPlayer(player.getName());
            return existing != null ? existing : new BukkitPlayer(player);
        } else if (obj != null && obj.getClass().getName().contains("EntityPlayer")) {
            try {
                Method method = obj.getClass().getDeclaredMethod("getBukkitEntity");
                return wrap(method.invoke(obj));
            } catch (Throwable e) {
                e.printStackTrace();
                return null;
            }
        } else {
            return null;
        }
    }

    @Override
    public void startMetrics() {
        Metrics metrics = new Metrics(plugin);
        metrics.start();
        TaskManager.IMP.task(new Runnable() {
            @Override
            public void run() {
                ArrayList<Class<?>> services = new ArrayList(Bukkit.getServicesManager().getKnownServices());
                services.forEach(service -> {
                    try {
                        service.getField("B_STATS_VERSION");
                        ArrayList<RegisteredServiceProvider<?>> providers = new ArrayList(Bukkit.getServicesManager().getRegistrations(service));
                        for (RegisteredServiceProvider<?> provider : providers) {
                            Object instance = provider.getProvider();

                            // Link it to FAWE's metrics instead
                            BStats.linkMetrics(instance);

                            // Disable the other metrics
                            Bukkit.getServicesManager().unregister(service, instance);
                            try {
                                Class<? extends Object> clazz = instance.getClass();
                                Field logFailedRequests = ReflectionUtils.findField(clazz, boolean.class);
                                logFailedRequests.set(null, false);
                                Field url = null;
                                try { url = clazz.getDeclaredField("URL"); } catch (NoSuchFieldException ignore) {
                                for (Field field : clazz.getDeclaredFields()) if (ReflectionUtils.setAccessible(field).get(null).toString().startsWith("http")) { url = field; break; }
                                }
                                if (url != null) ReflectionUtils.setFailsafeFieldValue(url, null, null);
                            } catch (NoSuchFieldError | IllegalAccessException ignore) {}
                            catch (Throwable e) {
                                Fawe.debug("Error linking metrics: " + instance);
                                e.printStackTrace();
                            }
                        }
                    } catch (NoSuchFieldException ignored) { }
                });
            }
        });
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
            this.debug("&dVault is used for persistent `/wea` toggles.");
        }
    }

    @Override
    public String getDebugInfo() {
        StringBuilder msg = new StringBuilder();
        List<String> pl = new ArrayList<>();
        msg.append("server.plugins: \n");
        for (Plugin p : Bukkit.getPluginManager().getPlugins()) {
            msg.append(" - " + p.getName() + ": " + p.getDescription().getVersion() + "\n");
        }
        msg.append("server.version: " + Bukkit.getVersion() + " / " + Bukkit.getBukkitVersion() + "\n");
        return msg.toString();
    }

    /**
     * The task manager handles sync/async tasks
     */
    @Override
    public TaskManager getTaskManager() {
        return new BukkitTaskMan(plugin);
    }

    private boolean hasNMS = true;
    private boolean playerChunk = false;

    @Override
    public FaweQueue getNewQueue(String world, boolean fast) {
        if (playerChunk != (playerChunk = true)) {
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
        }
        try {
            return getQueue(world);
        } catch (Throwable ignore) {
            // Disable incompatible settings
            Settings.IMP.QUEUE.PARALLEL_THREADS = 1; // BukkitAPI placer is too slow to parallel thread at the chunk level
            Settings.IMP.HISTORY.COMBINE_STAGES = false; // Performing a chunk copy (if possible) wouldn't be faster using the BukkitAPI
            if (hasNMS) {

                debug("====== NO NMS BLOCK PLACER FOUND ======");
                debug("FAWE couldn't find a fast block placer");
                debug("Bukkit version: " + Bukkit.getVersion());
                debug("NMS label: " + plugin.getClass().getSimpleName().split("_")[1]);
                debug("Fallback placer: " + BukkitQueue_All.class);
                debug("=======================================");
                debug("Download the version of FAWE for your platform");
                debug(" - http://ci.athion.net/job/FastAsyncWorldEdit/lastSuccessfulBuild/artifact/target");
                debug("=======================================");
                ignore.printStackTrace();
                debug("=======================================");
                TaskManager.IMP.laterAsync(new Runnable() {
                    @Override
                    public void run() {
                        MainUtil.sendAdmin("&cNo NMS placer found, see console!");
                    }
                }, 1);
                hasNMS = false;
            }
            return new BukkitQueue_All(world);
        }
    }

    /**
     * The FaweQueue is a core part of block placement<br>
     *  - The queue returned here is used in the SetQueue class (SetQueue handles the implementation specific queue)<br>
     *  - Block changes are grouped by chunk (as it's more efficient for lighting/packet sending)<br>
     *  - The FaweQueue returned here will provide the wrapper around the chunk object (FaweChunk)<br>
     *  - When a block change is requested, the SetQueue will first check if the chunk exists in the queue, or it will create and add it<br>
     */
    @Override
    public FaweQueue getNewQueue(World world, boolean fast) {
        if (fast) {
            if (playerChunk != (playerChunk = true)) {
                try {
                    Field fieldDirtyCount = ReflectionUtils.getRefClass("{nms}.PlayerChunk").getField("dirtyCount").getRealField();
                    fieldDirtyCount.setAccessible(true);
                    int mod = fieldDirtyCount.getModifiers();
                    if ((mod & Modifier.VOLATILE) == 0) {
                        Field modifiersField = Field.class.getDeclaredField("modifiers");
                        modifiersField.setAccessible(true);
                        modifiersField.setInt(fieldDirtyCount, mod + Modifier.VOLATILE);
                    }
                } catch (Throwable ignore) {
                }
            }
            Throwable error = null;
            try {
                return getQueue(world);
            } catch (Throwable ignore) {
                error = ignore;
            }
            // Disable incompatible settings
            Settings.IMP.QUEUE.PARALLEL_THREADS = 1; // BukkitAPI placer is too slow to parallel thread at the chunk level
            Settings.IMP.HISTORY.COMBINE_STAGES = false; // Performing a chunk copy (if possible) wouldn't be faster using the BukkitAPI
            if (hasNMS) {
                debug("====== NO NMS BLOCK PLACER FOUND ======");
                debug("FAWE couldn't find a fast block placer");
                debug("Bukkit version: " + Bukkit.getVersion());
                debug("NMS label: " + plugin.getClass().getSimpleName());
                debug("Fallback placer: " + BukkitQueue_All.class);
                debug("=======================================");
                debug("Download the version of FAWE for your platform");
                debug(" - http://ci.athion.net/job/FastAsyncWorldEdit/lastSuccessfulBuild/artifact/target");
                debug("=======================================");
                error.printStackTrace();
                debug("=======================================");
                TaskManager.IMP.laterAsync(new Runnable() {
                    @Override
                    public void run() {
                        MainUtil.sendAdmin("&cNo NMS placer found, see console!");
                    }
                }, 1);
                hasNMS = false;
            }
        }
        return new BukkitQueue_All(world);
    }

    public BukkitMain getPlugin() {
        return plugin;
    }

    @Override
    public String getWorldName(World world) {
        return world.getName();
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
                MainUtil.handleError(e);
            }
        }
        final Plugin plotmePlugin = Bukkit.getServer().getPluginManager().getPlugin("PlotMe");
        if ((plotmePlugin != null) && plotmePlugin.isEnabled()) {
            try {
                managers.add(new PlotMeFeature(plotmePlugin, this));
                Fawe.debug("Plugin 'PlotMe' found. Using it now.");
            } catch (final Throwable e) {
                MainUtil.handleError(e);
            }
        }
        final Plugin townyPlugin = Bukkit.getServer().getPluginManager().getPlugin("Towny");
        if ((townyPlugin != null) && townyPlugin.isEnabled()) {
            try {
                managers.add(new TownyFeature(townyPlugin, this));
                Fawe.debug("Plugin 'Towny' found. Using it now.");
            } catch (final Throwable e) {
                MainUtil.handleError(e);
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
                        MainUtil.handleError(e);
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
                MainUtil.handleError(e);
            }
        }
        final Plugin griefpreventionPlugin = Bukkit.getServer().getPluginManager().getPlugin("GriefPrevention");
        if ((griefpreventionPlugin != null) && griefpreventionPlugin.isEnabled()) {
            try {
                managers.add(new GriefPreventionFeature(griefpreventionPlugin, this));
                Fawe.debug("Plugin 'GriefPrevention' found. Using it now.");
            } catch (final Throwable e) {
                MainUtil.handleError(e);
            }
        }
        final Plugin preciousstonesPlugin = Bukkit.getServer().getPluginManager().getPlugin("PreciousStones");
        if ((preciousstonesPlugin != null) && preciousstonesPlugin.isEnabled()) {
            try {
                managers.add(new PreciousStonesFeature(preciousstonesPlugin, this));
                Fawe.debug("Plugin 'PreciousStones' found. Using it now.");
            } catch (final Throwable e) {
                MainUtil.handleError(e);
            }
        }
        return managers;
    }
//
//    @EventHandler
//    public void onWorldLoad(WorldLoadEvent event) {
//        org.bukkit.World world = event.getWorld();
//        world.setKeepSpawnInMemory(false);
//        WorldServer nmsWorld = ((CraftWorld) world).getHandle();
//        ChunkProviderServer provider = nmsWorld.getChunkProviderServer();
//        try {
//            Field fieldChunkLoader = provider.getClass().getDeclaredField("chunkLoader");
//            ReflectionUtils.setFailsafeFieldValue(fieldChunkLoader, provider, new FaweChunkLoader());
//        } catch (Throwable e) {
//            e.printStackTrace();
//        }
//    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        FawePlayer fp = FawePlayer.wrap(player);
        if (fp != null) {
            fp.unregister();
        }
        Fawe.get().unregister(event.getPlayer().getName());
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

    private boolean enabledBlocksHub = true;

    @Override
    public Object getBlocksHubApi() {
        if (!enabledBlocksHub) {
            return null;
        }
        Plugin blocksHubPlugin = Bukkit.getPluginManager().getPlugin("BlocksHub");
        if (blocksHubPlugin == null) {
            enabledBlocksHub = false;
            return null;
        }
        return ((BlocksHubBukkit) blocksHubPlugin).getApi();
    }

    private Version version = null;

    public Version getVersion() {
        Version tmp = this.version;
        if (tmp == null) {
            tmp = Version.NONE;
            for (Version v : Version.values()) {
                try {
                    BukkitQueue_0.checkVersion(v.name());
                    this.version = tmp = v;
                    if (tmp == Version.v1_12_R1) {
                        try {
                            Fawe.debug("Running 1.12 registry dumper!");
                            NMSRegistryDumper dumper = new NMSRegistryDumper(MainUtil.getFile(plugin.getDataFolder(), "extrablocks.json"));
                            dumper.run();
                        } catch (Throwable e) {
                            e.printStackTrace();
                        }
                    }
                    break;
                } catch (IllegalStateException e) {}
            }
        }
        return tmp;
    }

    public enum Version {
        v1_7_R4,
        v1_8_R3,
        v1_9_R2,
        v1_10_R1,
        v1_11_R1,
        v1_12_R1,
        v1_12_R2,
        v1_13_R1,
        NONE,
    }

    private FaweQueue getQueue(World world) {
        switch (getVersion()) {
            case v1_7_R4:
                return new BukkitQueue17(world);
            case v1_8_R3:
                return new BukkitQueue18R3(world);
            case v1_9_R2:
                return new BukkitQueue_1_9_R1(world);
            case v1_10_R1:
                return new BukkitQueue_1_10(world);
            case v1_11_R1:
                return new BukkitQueue_1_11(world);
            case v1_12_R1:
                return new BukkitQueue_1_12(world);
            default:
            case NONE:
                return new BukkitQueue_All(world);
        }
    }

    private FaweQueue getQueue(String world) {
        switch (getVersion()) {
            case v1_7_R4:
                return new BukkitQueue17(world);
            case v1_8_R3:
                return new BukkitQueue18R3(world);
            case v1_9_R2:
                return new BukkitQueue_1_9_R1(world);
            case v1_10_R1:
                return new BukkitQueue_1_10(world);
            case v1_11_R1:
                return new BukkitQueue_1_11(world);
            case v1_12_R1:
                return new BukkitQueue_1_12(world);
            default:
            case NONE:
                return new BukkitQueue_All(world);
        }
    }
}
