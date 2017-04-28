package com.boydti.fawe;

import com.boydti.fawe.command.Cancel;
import com.boydti.fawe.config.BBC;
import com.boydti.fawe.config.Commands;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.brush.visualization.VisualQueue;
import com.boydti.fawe.regions.general.plot.PlotSquaredFeature;
import com.boydti.fawe.util.FaweTimer;
import com.boydti.fawe.util.MainUtil;
import com.boydti.fawe.util.MemUtil;
import com.boydti.fawe.util.StringMan;
import com.boydti.fawe.util.TaskManager;
import com.boydti.fawe.util.TextureUtil;
import com.boydti.fawe.util.Updater;
import com.boydti.fawe.util.WEManager;
import com.sk89q.jnbt.NBTInputStream;
import com.sk89q.jnbt.NBTOutputStream;
import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.BlockWorldVector;
import com.sk89q.worldedit.CuboidClipboard;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.Vector2D;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.blocks.BaseBlock;
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
import com.sk89q.worldedit.command.tool.FloodFillTool;
import com.sk89q.worldedit.command.tool.LongRangeBuildTool;
import com.sk89q.worldedit.command.tool.RecursivePickaxe;
import com.sk89q.worldedit.command.tool.brush.GravityBrush;
import com.sk89q.worldedit.command.util.EntityRemover;
import com.sk89q.worldedit.event.extent.EditSessionEvent;
import com.sk89q.worldedit.extension.factory.DefaultBlockParser;
import com.sk89q.worldedit.extension.factory.DefaultMaskParser;
import com.sk89q.worldedit.extension.factory.HashTagPatternParser;
import com.sk89q.worldedit.extension.platform.AbstractPlayerActor;
import com.sk89q.worldedit.extension.platform.CommandManager;
import com.sk89q.worldedit.extension.platform.PlatformManager;
import com.sk89q.worldedit.extension.platform.PlayerProxy;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.MaskingExtent;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.SchematicReader;
import com.sk89q.worldedit.extent.clipboard.io.SchematicWriter;
import com.sk89q.worldedit.extent.inventory.BlockBagExtent;
import com.sk89q.worldedit.extent.transform.BlockTransformExtent;
import com.sk89q.worldedit.function.block.BlockReplace;
import com.sk89q.worldedit.function.block.ExtentBlockCopy;
import com.sk89q.worldedit.function.entity.ExtentEntityCopy;
import com.sk89q.worldedit.function.mask.BlockMask;
import com.sk89q.worldedit.function.mask.FuzzyBlockMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.MaskUnion;
import com.sk89q.worldedit.function.mask.Masks;
import com.sk89q.worldedit.function.mask.OffsetMask;
import com.sk89q.worldedit.function.mask.SolidBlockMask;
import com.sk89q.worldedit.function.operation.ChangeSetExecutor;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.pattern.BlockPattern;
import com.sk89q.worldedit.function.pattern.ClipboardPattern;
import com.sk89q.worldedit.function.pattern.Pattern;
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
import com.sk89q.worldedit.internal.LocalWorldAdapter;
import com.sk89q.worldedit.math.convolution.HeightMap;
import com.sk89q.worldedit.math.interpolation.KochanekBartelsInterpolation;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.EllipsoidRegion;
import com.sk89q.worldedit.regions.selector.CuboidRegionSelector;
import com.sk89q.worldedit.regions.shape.ArbitraryShape;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.session.PasteBuilder;
import com.sk89q.worldedit.session.SessionManager;
import com.sk89q.worldedit.session.request.Request;
import com.sk89q.worldedit.util.command.SimpleDispatcher;
import com.sk89q.worldedit.util.command.parametric.ParameterData;
import com.sk89q.worldedit.util.command.parametric.ParametricBuilder;
import com.sk89q.worldedit.util.command.parametric.ParametricCallable;
import com.sk89q.worldedit.util.formatting.Fragment;
import com.sk89q.worldedit.util.formatting.component.CommandListBox;
import com.sk89q.worldedit.util.formatting.component.CommandUsageBox;
import com.sk89q.worldedit.util.formatting.component.MessageBox;
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
import org.json.simple.parser.ParseException;

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
    private VisualQueue visualQueue;
    private Updater updater;
    private TextureUtil textures;

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
        MainUtil.deleteOlder(MainUtil.getFile(IMP.getDirectory(), Settings.IMP.PATHS.HISTORY), TimeUnit.DAYS.toMillis(Settings.IMP.HISTORY.DELETE_AFTER_DAYS));
        MainUtil.deleteOlder(MainUtil.getFile(IMP.getDirectory(), Settings.IMP.PATHS.CLIPBOARD), TimeUnit.DAYS.toMillis(Settings.IMP.CLIPBOARD.DELETE_AFTER_DAYS));

        TaskManager.IMP = this.IMP.getTaskManager();
        if (Settings.IMP.METRICS) {
            this.IMP.startMetrics();
        }
        this.setupCommands();
        /*
         * Instance independent stuff
         */
        this.setupMemoryListener();
        this.timer = new FaweTimer();
        Fawe.this.IMP.setupVault();

        // Delayed worldedit setup
        TaskManager.IMP.later(new Runnable() {
            @Override
            public void run() {
                try {
                    visualQueue = new VisualQueue();
                    WEManager.IMP.managers.addAll(Fawe.this.IMP.getMaskManagers());
                    WEManager.IMP.managers.add(new PlotSquaredFeature());
                    Fawe.debug("Plugin 'PlotSquared' found. Using it now.");
                } catch (Throwable e) {}
            }
        }, 0);

        TaskManager.IMP.repeat(timer, 1);

        if (Settings.IMP.UPDATE && isJava8()) {
            // Delayed updating
            updater = new Updater();
            TaskManager.IMP.async(new Runnable() {
                @Override
                public void run() {
                    update();
                }
            });
            TaskManager.IMP.repeatAsync(new Runnable() {
                @Override
                public void run() {
                    update();
                }
            }, 36000);
        }
    }

    private boolean update() {
        if (updater != null) {
            updater.update(IMP.getPlatform(), getVersion());
            return true;
        }
        return false;
    }

    private boolean isJava8 = MainUtil.getJavaVersion() >= 1.8;

    public boolean isJava8() {
        return isJava8;
    }

    /**
     * The FAWE updater class
     *  - Use to get basic update information (changelog/version etc)
     * @return
     */
    public Updater getUpdater() {
        return updater;
    }

    public TextureUtil getTextureUtil() {
        TextureUtil tmp = textures;
        if (tmp == null) {
            synchronized (this) {
                tmp = textures;
                if (tmp == null) {
                    try {
                        textures = tmp = new TextureUtil();
                        tmp.loadModTextures();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return tmp;
    }

    /**
     * The FaweTimer is a useful class for monitoring TPS
     * @return FaweTimer
     */
    public FaweTimer getTimer() {
        return timer;
    }

    /**
     * The visual queue is used to queue visualizations
     * @return
     */
    public VisualQueue getVisualQueue() {
        return visualQueue;
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

    private void setupCommands() {
        this.IMP.setupCommand("fcancel", new Cancel());
    }

    public void setupConfigs() {
        MainUtil.copyFile(MainUtil.getJarFile(), "de/messages.yml", null);
        // Setting up config.yml
        File file = new File(this.IMP.getDirectory(), "config.yml");
        Settings.IMP.PLATFORM = IMP.getPlatform().replace("\"", "");
        try {
            InputStream stream = getClass().getResourceAsStream("/fawe.properties");
            java.util.Scanner scanner = new java.util.Scanner(stream).useDelimiter("\\A");
            String versionString = scanner.next().trim();
            scanner.close();
            this.version = new FaweVersion(versionString);
            Settings.IMP.DATE = new Date(100 + version.year, version.month, version.day).toGMTString();
            Settings.IMP.BUILD = "https://ci.athion.net/job/FastAsyncWorldEdit/" + version.build;
            Settings.IMP.COMMIT = "https://github.com/boy0001/FastAsyncWorldedit/commit/" + Integer.toHexString(version.hash);
        } catch (Throwable ignore) {}
        Settings.IMP.reload(file);
        // Setting up message.yml
        BBC.load(new File(this.IMP.getDirectory(), "message.yml"));
    }

    public WorldEdit getWorldEdit() {
        return WorldEdit.getInstance();
    }

    public static void setupInjector() {
        /*
         * Modify the sessions
         *  - EditSession supports custom queue and a lot of optimizations
         *  - LocalSession supports VirtualPlayers and undo on disk
         */
        try {
            // Setting up commands.yml
            Commands.load(new File(INSTANCE.IMP.getDirectory(), "commands.yml"));
            Commands.inject(); // Translations
            EditSession.inject(); // Custom block placer + optimizations
            EditSessionEvent.inject(); // Add EditSession to event (API)
            LocalSession.inject(); // Add remember order / queue flushing / Optimizations for disk / brush visualization
            SessionManager.inject(); // Faster custom session saving + Memory improvements
            PlayerProxy.inject(); // Fixes getBlockInHand not being extended
            AbstractPlayerActor.inject(); // Don't use exception for getBlockInHand control flow
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
            // Formatting
            MessageBox.inject();
            Fragment.inject();
            CommandListBox.inject();
            CommandUsageBox.inject();
            // Schematic
            SchematicReader.inject(); // Optimizations
            SchematicWriter.inject(); // Optimizations
            ClipboardFormat.inject(); // Optimizations + new formats + api
            PasteBuilder.inject(); // Optimizations
            // Brushes/Tools
            FloodFillTool.inject();
            GravityBrush.inject(); // Fix for instant placement assumption
            LongRangeBuildTool.inject();
            AreaPickaxe.inject(); // Fixes
            RecursivePickaxe.inject(); // Fixes
            BrushTool.inject(); // Add transform + support for double action brushes + visualizations
            // Selectors
            CuboidRegionSelector.inject(); // Translations
            EllipsoidRegion.inject(); // Optimizations
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
            HeightMap.inject(); // Optimizations + Features
            // Entity create/remove
            EntityCreate.inject(); // Optimizations
            EntityRemove.inject(); // Optimizations
            EntityRemover.inject(); // Async fixes
            // Clipboards
            BlockArrayClipboard.inject(); // Optimizations + disk
            CuboidClipboard.inject(); // Optimizations
            ClipboardHolder.inject(); // Closeable
            // Regions
            CuboidRegion.inject(); // Optimizations
            // Extents
            MaskingExtent.inject(); // Features
            BlockTransformExtent.inject(); // Fix for cache not being mutable
            AbstractDelegateExtent.inject(); // Optimizations
            BlockBagExtent.inject(); // Fixes + Optimizations
            LocalWorldAdapter.inject();
            // Vector
            BlockWorldVector.inject(); // Optimizations
            BlockVector.inject(); // Optimizations
            Vector.inject(); // Optimizations
            Vector2D.inject(); // Optimizations
            // Block
            BaseBlock.inject(); // Optimizations
            // Pattern
            ArbitraryShape.inject(); // Optimizations + update from legacy code
            Pattern.inject(); // Simplify API
            com.sk89q.worldedit.patterns.Pattern.inject(); // Simplify API
            Patterns.inject(); // Optimizations (reduce object creation)
            RandomPattern.inject(); // Optimizations
            ClipboardPattern.inject(); // Optimizations
            HashTagPatternParser.inject(); // Add new patterns
            DefaultBlockParser.inject(); // Fix block lookups
            BlockPattern.inject(); // Optimization
            // Mask
            Mask.inject(); // Extend deprecated mask
            BlockMask.inject(); // Optimizations
            SolidBlockMask.inject(); // Optimizations
            FuzzyBlockMask.inject(); // Optimizations
            OffsetMask.inject(); // Optimizations
            DefaultMaskParser.inject(); // Add new masks
            Masks.inject(); // Optimizations
            MaskUnion.inject(); // Optimizations
            // Operations
            Operations.inject(); // Optimizations
            ExtentBlockCopy.inject(); // Optimizations
            BlockReplace.inject(); // Optimizations + Features
            ForwardExtentCopy.inject(); // Fixes + optimizations
            ChangeSetExecutor.inject(); // Optimizations
            // BlockData
            BlockData.inject(); // Temporary fix for 1.9.4
            BundledBlockData.inject(); // Add custom rotation
            File jar = MainUtil.getJarFile();
            File extraBlocks = MainUtil.copyFile(jar, "extrablocks.json", null);
            if (extraBlocks != null && extraBlocks.exists()) {
                try {
                    BundledBlockData.getInstance().loadFromResource();
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
                debug(" - http://builds.enginehub.org/job/worldedit?branch=master");
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
            debug("Contact Empire92 if you need assistance:");
            debug(" - Send me a PM or ask on IRC");
            debug(" - http://webchat.esper.net/?nick=&channels=IntellectualCrafters");
            debug("=======================================");
        }
        try {
            com.github.luben.zstd.util.Native.load();
        } catch (Throwable e) {
            if (Settings.IMP.CLIPBOARD.COMPRESSION_LEVEL > 6 || Settings.IMP.HISTORY.COMPRESSION_LEVEL > 6) {
                Settings.IMP.CLIPBOARD.COMPRESSION_LEVEL = Math.min(6, Settings.IMP.CLIPBOARD.COMPRESSION_LEVEL);
                Settings.IMP.HISTORY.COMPRESSION_LEVEL = Math.min(6, Settings.IMP.HISTORY.COMPRESSION_LEVEL);
                debug("====== ZSTD COMPRESSION BINDING NOT FOUND ======");
                debug(e);
                debug("===============================================");
                debug("FAWE will work but won't compress data as much");
                debug("===============================================");
            }
        }
        try {
            net.jpountz.util.Native.load();
        } catch (Throwable e) {
            debug("====== LZ4 COMPRESSION BINDING NOT FOUND ======");
            debug(e);
            debug("===============================================");
            debug("FAWE will work but compression will be slower");
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
        if (MainUtil.getJavaVersion() < 1.8) {
            debug("====== UPGRADE TO JAVA 8 ======");
            debug("You are running " + System.getProperty("java.version"));
            debug(" - This is only a recommendation");
            debug("====================================");
        }
    }

    private void setupMemoryListener() {
        if (Settings.IMP.MAX_MEMORY_PERCENT < 1 || Settings.IMP.MAX_MEMORY_PERCENT > 99) {
            return;
        }
        try {
            final MemoryMXBean memBean = ManagementFactory.getMemoryMXBean();
            final NotificationEmitter ne = (NotificationEmitter) memBean;

            ne.addNotificationListener(new NotificationListener() {
                @Override
                public void handleNotification(final Notification notification, final Object handback) {
                    final long heapSize = Runtime.getRuntime().totalMemory();
                    final long heapMaxSize = Runtime.getRuntime().maxMemory();
                    if (heapSize < heapMaxSize) {
                        return;
                    }
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
                    final long alert = (max * Settings.IMP.MAX_MEMORY_PERCENT) / 100;
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

    public static boolean isMainThread() {
        return INSTANCE != null ? INSTANCE.thread == Thread.currentThread() : true;
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
