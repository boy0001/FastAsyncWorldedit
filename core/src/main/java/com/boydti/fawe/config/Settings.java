package com.boydti.fawe.config;

import com.boydti.fawe.object.FaweLimit;
import com.boydti.fawe.object.FawePlayer;
import com.sk89q.worldedit.LocalSession;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Settings extends Config {

    @Comment("These first 6 aren't configurable") // This is a comment
    @Final // Indicates that this value isn't configurable
    public static final String ISSUES = "https://github.com/boy0001/FastAsyncWorldedit/issues";
    @Final
    public static final String WIKI = "https://github.com/boy0001/FastAsyncWorldedit/wiki/";
    @Final
    public static String DATE = null; // These values are set from FAWE before loading
    @Final
    public static String BUILD = null; // These values are set from FAWE before loading
    @Final
    public static String COMMIT = null; // These values are set from FAWE before loading
    @Final
    public static String PLATFORM = null; // These values are set from FAWE before loading

    @Comment("Allow the plugin to update")
    public static boolean UPDATE = true;
    @Comment("Send anonymous usage statistics to MCStats.org")
    public static boolean METRICS = true;
    @Comment("FAWE will skip chunks when there's not enough memory available")
    public static boolean PREVENT_CRASHES = false;
    @Comment({
            "Set true to enable WorldEdit restrictions per region (e.g. PlotSquared or WorldGuard).",
            "To be allowed to WorldEdit in a region, users need the appropriate",
            "fawe.<plugin>  permission. See the Permissions page for supported region plugins."
    })
    public static boolean REGION_RESTRICTIONS = true;
    @Comment({
            "FAWE will cancel non admin edits when memory consumption exceeds this %",
            " - Bypass with `/wea` or `//fast` or `fawe.bypass`",
            " - Disable with 100 or -1."
    })
    public static int MAX_MEMORY_PERCENT = 95;

    @Comment("Paths for various directories")
    public static final class PATHS {
        public static String HISTORY = "history";
        public static String CLIPBOARD = "clipboard";
    }


    @Create // This value will be generated automatically
    public static ConfigBlock<LIMITS> LIMITS = null;

    @Comment({
            "The \"default\" limit group affects those without a specific limit permission.",
            "To grant someone different limits, copy the default limits group",
            "and give it a different name (e.g. newbie). Then give the user the limit ",
            "permission node with that limit name (e.g. fawe.limit.newbie  )"
    })
    @BlockName("default") // The name for the default block
    public static final class LIMITS extends ConfigBlock {
        @Comment("Max actions that can be run concurrently (i.e. commands)")
        public int MAX_ACTIONS = 1;
        @Comment("Max number of block changes (e.g. by `//set stone`).")
        public int MAX_CHANGES = 50000000;
        @Comment("Max number of blocks checked (e.g. `//count stone` which doesn't change blocks)")
        public int MAX_CHECKS = 50000000;
        @Comment("Number of times a change can fail (e.g. if the player can't access that region)")
        public int MAX_FAILS = 50000000;
        @Comment("Allowed brush iterations (e.g. `//brush smooth`)")
        public int MAX_ITERATIONS = 1000;
        @Comment("Max allowed entities (e.g. cows)")
        public int MAX_ENTITIES = 1337;
        @Comment({
                "Blockstates include Banner, Beacon, BrewingStand, Chest, CommandBlock, ",
                "CreatureSpawner, Dispenser, Dropper, EndGateway, Furnace, Hopper, Jukebox, ",
                "NoteBlock, Sign, Skull, Structure"
        })
        public int MAX_BLOCKSTATES = 1337;
        @Comment({
                "Maximum size of the player's history in Megabytes:",
                " - History on disk or memory will be deleted",
        })
        public int MAX_HISTORY_MB = -1;
        @Comment("Needlessly make WorldEdit slower for this player (ms/block)")
        public int SPEED_REDUCTION = 0;
        @Comment({
                "Should WorldEdit use inventory?",
                "0 = No inventory usage (creative)",
                "1 = Inventory for removing and placing (freebuild)",
                "2 = Inventory for placing (survival)",
        })
        public static int INVENTORY_MODE = 0;
        @Comment({
            "Place chunks instead of individual blocks"
        })
        public static boolean FAST_PLACEMENT = true;
    }

    public static class HISTORY {
        @Comment({
                "Should history be saved on disk:",
                " - Frees up a lot of memory",
                " - Persists restarts",
                " - Unlimited undo",
                " - Enables the rollback command"
        })
        public static boolean USE_DISK = true;
        @Comment({
                "Use a database to store disk storage summaries:",
                " - Faster lookups and rollback from disk",
        })
        public static boolean USE_DATABASE = true;
        @Comment({
                "Record history with dispatching:",
                " - Faster as it avoids duplicate block checks",
                " - Worse compression since dispatch order is different"
        })
        public static boolean COMBINE_STAGES = true;
        @Comment({
                "Higher compression reduces the size of history at the expense of CPU",
                "0 = Uncompressed byte array (fastest)",
                "1 = 1 pass fast compressor (default)",
                "2 = 2 x fast",
                "3 = 3 x fast",
                "4 = 1 x medium, 1 x fast",
                "5 = 1 x medium, 2 x fast",
                "6 = 1 x medium, 3 x fast",
                "7 = 1 x high, 1 x medium, 1 x fast",
                "8 = 1 x high, 1 x medium, 2 x fast",
                "9 = 1 x high, 1 x medium, 3 x fast (best compression)",
                "NOTE: If using disk, do some compression (3+) as smaller files save faster"
        })
        public static int COMPRESSION_LEVEL = 3;
        @Comment({
                "The buffer size for compression:",
                " - Larger = better ratio but uses more upfront memory"
        })
        public static int BUFFER_SIZE = 531441;


        @Comment({
                "The maximum time in milliseconds to wait for a chunk to load for an edit.",
                " (50ms = 1 server tick, 0 = Fastest).",
                " The default value of 100 should be safe for most cases.",
                "",
                "Actions which require loaded chunks (e.g. copy) which do not load in time",
                " will use the last chunk as filler, which may appear as bands of duplicated blocks.",
                "Actions usually wait about 25-50ms for the chunk to load, more if the server is lagging.",
                "A value of 100ms does not force it to wait 100ms if the chunk loads in 10ms.",
                "",
                "This value is a timeout in case a chunk is never going to load (for whatever odd reason).",
                "If the action times out, the operation continues by using the previous chunk as filler,",
                " and displaying an error message.  In this case, either copy a smaller section,",
                " or increase chunk-wait-ms.",
                "A value of 0 is faster simply because it doesn't bother loading the chunks or waiting.",
        })
        public static int CHUNK_WAIT_MS = 1000;
        @Comment("Delete history on disk after a number of days")
        public static int DELETE_AFTER_DAYS = 7;
        @Comment("Delete history in memory on logout (does not effect disk)")
        public static boolean DELETE_ON_LOGOUT = true;
        @Comment({
                "If history should be enabled by default for plugins using WorldEdit:",
                " - It is faster to have disabled",
                " - Use of the FAWE API will not be effected"
        })
        public static boolean ENABLE_FOR_CONSOLE = true;
        @Comment({
                "Should redo information be stored:",
                " - History is about 20% larger",
                " - Enables use of /redo",
        })
        public static boolean STORE_REDO = true;
        @Comment({
                "Assumes all edits are smaller than 4096x256x4096:",
                " - Reduces history size by ~10%",
        })
        public static boolean SMALL_EDITS = false;
    }

    public static class QUEUE {
        @Comment({
            "If no blocks from completed edits are queued, and if the global queue has more available ",
            "chunks to place from still-processing edits than the target size setting, it will begin",
            "placing available blocks from edits still in the preprocessing stage."
        })
        public static int TARGET_SIZE = 64;
        @Comment({
            "This should equal the number of processors you have"
        })
        public static int PARALLEL_THREADS = Math.max(1, Runtime.getRuntime().availableProcessors());
        @Comment({
                "The time in milliseconds that the global queue can be idle before it is forced to start",
                "on edits which are still in the preprocessing stage."
        })
        public static int MAX_WAIT_MS = 1000;

        @Comment({
                "Increase or decrease queue intensity (0 = balance of performance / stability)",
                "Should not need to change this.  Increasing it (positive value) too high ",
                "will probably cause the server to freeze, and decreasing it (negative value)",
                "may reduce load on the server but should not be necessary."
        })
        public static int EXTRA_TIME_MS = 0;

        @Comment({
                "Discard edits which have been idle for a certain amount of time (ms) (e.g. a plugin creates",
                "an EditSession but never does anything with it)."
        })
        public static int DISCARD_AFTER_MS = 60000;

        public static class PROGRESS {
            @Comment("Display constant titles about the progress of a user's edit")
            public static boolean DISPLAY = false;
            @Comment("How often edit progress is displayed")
            public static int INTERVAL = 1;
        }
    }

    @Comment("Experimental options, use at your own risk")
    public static class EXPERIMENTAL {
        @Comment({
                "Directly modify the region files.",
        })
        public static boolean ANVIL_QUEUE_MODE = false;
    }

    public static class WEB {
        @Comment("I am already hosting a web interface for you here")
        public static String URL = "http://empcraft.com/fawe/";
    }

    public static class EXTENT {
        @Comment({
                "Don't bug console when these plugins slow down WorldEdit operations"
        })
        public static List<String> ALLOWED_PLUGINS = new ArrayList<>();
        @Comment("Disable the messages completely")
        public static boolean DEBUG = true;
    }

    @Comment("Generic tick limiter (not necessarily WorldEdit related, but useful to stop abuse)")
    public static class TICK_LIMITER {
        @Comment("Enable the limiter")
        public static boolean ENABLED = true;
        @Comment("The interval in ticks")
        public static int INTERVAL = 1;
        @Comment("Max falling blocks per interval (per chunk)")
        public static int FALLING = 512;
        @Comment("Max physics per interval (per chunk)")
        public static int PHYSICS = 512;
        @Comment("Max item spawns per interval (per chunk)")
        public static int ITEMS = 128;

    }

    public static class CLIPBOARD {
        @Comment("Store the clipboard on disk instead of memory")
        public static boolean USE_DISK = true;
        @Comment({
                "Compress the clipboard to reduce the size:",
                " - TODO: Buffered random access with compression is not implemented on disk yet",
                " - 0 = No compression",
                " - 1 = Fast compression",
                " - 2-17 = Slower compression"
        })
        public static int COMPRESSION_LEVEL = 1;
        @Comment("Number of days to keep history on disk before deleting it")
        public static int DELETE_AFTER_DAYS = 1;
    }

    public static class LIGHTING {
        @Comment({
                "If chunk lighting should be done asynchronously:",
                " - Async lighting is less intense, but more likely to glitch"
        })
        public static boolean ASYNC = true;
        @Comment({
                "The relighting mode to use:",
                " - 0 = None (Do no relighting)",
                " - 1 = Optimal (Relight changed light sources and changed blocks)",
                " - 2 = All (Slowly relight every blocks)"
        })
        public static int MODE = 1;
    }

    public static void save(File file) {
        save(file, Settings.class);
    }

    public static void load(File file) {
        load(file, Settings.class);
        if (HISTORY.USE_DISK) {
            LocalSession.MAX_HISTORY_SIZE = Integer.MAX_VALUE;
        }
    }

    public static FaweLimit getLimit(FawePlayer player) {
        FaweLimit limit;
        if (player.hasWorldEditBypass()) {
            limit = FaweLimit.MAX.copy();
        } else {
            limit = new FaweLimit();
        }
        Collection<String> keys = LIMITS.getSections();
        for (String key : keys) {
            if (key.equals("default") || (player != null && player.hasPermission("fawe.limit." + key))) {
                LIMITS newLimit = LIMITS.get(key);
                limit.MAX_ACTIONS = Math.max(limit.MAX_ACTIONS, newLimit.MAX_ACTIONS != -1 ? newLimit.MAX_ACTIONS : Integer.MAX_VALUE);
                limit.MAX_CHANGES = Math.max(limit.MAX_CHANGES, newLimit.MAX_CHANGES != -1 ? newLimit.MAX_CHANGES : Integer.MAX_VALUE);
                limit.MAX_BLOCKSTATES = Math.max(limit.MAX_BLOCKSTATES, newLimit.MAX_BLOCKSTATES != -1 ? newLimit.MAX_BLOCKSTATES : Integer.MAX_VALUE);
                limit.MAX_CHECKS = Math.max(limit.MAX_CHECKS, newLimit.MAX_CHECKS != -1 ? newLimit.MAX_CHECKS : Integer.MAX_VALUE);
                limit.MAX_ENTITIES = Math.max(limit.MAX_ENTITIES, newLimit.MAX_ENTITIES != -1 ? newLimit.MAX_ENTITIES : Integer.MAX_VALUE);
                limit.MAX_FAILS = Math.max(limit.MAX_FAILS, newLimit.MAX_FAILS != -1 ? newLimit.MAX_FAILS : Integer.MAX_VALUE);
                limit.MAX_ITERATIONS = Math.max(limit.MAX_ITERATIONS, newLimit.MAX_ITERATIONS != -1 ? newLimit.MAX_ITERATIONS : Integer.MAX_VALUE);
                limit.MAX_HISTORY = Math.max(limit.MAX_HISTORY, newLimit.MAX_HISTORY_MB != -1 ? newLimit.MAX_HISTORY_MB : Integer.MAX_VALUE);
                limit.INVENTORY_MODE = Math.min(limit.INVENTORY_MODE, newLimit.INVENTORY_MODE);
                limit.SPEED_REDUCTION = Math.min(limit.SPEED_REDUCTION, newLimit.SPEED_REDUCTION);
                limit.FAST_PLACEMENT |= newLimit.FAST_PLACEMENT;
            }
        }
        return limit;
    }
}