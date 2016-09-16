package com.boydti.fawe.object;

/**
 * Created by Jesse on 4/5/2016.
 */
public class FaweLimit {
    public int MAX_ACTIONS = 0;
    public int MAX_CHANGES = 0;
    public int MAX_FAILS = 0;
    public int MAX_CHECKS = 0;
    public int MAX_ITERATIONS = 0;
    public int MAX_BLOCKSTATES = 0;
    public int MAX_ENTITIES = 0;
    public int MAX_HISTORY = 0;


    public static FaweLimit MAX;
    static {
        MAX = new FaweLimit() {
            @Override
            public boolean MAX_CHANGES() {
                return true;
            }
            @Override
            public boolean MAX_BLOCKSTATES() {
                return true;
            }
            @Override
            public boolean MAX_CHECKS() {
                return true;
            }
            @Override
            public boolean MAX_ENTITIES() {
                return true;
            }
            @Override
            public boolean MAX_FAILS() {
                return true;
            }
            @Override
            public boolean MAX_ITERATIONS() {
                return true;
            }
        };
        MAX.MAX_ACTIONS = Integer.MAX_VALUE;
        MAX.MAX_CHANGES = Integer.MAX_VALUE;
        MAX.MAX_FAILS = Integer.MAX_VALUE;
        MAX.MAX_CHECKS = Integer.MAX_VALUE;
        MAX.MAX_ITERATIONS = Integer.MAX_VALUE;
        MAX.MAX_BLOCKSTATES = Integer.MAX_VALUE;
        MAX.MAX_ENTITIES = Integer.MAX_VALUE;
        MAX.MAX_HISTORY = Integer.MAX_VALUE;
    }

    public boolean MAX_CHANGES() {
        return MAX_CHANGES-- > 0;
    }

    public boolean MAX_FAILS() {
        return MAX_FAILS-- > 0;
    }

    public boolean MAX_CHECKS() {
        return MAX_CHECKS-- > 0;
    }

    public boolean MAX_ITERATIONS() {
        return MAX_ITERATIONS-- > 0;
    }

    public boolean MAX_BLOCKSTATES() {
        return MAX_BLOCKSTATES-- > 0;
    }

    public boolean MAX_ENTITIES() {
        return MAX_ENTITIES-- > 0;
    }


    public FaweLimit copy() {
        FaweLimit limit = new FaweLimit();
        limit.MAX_ACTIONS = MAX_ACTIONS;
        limit.MAX_CHANGES = MAX_CHANGES;
        limit.MAX_BLOCKSTATES = MAX_BLOCKSTATES;
        limit.MAX_CHECKS = MAX_CHECKS;
        limit.MAX_ENTITIES = MAX_ENTITIES;
        limit.MAX_FAILS = MAX_FAILS;
        limit.MAX_ITERATIONS = MAX_ITERATIONS;
        limit.MAX_HISTORY = MAX_HISTORY;
        return limit;
    }

    @Override
    public String toString() {
        return MAX_CHANGES + "";
    }
}
