package com.boydti.fawe.bukkit.regions;

import com.boydti.fawe.bukkit.FaweBukkit;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.regions.FaweMask;
import com.sk89q.worldedit.BlockVector;
import java.util.List;
import net.sacredlabyrinth.Phaed.PreciousStones.PreciousStones;
import net.sacredlabyrinth.Phaed.PreciousStones.field.Field;
import net.sacredlabyrinth.Phaed.PreciousStones.field.FieldFlag;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

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
        if (fields.isEmpty()) {
            return null;
        }
        String name = player.getName();
        boolean member = fp.hasPermission("fawe.preciousstones.member");
        for (final Field myField : fields) {
            if (myField.isOwner(name) || (member && myField.getAllowed().contains(player.getName()))) {
                BlockVector pos1 = new BlockVector(myField.getMinx(), myField.getMiny(), myField.getMinz());
                BlockVector pos2 = new BlockVector(myField.getMaxx(), myField.getMaxy(), myField.getMaxz());
                return new FaweMask(pos1, pos2, "FIELD: " + myField);
            }
        }
        return null;
    }
}
