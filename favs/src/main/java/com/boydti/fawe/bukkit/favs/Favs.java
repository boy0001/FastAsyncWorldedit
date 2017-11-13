package com.boydti.fawe.bukkit.favs;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.bukkit.BukkitCommand;
import com.boydti.fawe.object.FaweCommand;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.util.Jars;
import com.boydti.fawe.util.MainUtil;
import com.thevoxelbox.voxelsniper.RangeBlockHelper;
import com.thevoxelbox.voxelsniper.SnipeData;
import com.thevoxelbox.voxelsniper.Sniper;
import com.thevoxelbox.voxelsniper.brush.WarpBrush;
import com.thevoxelbox.voxelsniper.brush.perform.PerformBrush;
import com.thevoxelbox.voxelsniper.command.VoxelVoxelCommand;
import com.thevoxelbox.voxelsniper.event.SniperBrushChangedEvent;
import com.thevoxelbox.voxelsniper.event.SniperMaterialChangedEvent;
import java.io.File;
import java.io.FileOutputStream;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class Favs extends JavaPlugin {

    @Override
    public void onEnable() {
        try {
            if (Bukkit.getPluginManager().getPlugin("VoxelSniper") == null) {
                try {
                    File thisFile = MainUtil.getJarFile();
                    String thisName = thisFile.getName().toLowerCase();
                    File output = null;
                    for (File file : getDataFolder().getParentFile().listFiles()) {
                        String name = file.getName().toLowerCase();
                        if (name.endsWith(".jar") && name.contains("voxelsniper") && !name.contains("fastasyncvoxelsniper")) {
                            output = file;
                            break;
                        }
                    }
                    if (output == null) {
                        output = new File(this.getDataFolder().getParentFile(), "VoxelSniper.jar");
                        byte[] vsJar = Jars.VS_B_5_171_0.download();
                        try (FileOutputStream fos = new FileOutputStream(output)) {
                            fos.write(vsJar);
                        }
                    }
                    Bukkit.getPluginManager().loadPlugin(output);
                } catch (Throwable e) {
                    e.printStackTrace();
                    Fawe.debug("====== INSTALL VOXELSNIPER ======");
                    Fawe.debug("FAVS requires VoxelSniper to function correctly");
                    Fawe.debug("Info: https://github.com/boy0001/FastAsyncWorldedit/releases/");
                    Fawe.debug("===============================");
                    return;
                }
            }
            SnipeData.inject();
            Sniper.inject();
            VoxelVoxelCommand.inject();
            PerformBrush.inject();
            RangeBlockHelper.inject();
            SniperBrushChangedEvent.inject();
            SniperMaterialChangedEvent.inject();

            WarpBrush.inject(); // Fixes for async tp
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
