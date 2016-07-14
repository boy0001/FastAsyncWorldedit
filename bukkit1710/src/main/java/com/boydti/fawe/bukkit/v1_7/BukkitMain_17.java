package com.boydti.fawe.bukkit.v1_7;

import com.boydti.fawe.bukkit.ABukkitMain;
import com.boydti.fawe.bukkit.v0.BukkitQueue_0;

public class BukkitMain_17 extends ABukkitMain {

    @Override
    public BukkitQueue_0 getQueue(String world) {
        return new BukkitQueue17(world);
    }
}
