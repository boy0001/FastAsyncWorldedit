package com.boydti.fawe.bukkit.v1_9;

import com.boydti.fawe.bukkit.ABukkitMain;
import com.boydti.fawe.bukkit.v0.BukkitQueue_0;
import com.boydti.fawe.object.FaweQueue;
import com.sk89q.worldedit.world.World;

public class BukkitMain_19 extends ABukkitMain {
    @Override
    public BukkitQueue_0 getQueue(World world) {
        return new BukkitQueue_1_9_R1(world);
    }

    @Override
    public FaweQueue getQueue(String world) {
        return new BukkitQueue_1_9_R1(world);
    }
}
