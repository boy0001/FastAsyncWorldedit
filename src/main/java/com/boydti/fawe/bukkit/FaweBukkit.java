package com.boydti.fawe.bukkit;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.IFawe;
import com.boydti.fawe.bukkit.regions.FactionsFeature;
import com.boydti.fawe.bukkit.regions.FactionsUUIDFeature;
import com.boydti.fawe.bukkit.regions.GriefPreventionFeature;
import com.boydti.fawe.bukkit.regions.PlotMeFeature;
import com.boydti.fawe.bukkit.regions.PlotSquaredFeature;
import com.boydti.fawe.bukkit.regions.PreciousStonesFeature;
import com.boydti.fawe.bukkit.regions.ResidenceFeature;
import com.boydti.fawe.bukkit.regions.TownyFeature;
import com.boydti.fawe.bukkit.regions.Worldguard;
import com.boydti.fawe.bukkit.v1_8.BukkitEditSessionWrapper_1_8;
import com.boydti.fawe.bukkit.v1_8.BukkitQueue_1_8;
import com.boydti.fawe.object.EditSessionWrapper;
import com.boydti.fawe.object.FaweCommand;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.regions.FaweMaskManager;
import com.boydti.fawe.util.FaweQueue;
import com.boydti.fawe.util.StringMan;
import com.boydti.fawe.util.TaskManager;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;

public class FaweBukkit extends JavaPlugin implements IFawe {
    
    private VaultUtil vault;
    private WorldEditPlugin worldedit;
    
    public VaultUtil getVault() {
        return vault;
    }
    
    public WorldEditPlugin getWorldEditPlugin() {
        if (worldedit == null) {
            worldedit = (WorldEditPlugin) Bukkit.getPluginManager().getPlugin("WorldEdit");
        }
        return worldedit;
    }

    @Override
    public void onEnable() {
        try {
            Fawe.set(this);
            try {
                Class<?> clazz = Class.forName("org.spigotmc.AsyncCatcher");
                Field field = clazz.getDeclaredField("enabled");
                field.set(null, false);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
            getServer().shutdown();
        }
    }

    @Override
    public void debug(final String s) {
        getLogger().info(ChatColor.translateAlternateColorCodes('&', s));
    }
    
    @Override
    public File getDirectory() {
        return getDataFolder();
    }
    
    @Override
    public void setupCommand(final String label, final FaweCommand cmd) {
        getCommand(label).setExecutor(new BukkitCommand(cmd));
    }
    
    @Override
    public FawePlayer<Player> wrap(final Object obj) {
        if (obj.getClass() == String.class) {
            return new BukkitPlayer(Bukkit.getPlayer((String) obj));
        } else if (obj instanceof Player) {
            return new BukkitPlayer((Player) obj);
        } else {
            return null;
        }
    }
    
    @Override
    public void setupWEListener() {
        getServer().getPluginManager().registerEvents(new WEListener(), this);
    }
    
    @Override
    public void setupVault() {
        try {
            vault = new VaultUtil();
        } catch (final Throwable e) {
            debug("&cPlease install vault!");
        }
    }
    
    @Override
    public TaskManager getTaskManager() {
        return new BukkitTaskMan(this);
    }
    
    @Override
    public int[] getVersion() {
        try {
            final int[] version = new int[3];
            final String[] split = Bukkit.getBukkitVersion().split("-")[0].split("\\.");
            version[0] = Integer.parseInt(split[0]);
            version[1] = Integer.parseInt(split[1]);
            if (split.length == 3) {
                version[2] = Integer.parseInt(split[2]);
            }
            return version;
        } catch (final Exception e) {
            e.printStackTrace();
            debug(StringMan.getString(Bukkit.getBukkitVersion()));
            debug(StringMan.getString(Bukkit.getBukkitVersion().split("-")[0].split("\\.")));
            return new int[] { Integer.MAX_VALUE, 0, 0 };
        }
    }
    
    @Override
    public FaweQueue getQueue() {
        return new BukkitQueue_1_8();
    }
    
    @Override
    public EditSessionWrapper getEditSessionWrapper(final EditSession session) {
        return new BukkitEditSessionWrapper_1_8(session);
    }
    
    @Override
    public Collection<FaweMaskManager> getMaskManagers() {
        final Plugin worldguardPlugin = Bukkit.getServer().getPluginManager().getPlugin("WorldGuard");
        final ArrayList<FaweMaskManager> managers = new ArrayList<>();
        if ((worldguardPlugin != null) && worldguardPlugin.isEnabled()) {
            managers.add(new Worldguard(worldguardPlugin, this));
            Fawe.debug("Plugin 'WorldGuard' found. Using it now.");
        } else {
            Fawe.debug("Plugin 'WorldGuard' not found. Worldguard features disabled.");
        }
        final Plugin plotmePlugin = Bukkit.getServer().getPluginManager().getPlugin("PlotMe");
        if ((plotmePlugin != null) && plotmePlugin.isEnabled()) {
            managers.add(new PlotMeFeature(plotmePlugin, this));
            Fawe.debug("Plugin 'PlotMe' found. Using it now.");
        } else {
            Fawe.debug("Plugin 'PlotMe' not found. PlotMe features disabled.");
        }
        final Plugin townyPlugin = Bukkit.getServer().getPluginManager().getPlugin("Towny");
        if ((townyPlugin != null) && townyPlugin.isEnabled()) {
            managers.add(new TownyFeature(townyPlugin, this));
            Fawe.debug("Plugin 'Towny' found. Using it now.");
        } else {
            Fawe.debug("Plugin 'Towny' not found. Towny features disabled.");
        }
        final Plugin factionsPlugin = Bukkit.getServer().getPluginManager().getPlugin("Factions");
        if ((factionsPlugin != null) && factionsPlugin.isEnabled()) {
            try {
                managers.add(new FactionsFeature(factionsPlugin, this));
                Fawe.debug("Plugin 'Factions' found. Using it now.");
            } catch (final Throwable e) {
                managers.add(new FactionsUUIDFeature(factionsPlugin, this));
                Fawe.debug("Plugin 'FactionsUUID' found. Using it now.");
            }
        } else {
            Fawe.debug("Plugin 'Factions' not found. Factions features disabled.");
        }
        final Plugin residencePlugin = Bukkit.getServer().getPluginManager().getPlugin("Residence");
        if ((residencePlugin != null) && residencePlugin.isEnabled()) {
            managers.add(new ResidenceFeature(residencePlugin, this));
            Fawe.debug("Plugin 'Residence' found. Using it now.");
        } else {
            Fawe.debug("Plugin 'Residence' not found. Factions features disabled.");
        }
        final Plugin griefpreventionPlugin = Bukkit.getServer().getPluginManager().getPlugin("GriefPrevention");
        if ((griefpreventionPlugin != null) && griefpreventionPlugin.isEnabled()) {
            managers.add(new GriefPreventionFeature(griefpreventionPlugin, this));
            Fawe.debug("Plugin 'GriefPrevention' found. Using it now.");
        } else {
            Fawe.debug("Plugin 'GriefPrevention' not found. GriefPrevention features disabled.");
        }
        final Plugin plotsquaredPlugin = Bukkit.getServer().getPluginManager().getPlugin("PlotSquared");
        if ((plotsquaredPlugin != null) && plotsquaredPlugin.isEnabled()) {
            managers.add(new PlotSquaredFeature(plotsquaredPlugin, this));
            Fawe.debug("Plugin 'PlotSquared' found. Using it now.");
        } else {
            Fawe.debug("Plugin 'PlotSquared' not found. PlotSquared features disabled.");
        }
        final Plugin preciousstonesPlugin = Bukkit.getServer().getPluginManager().getPlugin("PreciousStones");
        if ((preciousstonesPlugin != null) && preciousstonesPlugin.isEnabled()) {
            managers.add(new PreciousStonesFeature(preciousstonesPlugin, this));
            Fawe.debug("Plugin 'PreciousStones' found. Using it now.");
        } else {
            Fawe.debug("Plugin 'PreciousStones' not found. PreciousStones features disabled.");
        }
        return managers;
    }
}
