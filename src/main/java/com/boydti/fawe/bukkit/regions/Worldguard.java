package com.boydti.fawe.bukkit.regions;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import com.boydti.fawe.bukkit.FaweBukkit;
import com.boydti.fawe.object.FawePlayer;
import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

public class Worldguard extends BukkitMaskManager implements Listener {
    WorldGuardPlugin worldguard;
    FaweBukkit plugin;
    
    private WorldGuardPlugin getWorldGuard() {
        final Plugin plugin = Bukkit.getPluginManager().getPlugin("WorldGuard");
        
        // WorldGuard may not be loaded
        if ((plugin == null) || !(plugin instanceof WorldGuardPlugin)) {
            return null; // Maybe you want throw an exception instead
        }
        
        return (WorldGuardPlugin) plugin;
    }
    
    public Worldguard(final Plugin p2, final FaweBukkit p3) {
        super(p2.getName());
        worldguard = getWorldGuard();
        plugin = p3;
        
    }
    
    public ProtectedRegion isowner(final Player player, final Location location) {
        final com.sk89q.worldguard.LocalPlayer localplayer = worldguard.wrapPlayer(player);
        final RegionManager manager = worldguard.getRegionManager(player.getWorld());
        final ApplicableRegionSet regions = manager.getApplicableRegions(player.getLocation());
        for (final ProtectedRegion region : regions) {
            if (region.isOwner(localplayer)) {
                return region;
            } else if (region.getId().toLowerCase().equals(player.getName().toLowerCase())) {
                return region;
            } else if (region.getId().toLowerCase().contains(player.getName().toLowerCase() + "//")) {
                return region;
            } else if (region.isOwner("*")) {
                return region;
            }
        }
        return null;
    }
    
    public ProtectedRegion getregion(final Player player, final BlockVector location) {
        final com.sk89q.worldguard.LocalPlayer localplayer = worldguard.wrapPlayer(player);
        final ApplicableRegionSet regions = worldguard.getRegionManager(player.getWorld()).getApplicableRegions(location);
        for (final ProtectedRegion region : regions) {
            if (region.isOwner(localplayer)) {
                return region;
            } else if (region.getId().toLowerCase().equals(player.getName().toLowerCase())) {
                return region;
            } else if (region.getId().toLowerCase().contains(player.getName().toLowerCase() + "//")) {
                return region;
            } else if (region.isOwner("*")) {
                return region;
            }
        }
        return null;
    }
    
    @Override
    public FaweMask getMask(final FawePlayer<Player> fp) {
        final Player player = fp.parent;
        final Location location = player.getLocation();
        final ProtectedRegion myregion = isowner(player, location);
        if (myregion != null) {
            final Location pos1 = new Location(location.getWorld(), myregion.getMinimumPoint().getBlockX(), myregion.getMinimumPoint().getBlockY(), myregion.getMinimumPoint().getBlockZ());
            final Location pos2 = new Location(location.getWorld(), myregion.getMaximumPoint().getBlockX(), myregion.getMaximumPoint().getBlockY(), myregion.getMaximumPoint().getBlockZ());
            return new FaweMask(pos1, pos2) {
                @Override
                public String getName() {
                    return myregion.getId();
                }
            };
        } else {
            return null;
        }
        
    }
}
