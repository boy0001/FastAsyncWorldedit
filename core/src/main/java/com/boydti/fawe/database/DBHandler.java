package com.boydti.fawe.database;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DBHandler {
    public final static DBHandler IMP = new DBHandler();

    private Map<String, RollbackDatabase> databases = new ConcurrentHashMap<>();
    
    public RollbackDatabase getDatabase(String world) {
        RollbackDatabase database = databases.get(world);
        if (database != null) {
            return database;
        }
        try {
            database = new RollbackDatabase(world);
            databases.put(world, database);
            return database;
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
    }
}
