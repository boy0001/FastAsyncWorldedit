package com.boydti.fawe.bukkit.regions;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import com.boydti.fawe.bukkit.FaweBukkit;
import com.boydti.fawe.object.FawePlayer;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.object.PlayerCache;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyUniverse;
import com.palmergames.bukkit.towny.object.WorldCoord;

public class TownyFeature extends BukkitMaskManager implements Listener {
    FaweBukkit plugin;
    Plugin towny;

    public TownyFeature(final Plugin townyPlugin, final FaweBukkit p3) {
        super(townyPlugin.getName());
        this.towny = townyPlugin;
        this.plugin = p3;
    }

    @Override
    public FaweMask getMask(final FawePlayer<Player> fp) {
        final Player player = fp.parent;
        final Location location = player.getLocation();
        try {
            final PlayerCache cache = ((Towny) this.towny).getCache(player);
            final WorldCoord mycoord = cache.getLastTownBlock();
            if (mycoord == null) {
                return null;
            } else {
                final TownBlock myplot = mycoord.getTownBlock();
                if (myplot == null) {
                    return null;
                } else {
                    boolean isMember = false;
                    try {
                        if (myplot.getResident().getName().equals(player.getName())) {
                            isMember = true;
                        }
                    } catch (final Exception e) {

                    }
                    if (!isMember) {
                        if (player.hasPermission("fawe.towny.*")) {
                            isMember = true;
                        } else if (myplot.getTown().isMayor(TownyUniverse.getDataSource().getResident(player.getName()))) {
                            isMember = true;
                        }
                    }
                    if (isMember) {
                        final Chunk chunk = location.getChunk();
                        final Location pos1 = new Location(location.getWorld(), chunk.getX() * 16, 0, chunk.getZ() * 16);
                        final Location pos2 = new Location(location.getWorld(), (chunk.getX() * 16) + 15, 156, (chunk.getZ() * 16) + 15);
                        return new FaweMask(pos1, pos2) {
                            @Override
                            public String getName() {
                                return "PLOT:" + location.getChunk().getX() + "," + location.getChunk().getZ();
                            }
                        };
                    }
                }
            }
        } catch (final Exception e) {}
        return null;
    }
}
