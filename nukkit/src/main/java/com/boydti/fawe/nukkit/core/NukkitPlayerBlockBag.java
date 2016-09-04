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

import cn.nukkit.Player;
import cn.nukkit.inventory.PlayerInventory;
import cn.nukkit.item.Item;
import com.sk89q.worldedit.WorldVector;
import com.sk89q.worldedit.blocks.BaseItem;
import com.sk89q.worldedit.blocks.BaseItemStack;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.blocks.ItemType;
import com.sk89q.worldedit.extent.inventory.BlockBag;
import com.sk89q.worldedit.extent.inventory.BlockBagException;
import com.sk89q.worldedit.extent.inventory.OutOfBlocksException;
import com.sk89q.worldedit.extent.inventory.OutOfSpaceException;
import java.util.Map;

public class NukkitPlayerBlockBag extends BlockBag {

    private Player player;
    private Map<Integer, Item> items;
    private int size;

    /**
     * Construct the object.
     *
     * @param player the player
     */
    public NukkitPlayerBlockBag(Player player) {
        this.player = player;
    }

    /**
     * Loads inventory on first use.
     */
    private void loadInventory() {
        if (items == null) {
            PlayerInventory inv = player.getInventory();
            items = inv.getContents();
            this.size = inv.getSize();
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

        for (Map.Entry<Integer, Item> entry : items.entrySet()) {
            int slot = entry.getKey();
            Item nukkitItem = entry.getValue();
            if (nukkitItem == null) {
                continue;
            }
            if (nukkitItem.getId() != id) {
                // Type id doesn't fit
                continue;
            }
            if (usesDamageValue && nukkitItem.getDamage() != damage) {
                // Damage value doesn't fit.
                continue;
            }

            int currentCount = nukkitItem.getCount();
            if (currentCount < 0) {
                // Unlimited
                return;
            }

            if (currentCount > 1) {
                nukkitItem.setCount(currentCount - 1);
                found = true;
            } else {
                items.remove(slot);
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
        for (int slot = 0; slot < size; ++slot) {
            Item nukkitItem = items.get(slot);

            if (nukkitItem == null) {
                // Delay using up a free slot until we know there are no stacks
                // of this item to merge into

                if (freeSlot == -1) {
                    freeSlot = slot;
                }
                continue;
            }

            if (nukkitItem.getId() != id) {
                // Type id doesn't fit
                continue;
            }

            if (usesDamageValue && nukkitItem.getDamage() != damage) {
                // Damage value doesn't fit.
                continue;
            }

            int currentCount = nukkitItem.getCount();
            if (currentCount < 0) {
                // Unlimited
                return;
            }
            if (currentCount >= 64) {
                // Full stack
                continue;
            }

            int spaceLeft = 64 - currentCount;
            if (spaceLeft >= amount) {
                nukkitItem.setCount(currentCount + amount);
                return;
            }

            nukkitItem.setCount(64);
            amount -= spaceLeft;
        }

        if (freeSlot > -1) {
            items.put(freeSlot, new Item(id, 0, amount));
            return;
        }

        throw new OutOfSpaceException(id);
    }

    @Override
    public void flushChanges() {
        if (items != null) {
            player.getInventory().setContents(items);
            items = null;
            player.getInventory().sendContents(player);
        }
    }

    @Override
    public void addSourcePosition(WorldVector pos) {
    }

    @Override
    public void addSingleSourcePosition(WorldVector pos) {
    }

}