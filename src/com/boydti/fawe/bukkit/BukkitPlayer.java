package com.boydti.fawe.bukkit;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.object.FaweLocation;
import com.boydti.fawe.object.FawePlayer;

public class BukkitPlayer extends FawePlayer<Player> {
    
    public BukkitPlayer(final Player parent) {
        super(parent);
    }
    
    @Override
    public String getName() {
        return parent.getName();
    }
    
    @Override
    public UUID getUUID() {
        return parent.getUniqueId();
    }
    
    @Override
    public boolean hasPermission(final String perm) {
        return parent.hasPermission(perm);
    }
    
    @Override
    public void setPermission(final String perm, final boolean flag) {
        if (flag) {
            Fawe.<FaweBukkit> imp().getVault().permission.playerAdd(parent, perm);
        } else {
            Fawe.<FaweBukkit> imp().getVault().permission.playerRemove(parent, perm);
        }
    }
    
    @Override
    public void sendMessage(final String message) {
        parent.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }
    
    @Override
    public void executeCommand(final String cmd) {
        Bukkit.getServer().dispatchCommand(parent, cmd);
    }
    
    @Override
    public FaweLocation getLocation() {
        Location loc = parent.getLocation();
        return new FaweLocation(loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }
    
    @Override
    public com.sk89q.worldedit.entity.Player getPlayer() {
        return Fawe.<FaweBukkit> imp().getWorldEditPlugin().wrapPlayer(parent);
    }
    
}
