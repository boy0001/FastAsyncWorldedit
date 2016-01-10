package com.boydti.fawe.bukkit.regions;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import com.boydti.fawe.bukkit.FaweBukkit;
import com.boydti.fawe.object.FawePlayer;
import com.massivecraft.factions.entity.BoardColl;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.massivecore.ps.PS;

public class FactionsFeature extends BukkitMaskManager implements Listener {
    FaweBukkit plugin;
    Plugin factions;
    
    public FactionsFeature(final Plugin factionsPlugin, final FaweBukkit p3) {
        super(factionsPlugin.getName());
        factions = factionsPlugin;
        plugin = p3;
        BoardColl.get();
    }
    
    @Override
    public FaweMask getMask(final FawePlayer<Player> fp) {
        final Player player = fp.parent;
        final Location loc = player.getLocation();
        PS ps = PS.valueOf(loc);
        final Faction fac = BoardColl.get().getFactionAt(ps);
        if (fac != null) {
            if (fac.getOnlinePlayers().contains(player)) {
                if (fac.getComparisonName().equals("wilderness") == false) {
                    final Chunk chunk = loc.getChunk();
                    final Location pos1 = new Location(loc.getWorld(), chunk.getX() * 16, 0, chunk.getZ() * 16);
                    final Location pos2 = new Location(loc.getWorld(), (chunk.getX() * 16) + 15, 156, (chunk.getZ() * 16) + 15);
                    return new FaweMask(pos1, pos2) {
                        @Override
                        public String getName() {
                            return "CHUNK:" + loc.getChunk().getX() + "," + loc.getChunk().getZ();
                        }
                    };
                }
            }
        }
        return null;
    }
}
