package com.boydti.fawe.bukkit.regions;

import com.boydti.fawe.bukkit.FaweBukkit;
import com.boydti.fawe.object.FawePlayer;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

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
        this.worldguard = this.getWorldGuard();
        this.plugin = p3;

    }

    public ProtectedRegion getRegion(final Player player, final Location loc) {
        final com.sk89q.worldguard.LocalPlayer localplayer = this.worldguard.wrapPlayer(player);
        RegionManager manager = this.worldguard.getRegionManager(player.getWorld());
        if (manager == null) {
            if (this.worldguard.getGlobalStateManager().get(player.getWorld()).useRegions) {
                System.out.println("Region capability is not enabled for WorldGuard.");
            } else {
                System.out.println("WorldGuard is not enabled for that world.");
            }
            return null;
        }
        final ProtectedRegion global = manager.getRegion("__global__");
        if (global != null && isAllowed(localplayer, global)) {
            return global;
        }
        final ApplicableRegionSet regions = manager.getApplicableRegions(player.getLocation());
        for (final ProtectedRegion region : regions) {
            if (isAllowed(localplayer, region)) {
                return region;
            }
        }
        return null;
    }

    public boolean isAllowed(LocalPlayer localplayer, ProtectedRegion region) {
        if (region.isOwner(localplayer) || region.isOwner(localplayer.getName())) {
            return true;
        } else if (region.getId().toLowerCase().equals(localplayer.getName().toLowerCase())) {
            return true;
        } else if (region.getId().toLowerCase().contains(localplayer.getName().toLowerCase() + "//")) {
            return true;
        } else if (region.isOwner("*")) {
            return true;
        }
        if (localplayer.hasPermission("fawe.worldguard.member")) {
            if (region.isMember(localplayer) || region.isMember(localplayer.getName())) {
                return true;
            } else if (region.isMember("*")) {
                return true;
            }
        }
        return false;
    }

    @Override
    public BukkitMask getMask(final FawePlayer<Player> fp) {
        final Player player = fp.parent;
        final Location location = player.getLocation();
        final ProtectedRegion myregion = this.getRegion(player, location);
        if (myregion != null) {
            final Location pos1;
            final Location pos2;
            if (myregion.getId().equals("__global__")) {
                pos1 = new Location(location.getWorld(), Integer.MIN_VALUE, 0, Integer.MIN_VALUE);
                pos2 = new Location(location.getWorld(), Integer.MAX_VALUE, 255, Integer.MAX_VALUE);
            } else {
                pos1 = new Location(location.getWorld(), myregion.getMinimumPoint().getBlockX(), myregion.getMinimumPoint().getBlockY(), myregion.getMinimumPoint().getBlockZ());
                pos2 = new Location(location.getWorld(), myregion.getMaximumPoint().getBlockX(), myregion.getMaximumPoint().getBlockY(), myregion.getMaximumPoint().getBlockZ());
            }
            return new BukkitMask(pos1, pos2) {
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
