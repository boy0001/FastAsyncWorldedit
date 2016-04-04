package com.boydti.fawe.config;

import com.boydti.fawe.configuration.file.YamlConfiguration;
import com.sk89q.worldedit.LocalSession;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class Settings {

    public static int MAX_BLOCKSTATES = 1337;
    public static int MAX_ENTITIES = 1337;
    public static long WE_MAX_ITERATIONS = 1000;
    public static long WE_MAX_VOLUME = 50000000;
    public static boolean REQUIRE_SELECTION = false;
    public static boolean FIX_ALL_LIGHTING = true;
    public static boolean COMMAND_PROCESSOR = false;
    public static List<String> WE_BLACKLIST = Arrays.asList("cs", ".s", "restore", "snapshot", "delchunks", "listchunks");
    public static long MEM_FREE = 95;
    public static boolean ENABLE_HARD_LIMIT = true;
    public static boolean STORE_HISTORY_ON_DISK = false;
    public static int COMPRESSION_LEVEL = 0;
    public static int BUFFER_SIZE = 531441;
    public static boolean METRICS = true;

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

        final Map<String, Object> options = new HashMap<>();
        options.put("max-blockstates", MAX_BLOCKSTATES);
        options.put("max-entities", MAX_ENTITIES);
        options.put("max-iterations", WE_MAX_ITERATIONS);
        options.put("max-volume", WE_MAX_VOLUME);
        options.put("require-selection-in-mask", REQUIRE_SELECTION);
        options.put("command-blacklist", WE_BLACKLIST);
        options.put("command-processor", COMMAND_PROCESSOR);
        options.put("max-memory-percent", MEM_FREE);
        options.put("crash-mitigation", ENABLE_HARD_LIMIT);
        options.put("fix-all-lighting", FIX_ALL_LIGHTING);
        options.put("history.use-disk", STORE_HISTORY_ON_DISK);
        options.put("history.compress", false);
        options.put("metrics", METRICS);

        for (final Entry<String, Object> node : options.entrySet()) {
            if (!config.contains(node.getKey())) {
                config.set(node.getKey(), node.getValue());
            }
        }
        FIX_ALL_LIGHTING = config.getBoolean("fix-all-lighting");
        COMMAND_PROCESSOR = config.getBoolean("command-processor");
        MAX_BLOCKSTATES = config.getInt("max-blockstates");
        MAX_ENTITIES = config.getInt("max-entities");
        WE_MAX_ITERATIONS = config.getInt("max-iterations");
        WE_MAX_VOLUME = config.getInt("max-volume");
        MEM_FREE = config.getInt("max-memory-percent");
        REQUIRE_SELECTION = config.getBoolean("require-selection-in-mask");
        WE_BLACKLIST = config.getStringList("command-blacklist");
        ENABLE_HARD_LIMIT = config.getBoolean("crash-mitigation");
        METRICS = config.getBoolean("metrics");
        COMPRESSION_LEVEL = config.getInt("history.compression-level", config.getBoolean("history.compress") ? 1 : 0);
        BUFFER_SIZE = config.getInt("history.buffer-size", 59049);
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
