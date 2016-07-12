package com.boydti.fawe.bukkit.v1_9;

import com.boydti.fawe.bukkit.ABukkitMain;
import com.boydti.fawe.bukkit.v0.BukkitQueue_0;

public class BukkitMain_19 extends ABukkitMain {
    @Override
    public BukkitQueue_0 getQueue(String world) {
        return new BukkitQueue_1_9_R1(world);
    }
}
