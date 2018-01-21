package com.thevoxelbox.voxelsniper.brush;

import com.boydti.fawe.object.RunnableVal;
import com.boydti.fawe.util.TaskManager;
import com.thevoxelbox.voxelsniper.Message;
import com.thevoxelbox.voxelsniper.SnipeData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * @author MikeMatrix
 */
public class WarpBrush extends Brush
{
    /**
     *
     */
    public WarpBrush()
    {
        this.setName("Warp");
    }

    @Override
    public final void info(final Message vm)
    {
        vm.brushName(this.getName());
    }

    @Override
    protected final void arrow(final SnipeData v)
    {
        Player player = v.owner().getPlayer();
        Location location = this.getLastBlock().getLocation();
        Location playerLocation = player.getLocation();
        location.setPitch(playerLocation.getPitch());
        location.setYaw(playerLocation.getYaw());
        location.setWorld(Bukkit.getWorld(location.getWorld().getName()));
        TaskManager.IMP.sync(new RunnableVal<Object>() {
            @Override
            public void run(Object value) {
                player.teleport(location);
            }
        });
    }

    @Override
    protected final void powder(final SnipeData v)
    {
        Player player = v.owner().getPlayer();
        Location location = this.getLastBlock().getLocation();
        Location playerLocation = player.getLocation();
        location.setPitch(playerLocation.getPitch());
        location.setYaw(playerLocation.getYaw());
        location.setWorld(Bukkit.getWorld(location.getWorld().getName()));
        TaskManager.IMP.sync(new RunnableVal<Object>() {
            @Override
            public void run(Object value) {
                player.teleport(location);
            }
        });
    }

    @Override
    public String getPermissionNode()
    {
        return "voxelsniper.brush.warp";
    }

    public static Class<?> inject() {
        return WarpBrush.class;
    }
}