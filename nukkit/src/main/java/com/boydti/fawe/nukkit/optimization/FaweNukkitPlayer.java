package com.boydti.fawe.nukkit.optimization;

import cn.nukkit.Player;
import cn.nukkit.command.ConsoleCommandSender;
import cn.nukkit.level.Location;
import com.boydti.fawe.Fawe;
import com.boydti.fawe.config.BBC;
import com.boydti.fawe.nukkit.core.NukkitPlatform;
import com.boydti.fawe.nukkit.core.NukkitPlayer;
import com.boydti.fawe.object.FaweLocation;
import com.boydti.fawe.object.FawePlayer;
import com.google.common.base.Charsets;
import java.util.UUID;

public class FaweNukkitPlayer extends FawePlayer<Player> {

    private static ConsoleCommandSender console;

    public FaweNukkitPlayer(final Player parent) {
        super(parent);
    }

    @Override
    public String getName() {
        return this.parent.getName();
    }

    @Override
    public UUID getUUID() {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + parent.getName().toLowerCase()).getBytes(Charsets.UTF_8));
    }

    @Override
    public boolean hasPermission(final String perm) {
        return this.parent.hasPermission(perm);
    }

    @Override
    public void setPermission(final String perm, final boolean flag) {
        this.parent.addAttachment(Fawe.<FaweNukkit> imp().getPlugin()).setPermission(perm, flag);
    }


    @Override
    public void resetTitle() {
        sendTitle("","");
    }

    public void sendTitle(String title, String sub) {
        throw new UnsupportedOperationException("Titles are not implemented in MCPE!");
    }

    @Override
    public void sendMessage(final String message) {
        this.parent.sendMessage(BBC.color(message));
    }

    @Override
    public void executeCommand(final String cmd) {
        Fawe.<FaweNukkit> imp().getPlugin().getServer().dispatchCommand(parent, cmd);
    }

    @Override
    public FaweLocation getLocation() {
        final Location loc = this.parent.getLocation();
        return new FaweLocation(loc.getLevel().getName(), loc.getFloorX(), loc.getFloorY(), loc.getFloorZ());
    }

    @Override
    public com.sk89q.worldedit.entity.Player toWorldEditPlayer() {
        return new NukkitPlayer((NukkitPlatform) Fawe.<FaweNukkit> imp().getPlugin().getPlatform(), parent);
    }

    @Override
    public boolean isSneaking() {
        return parent.isSneaking();
    }
}
