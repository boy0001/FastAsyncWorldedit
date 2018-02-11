/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.boydti.fawe.nukkit.core;

import cn.nukkit.AdventureSettings;
import cn.nukkit.Player;
import cn.nukkit.inventory.PlayerInventory;
import cn.nukkit.item.Item;
import cn.nukkit.level.Location;
import com.google.common.base.Charsets;
import com.sk89q.worldedit.*;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.extent.inventory.BlockBag;
import com.sk89q.worldedit.internal.cui.CUIEvent;
import com.sk89q.worldedit.session.SessionKey;
import java.util.UUID;
import javax.annotation.Nullable;

public class NukkitPlayer extends LocalPlayer {

    private final NukkitPlatform platform;
    private Player player;

    public NukkitPlayer(NukkitPlatform platform, Player player) {
        this.platform = platform;
        this.player = player;
    }

    @Override
    public UUID getUniqueId() {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + player.getName().toLowerCase()).getBytes(Charsets.UTF_8));
    }

    @Override
    public int getItemInHand() {
        PlayerInventory inv = player.getInventory();
        Item itemStack = inv.getItemInHand();
        return itemStack != null ? itemStack.getId() : 0;
    }

    @Override
    public BaseBlock getBlockInHand() throws WorldEditException {
        PlayerInventory inv = player.getInventory();
        Item itemStack = inv.getItemInHand();
        if (itemStack == null) {
            return EditSession.nullBlock;
        }
        return new BaseBlock(itemStack.getId(), itemStack.getMaxDurability() != 0 ? 0 : itemStack.getDamage());
    }

    @Override
    public String getName() {
        return player.getName();
    }

    @Override
    public WorldVector getPosition() {
        Location loc = player.getLocation();
        return new WorldVector(NukkitUtil.getLocalWorld(loc.getLevel()),
                loc.getX(), loc.getY(), loc.getZ());
    }

    @Override
    public double getPitch() {
        return player.getLocation().getPitch();
    }

    @Override
    public double getYaw() {
        return player.getLocation().getYaw();
    }

    @Override
    public void giveItem(int type, int amt) {
        player.getInventory().addItem(new Item(type, 0, amt));
    }

    @Override
    public void printRaw(String msg) {
        for (String part : msg.split("\n")) {
            player.sendMessage(part);
        }
    }

    @Override
    public void print(String msg) {
        for (String part : msg.split("\n")) {
            player.sendMessage("\u00A7d" + part);
        }
    }

    @Override
    public void printDebug(String msg) {
        for (String part : msg.split("\n")) {
            player.sendMessage("\u00A77" + part);
        }
    }

    @Override
    public void printError(String msg) {
        for (String part : msg.split("\n")) {
            player.sendMessage("\u00A7c" + part);
        }
    }

    @Override
    public void setPosition(Vector pos, float pitch, float yaw) {
        player.teleport(new Location(pos.getX(), pos.getY(), pos.getZ(), yaw, pitch, player.getLevel()));
    }

    @Override
        public String[] getGroups() {
        // Is this ever used?
        return new String[0];
    }

    @Override
    public BlockBag getInventoryBlockBag() {
        return new NukkitPlayerBlockBag(player);
    }

    @Override
    public boolean hasPermission(String perm) {
        NukkitConfiguration config = platform.getMod().getWEConfig();
        return (!config.noOpPermissions && player.isOp()) || player.hasPermission(perm);
    }

    @Override
    public LocalWorld getWorld() {
        return NukkitUtil.getLocalWorld(player.getLevel());
    }

    @Override
    public void dispatchCUIEvent(CUIEvent event) {
        // No WE-CUI on MCPE
        return;
    }

    public Player getPlayer() {
        return player;
    }

    @Override
    public boolean hasCreativeMode() {
        return player.getGamemode() == 1;
    }

    @Override
    public void floatAt(int x, int y, int z, boolean alwaysGlass) {
        if (alwaysGlass || !player.getAllowFlight()) {
            super.floatAt(x, y, z, alwaysGlass);
            return;
        }

        setPosition(new Vector(x + 0.5, y, z + 0.5));
        player.getAdventureSettings().set(AdventureSettings.Type.ALLOW_FLIGHT, true);
        player.getAdventureSettings().set(AdventureSettings.Type.FLYING, true);
        player.getAdventureSettings().update();
    }

    @Override
    public BaseEntity getState() {
        throw new UnsupportedOperationException("Cannot create a state from this object");
    }

    @Override
    public com.sk89q.worldedit.util.Location getLocation() {
        Location nativeLocation = player.getLocation();
        Vector position = NukkitUtil.toVector(nativeLocation);
        return new com.sk89q.worldedit.util.Location(
                getWorld(),
                position,
                (float) nativeLocation.getYaw(),
                (float) nativeLocation.getPitch());
    }

    @Nullable
    @Override
    public <T> T getFacet(Class<? extends T> cls) {
        return null;
    }

    @Override
    public SessionKey getSessionKey() {
        return new SessionKeyImpl(this.player.getUniqueId(), player.getName());
    }

    private static class SessionKeyImpl implements SessionKey {
        // If not static, this will leak a reference

        private final UUID uuid;
        private final String name;

        private SessionKeyImpl(UUID uuid, String name) {
            this.uuid = uuid;
            this.name = name;
        }

        @Override
        public UUID getUniqueId() {
            return uuid;
        }

        @Nullable
        @Override
        public String getName() {
            return name;
        }

        @Override
        public boolean isActive() {
            // This is a thread safe call on CraftNukkit because it uses a
            // CopyOnWrite list for the list of players, but the Nukkit
            // specification doesn't require thread safety (though the
            // spec is extremely incomplete)
            return NukkitWorldEdit.inst().getServer().getPlayer(name) != null;
        }

        @Override
        public boolean isPersistent() {
            return true;
        }

    }

}