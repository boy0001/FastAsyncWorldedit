package com.boydti.fawe.nukkit.core;

import cn.nukkit.Player;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.EntityLiving;
import cn.nukkit.entity.item.EntityBoat;
import cn.nukkit.entity.item.EntityFallingBlock;
import cn.nukkit.entity.item.EntityItem;
import cn.nukkit.entity.item.EntityMinecartEmpty;
import cn.nukkit.entity.item.EntityPainting;
import cn.nukkit.entity.item.EntityPrimedTNT;
import cn.nukkit.entity.item.EntityXPOrb;
import cn.nukkit.entity.passive.EntityAnimal;
import cn.nukkit.entity.passive.EntityNPC;
import cn.nukkit.entity.passive.EntityTameable;
import cn.nukkit.entity.projectile.EntityProjectile;
import com.sk89q.worldedit.entity.metadata.EntityType;


import static com.google.common.base.Preconditions.checkNotNull;

public class NukkitEntityType implements EntityType {

    private final Entity entity;

    public NukkitEntityType(Entity entity) {
        checkNotNull(entity);
        this.entity = entity;
    }

    @Override
    public boolean isPlayerDerived() {
        return entity instanceof Player;
    }

    @Override
    public boolean isProjectile() {
        return entity instanceof EntityProjectile;
    }

    @Override
    public boolean isItem() {
        return entity instanceof EntityItem;
    }

    @Override
    public boolean isFallingBlock() {
        return entity instanceof EntityFallingBlock;
    }

    @Override
    public boolean isPainting() {
        return entity instanceof EntityPainting;
    }

    @Override
    public boolean isItemFrame() {
        // No item frames on MCPE
        return false;
    }

    @Override
    public boolean isBoat() {
        return entity instanceof EntityBoat;
    }

    @Override
    public boolean isMinecart() {
        return entity instanceof EntityMinecartEmpty;
    }

    @Override
    public boolean isTNT() {
        return entity instanceof EntityPrimedTNT;
    }

    @Override
    public boolean isExperienceOrb() {
        return entity instanceof EntityXPOrb;
    }

    @Override
    public boolean isLiving() {
        return entity instanceof EntityLiving;
    }

    @Override
    public boolean isAnimal() {
        return entity instanceof EntityAnimal;
    }

    @Override
    public boolean isAmbient() {
        // No bats implemented on MCPE
        return false;
    }

    @Override
    public boolean isNPC() {
        return entity instanceof EntityNPC;
    }

    @Override
    public boolean isGolem() {
        // No golem on MCPE
        return false;
    }

    @Override
    public boolean isTamed() {
        return entity instanceof EntityTameable && ((EntityTameable) entity).isTamed();
    }

    @Override
    public boolean isTagged() {
        return entity.hasCustomName();
    }

    @Override
    public boolean isArmorStand() {
        // No armor stand
        return false;
    }
}
