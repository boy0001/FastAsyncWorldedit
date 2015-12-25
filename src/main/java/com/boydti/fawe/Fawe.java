package com.boydti.fawe;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.util.List;

import javax.management.InstanceAlreadyExistsException;
import javax.management.Notification;
import javax.management.NotificationEmitter;
import javax.management.NotificationListener;

import com.boydti.fawe.command.FixLighting;
import com.boydti.fawe.command.Stream;
import com.boydti.fawe.command.Wea;
import com.boydti.fawe.command.WorldEditRegion;
import com.boydti.fawe.config.BBC;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.util.Lag;
import com.boydti.fawe.util.MemUtil;
import com.boydti.fawe.util.SetBlockQueue;
import com.boydti.fawe.util.TaskManager;
import com.boydti.fawe.util.WEManager;
import com.boydti.fawe.util.WESubscriber;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.command.SchematicCommands;
import com.sk89q.worldedit.command.ScriptingCommands;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.visitor.BreadthFirstSearch;
import com.sk89q.worldedit.function.visitor.DownwardVisitor;
import com.sk89q.worldedit.function.visitor.EntityVisitor;
import com.sk89q.worldedit.function.visitor.FlatRegionVisitor;
import com.sk89q.worldedit.function.visitor.LayerVisitor;
import com.sk89q.worldedit.function.visitor.NonRisingVisitor;
import com.sk89q.worldedit.function.visitor.RecursiveVisitor;
import com.sk89q.worldedit.function.visitor.RegionVisitor;

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
        IMP = implementation;
        
        this.thread = Thread.currentThread();
        /*
         * Implementation dependent stuff
         */
        setupConfigs();
        setupCommands();
        
        // TODO command event - queue?

        TaskManager.IMP = IMP.getTaskManager();
        SetBlockQueue.IMP.queue = IMP.getQueue();
        
        // Delayed setup
        TaskManager.IMP.later(new Runnable() {
            @Override
            public void run() {
                // worldedit
                WEManager.IMP.managers.addAll(IMP.getMaskManagers());
                worldedit = WorldEdit.getInstance();
                // Events
                setupEvents();
                IMP.setupVault();
            }
        }, 0);
        
        /*
         * Instance independent stuff
         */
        setupInjector();
        setupMemoryListener();
        
        // Lag
        final Lag lag = new Lag();
        TaskManager.IMP.repeat(lag, 100);
    }
    
    private void setupEvents() {
        WorldEdit.getInstance().getEventBus().register(new WESubscriber());
        if (Settings.COMMAND_PROCESSOR) {
            IMP.setupWEListener();
        }
    }
    
    private void setupCommands() {
        IMP.setupCommand("wea", new Wea());
        IMP.setupCommand("fixlighting", new FixLighting());
        IMP.setupCommand("stream", new Stream());
        IMP.setupCommand("wrg", new WorldEditRegion());
    }
    
    private void setupConfigs() {
        // Setting up config.yml
        Settings.setup(new File(IMP.getDirectory(), "config.yml"));
        // Setting up message.yml
        BBC.load(new File(IMP.getDirectory(), "message.yml"));
    }
    
    private WorldEdit worldedit;
    
    public WorldEdit getWorldEdit() {
        return worldedit;
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
        return thread;
    }
    
    /*
     * TODO FIXME
     *  - Async packet sending
     *  - Redo WEManager delay / command queue
     *  - Support older versions of bukkit
     *  - Optimize lighting updates / chunk sending 
     */
}
