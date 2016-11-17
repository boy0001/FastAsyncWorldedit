package com.boydti.fawe.bukkit.v1_11;

import com.boydti.fawe.bukkit.ABukkitMain;
import com.boydti.fawe.bukkit.v0.BukkitQueue_0;
import com.boydti.fawe.object.FaweQueue;
import com.sk89q.worldedit.world.World;

public class BukkitMain_111 extends ABukkitMain {
    @Override
    public BukkitQueue_0 getQueue(World world) {
        return new com.boydti.fawe.bukkit.v1_11.BukkitQueue_1_11(world);
    }

    @Override
    public FaweQueue getQueue(String world) {
        return new com.boydti.fawe.bukkit.v1_11.BukkitQueue_1_11(world);
    }

    @Override
    public void onEnable() {
        super.onEnable();
    }
}