package com.boydti.fawe.forge;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.config.BBC;
import com.boydti.fawe.object.FaweLocation;
import com.boydti.fawe.object.FawePlayer;
import java.util.UUID;
import net.minecraft.entity.player.EntityPlayerMP;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

public class SpongePlayer extends FawePlayer<Player> {
    public SpongePlayer(final Player parent) {
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
        Object meta = getMeta(perm);
        return meta instanceof Boolean ? (boolean) meta : this.parent.hasPermission(perm);
    }

    @Override
    public void setPermission(final String perm, final boolean flag) {
        setMeta(perm, flag);
    }

    @Override
    public void sendMessage(final String message) {
        this.parent.sendMessage(TextSerializers.LEGACY_FORMATTING_CODE.deserialize(BBC.color(message)));
    }

    @Override
    public void executeCommand(final String cmd) {
        Sponge.getGame().getCommandManager().process(this.parent, cmd);
    }

    @Override
    public FaweLocation getLocation() {
        Location<World> loc = this.parent.getLocation();
        return new FaweLocation(loc.getExtent().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    @Override
    public com.sk89q.worldedit.entity.Player getPlayer() {
        return (com.sk89q.worldedit.entity.Player) Fawe.<FaweSponge> imp().getWorldEditPlugin().wrap((EntityPlayerMP) this.parent);
    }

    @Override
    public boolean hasWorldEditBypass() {
        return hasPermission("fawe.bypass") || getMeta("fawe.bypass") == Boolean.TRUE;
    }
}
