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

package com.sk89q.worldedit.bukkit;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweCache;
import com.boydti.fawe.bukkit.FaweBukkit;
import com.boydti.fawe.bukkit.block.BrushBoundBaseBlock;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.RunnableVal;
import com.boydti.fawe.util.TaskManager;
import com.boydti.fawe.wrappers.WorldWrapper;
import com.sk89q.util.StringUtil;
import com.sk89q.worldedit.*;
import com.sk89q.worldedit.blocks.*;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.extent.inventory.BlockBag;
import com.sk89q.worldedit.internal.cui.CUIEvent;
import com.sk89q.worldedit.session.SessionKey;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import org.bukkit.*;
import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.material.Dye;

public class BukkitPlayer extends LocalPlayer {

    private Player player;
    private WorldEditPlugin plugin;

    public BukkitPlayer(WorldEditPlugin plugin, ServerInterface server, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    @Override
    public UUID getUniqueId() {
        return player.getUniqueId();
    }

    @Override
    public int getItemInHand() {
        ItemStack itemStack = player.getItemInHand();
        return itemStack != null ? itemStack.getTypeId() : 0;
    }

    @Override
    public BaseBlock getBlockInHand() throws WorldEditException {
        ItemStack itemStack = player.getItemInHand();
        if (itemStack == null) {
            return EditSession.nullBlock;
        }
        final int typeId = itemStack.getTypeId();
        switch (typeId) {
            case 0:
                return EditSession.nullBlock;
            case ItemID.INK_SACK:
                final Dye materialData = (Dye) itemStack.getData();
                if (materialData.getColor() == DyeColor.BROWN) {
                    return FaweCache.getBlock(BlockID.COCOA_PLANT, 0);
                }
                break;
            case ItemID.HEAD:
                return new SkullBlock(0, (byte) itemStack.getDurability());

            default:
                final BaseBlock baseBlock = BlockType.getBlockForItem(typeId, itemStack.getDurability());
                if (baseBlock != null) {
                    return baseBlock;
                }
                break;
        }
        BaseBlock block = FaweCache.getBlock(typeId, itemStack.getType().getMaxDurability() != 0 ? 0 : Math.max(0, itemStack.getDurability()));
        if (Settings.IMP.EXPERIMENTAL.PERSISTENT_BRUSHES && Fawe.<FaweBukkit>imp().getItemUtil() != null) {
            return new BrushBoundBaseBlock(this, WorldEdit.getInstance().getSession(this), itemStack);
        }
        return block;
    }

    @Override
    public String getName() {
        return player.getName();
    }

    @Override
    public WorldVector getPosition() {
        Location loc = player.getLocation();
        return new WorldVector(BukkitUtil.getLocalWorld(loc.getWorld()),
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
        final PlayerInventory inv = player.getInventory();
        final ItemStack newItem = new ItemStack(type, amt);
        if (type == WorldEdit.getInstance().getConfiguration().wandItem) {
            inv.remove(newItem);
        }
        final ItemStack item = player.getItemInHand();
        player.setItemInHand(newItem);
        if (item != null) {
            HashMap<Integer, ItemStack> overflow = inv.addItem(item);
            if (overflow != null && !overflow.isEmpty()) {
                TaskManager.IMP.sync(new RunnableVal<Object>() {
                    @Override
                    public void run(Object value) {
                        for (Map.Entry<Integer, ItemStack> entry : overflow.entrySet()) {
                            ItemStack stack = entry.getValue();
                            if (stack.getType() != Material.AIR && stack.getAmount() > 0) {
                                Item dropped = player.getWorld().dropItem(player.getLocation(), stack);
                                PlayerDropItemEvent event = new PlayerDropItemEvent(player, dropped);
                                if (event.isCancelled()) {
                                    dropped.remove();
                                }
                            }
                        }
                    }
                });
            }
        }
        player.updateInventory();
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
        World world;
        if (pos instanceof WorldVector) {
            world = ((BukkitWorld) WorldWrapper.unwrap(((WorldVector) pos).getWorld())).getWorld();
        } else {
            world = player.getWorld();
        }
        player.teleport(new Location(world, pos.getX(), pos.getY(), pos.getZ(), yaw, pitch));
    }

    @Override
    public String[] getGroups() {
        return plugin.getPermissionsResolver().getGroups(player);
    }

    @Override
    public BlockBag getInventoryBlockBag() {
        return new BukkitPlayerBlockBag(player);
    }

    @Override
    public boolean hasPermission(String perm) {
        return (!plugin.getLocalConfiguration().noOpPermissions && player.isOp())
                || plugin.getPermissionsResolver().hasPermission(
                player.getWorld().getName(), player, perm);
    }

    @Override
    public LocalWorld getWorld() {
        return BukkitUtil.getLocalWorld(player.getWorld());
    }

    @Override
    public void dispatchCUIEvent(CUIEvent event) {
        String[] params = event.getParameters();
        String send = event.getTypeId();
        if (params.length > 0) {
            send = send + "|" + StringUtil.joinString(params, "|");
        }
        player.sendPluginMessage(plugin, WorldEditPlugin.CUI_PLUGIN_CHANNEL, send.getBytes(CUIChannelListener.UTF_8_CHARSET));
    }

    public Player getPlayer() {
        return player;
    }

    @Override
    public boolean hasCreativeMode() {
        return player.getGameMode() == GameMode.CREATIVE;
    }

    @Override
    public void floatAt(int x, int y, int z, boolean alwaysGlass) {
        if (alwaysGlass || !player.getAllowFlight()) {
            super.floatAt(x, y, z, alwaysGlass);
            return;
        }

        setPosition(new Vector(x + 0.5, y, z + 0.5));
        player.setFlying(true);
    }

    @Override
    public BaseEntity getState() {
        throw new UnsupportedOperationException("Cannot create a state from this object");
    }

    @Override
    public com.sk89q.worldedit.util.Location getLocation() {
        Location nativeLocation = player.getLocation();
        Vector position = BukkitUtil.toVector(nativeLocation);
        return new com.sk89q.worldedit.util.Location(
                getWorld(),
                position,
                nativeLocation.getYaw(),
                nativeLocation.getPitch());
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
            // This is a thread safe call on CraftBukkit because it uses a
            // CopyOnWrite list for the list of players, but the Bukkit
            // specification doesn't require thread safety (though the
            // spec is extremely incomplete)
            return Bukkit.getServer().getPlayerExact(name) != null;
        }

        @Override
        public boolean isPersistent() {
            return true;
        }

    }

    public static Class<BukkitPlayer> inject() {
        return BukkitPlayer.class;
    }
}