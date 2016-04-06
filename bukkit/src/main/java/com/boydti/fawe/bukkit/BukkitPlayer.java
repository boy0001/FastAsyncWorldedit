package com.boydti.fawe.bukkit;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.object.FaweLocation;
import com.boydti.fawe.object.FawePlayer;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class BukkitPlayer extends FawePlayer<Player> {

    public BukkitPlayer(final Player parent) {
        super(parent);
    }

    @Override
    public String getName() {
        return this.parent.getName();
    }

    @Override
    public UUID getUUID() {
        return this.parent.getUniqueId();
    }

    @Override
    public boolean hasPermission(final String perm) {
        return this.parent.hasPermission(perm);
    }

    @Override
    public void setPermission(final String perm, final boolean flag) {
        /*
         *  Permissions are used to managing WorldEdit region restrictions
         *   - The `/wea` command will give/remove the required bypass permission
         */
        if (Fawe.<FaweBukkit> imp().getVault() == null || Fawe.<FaweBukkit> imp().getVault().permission == null) {
            this.parent.addAttachment(Fawe.<FaweBukkit> imp()).setPermission("fawe.bypass", flag);
        } else if (flag) {
            if (!Fawe.<FaweBukkit> imp().getVault().permission.playerAdd(this.parent, perm)) {
                this.parent.addAttachment(Fawe.<FaweBukkit> imp()).setPermission("fawe.bypass", flag);
            }
        } else {
            if (!Fawe.<FaweBukkit> imp().getVault().permission.playerRemove(this.parent, perm)) {
                this.parent.addAttachment(Fawe.<FaweBukkit> imp()).setPermission("fawe.bypass", flag);
            }
        }
    }

    @Override
    public void sendMessage(final String message) {
        this.parent.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }

    @Override
    public void executeCommand(final String cmd) {
        Bukkit.getServer().dispatchCommand(this.parent, cmd);
    }

    @Override
    public FaweLocation getLocation() {
        final Location loc = this.parent.getLocation();
        return new FaweLocation(loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    @Override
    public com.sk89q.worldedit.entity.Player getPlayer() {
        return Fawe.<FaweBukkit> imp().getWorldEditPlugin().wrapPlayer(this.parent);
    }

}
