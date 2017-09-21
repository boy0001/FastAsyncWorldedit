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
import com.boydti.fawe.bukkit.FaweBukkit;
import com.boydti.fawe.bukkit.util.ItemUtil;
import com.sk89q.worldedit.WorldVector;
import com.sk89q.worldedit.blocks.BaseItem;
import com.sk89q.worldedit.blocks.BaseItemStack;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.blocks.ItemType;
import com.sk89q.worldedit.extent.inventory.BlockBag;
import com.sk89q.worldedit.extent.inventory.BlockBagException;
import com.sk89q.worldedit.extent.inventory.OutOfBlocksException;
import com.sk89q.worldedit.extent.inventory.OutOfSpaceException;
import com.sk89q.worldedit.extent.inventory.SlottableBlockBag;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class BukkitPlayerBlockBag extends BlockBag implements SlottableBlockBag {

    private Player player;
    private ItemStack[] items;

    /**
     * Construct the object.
     *
     * @param player the player
     */
    public BukkitPlayerBlockBag(Player player) {
        this.player = player;
    }

    /**
     * Loads inventory on first use.
     */
    private void loadInventory() {
        if (items == null) {
            items = player.getInventory().getContents();
        }
    }

    /**
     * Get the player.
     *
     * @return the player
     */
    public Player getPlayer() {
        return player;
    }

    @Override
    public BaseItem getItem(int slot) {
        loadInventory();
        return toBaseItem(items[slot]);
    }

    @Override
    public void setItem(int slot, BaseItem block) {
        loadInventory();
        items[slot] = toItemStack(block);
    }

    @Override
    public int getSelectedSlot() {
        return player.getInventory().getHeldItemSlot();
    }

    private BaseItem toBaseItem(ItemStack item) {
        if (item == null) return new BaseItemStack(0, 0);
        int id = item.getTypeId();
        short data;
        if (id < 256) {
            data = item.getData().getData();
        } else {
            data = item.getDurability();
        }
        BaseItemStack baseItem = new BaseItemStack(id, item.getAmount(), data);
        ItemUtil itemUtil = Fawe.<FaweBukkit>imp().getItemUtil();
        if (itemUtil != null && item.hasItemMeta()) {
            baseItem.setNbtData(itemUtil.getNBT(item));
        }
        return baseItem;
    }

    private ItemStack toItemStack(BaseItem item) {
        if (item == null) return null;
        final int id = item.getType();
        final int damage = item.getData();
        int amount = (item instanceof BaseItemStack) ? ((BaseItemStack) item).getAmount() : 1;
        ItemStack bukkitItem;
        if (id < 256) {
            bukkitItem = new ItemStack(id, amount, (short) 0, (byte) damage);
        } else {
            bukkitItem = new ItemStack(id, amount, (short) damage);
        }
        ItemUtil itemUtil = Fawe.<FaweBukkit>imp().getItemUtil();
        if (itemUtil != null && item.hasNBTData()) {
            bukkitItem = itemUtil.setNBT(bukkitItem, item.getNbtData());
        }
        return bukkitItem;
    }


    @Override
    public void fetchItem(BaseItem item) throws BlockBagException {
        final int id = item.getType();
        final int damage = item.getData();
        int amount = (item instanceof BaseItemStack) ? ((BaseItemStack) item).getAmount() : 1;
        assert(amount == 1);
        boolean usesDamageValue = ItemType.usesDamageValue(id);

        if (id == BlockID.AIR) {
            throw new IllegalArgumentException("Can't fetch air block");
        }

        loadInventory();

        boolean found = false;

        for (int slot = 0; slot < items.length; ++slot) {
            ItemStack bukkitItem = items[slot];

            if (bukkitItem == null) {
                continue;
            }

            if (bukkitItem.getTypeId() != id) {
                // Type id doesn't fit
                continue;
            }

            if (usesDamageValue && bukkitItem.getDurability() != damage) {
                // Damage value doesn't fit.
                continue;
            }

            int currentAmount = bukkitItem.getAmount();
            if (currentAmount < 0) {
                // Unlimited
                return;
            }

            if (currentAmount > 1) {
                bukkitItem.setAmount(currentAmount - 1);
                found = true;
            } else {
                bukkitItem.setAmount(0);
                items[slot] = null;
                found = true;
            }

            break;
        }

        if (!found) {
            throw new OutOfBlocksException();
        }
    }

    @Override
    public void storeItem(BaseItem item) throws BlockBagException {
        final int id = item.getType();
        final int damage = item.getData();
        int amount = (item instanceof BaseItemStack) ? ((BaseItemStack) item).getAmount() : 1;
        assert(amount <= 64);
        boolean usesDamageValue = ItemType.usesDamageValue(id);

        if (id == BlockID.AIR) {
            throw new IllegalArgumentException("Can't store air block");
        }

        loadInventory();

        int freeSlot = -1;

        for (int slot = 0; slot < items.length; ++slot) {
            ItemStack bukkitItem = items[slot];

            if (bukkitItem == null) {
                // Delay using up a free slot until we know there are no stacks
                // of this item to merge into

                if (freeSlot == -1) {
                    freeSlot = slot;
                }
                continue;
            }

            if (bukkitItem.getTypeId() != id) {
                // Type id doesn't fit
                continue;
            }

            if (usesDamageValue && bukkitItem.getDurability() != damage) {
                // Damage value doesn't fit.
                continue;
            }

            int currentAmount = bukkitItem.getAmount();
            if (currentAmount < 0) {
                // Unlimited
                return;
            }
            if (currentAmount >= 64) {
                // Full stack
                continue;
            }

            int spaceLeft = 64 - currentAmount;
            if (spaceLeft >= amount) {
                bukkitItem.setAmount(currentAmount + amount);
                return;
            }

            bukkitItem.setAmount(64);
            amount -= spaceLeft;
        }

        if (freeSlot > -1) {
            items[freeSlot] = new ItemStack(id, amount);
            return;
        }

        throw new OutOfSpaceException(id);
    }

    @Override
    public void flushChanges() {
        if (items != null) {
            player.getInventory().setContents(items);
            items = null;
        }
    }

    @Override
    public void addSourcePosition(WorldVector pos) {
    }

    @Override
    public void addSingleSourcePosition(WorldVector pos) {
    }

    public static Class<?> inject() {
        return BukkitPlayerBlockBag.class;
    }
}