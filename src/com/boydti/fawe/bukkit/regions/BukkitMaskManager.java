package com.boydti.fawe.bukkit.regions;

import org.bukkit.entity.Player;

import com.boydti.fawe.regions.FaweMaskManager;

public abstract class BukkitMaskManager extends FaweMaskManager<Player> {
    
    public BukkitMaskManager(final String plugin) {
        super(plugin);
    }
}
