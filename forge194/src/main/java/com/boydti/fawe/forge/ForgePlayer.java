package com.boydti.fawe.forge;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.config.BBC;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.FaweLocation;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.wrappers.PlayerWrapper;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.forge.ForgeWorldEdit;
import java.util.UUID;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentBase;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

public class ForgePlayer extends FawePlayer<EntityPlayerMP> {
    public ForgePlayer(EntityPlayerMP parent) {
        super(parent);
    }

    @Override
    public void sendTitle(String head, String sub) { // Not supported
        Settings.QUEUE.PROGRESS.DISPLAY = false;
    }

    @Override
    public void resetTitle() { // Not supported
        Settings.QUEUE.PROGRESS.DISPLAY = false;
    }

    @Override
    public String getName() {
        return parent.getName();
    }

    @Override
    public UUID getUUID() {
        return parent.getUniqueID();
    }

    @Override
    public boolean hasPermission(String perm) {
        Object meta = getMeta(perm);
        return meta instanceof Boolean ? (boolean) meta : ForgeWorldEdit.inst.getPermissionsProvider().hasPermission(parent, perm);
    }

    @Override
    public void setPermission(String perm, boolean flag) {
        setMeta(perm, flag);
    }

    @Override
    public void sendMessage(String msg) {
        for (String part : msg.split("\n")) {
            part = BBC.color(part);
            TextComponentBase text = new TextComponentString(part);
            this.parent.addChatMessage(text);
        }
    }

    @Override
    public void executeCommand(String substring) {
        throw new UnsupportedOperationException("NOT IMPLEMENTED");
    }

    @Override
    public FaweLocation getLocation() {
        World world = parent.worldObj;
        BlockPos pos = parent.getPosition();
        return new FaweLocation(Fawe.<FaweForge>imp().getWorldName(world), pos.getX(), pos.getY(), pos.getZ());
    }

    @Override
    public Player getPlayer() {
        return PlayerWrapper.wrap(ForgeWorldEdit.inst.wrap(this.parent));
    }
}
