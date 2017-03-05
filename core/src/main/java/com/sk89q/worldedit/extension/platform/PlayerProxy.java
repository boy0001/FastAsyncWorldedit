//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.sk89q.worldedit.extension.platform;

import com.google.common.base.Preconditions;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.WorldVector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extent.inventory.BlockBag;
import com.sk89q.worldedit.internal.cui.CUIEvent;
import com.sk89q.worldedit.session.SessionKey;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.World;
import java.util.UUID;
import javax.annotation.Nullable;

public class PlayerProxy extends AbstractPlayerActor {
    private final Player basePlayer;
    private final Actor permActor;
    private final Actor cuiActor;
    private final World world;

    public PlayerProxy(Player basePlayer, Actor permActor, Actor cuiActor, World world) {
        Preconditions.checkNotNull(basePlayer);
        Preconditions.checkNotNull(permActor);
        Preconditions.checkNotNull(cuiActor);
        Preconditions.checkNotNull(world);
        this.basePlayer = basePlayer;
        this.permActor = permActor;
        this.cuiActor = cuiActor;
        this.world = world;
    }

    public UUID getUniqueId() {
        return this.basePlayer.getUniqueId();
    }

    public int getItemInHand() {
        return this.basePlayer.getItemInHand();
    }

    @Override
    public BaseBlock getBlockInHand() throws WorldEditException {
        return this.basePlayer.getBlockInHand();
    }

    public void giveItem(int type, int amount) {
        this.basePlayer.giveItem(type, amount);
    }

    public BlockBag getInventoryBlockBag() {
        return this.basePlayer.getInventoryBlockBag();
    }

    public String getName() {
        return this.basePlayer.getName();
    }

    public BaseEntity getState() {
        throw new UnsupportedOperationException("Can\'t getState() on a player");
    }

    public Location getLocation() {
        return this.basePlayer.getLocation();
    }

    public WorldVector getPosition() {
        return this.basePlayer.getPosition();
    }

    public double getPitch() {
        return this.basePlayer.getPitch();
    }

    public double getYaw() {
        return this.basePlayer.getYaw();
    }

    public void setPosition(Vector pos, float pitch, float yaw) {
        this.basePlayer.setPosition(pos, pitch, yaw);
    }

    public World getWorld() {
        return this.world;
    }

    public void printRaw(String msg) {
        this.basePlayer.printRaw(msg);
    }

    public void printDebug(String msg) {
        this.basePlayer.printDebug(msg);
    }

    public void print(String msg) {
        this.basePlayer.print(msg);
    }

    public void printError(String msg) {
        this.basePlayer.printError(msg);
    }

    public String[] getGroups() {
        return this.permActor.getGroups();
    }

    public boolean hasPermission(String perm) {
        return this.permActor.hasPermission(perm);
    }

    public void dispatchCUIEvent(CUIEvent event) {
        this.cuiActor.dispatchCUIEvent(event);
    }

    @Nullable
    public <T> T getFacet(Class<? extends T> cls) {
        return this.basePlayer.getFacet(cls);
    }

    public SessionKey getSessionKey() {
        return this.basePlayer.getSessionKey();
    }

    public static Class inject() {
        return PlayerProxy.class;
    }
}
