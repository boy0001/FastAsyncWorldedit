package com.boydti.fawe.bukkit.regions;

import java.util.List;

import net.sacredlabyrinth.Phaed.PreciousStones.FieldFlag;
import net.sacredlabyrinth.Phaed.PreciousStones.PreciousStones;
import net.sacredlabyrinth.Phaed.PreciousStones.vectors.Field;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import com.boydti.fawe.bukkit.FaweBukkit;
import com.boydti.fawe.object.FawePlayer;

public class PreciousStonesFeature extends BukkitMaskManager implements Listener {
    FaweBukkit plugin;
    Plugin preciousstones;

    public PreciousStonesFeature(final Plugin preciousstonesPlugin, final FaweBukkit p3) {
        super(preciousstonesPlugin.getName());
        this.preciousstones = preciousstonesPlugin;
        this.plugin = p3;

    }

    @Override
    public FaweMask getMask(final FawePlayer<Player> fp) {
        final Player player = fp.parent;
        final Location location = player.getLocation();
        final List<Field> fields = PreciousStones.API().getFieldsProtectingArea(FieldFlag.PLOT, location);
        for (final Field myfield : fields) {
            if (myfield.getOwner().equalsIgnoreCase(player.getName()) || (myfield.getAllowed().contains(player.getName()))) {
                final Location pos1 = new Location(location.getWorld(), myfield.getCorners().get(0).getBlockX(), myfield.getCorners().get(0).getBlockY(), myfield.getCorners().get(0).getBlockZ());
                final Location pos2 = new Location(location.getWorld(), myfield.getCorners().get(1).getBlockX(), myfield.getCorners().get(1).getBlockY(), myfield.getCorners().get(1).getBlockZ());
                return new FaweMask(pos1, pos2) {
                    @Override
                    public String getName() {
                        return "FIELD:" + myfield.toString();
                    }
                };
            }
        }
        return null;
    }
}
