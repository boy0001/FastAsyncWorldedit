package com.boydti.fawe.bukkit;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.object.FaweCommand;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.FaweQueue;
import com.thevoxelbox.voxelsniper.SnipeData;
import com.thevoxelbox.voxelsniper.Sniper;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public abstract class ABukkitMain extends JavaPlugin {

    @Override
    public void onEnable() {
        FaweBukkit imp = new FaweBukkit(this);
        try {
            SnipeData.inject();
            Sniper.inject();
            // Forward the commands so //p and //d will work
            imp.setupCommand("/p", new FaweCommand("voxelsniper.sniper") {
                @Override
                public boolean execute(FawePlayer fp, String... args) {
                    Player player = (Player) fp.parent;
                    return (Bukkit.getPluginManager().getPlugin("VoxelSniper")).onCommand(player, new Command("p") {
                        @Override
                        public boolean execute(CommandSender sender, String commandLabel, String[] args) {
                            return false;
                        }
                    }, null, args);

                }
            });
            imp.setupCommand("/d", new FaweCommand("voxelsniper.sniper") {
                @Override
                public boolean execute(FawePlayer fp, String... args) {
                    Player player = (Player) fp.parent;
                    return (Bukkit.getPluginManager().getPlugin("VoxelSniper")).onCommand(player, new Command("d") {
                        @Override
                        public boolean execute(CommandSender sender, String commandLabel, String[] args) {
                            return false;
                        }
                    }, null, args);

                }
            });
            Fawe.debug("Injected VoxelSniper classes");
        } catch (Throwable ignore) {}
    }

    public abstract FaweQueue getQueue(String world);
}