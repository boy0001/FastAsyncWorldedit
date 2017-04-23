package com.boydti.fawe.bukkit.favs;

import com.boydti.fawe.bukkit.BukkitCommand;
import com.boydti.fawe.object.FaweCommand;
import com.boydti.fawe.object.FawePlayer;
import com.thevoxelbox.voxelsniper.SnipeData;
import com.thevoxelbox.voxelsniper.Sniper;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class Favs extends JavaPlugin {
    @Override
    public void onEnable() {
        try {
            SnipeData.inject();
            Sniper.inject();
            // Forward the commands so //p and //d will work
            setupCommand("/p", new FaweCommand("voxelsniper.sniper") {
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
            setupCommand("/d", new FaweCommand("voxelsniper.sniper") {
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
        } catch (Throwable ignore) {}
    }

    public void setupCommand(final String label, final FaweCommand cmd) {
        this.getCommand(label).setExecutor(new BukkitCommand(cmd));
    }
}
