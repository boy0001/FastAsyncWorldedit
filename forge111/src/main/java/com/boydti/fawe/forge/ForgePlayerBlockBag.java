package com.boydti.fawe.forge;

import com.sk89q.worldedit.WorldVector;
import com.sk89q.worldedit.blocks.BaseItem;
import com.sk89q.worldedit.blocks.BaseItemStack;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.blocks.ItemType;
import com.sk89q.worldedit.extent.inventory.BlockBag;
import com.sk89q.worldedit.extent.inventory.BlockBagException;
import com.sk89q.worldedit.extent.inventory.OutOfBlocksException;
import com.sk89q.worldedit.extent.inventory.OutOfSpaceException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class ForgePlayerBlockBag extends BlockBag {

    private EntityPlayerMP player;
    private ItemStack[] items;
    private boolean changed;

    /**
     * Construct the object.
     *
     * @param player the player
     */
    public ForgePlayerBlockBag(EntityPlayerMP player) {
        this.player = player;
    }

    /**
     * Loads inventory on first use.
     */
    private void loadInventory() {
        if (items == null) {
            items = new ItemStack[player.inventory.getSizeInventory()];
            for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
                items[i] = player.inventory.getStackInSlot(i);
            }
        }
    }

    /**
     * Get the player.
     *
     * @return the player
     */
    public EntityPlayerMP getPlayer() {
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

        for (int slot = 0; slot < items.length; ++slot) {
            ItemStack forgeItem = items[slot];

            if (forgeItem == null) {
                continue;
            }
            int itemId = Item.getIdFromItem(forgeItem.getItem());
            if (itemId != id) {
                // Type id doesn't fit
                continue;
            }

            if (usesDamageValue && forgeItem.getItemDamage() != damage) {
                // Damage value doesn't fit.
                continue;
            }

            int currentAmount = forgeItem.getCount();
            if (currentAmount < 0) {
                // Unlimited
                return;
            }

            changed = true;

            if (currentAmount > 1) {
                forgeItem.setCount(forgeItem.getCount() - 1);;
                found = true;
            } else {
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
            ItemStack forgeItem = items[slot];

            if (forgeItem == null) {
                // Delay using up a free slot until we know there are no stacks
                // of this item to merge into

                if (freeSlot == -1) {
                    freeSlot = slot;
                }
                continue;
            }

            int itemId = Item.getIdFromItem(forgeItem.getItem());
            if (itemId != id) {
                // Type id doesn't fit
                continue;
            }

            if (usesDamageValue && forgeItem.getItemDamage() != damage) {
                // Damage value doesn't fit.
                continue;
            }

            int currentAmount = forgeItem.getCount();
            if (currentAmount < 0) {
                // Unlimited
                return;
            }
            if (currentAmount >= 64) {
                // Full stack
                continue;
            }

            changed = true;

            int spaceLeft = 64 - currentAmount;
            if (spaceLeft >= amount) {
                forgeItem.setCount(forgeItem.getCount() + amount);
                return;
            }

            forgeItem.setCount(64);
            amount -= spaceLeft;
        }

        if (freeSlot > -1) {
            changed = true;
            items[freeSlot] = new ItemStack(Item.getItemById(id), amount);
            return;
        }

        throw new OutOfSpaceException(id);
    }

    @Override
    public void flushChanges() {
        if (items != null && changed) {
            for (int i = 0; i < items.length; i++) {
                player.inventory.setInventorySlotContents(i, items[i]);
            }
            items = null;
            changed = false;
        }
    }

    @Override
    public void addSourcePosition(WorldVector pos) {
    }

    @Override
    public void addSingleSourcePosition(WorldVector pos) {
    }

}