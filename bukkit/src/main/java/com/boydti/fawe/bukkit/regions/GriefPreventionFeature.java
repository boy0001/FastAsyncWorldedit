package com.boydti.fawe.bukkit.regions;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import com.boydti.fawe.bukkit.FaweBukkit;
import com.boydti.fawe.object.FawePlayer;

public class GriefPreventionFeature extends BukkitMaskManager implements Listener {
    FaweBukkit plugin;
    Plugin griefprevention;

    public GriefPreventionFeature(final Plugin griefpreventionPlugin, final FaweBukkit p3) {
        super(griefpreventionPlugin.getName());
        this.griefprevention = griefpreventionPlugin;
        this.plugin = p3;
    }

    @Override
    public BukkitMask getMask(final FawePlayer<Player> fp) {
        final Player player = fp.parent;
        final Location location = player.getLocation();
        final Claim claim = GriefPrevention.instance.dataStore.getClaimAt(location, true, null);
        if (claim != null) {
            final String uuid = player.getUniqueId().toString();
            if (claim.getOwnerName().equalsIgnoreCase(player.getName()) || claim.getOwnerName().equals(uuid)) {
                claim.getGreaterBoundaryCorner().getBlockX();
                final Location pos1 = new Location(location.getWorld(), claim.getLesserBoundaryCorner().getBlockX(), 0, claim.getLesserBoundaryCorner().getBlockZ());
                final Location pos2 = new Location(location.getWorld(), claim.getGreaterBoundaryCorner().getBlockX(), 256, claim.getGreaterBoundaryCorner().getBlockZ());
                return new BukkitMask(pos1, pos2) {
                    @Override
                    public String getName() {
                        return "CLAIM:" + claim.toString();
                    }
                };
            }
        }
        return null;

    }
}
