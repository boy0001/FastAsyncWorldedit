package com.boydti.fawe;

import com.boydti.fawe.command.Cancel;
import com.boydti.fawe.command.Reload;
import com.boydti.fawe.command.Wea;
import com.boydti.fawe.command.WorldEditRegion;
import com.boydti.fawe.config.BBC;
import com.boydti.fawe.config.Commands;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.regions.general.plot.PlotSquaredFeature;
import com.boydti.fawe.util.FaweTimer;
import com.boydti.fawe.util.MainUtil;
import com.boydti.fawe.util.MemUtil;
import com.boydti.fawe.util.StringMan;
import com.boydti.fawe.util.TaskManager;
import com.boydti.fawe.util.Updater;
import com.boydti.fawe.util.WEManager;
import com.boydti.fawe.util.WESubscriber;
import com.sk89q.jnbt.NBTInputStream;
import com.sk89q.jnbt.NBTOutputStream;
import com.sk89q.worldedit.CuboidClipboard;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.blocks.BlockData;
import com.sk89q.worldedit.command.BiomeCommands;
import com.sk89q.worldedit.command.BrushCommands;
import com.sk89q.worldedit.command.ChunkCommands;
import com.sk89q.worldedit.command.ClipboardCommands;
import com.sk89q.worldedit.command.FlattenedClipboardTransform;
import com.sk89q.worldedit.command.GeneralCommands;
import com.sk89q.worldedit.command.GenerationCommands;
import com.sk89q.worldedit.command.HistoryCommands;
import com.sk89q.worldedit.command.NavigationCommands;
import com.sk89q.worldedit.command.RegionCommands;
import com.sk89q.worldedit.command.SchematicCommands;
import com.sk89q.worldedit.command.ScriptingCommands;
import com.sk89q.worldedit.command.SnapshotCommands;
import com.sk89q.worldedit.command.SnapshotUtilCommands;
import com.sk89q.worldedit.command.SuperPickaxeCommands;
import com.sk89q.worldedit.command.ToolCommands;
import com.sk89q.worldedit.command.ToolUtilCommands;
import com.sk89q.worldedit.command.UtilityCommands;
import com.sk89q.worldedit.command.WorldEditCommands;
import com.sk89q.worldedit.command.composition.SelectionCommand;
import com.sk89q.worldedit.command.tool.AreaPickaxe;
import com.sk89q.worldedit.command.tool.BrushTool;
import com.sk89q.worldedit.command.tool.LongRangeBuildTool;
import com.sk89q.worldedit.command.tool.RecursivePickaxe;
import com.sk89q.worldedit.command.tool.brush.GravityBrush;
import com.sk89q.worldedit.event.extent.EditSessionEvent;
import com.sk89q.worldedit.extension.factory.DefaultMaskParser;
import com.sk89q.worldedit.extension.factory.HashTagPatternParser;
import com.sk89q.worldedit.extension.platform.CommandManager;
import com.sk89q.worldedit.extension.platform.PlatformManager;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.SchematicReader;
import com.sk89q.worldedit.extent.clipboard.io.SchematicWriter;
import com.sk89q.worldedit.extent.inventory.BlockBagExtent;
import com.sk89q.worldedit.extent.transform.BlockTransformExtent;
import com.sk89q.worldedit.function.entity.ExtentEntityCopy;
import com.sk89q.worldedit.function.mask.BlockMask;
import com.sk89q.worldedit.function.mask.FuzzyBlockMask;
import com.sk89q.worldedit.function.mask.MaskUnion;
import com.sk89q.worldedit.function.mask.Masks;
import com.sk89q.worldedit.function.mask.OffsetMask;
import com.sk89q.worldedit.function.mask.SolidBlockMask;
import com.sk89q.worldedit.function.operation.ChangeSetExecutor;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.pattern.ClipboardPattern;
import com.sk89q.worldedit.function.pattern.Patterns;
import com.sk89q.worldedit.function.pattern.RandomPattern;
import com.sk89q.worldedit.function.visitor.BreadthFirstSearch;
import com.sk89q.worldedit.function.visitor.DownwardVisitor;
import com.sk89q.worldedit.function.visitor.EntityVisitor;
import com.sk89q.worldedit.function.visitor.FlatRegionVisitor;
import com.sk89q.worldedit.function.visitor.LayerVisitor;
import com.sk89q.worldedit.function.visitor.NonRisingVisitor;
import com.sk89q.worldedit.function.visitor.RecursiveVisitor;
import com.sk89q.worldedit.function.visitor.RegionVisitor;
import com.sk89q.worldedit.history.change.EntityCreate;
import com.sk89q.worldedit.history.change.EntityRemove;
import com.sk89q.worldedit.math.interpolation.KochanekBartelsInterpolation;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.selector.CuboidRegionSelector;
import com.sk89q.worldedit.session.PasteBuilder;
import com.sk89q.worldedit.session.SessionManager;
import com.sk89q.worldedit.session.request.Request;
import com.sk89q.worldedit.util.command.SimpleDispatcher;
import com.sk89q.worldedit.util.command.parametric.ParameterData;
import com.sk89q.worldedit.util.command.parametric.ParametricBuilder;
import com.sk89q.worldedit.util.command.parametric.ParametricCallable;
import com.sk89q.worldedit.world.registry.BundledBlockData;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import javax.management.InstanceAlreadyExistsException;
import javax.management.Notification;
import javax.management.NotificationEmitter;
import javax.management.NotificationListener;

/**[ WorldEdit action]
*       |
*      \|/
* [ EditSession ] - The change is processed (area restrictions, change limit, block type) 
*       |
*      \|/
* [Block change] - A block change from some location
*       |
*      \|/
* [ Set Queue ] - The SetQueue manages the implementation specific queue
*       |
*      \|/
* [ Fawe Queue] - A queue of chunks - check if the queue has the chunk for a change 
*       |
*      \|/   
* [ Fawe Chunk Implementation ] - Otherwise create a new FaweChunk object which is a wrapper around the Chunk object
*       |
*      \|/
* [ Execution ] - When done, the queue then sets the blocks for the chunk, performs lighting updates and sends the chunk packet to the clients
* 
*  Why it's faster:
*   - The chunk is modified directly rather than through the API
*      \ Removes some overhead, and means some processing can be done async 
*   - Lighting updates are performed on the chunk level rather than for every block
*      \ e.g. A blob of stone: only the visible blocks need to have the lighting calculated
*   - Block changes are sent with a chunk packet
*      \ A chunk packet is generally quicker to create and smaller for large world edits
*   - No physics updates
*      \ Physics updates are slow, and are usually performed on each block
*   - Block data shortcuts
*      \ Some known blocks don't need to have the data set or accessed (e.g. air is never going to have data)
*   - Remove redundant extents
*      \ Up to 11 layers of extents can be removed
*   - History bypassing
*      \ FastMode bypasses history and means blocks in the world don't need to be checked and recorded
*/
public class Fawe { 
    /**
     * The FAWE instance;
     */
    private static Fawe INSTANCE;

    /**
     * TPS timer
     */
    private final FaweTimer timer;
    private FaweVersion version;

    /**
     * Get the implementation specific class
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T extends IFawe> T imp() {
        return INSTANCE != null ? (T) INSTANCE.IMP : null;
    }

    /**
     * Get the implementation independent class
     * @return
     */
    public static Fawe get() {
        return INSTANCE;
    }

    /**
     * Setup Fawe
     * @param implementation
     * @throws InstanceAlreadyExistsException
     */
    public static void set(final IFawe implementation) throws InstanceAlreadyExistsException, IllegalArgumentException {
        if (INSTANCE != null) {
            throw new InstanceAlreadyExistsException("FAWE has already been initialized with: " + INSTANCE.IMP);
        }
        if (implementation == null) {
            throw new IllegalArgumentException("Implementation may not be null.");
        }
        INSTANCE = new Fawe(implementation);
    }

    public static void debugPlain(String s) {
        if (INSTANCE != null) {
            INSTANCE.IMP.debug(StringMan.getString(s));
        } else {
            System.out.println(s);
        }
    }

    /**
     * Write something to the console
     * @param s
     */
    public static void debug(Object s) {
        debugPlain(BBC.PREFIX.original() + " " + s);
    }

    /**
     * The platform specific implementation
     */
    private final IFawe IMP;
    private Thread thread = Thread.currentThread();

    private Fawe(final IFawe implementation) {
        this.INSTANCE = this;
        this.IMP = implementation;
        this.thread = Thread.currentThread();
        /*
         * Implementation dependent stuff
         */
        this.setupConfigs();
        MainUtil.deleteOlder(MainUtil.getFile(IMP.getDirectory(), Settings.PATHS.HISTORY), TimeUnit.DAYS.toMillis(Settings.HISTORY.DELETE_AFTER_DAYS));
        MainUtil.deleteOlder(MainUtil.getFile(IMP.getDirectory(), Settings.PATHS.CLIPBOARD), TimeUnit.DAYS.toMillis(Settings.CLIPBOARD.DELETE_AFTER_DAYS));

        TaskManager.IMP = this.IMP.getTaskManager();
        TaskManager.IMP.repeat(timer = new FaweTimer(), 1);
        if (Settings.METRICS) {
            this.IMP.startMetrics();
        }
        this.setupCommands();
        /*
         * Instance independent stuff
         */
        this.setupMemoryListener();

        // Delayed event setup
        TaskManager.IMP.later(new Runnable() {
            @Override
            public void run() {
                // Events
                Fawe.this.IMP.setupVault();
            }
        }, 0);

        if (Settings.UPDATE) {
            // Delayed updating
            TaskManager.IMP.async(new Runnable() {
                @Override
                public void run() {
                    Updater.update(implementation.getPlatform(), getVersion());
                }
            });
        }
    }

    private boolean isJava8 = MainUtil.getJavaVersion() >= 1.8;

    public boolean isJava8() {
        return isJava8;
    }

    /**
     * The FaweTimer is a useful class for monitoring TPS
     * @return FaweTimer
     */
    public FaweTimer getTimer() {
        return timer;
    }

    /**
     * The FAWE version
     *  - Unofficial jars may be lacking version information
     * @return FaweVersion
     */
    public @Nullable FaweVersion getVersion() {
        return version;
    }

    public double getTPS() {
        return timer.getTPS();
    }

    private void setupEvents() {
        WorldEdit.getInstance().getEventBus().register(new WESubscriber());
    }

    private void setupCommands() {
        this.IMP.setupCommand("wea", new Wea());
        this.IMP.setupCommand("select", new WorldEditRegion());
        this.IMP.setupCommand("fawe", new Reload());
        this.IMP.setupCommand("fcancel", new Cancel());
    }

    public void setupConfigs() {
        // Setting up config.yml
        File file = new File(this.IMP.getDirectory(), "config.yml");
        Settings.PLATFORM = IMP.getPlatform().replace("\"", "");
        try {
            InputStream stream = getClass().getResourceAsStream("/fawe.properties");
            java.util.Scanner scanner = new java.util.Scanner(stream).useDelimiter("\\A");
            String versionString = scanner.next().trim();
            scanner.close();
            this.version = new FaweVersion(versionString);
            Settings.DATE = new Date(100 + version.year, version.month, version.day).toGMTString();
            Settings.BUILD = "https://ci.athion.net/job/FastAsyncWorldEdit/" + version.build;
            Settings.COMMIT = "https://github.com/boy0001/FastAsyncWorldedit/commit/" + Integer.toHexString(version.hash);
        } catch (Throwable ignore) {}
        Settings.load(file);
        Settings.save(file);
        // Setting up message.yml
        BBC.load(new File(this.IMP.getDirectory(), "message.yml"));
    }

    private WorldEdit worldedit;

    public WorldEdit getWorldEdit() {
        if (this.worldedit == null) {
            return worldedit = WorldEdit.getInstance();
        }
        return this.worldedit;
    }

    public void setupInjector() {
        /*
         * Modify the sessions
         *  - EditSession supports custom queue and a lot of optimizations
         *  - LocalSession supports VirtualPlayers and undo on disk
         */
        try {
            // Delayed worldedit setup
            TaskManager.IMP.later(new Runnable() {
                @Override
                public void run() {
                    try {
                        WEManager.IMP.managers.addAll(Fawe.this.IMP.getMaskManagers());
                        WEManager.IMP.managers.add(new PlotSquaredFeature());
                        Fawe.debug("Plugin 'PlotSquared' found. Using it now.");
                    } catch (Throwable e) {}
                    Fawe.this.worldedit = WorldEdit.getInstance();
                    Fawe.this.setupEvents();
                }
            }, 0);
            // Setting up commands.yml
            Commands.load(new File(this.IMP.getDirectory(), "commands.yml"));
            EditSession.inject(); // Custom block placer + optimizations
            EditSessionEvent.inject(); // Add EditSession to event (API)
            LocalSession.inject(); // Add remember order / queue flushing / Optimizations for disk
            SessionManager.inject(); // Faster custom session saving + Memory improvements
            Request.inject(); // Custom pattern extent
            // Commands
            BiomeCommands.inject(); // Translations + Optimizations
            ChunkCommands.inject(); // Translations + Optimizations
            GenerationCommands.inject(); // Translations + Optimizations
            SnapshotCommands.inject(); // Translations + Optimizations
            SnapshotUtilCommands.inject(); // Translations + Optimizations
            SuperPickaxeCommands.inject(); // Translations + Optimizations
            UtilityCommands.inject(); // Translations + Optimizations
            WorldEditCommands.inject(); // Translations + Optimizations
            BrushCommands.inject(); // Translations + heightmap
            ToolCommands.inject(); // Translations + inspect
            ClipboardCommands.inject(); // Translations + lazycopy + paste optimizations
            SchematicCommands.inject(); // Translations
            ScriptingCommands.inject(); // Translations
            SelectionCommand.inject(); // Translations + set optimizations
            RegionCommands.inject(); // Translations
            HistoryCommands.inject(); // Translations + rollback command
            NavigationCommands.inject(); // Translations + thru fix
            ParametricBuilder.inject(); // Translations
            ParametricCallable.inject(); // Translations
            ParameterData.inject(); // Translations
            ToolUtilCommands.inject(); // Fixes + Translations
            GeneralCommands.inject(); // Translations + gmask args
            // Schematic
            SchematicReader.inject(); // Optimizations
            SchematicWriter.inject(); // Optimizations
            ClipboardFormat.inject(); // Optimizations + new formats + api
            PasteBuilder.inject(); // Optimizations
            // Brushes/Tools
            GravityBrush.inject(); // Fix for instant placement assumption
            LongRangeBuildTool.inject();
            AreaPickaxe.inject(); // Fixes
            RecursivePickaxe.inject(); // Fixes
            BrushTool.inject(); // Add transform
            // Selectors
            CuboidRegionSelector.inject(); // Translations
            // Visitors
            BreadthFirstSearch.inject(); // Translations + Optimizations
            DownwardVisitor.inject(); // Optimizations
            EntityVisitor.inject(); // Translations + Optimizations
            FlatRegionVisitor.inject(); // Translations + Optimizations
            LayerVisitor.inject(); // Optimizations
            NonRisingVisitor.inject(); // Optimizations
            RecursiveVisitor.inject(); // Optimizations
            RegionVisitor.inject(); // Translations + Optimizations
            ExtentEntityCopy.inject(); // Async entity create fix
            // Transforms
            FlattenedClipboardTransform.inject(); // public access
            // Entity create/remove
            EntityCreate.inject(); // Optimizations
            EntityRemove.inject(); // Optimizations
            // Clipboards
            BlockArrayClipboard.inject(); // Optimizations + disk
            CuboidClipboard.inject(); // Optimizations
            // Regions
            CuboidRegion.inject(); // Optimizations
            // Extents
            BlockTransformExtent.inject(); // Fix for cache not being mutable
            AbstractDelegateExtent.inject(); // Optimizations
            BlockBagExtent.inject(); // Fixes + Optimizations
            // Vector
            Vector.inject(); // Optimizations
            // Pattern
            Patterns.inject(); // Optimizations (reduce object creation)
            RandomPattern.inject(); // Optimizations
            ClipboardPattern.inject(); // Optimizations
            HashTagPatternParser.inject(); // Add new patterns
            // Mask
            BlockMask.inject(); // Optimizations
            SolidBlockMask.inject(); // Optimizations
            FuzzyBlockMask.inject(); // Optimizations
            OffsetMask.inject(); // Optimizations
            DefaultMaskParser.inject(); // Add new masks
            Masks.inject(); // Optimizations
            MaskUnion.inject(); // Optimizations
            // Operations
            Operations.inject(); // Optimizations
            ForwardExtentCopy.inject(); // Fixes + optimizations
            ChangeSetExecutor.inject(); // Optimizations
            // BlockData
            BlockData.inject(); // Temporary fix for 1.9.4
            BundledBlockData.inject(); // Add custom rotation
            try {
                BundledBlockData.getInstance().loadFromResource();
            } catch (IOException e) {
                MainUtil.handleError(e);
            }
            File jar = MainUtil.getJarFile();
            File extraBlocks = MainUtil.copyFile(jar, "extrablocks.json", null);
            if (extraBlocks != null && extraBlocks.exists()) {
                try {
                    BundledBlockData.getInstance().add(extraBlocks.toURI().toURL(), true);
                } catch (Throwable ignore) {
                    Fawe.debug("Invalid format: extrablocks.json");
                }
            }
            // NBT
            NBTInputStream.inject(); // Add actual streaming + Optimizations + New methods
            NBTOutputStream.inject(); // New methods
            // Math
            KochanekBartelsInterpolation.inject(); // Optimizations
            AffineTransform.inject(); // Optimizations
            try {
                CommandManager.inject(); // Async commands
                PlatformManager.inject(); // Async brushes / tools
                SimpleDispatcher.inject(); // Optimize perm checks
            } catch (Throwable e) {
                debug("====== UPDATE WORLDEDIT TO 6.1.1 ======");
                MainUtil.handleError(e, false);
                debug("=======================================");
                debug("Update the plugin, or contact the Author!");
                if (IMP.getPlatform().equals("bukkit")) {
                    debug(" - http://builds.enginehub.org/job/worldedit?branch=master");
                } else {
                    debug(" - http://builds.enginehub.org/job/worldedit?branch=forge-archive%2F1.8.9 (FORGE)");
                    debug(" - https://ci.minecrell.net/job/worldedit-spongevanilla/ (SV)");
                }
                debug("=======================================");
            }
        } catch (Throwable e) {
            debug("====== FAWE FAILED TO INITIALIZE ======");
            MainUtil.handleError(e, false);
            debug("=======================================");
            debug("Things to check: ");
            debug(" - Using the latest version of WorldEdit/FAWE");
            debug(" - AsyncWorldEdit/WorldEditRegions isn't installed");
            debug(" - Any other errors in the startup log");
            debug(" - Contact Empire92 for assistance!");
            debug("=======================================");
        }
        try {
            com.github.luben.zstd.util.Native.load();
        } catch (Throwable e) {
            Settings.CLIPBOARD.COMPRESSION_LEVEL = Math.min(6, Settings.CLIPBOARD.COMPRESSION_LEVEL);
            Settings.HISTORY.COMPRESSION_LEVEL = Math.min(6, Settings.HISTORY.COMPRESSION_LEVEL);
            debug("====== ZSTD COMPRESSION BINDING NOT FOUND ======");
            MainUtil.handleError(e, false);
            debug("===============================================");
            debug("FAWE will still work, but some things may be slower");
            debug(" - Try updating your JVM / OS");
            debug(" - Report this issue if you cannot resolve it");
            debug("===============================================");
        }
        try {
            net.jpountz.util.Native.load();
        } catch (Throwable e) {
            debug("====== LZ4 COMPRESSION BINDING NOT FOUND ======");
            MainUtil.handleError(e, false);
            debug("===============================================");
            debug("FAWE will still work, but some things may be slower");
            debug(" - Try updating your JVM / OS");
            debug(" - Report this issue if you cannot resolve it");
            debug("===============================================");
        }
        try {
            String arch = System.getenv("PROCESSOR_ARCHITECTURE");
            String wow64Arch = System.getenv("PROCESSOR_ARCHITEW6432");
            boolean x86OS = arch.endsWith("64") || wow64Arch != null && wow64Arch.endsWith("64") ? false : true;
            boolean x86JVM = System.getProperty("sun.arch.data.model").equals("32");
            if (x86OS != x86JVM) {
                debug("====== UPGRADE TO 64-BIT JAVA ======");
                debug("You are running 32-bit Java on a 64-bit machine");
                debug(" - This is only a recommendation");
                debug("====================================");
            }
        } catch (Throwable ignore) {}
        if (!isJava8) {
            debug("====== UPGRADE TO JAVA 8 ======");
            debug("You are running " + System.getProperty("java.version"));
            debug(" - This is only a recommendation");
            debug("====================================");
        }
    }

    private void setupMemoryListener() {
        if (Settings.MAX_MEMORY_PERCENT < 1 || Settings.MAX_MEMORY_PERCENT > 99) {
            return;
        }
        try {
            final MemoryMXBean memBean = ManagementFactory.getMemoryMXBean();
            final NotificationEmitter ne = (NotificationEmitter) memBean;

            ne.addNotificationListener(new NotificationListener() {
                @Override
                public void handleNotification(final Notification notification, final Object handback) {
                    MemUtil.memoryLimitedTask();
                }
            }, null, null);

            final List<MemoryPoolMXBean> memPools = ManagementFactory.getMemoryPoolMXBeans();
            for (final MemoryPoolMXBean mp : memPools) {
                if (mp.isUsageThresholdSupported()) {
                    final MemoryUsage mu = mp.getUsage();
                    final long max = mu.getMax();
                    if (max < 0) {
                        continue;
                    }
                    final long alert = (max * Settings.MAX_MEMORY_PERCENT) / 100;
                    mp.setUsageThreshold(alert);
                }
            }
        } catch (Throwable e) {
            debug("====== MEMORY LISTENER ERROR ======");
            MainUtil.handleError(e, false);
            debug("===================================");
            debug("FAWE needs access to the JVM memory system:");
            debug(" - Change your Java security settings");
            debug(" - Disable this with `max-memory-percent: -1`");
            debug("===================================");
        }
    }

    /**
     * Get the main thread
     * @return
     */
    public Thread getMainThread() {
        return this.thread;
    }

    public boolean isMainThread() {
        return Thread.currentThread() == thread;
    }

    /**
     * Sets the main thread to the current thread
     * @return
     */
    public Thread setMainThread() {
        return this.thread = Thread.currentThread();
    }

    private ConcurrentHashMap<String, FawePlayer> players = new ConcurrentHashMap<>(8, 0.9f, 1);

    public <T> void register(FawePlayer<T> player) {
        players.put(player.getName(), player);
    }

    public <T> void unregister(String name) {
        players.remove(name);
    }

    public FawePlayer getCachedPlayer(String name) {
        return players.get(name);
    }

    public Collection<FawePlayer> getCachedPlayers() {
        return players.values();
    }

    /*
     * TODO FIXME
     *  - Async packet sending
     *  - Optimize lighting updates / chunk sending
     */
}
