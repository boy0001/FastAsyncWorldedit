package com.boydti.fawe.logging.rollback;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.database.DBHandler;
import com.boydti.fawe.database.RollbackDatabase;
import com.boydti.fawe.object.changeset.DiskStorageHistory;
import com.sk89q.worldedit.world.World;
import java.io.IOException;
import java.util.UUID;

public class RollbackOptimizedHistory extends DiskStorageHistory {
    private final long time;

    private int minX;
    private int maxX;
    private int minY;
    private int maxY;
    private int minZ;
    private int maxZ;

    public RollbackOptimizedHistory(World world, UUID uuid, int index) {
        super(world, uuid, index);
        this.time = System.currentTimeMillis();
    }

    public RollbackOptimizedHistory(World world, UUID uuid) {
        super(world, uuid);
        this.time = System.currentTimeMillis();
    }

    public long getTime() {
        return time;
    }

    public int getMinX() {
        return minX;
    }

    public int getMaxX() {
        return maxX;
    }

    public int getMinY() {
        return minY;
    }

    public int getMaxY() {
        return maxY;
    }

    public int getMinZ() {
        return minZ;
    }

    public int getMaxZ() {
        return maxZ;
    }

    @Override
    public boolean flush() {
        if (super.flush()) {
            // Save to DB
            RollbackDatabase db = DBHandler.IMP.getDatabase(Fawe.imp().getWorldName(getWorld()));
            db.logEdit(this);
            return true;
        }
        return false;
    }

    @Override
    public void add(int x, int y, int z, int combinedFrom, int combinedTo) {
        super.add(x, y, z, combinedFrom, combinedTo);
        if (x < minX) {
            minX = x;
        } else if (x > maxX) {
            maxX = x;
        }
        if (y < minY) {
            minY = y;
        } else if (y > maxY) {
            maxY = y;
        }
        if (z < minZ) {
            minZ = z;
        } else if (z > maxZ) {
            maxZ = z;
        }
    }

    @Override
    public void writeHeader(int x, int y, int z) throws IOException {
        minX = x;
        maxX = x;
        minY = y;
        maxY = y;
        minZ = z;
        maxZ = z;
        super.writeHeader(x, y, z);
    }
}
