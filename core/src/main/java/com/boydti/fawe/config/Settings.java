package com.boydti.fawe.config;

import com.boydti.fawe.configuration.file.YamlConfiguration;
import com.boydti.fawe.object.FaweLimit;
import com.boydti.fawe.object.FawePlayer;
import com.sk89q.worldedit.LocalSession;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class Settings {

    public static boolean REQUIRE_SELECTION = false;
    public static boolean COMMAND_PROCESSOR = false;
    public static List<String> WE_BLACKLIST = Arrays.asList("cs", ".s", "restore", "snapshot", "delchunks", "listchunks");
    public static long MEM_FREE = 95;
    public static boolean ENABLE_HARD_LIMIT = true;
    public static boolean STORE_HISTORY_ON_DISK = false;
    public static int COMPRESSION_LEVEL = 0;
    public static int BUFFER_SIZE = 531441;
    public static boolean METRICS = true;
    public static int CHUNK_WAIT = 0;
    public static boolean REGION_RESTRICTIONS = true;
    public static int ALLOCATE = 0;
    public static int QUEUE_SIZE = 64;
    public static int QUEUE_MAX_WAIT = 1000;
    public static int QUEUE_DISCARD_AFTER = 60000;
    public static List<String> ALLOWED_3RDPARTY_EXTENTS;

    public static boolean FIX_ALL_LIGHTING = true;
    public static boolean ASYNC_LIGHTING = true;

    public static HashMap<String, FaweLimit> limits;

    public static FaweLimit getLimit(FawePlayer player) {
        FaweLimit limit = new FaweLimit();
        for (Entry<String, FaweLimit> entry : limits.entrySet()) {
            String key = entry.getKey();
            if (key.equals("default") || player.hasPermission("fawe.limit." + key)) {
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
                e.printStackTrace();
            }
        }
        final YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        limits = new HashMap<>();

        final Map<String, Object> options = new HashMap<>();
        options.put("require-selection-in-mask", REQUIRE_SELECTION);
        options.put("command-blacklist", WE_BLACKLIST);
        options.put("command-processor", COMMAND_PROCESSOR);
        options.put("max-memory-percent", MEM_FREE);
        options.put("crash-mitigation", ENABLE_HARD_LIMIT);
        options.put("lighting.fix-all", FIX_ALL_LIGHTING);
        options.put("lighting.async", ASYNC_LIGHTING);
        options.put("history.use-disk", STORE_HISTORY_ON_DISK);
        options.put("history.compress", false);
        options.put("history.chunk-wait-ms", CHUNK_WAIT);
        options.put("history.buffer-size", BUFFER_SIZE);
        options.put("region-restrictions", REGION_RESTRICTIONS);
        options.put("queue.extra-time-ms", ALLOCATE);
        options.put("queue.target-size", QUEUE_SIZE);
        options.put("queue.max-wait-ms", QUEUE_MAX_WAIT);
        options.put("queue.discard-after-ms", QUEUE_DISCARD_AFTER);
        options.put("extent.allowed-plugins", new ArrayList<String>());
        options.put("metrics", METRICS);

        // Default limit
        FaweLimit defaultLimit = new FaweLimit();
        if (!config.contains("limits.default")) {
            config.createSection("limits.default");
        }
        defaultLimit.load(config.getConfigurationSection("limits.default"), null, true);
        for (String key : config.getConfigurationSection("limits").getKeys(false)) {
            FaweLimit limit = new FaweLimit();
            limit.load(config.getConfigurationSection("limits." + key), defaultLimit, false);
        }
        for (final Entry<String, Object> node : options.entrySet()) {
            if (!config.contains(node.getKey())) {
                config.set(node.getKey(), node.getValue());
            }
        }
        FIX_ALL_LIGHTING = config.getBoolean("lighting.fix-all");
        ASYNC_LIGHTING = config.getBoolean("lighting.async");
        COMMAND_PROCESSOR = config.getBoolean("command-processor");
        MEM_FREE = config.getInt("max-memory-percent");
        REQUIRE_SELECTION = config.getBoolean("require-selection-in-mask");
        WE_BLACKLIST = config.getStringList("command-blacklist");
        ENABLE_HARD_LIMIT = config.getBoolean("crash-mitigation");
        REGION_RESTRICTIONS = config.getBoolean("region-restrictions");
        METRICS = config.getBoolean("metrics");
        COMPRESSION_LEVEL = config.getInt("history.compression-level", config.getBoolean("history.compress") ? 1 : 0);
        BUFFER_SIZE = config.getInt("history.buffer-size", BUFFER_SIZE);
        CHUNK_WAIT = config.getInt("history.chunk-wait-ms");
        ALLOCATE = config.getInt("queue.extra-time-ms");
        QUEUE_SIZE = config.getInt("queue.target-size");
        QUEUE_MAX_WAIT = config.getInt("queue.max-wait-ms");
        QUEUE_DISCARD_AFTER = config.getInt("queue.discard-after-ms");

        ALLOWED_3RDPARTY_EXTENTS = config.getStringList("extent.allowed-plugins");

        if (STORE_HISTORY_ON_DISK = config.getBoolean("history.use-disk")) {
            LocalSession.MAX_HISTORY_SIZE = Integer.MAX_VALUE;
        }

        try {
            config.save(file);
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }
}
