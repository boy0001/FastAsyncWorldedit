package com.boydti.fawe.bukkit.regions;

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;
import com.bekvon.bukkit.residence.protection.CuboidArea;
import com.boydti.fawe.bukkit.FaweBukkit;
import com.boydti.fawe.object.FawePlayer;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

public class ResidenceFeature extends BukkitMaskManager implements Listener {
    FaweBukkit plugin;
    Plugin residence;

    public ResidenceFeature(final Plugin residencePlugin, final FaweBukkit p3) {
        super(residencePlugin.getName());
        this.residence = residencePlugin;
        this.plugin = p3;

    }

    @Override
    public BukkitMask getMask(final FawePlayer<Player> fp) {
        final Player player = fp.parent;
        final Location location = player.getLocation();
        final ClaimedResidence residence = Residence.getResidenceManager().getByLoc(location);
        if (residence != null) {
            if (residence.getPlayersInResidence().contains(player)) {
                final CuboidArea area = residence.getAreaArray()[0];
                final Location pos1 = area.getHighLoc();
                final Location pos2 = area.getLowLoc();
                return new BukkitMask(pos1, pos2) {
                    @Override
                    public String getName() {
                        return "RESIDENCE: " + residence.getName();
                    }
                };
            }
        }
        return null;
    }
}
