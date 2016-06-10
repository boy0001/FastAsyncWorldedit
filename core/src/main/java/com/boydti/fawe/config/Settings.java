package com.boydti.fawe.config;

import com.boydti.fawe.configuration.file.YamlConfiguration;
import com.boydti.fawe.object.FaweLimit;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.util.MainUtil;
import com.sk89q.worldedit.LocalSession;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class Settings {

    public static long MEM_FREE = 95;
    public static boolean ENABLE_HARD_LIMIT = true;
    public static boolean STORE_HISTORY_ON_DISK = false;
    public static boolean STORE_CLIPBOARD_ON_DISK = false;
    public static boolean CONSOLE_HISTORY = true;
    public static int DELETE_HISTORY_AFTER_DAYS = 7;
    public static boolean CLEAN_HISTORY_ON_LOGOUT = true;
    public static int DELETE_CLIPBOARD_AFTER_DAYS = 1;
    public static boolean METRICS = true;
    public static int CHUNK_WAIT = 100;
    public static boolean REGION_RESTRICTIONS = true;
    public static int ALLOCATE = 0;
    public static int QUEUE_SIZE = 64;
    public static int QUEUE_MAX_WAIT = 1000;
    public static boolean DISPLAY_PROGRESS = false;
    public static int DISPLAY_PROGRESS_INTERVAL = 1;
    public static List<String> ALLOWED_3RDPARTY_EXTENTS;
    public static boolean EXTENT_DEBUG = true;
    public static boolean FIX_ALL_LIGHTING = true;
    public static boolean ASYNC_LIGHTING = true;
    public static int PHYSICS_PER_TICK = 500000;
    public static int ITEMS_PER_TICK = 50000;
    public static String WEB_URL = "http://empcraft.com/fawe/";

    // Maybe confusing?
    // - `compression: false` just uses cheaper compression, but still compresses
    public static int COMPRESSION_LEVEL = 0;
    public static boolean COMBINE_HISTORY_STAGE = false;
    public static int PARALLEL_THREADS = 1;

    // Non configurable (yet / shouldn't be?)
    public static int BUFFER_SIZE = 531441;
    public static int QUEUE_DISCARD_AFTER = 60000;

    public static HashMap<String, FaweLimit> limits;

    public static FaweLimit getLimit(FawePlayer player) {
        FaweLimit limit = new FaweLimit();
        limit.MAX_CHANGES = 0;
        limit.MAX_FAILS = 0;
        limit.MAX_CHECKS = 0;
        limit.MAX_ENTITIES = 0;
        limit.MAX_BLOCKSTATES = 0;
        limit.MAX_ITERATIONS = 0;
        for (Entry<String, FaweLimit> entry : limits.entrySet()) {
            String key = entry.getKey();
            if (key.equals("default") || (player != null && player.hasPermission("fawe.limit." + key))) {
                FaweLimit newLimit = entry.getValue();
                limit.MAX_CHANGES = Math.max(limit.MAX_CHANGES, newLimit.MAX_CHANGES != -1 ? newLimit.MAX_CHANGES : Integer.MAX_VALUE);
                limit.MAX_BLOCKSTATES = Math.max(limit.MAX_BLOCKSTATES, newLimit.MAX_BLOCKSTATES != -1 ? newLimit.MAX_BLOCKSTATES : Integer.MAX_VALUE);
                limit.MAX_CHECKS = Math.max(limit.MAX_CHECKS, newLimit.MAX_CHECKS != -1 ? newLimit.MAX_CHECKS : Integer.MAX_VALUE);
                limit.MAX_ENTITIES = Math.max(limit.MAX_ENTITIES, newLimit.MAX_ENTITIES != -1 ? newLimit.MAX_ENTITIES : Integer.MAX_VALUE);
                limit.MAX_FAILS = Math.max(limit.MAX_FAILS, newLimit.MAX_FAILS != -1 ? newLimit.MAX_FAILS : Integer.MAX_VALUE);
                limit.MAX_ITERATIONS = Math.max(limit.MAX_ITERATIONS, newLimit.MAX_ITERATIONS != -1 ? newLimit.MAX_ITERATIONS : Integer.MAX_VALUE);
            }
        }
        return limit;
    }

    public static void setup(final File file) {
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            try {
                file.createNewFile();
            } catch (final IOException e) {
                MainUtil.handleError(e);
            }
        }
        final YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        config.set("DOCUMENTATION","https://github.com/boy0001/FastAsyncWorldedit/wiki/Configuration");

        limits = new HashMap<>();

        final Map<String, Object> options = new HashMap<>();
        options.put("max-memory-percent", MEM_FREE);
        options.put("crash-mitigation", ENABLE_HARD_LIMIT);
        options.put("lighting.fix-all", FIX_ALL_LIGHTING);
        options.put("lighting.async", ASYNC_LIGHTING);
        options.put("clipboard.use-disk", STORE_CLIPBOARD_ON_DISK);
        options.put("clipboard.delete-after-days", DELETE_CLIPBOARD_AFTER_DAYS);
        options.put("history.use-disk", STORE_HISTORY_ON_DISK);
        options.put("history.compress", false);
        options.put("history.chunk-wait-ms", CHUNK_WAIT);
        options.put("history.delete-after-days", DELETE_HISTORY_AFTER_DAYS);
        options.put("history.delete-on-logout", CLEAN_HISTORY_ON_LOGOUT);
        options.put("history.enable-for-console", CONSOLE_HISTORY);
        options.put("region-restrictions", REGION_RESTRICTIONS);
        options.put("queue.extra-time-ms", ALLOCATE);
        options.put("queue.progress.display", DISPLAY_PROGRESS);
        options.put("queue.progress.interval", DISPLAY_PROGRESS_INTERVAL);
        options.put("queue.target-size", QUEUE_SIZE);
        options.put("queue.max-wait-ms", QUEUE_MAX_WAIT);
        options.put("extent.allowed-plugins", new ArrayList<String>());
        options.put("extent.debug", EXTENT_DEBUG);
        options.put("web.url", WEB_URL);
        options.put("metrics", METRICS);

        // Possibly confusing? - leave configurable since not entirely stable yet
        options.put("history.combine-stages", COMBINE_HISTORY_STAGE);
        options.put("queue.parallel-threads", Math.max(1, Runtime.getRuntime().availableProcessors()));

        if (config.getInt("tick-limiter.physics") == 1337) {
            config.set("tick-limiter.physics", PHYSICS_PER_TICK);
        }
        options.put("tick-limiter.physics", PHYSICS_PER_TICK);
        options.put("tick-limiter.items", ITEMS_PER_TICK);

        // Default limit
        FaweLimit defaultLimit = new FaweLimit();
        if (!config.contains("limits.default")) {
            config.createSection("limits.default");
        }
        defaultLimit.load(config.getConfigurationSection("limits.default"), null, true);
        for (String key : config.getConfigurationSection("limits").getKeys(false)) {
            FaweLimit limit = new FaweLimit();
            limit.load(config.getConfigurationSection("limits." + key), defaultLimit, false);
            limits.put(key, limit);
        }
        for (final Entry<String, Object> node : options.entrySet()) {
            if (!config.contains(node.getKey())) {
                config.set(node.getKey(), node.getValue());
            }
        }
        FIX_ALL_LIGHTING = config.getBoolean("lighting.fix-all");
        ASYNC_LIGHTING = config.getBoolean("lighting.async");
        MEM_FREE = config.getInt("max-memory-percent");
        ENABLE_HARD_LIMIT = config.getBoolean("crash-mitigation");
        REGION_RESTRICTIONS = config.getBoolean("region-restrictions");
        METRICS = config.getBoolean("metrics");
        COMPRESSION_LEVEL = config.getInt("history.compression-level", config.getBoolean("history.compress") ? 1 : 0);
        DELETE_HISTORY_AFTER_DAYS = config.getInt("history.delete-after-days");
        CLEAN_HISTORY_ON_LOGOUT = config.getBoolean("history.delete-on-logout");
        CHUNK_WAIT = config.getInt("history.chunk-wait-ms");
        CONSOLE_HISTORY = config.getBoolean("history.enable-for-console");
        ALLOCATE = config.getInt("queue.extra-time-ms");
        QUEUE_SIZE = config.getInt("queue.target-size");
        QUEUE_MAX_WAIT = config.getInt("queue.max-wait-ms");
        DISPLAY_PROGRESS = config.getBoolean("queue.progress.display");
        DISPLAY_PROGRESS_INTERVAL = config.getInt("queue.progress.interval");
        PARALLEL_THREADS = config.getInt("queue.parallel-threads", Math.max(1, Runtime.getRuntime().availableProcessors()));
        ALLOWED_3RDPARTY_EXTENTS = config.getStringList("extent.allowed-plugins");
        EXTENT_DEBUG = config.getBoolean("extent.debug");
        STORE_CLIPBOARD_ON_DISK = config.getBoolean("clipboard.use-disk");
        DELETE_CLIPBOARD_AFTER_DAYS = config.getInt("clipboard.delete-after-days");
        PHYSICS_PER_TICK = config.getInt("tick-limiter.physics");
        ITEMS_PER_TICK = config.getInt("tick-limiter.items");

        // Not usually configurable
        BUFFER_SIZE = config.getInt("history.buffer-size", BUFFER_SIZE);
        QUEUE_DISCARD_AFTER = config.getInt("queue.discard-after-ms", QUEUE_DISCARD_AFTER);
        COMBINE_HISTORY_STAGE = config.getBoolean("history.combine-stages", COMBINE_HISTORY_STAGE);

        WEB_URL = config.getString("web.url");

        if (STORE_HISTORY_ON_DISK = config.getBoolean("history.use-disk")) {
            LocalSession.MAX_HISTORY_SIZE = Integer.MAX_VALUE;
        }

        try {
            config.save(file);
        } catch (final IOException e) {
            MainUtil.handleError(e);
        }
    }
}
