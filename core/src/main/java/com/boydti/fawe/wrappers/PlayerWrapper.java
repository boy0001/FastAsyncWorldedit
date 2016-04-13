package com.boydti.fawe.wrappers;

import com.sk89q.worldedit.PlayerDirection;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.WorldVector;
import com.sk89q.worldedit.WorldVectorFace;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.inventory.BlockBag;
import com.sk89q.worldedit.internal.cui.CUIEvent;
import com.sk89q.worldedit.session.SessionKey;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.util.auth.AuthorizationException;
import com.sk89q.worldedit.world.AbstractWorld;
import com.sk89q.worldedit.world.World;
import java.io.File;
import java.util.UUID;
import javax.annotation.Nullable;

public class PlayerWrapper implements Player {
    private final Player parent;

    public PlayerWrapper(Player parent) {
        this.parent = parent;
    }

    @Override
    public World getWorld() {
        return new WorldWrapper((AbstractWorld) parent.getWorld());
    }

    @Override
    public boolean isHoldingPickAxe() {
        return parent.isHoldingPickAxe();
    }

    @Override
    public PlayerDirection getCardinalDirection(int yawOffset) {
        return parent.getCardinalDirection(yawOffset);
    }

    @Override
    public int getItemInHand() {
        return parent.getItemInHand();
    }

    @Override
    public BaseBlock getBlockInHand() throws WorldEditException {
        return parent.getBlockInHand();
    }

    @Override
    public void giveItem(int type, int amount) {
        parent.giveItem(type, amount);
    }

    @Override
    public BlockBag getInventoryBlockBag() {
        return parent.getInventoryBlockBag();
    }

    @Override
    public boolean hasCreativeMode() {
        return parent.hasCreativeMode();
    }

    @Override
    public void findFreePosition(WorldVector searchPos) {
        parent.findFreePosition(searchPos);
    }

    @Override
    public void setOnGround(WorldVector searchPos) {
        parent.setOnGround(searchPos);
    }

    @Override
    public void findFreePosition() {
        parent.findFreePosition();
    }

    @Override
    public boolean ascendLevel() {
        return parent.ascendLevel();
    }

    @Override
    public boolean descendLevel() {
        return parent.descendLevel();
    }

    @Override
    public boolean ascendToCeiling(int clearance) {
        return parent.ascendToCeiling(clearance);
    }

    @Override
    public boolean ascendToCeiling(int clearance, boolean alwaysGlass) {
        return parent.ascendToCeiling(clearance, alwaysGlass);
    }

    @Override
    public boolean ascendUpwards(int distance) {
        return parent.ascendUpwards(distance);
    }

    @Override
    public boolean ascendUpwards(int distance, boolean alwaysGlass) {
        return parent.ascendUpwards(distance, alwaysGlass);
    }

    @Override
    public void floatAt(int x, int y, int z, boolean alwaysGlass) {
        parent.floatAt(x, y, z, alwaysGlass);
    }

    @Override
    public WorldVector getBlockIn() {
        return parent.getBlockIn();
    }

    @Override
    public WorldVector getBlockOn() {
        return parent.getBlockOn();
    }

    @Override
    public WorldVector getBlockTrace(int range, boolean useLastBlock) {
        return parent.getBlockTrace(range, useLastBlock);
    }

    @Override
    public WorldVectorFace getBlockTraceFace(int range, boolean useLastBlock) {
        return parent.getBlockTraceFace(range, useLastBlock);
    }

    @Override
    public WorldVector getBlockTrace(int range) {
        return parent.getBlockTrace(range);
    }

    @Override
    public WorldVector getSolidBlockTrace(int range) {
        return parent.getSolidBlockTrace(range);
    }

    @Override
    public PlayerDirection getCardinalDirection() {
        return parent.getCardinalDirection();
    }

    @Override
    @Deprecated
    public WorldVector getPosition() {
        return parent.getPosition();
    }

    @Override
    @Deprecated
    public double getPitch() {
        return parent.getPitch();
    }

    @Override
    @Deprecated
    public double getYaw() {
        return parent.getYaw();
    }

    @Override
    public boolean passThroughForwardWall(int range) {
        return parent.passThroughForwardWall(range);
    }

    @Override
    public void setPosition(Vector pos, float pitch, float yaw) {
        parent.setPosition(pos, pitch, yaw);
    }

    @Override
    public void setPosition(Vector pos) {
        parent.setPosition(pos);
    }

    @Override
    @Nullable
    public BaseEntity getState() {
        return parent.getState();
    }

    @Override
    public Location getLocation() {
        return parent.getLocation();
    }

    @Override
    public Extent getExtent() {
        return parent.getExtent();
    }

    @Override
    public boolean remove() {
        return parent.remove();
    }

    @Override
    @Nullable
    public <T> T getFacet(Class<? extends T> cls) {
        return parent.getFacet(cls);
    }

    @Override
    public String getName() {
        return parent.getName();
    }

    @Override
    public void printRaw(String msg) {
        parent.printRaw(msg);
    }

    @Override
    public void printDebug(String msg) {
        parent.printDebug(msg);
    }

    @Override
    public void print(String msg) {
        parent.print(msg);
    }

    @Override
    public void printError(String msg) {
        parent.printError(msg);
    }

    @Override
    public boolean canDestroyBedrock() {
        return parent.canDestroyBedrock();
    }

    @Override
    public boolean isPlayer() {
        return parent.isPlayer();
    }

    @Override
    public File openFileOpenDialog(String[] extensions) {
        return parent.openFileOpenDialog(extensions);
    }

    @Override
    public File openFileSaveDialog(String[] extensions) {
        return parent.openFileSaveDialog(extensions);
    }

    @Override
    public void dispatchCUIEvent(CUIEvent event) {
        parent.dispatchCUIEvent(event);
    }

    @Override
    public UUID getUniqueId() {
        return parent.getUniqueId();
    }

    @Override
    public SessionKey getSessionKey() {
        return parent.getSessionKey();
    }

    @Override
    public String[] getGroups() {
        return parent.getGroups();
    }

    @Override
    public void checkPermission(String permission) throws AuthorizationException {
        parent.checkPermission(permission);
    }

    @Override
    public boolean hasPermission(String permission) {
        return parent.hasPermission(permission);
    }
}
