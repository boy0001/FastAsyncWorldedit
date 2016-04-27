package com.boydti.fawe.object;

import com.boydti.fawe.configuration.ConfigurationSection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Jesse on 4/5/2016.
 */
public class FaweLimit {
    public int MAX_CHANGES = 50000000;
    public int MAX_FAILS = 50000000;
    public int MAX_CHECKS = 50000000;
    public int MAX_ITERATIONS = 1000;
    public int MAX_BLOCKSTATES = 1337;
    public int MAX_ENTITIES = 1337;

    public static FaweLimit MAX;
    static {
        MAX = new FaweLimit();
        MAX.MAX_CHANGES = Integer.MAX_VALUE;
        MAX.MAX_FAILS = Integer.MAX_VALUE;
        MAX.MAX_CHECKS = Integer.MAX_VALUE;
        MAX.MAX_ITERATIONS = Integer.MAX_VALUE;
        MAX.MAX_BLOCKSTATES = Integer.MAX_VALUE;
        MAX.MAX_ENTITIES = Integer.MAX_VALUE;
    }

    public boolean load(ConfigurationSection section, FaweLimit defaultLimit, boolean save) {
        this.MAX_CHANGES = section.getInt("max-changes", defaultLimit == null ? MAX_CHANGES : defaultLimit.MAX_CHANGES);
        this.MAX_FAILS = section.getInt("max-fails", defaultLimit == null ? MAX_FAILS : defaultLimit.MAX_FAILS);
        this.MAX_CHECKS = section.getInt("max-checks", defaultLimit == null ? MAX_CHECKS : defaultLimit.MAX_CHECKS);
        this.MAX_ITERATIONS = section.getInt("max-iterations", defaultLimit == null ? MAX_ITERATIONS : defaultLimit.MAX_ITERATIONS);
        this.MAX_BLOCKSTATES = section.getInt("max-blockstates", defaultLimit == null ? MAX_BLOCKSTATES : defaultLimit.MAX_BLOCKSTATES);
        this.MAX_ENTITIES = section.getInt("max-entities", defaultLimit == null ? MAX_ENTITIES : defaultLimit.MAX_ENTITIES);
        boolean changed = false;
        if (save) {
            HashMap<String, Object> options = new HashMap<>();
            options.put("max-changes", MAX_CHANGES);
            options.put("max-fails", MAX_FAILS);
            options.put("max-checks", MAX_CHECKS);
            options.put("max-iterations", MAX_ITERATIONS);
            options.put("max-blockstates", MAX_BLOCKSTATES);
            options.put("max-entities", MAX_ENTITIES);
            for (Map.Entry<String, Object> entry : options.entrySet()) {
                if (!section.contains(entry.getKey())) {
                    section.set(entry.getKey(), entry.getValue());
                    changed = true;
                }
            }
        }
        return changed;
    }

    public FaweLimit copy() {
        FaweLimit limit = new FaweLimit();
        limit.MAX_CHANGES = MAX_CHANGES;
        limit.MAX_BLOCKSTATES = MAX_BLOCKSTATES;
        limit.MAX_CHECKS = MAX_CHECKS;
        limit.MAX_ENTITIES = MAX_ENTITIES;
        limit.MAX_FAILS = MAX_FAILS;
        limit.MAX_ITERATIONS = MAX_ITERATIONS;
        return limit;
    }

    @Override
    public String toString() {
        return MAX_CHANGES + "";
    }
}
