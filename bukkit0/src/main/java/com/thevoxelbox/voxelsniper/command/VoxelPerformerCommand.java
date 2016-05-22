package com.thevoxelbox.voxelsniper.command;

import com.thevoxelbox.voxelsniper.SnipeData;
import com.thevoxelbox.voxelsniper.Sniper;
import com.thevoxelbox.voxelsniper.VoxelSniper;
import com.thevoxelbox.voxelsniper.api.command.VoxelCommand;
import com.thevoxelbox.voxelsniper.brush.IBrush;
import com.thevoxelbox.voxelsniper.brush.perform.Performer;
import java.util.logging.Level;
import org.bukkit.entity.Player;

public class VoxelPerformerCommand
        extends VoxelCommand
{
    public VoxelPerformerCommand(VoxelSniper plugin)
    {
        super("VoxelPerformer", plugin);
        setIdentifier("/p");
        setPermission("voxelsniper.sniper");
    }

    public boolean onCommand(Player player, String[] args)
    {
        Sniper sniper = this.plugin.getSniperManager().getSniperForPlayer(player);
        SnipeData snipeData = sniper.getSnipeData(sniper.getCurrentToolId());
        try
        {
            if ((args == null) || (args.length == 0))
            {
                IBrush brush = sniper.getBrush(sniper.getCurrentToolId());
                if ((brush instanceof Performer)) {
                    ((Performer)brush).parse(new String[] { "m" }, snipeData);
                } else {
                    player.sendMessage("This brush is not a performer brush.");
                }
            }
            else
            {
                IBrush brush = sniper.getBrush(sniper.getCurrentToolId());
                if ((brush instanceof Performer)) {
                    ((Performer)brush).parse(args, snipeData);
                } else {
                    player.sendMessage("This brush is not a performer brush.");
                }
            }
            return true;
        }
        catch (Exception exception)
        {
            this.plugin.getLogger().log(Level.WARNING, "Command error from " + player.getName(), exception);
        }
        return true;
    }

    public static Class<?> inject() {
        return VoxelPerformerCommand.class;
    }
}
