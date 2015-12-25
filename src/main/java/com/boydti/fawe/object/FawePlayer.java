package com.boydti.fawe.object;

import java.util.HashSet;
import java.util.UUID;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.util.WEManager;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.regions.RegionSelector;
import com.sk89q.worldedit.regions.selector.CuboidRegionSelector;

public abstract class FawePlayer<T> {
    
    public final T parent;
    private LocalSession session;
    
    public static <T> FawePlayer<T> wrap(final Object obj) {
        return Fawe.imp().wrap(obj);
    }
    
    public FawePlayer(final T parent) {
        this.parent = parent;
    }
    
    public abstract String getName();
    
    public abstract UUID getUUID();
    
    public abstract boolean hasPermission(final String perm);
    
    public abstract void setPermission(final String perm, final boolean flag);
    
    public abstract void sendMessage(final String message);
    
    public abstract void executeCommand(final String substring);
    
    public abstract FaweLocation getLocation();
    
    public abstract Player getPlayer();
    
    public Region getSelection() {
        try {
            return getSession().getSelection(getPlayer().getWorld());
        } catch (IncompleteRegionException e) {
            return null;
        }
    }

    public LocalSession getSession() {
        return session != null ? session : Fawe.get().getWorldEdit().getSession(getPlayer());
    }
    
    public HashSet<RegionWrapper> getCurrentRegions() {
        return WEManager.IMP.getMask(this);
    }
    
    public void setSelection(RegionWrapper region) {
        Player player = getPlayer();
        RegionSelector selector = new CuboidRegionSelector(player.getWorld(), region.getBottomVector(), region.getTopVector());
        getSession().setRegionSelector(player.getWorld(), selector);
    }

    public RegionWrapper getLargestRegion() {
        int area = 0;
        RegionWrapper max = null;
        for (RegionWrapper region : getCurrentRegions()) {
            int tmp = (region.maxX - region.minX) * (region.maxZ - region.minZ);
            if (tmp > area) {
                area = tmp;
                max = region;
            }
        }
        return max;
    }
    
    @Override
    public String toString() {
        return getName();
    }
    
    public boolean hasWorldEditBypass() {
        return hasPermission("fawe.bypass");
    }
}
