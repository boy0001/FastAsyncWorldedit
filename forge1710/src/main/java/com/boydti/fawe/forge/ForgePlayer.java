package com.boydti.fawe.forge;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.config.BBC;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.FaweLocation;
import com.boydti.fawe.object.FawePlayer;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.forge.ForgeWorldEdit;
import java.util.UUID;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;

public class ForgePlayer extends FawePlayer<EntityPlayerMP> {
    public ForgePlayer(EntityPlayerMP parent) {
        super(parent);
    }

    @Override
    public void sendTitle(String head, String sub) { // Not supported
        Settings.IMP.QUEUE.PROGRESS.DISPLAY = "false";
    }

    @Override
    public void resetTitle() { // Not supported
        Settings.IMP.QUEUE.PROGRESS.DISPLAY = "false";
    }

    @Override
    public String getName() {
        return parent.getCommandSenderName();
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
            ChatComponentText component = new ChatComponentText(part);
            component.getChatStyle().setColor(EnumChatFormatting.LIGHT_PURPLE);
            this.parent.addChatMessage(component);
        }
    }

    @Override
    public void executeCommand(String substring) {
        throw new UnsupportedOperationException("NOT IMPLEMENTED");
    }

    @Override
    public FaweLocation getLocation() {
        World world = parent.worldObj;
        ChunkCoordinates pos = parent.getPlayerCoordinates();
        return new FaweLocation(Fawe.<FaweForge>imp().getWorldName(world), pos.posX, pos.posY, pos.posZ);
    }

    @Override
    public Player toWorldEditPlayer() {
        return ForgeWorldEdit.inst.wrap(this.parent);
    }
}
