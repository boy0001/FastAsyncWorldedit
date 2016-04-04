package com.boydti.fawe;

import com.boydti.fawe.command.FixLighting;
import com.boydti.fawe.command.Reload;
import com.boydti.fawe.command.Stream;
import com.boydti.fawe.command.Wea;
import com.boydti.fawe.command.WorldEditRegion;
import com.boydti.fawe.config.BBC;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.regions.general.PlotSquaredFeature;
import com.boydti.fawe.util.Lag;
import com.boydti.fawe.util.MainUtil;
import com.boydti.fawe.util.MemUtil;
import com.boydti.fawe.util.TaskManager;
import com.boydti.fawe.util.WEManager;
import com.boydti.fawe.util.WESubscriber;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.command.SchematicCommands;
import com.sk89q.worldedit.command.ScriptingCommands;
import com.sk89q.worldedit.extension.platform.CommandManager;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.function.operation.Operations;
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
import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import javax.management.InstanceAlreadyExistsException;
import javax.management.Notification;
import javax.management.NotificationEmitter;
import javax.management.NotificationListener;
import net.jpountz.util.Native;

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

    /**
     * Write something to the console
     * @param s
     */
    public static void debug(final String s) {
        if (INSTANCE != null) {
            INSTANCE.IMP.debug(s);
        } else {
            System.out.print(s);
        }
    }

    /**
     * The platform specific implementation
     */
    private final IFawe IMP;
    private Thread thread = Thread.currentThread();

    private Fawe(final IFawe implementation) {
        this.IMP = implementation;

        this.thread = Thread.currentThread();
        /*
         * Implementation dependent stuff
         */
        this.setupConfigs();
        this.setupCommands();

        // TODO command event - queue?

        TaskManager.IMP = this.IMP.getTaskManager();
        if (Settings.METRICS) {
            this.IMP.startMetrics();
        }

        // Delete old history
        MainUtil.deleteDirectory(new File(IMP.getDirectory(), "history"));

        // Delayed setup
        TaskManager.IMP.later(new Runnable() {
            @Override
            public void run() {
                // worldedit
                WEManager.IMP.managers.addAll(Fawe.this.IMP.getMaskManagers());
                try {
                    WEManager.IMP.managers.add(new PlotSquaredFeature());
                } catch (Throwable e) {}
                Fawe.this.worldedit = WorldEdit.getInstance();
                // Events
                Fawe.this.setupEvents();
                Fawe.this.IMP.setupVault();
            }
        }, 0);

        /*
         * Instance independent stuff
         */
        this.setupInjector();
        this.setupMemoryListener();

        // Lag
        final Lag lag = new Lag();
        TaskManager.IMP.repeat(lag, 100);
    }

    private void setupEvents() {
        WorldEdit.getInstance().getEventBus().register(new WESubscriber());
        if (Settings.COMMAND_PROCESSOR) {
            this.IMP.setupWEListener();
        }
    }

    private void setupCommands() {
        this.IMP.setupCommand("wea", new Wea());
        this.IMP.setupCommand("fixlighting", new FixLighting());
        this.IMP.setupCommand("stream", new Stream());
        this.IMP.setupCommand("wrg", new WorldEditRegion());
        this.IMP.setupCommand("fawe", new Reload());
    }

    public void setupConfigs() {
        // Setting up config.yml
        Settings.setup(new File(this.IMP.getDirectory(), "config.yml"));
        // Setting up message.yml
        BBC.load(new File(this.IMP.getDirectory(), "message.yml"));
    }

    private WorldEdit worldedit;

    public WorldEdit getWorldEdit() {
        return this.worldedit;
    }

    private void setupInjector() {
        EditSession.inject();
        Operations.inject();
        SchematicCommands.inject();
        ScriptingCommands.inject();
        BreadthFirstSearch.inject();
        DownwardVisitor.inject();
        EntityVisitor.inject();
        FlatRegionVisitor.inject();
        LayerVisitor.inject();
        NonRisingVisitor.inject();
        RecursiveVisitor.inject();
        RegionVisitor.inject();
        EntityCreate.inject();
        EntityRemove.inject();
        LocalSession.inject();
        BlockArrayClipboard.inject();
        try {
            CommandManager.inject();
        } catch (Throwable e) {
            e.printStackTrace();
            IMP.debug("Incompatible version of WorldEdit, please update the plugin or contact the Author!");
        }
        try {
            Native.load();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private void setupMemoryListener() {
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
                final long alert = (max * Settings.MEM_FREE) / 100;
                mp.setUsageThreshold(alert);

            }
        }
    }

    public Thread getMainThread() {
        return this.thread;
    }

    private ConcurrentHashMap<String, FawePlayer> players = new ConcurrentHashMap<>();

    public <T> void register(FawePlayer<T> player) {
        players.put(player.getName(), player);
    }

    public <T> void unregister(String name) {
        players.remove(name);
    }

    public FawePlayer getCachedPlayer(String name) {
        return players.get(name);
    }

    /*
     * TODO FIXME
     *  - Async packet sending
     *  - Redo WEManager delay / command queue
     *  - Support older versions of bukkit
     *  - Optimize lighting updates / chunk sending
     */
}
