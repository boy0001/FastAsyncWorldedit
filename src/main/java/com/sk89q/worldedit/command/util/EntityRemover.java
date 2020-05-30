package com.sk89q.worldedit.command.util;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.util.SetQueue;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.entity.metadata.EntityType;
import com.sk89q.worldedit.function.EntityFunction;
import com.sk89q.worldedit.world.registry.EntityRegistry;
import java.util.regex.Pattern;
import javax.annotation.Nullable;


import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The implementation of /remove.
 */
public class EntityRemover {

    public enum Type {
        ALL("all") {
            @Override
            boolean matches(EntityType type) {
                for (Type value : values()) {
                    if (value != this && value.matches(type)) {
                        return true;
                    }
                }
                return false;
            }
        },
        PROJECTILES("projectiles?|arrows?") {
            @Override
            boolean matches(EntityType type) {
                return type.isProjectile();
            }
        },
        ITEMS("items?|drops?") {
            @Override
            boolean matches(EntityType type) {
                return type.isItem();
            }
        },
        FALLING_BLOCKS("falling(blocks?|sand|gravel)") {
            @Override
            boolean matches(EntityType type) {
                return type.isFallingBlock();
            }
        },
        PAINTINGS("paintings?|art") {
            @Override
            boolean matches(EntityType type) {
                return type.isPainting();
            }
        },
        ITEM_FRAMES("(item)frames?") {
            @Override
            boolean matches(EntityType type) {
                return type.isItemFrame();
            }
        },
        BOATS("boats?") {
            @Override
            boolean matches(EntityType type) {
                return type.isBoat();
            }
        },
        MINECARTS("(mine)?carts?") {
            @Override
            boolean matches(EntityType type) {
                return type.isMinecart();
            }
        },
        TNT("tnt") {
            @Override
            boolean matches(EntityType type) {
                return type.isTNT();
            }
        },
        XP_ORBS("xp") {
            @Override
            boolean matches(EntityType type) {
                return type.isExperienceOrb();
            }
        };

        private final Pattern pattern;

        Type(String pattern) {
            this.pattern = Pattern.compile(pattern);
        }

        public boolean matches(String str) {
            return pattern.matcher(str).matches();
        }

        abstract boolean matches(EntityType type);

        @Nullable
        public static Type findByPattern(String str) {
            for (Type type : values()) {
                if (type.matches(str)) {
                    return type;
                }
            }

            return null;
        }
    }

    private Type type;

    public void fromString(String str) throws CommandException {
        Type type = Type.findByPattern(str);
        if (type != null) {
            this.type = type;
        } else {
            throw new CommandException("Acceptable types: projectiles, items, paintings, itemframes, boats, minecarts, tnt, xp, or all");
        }
    }

    public EntityFunction createFunction(final EntityRegistry entityRegistry) {
        final Type type = this.type;
        checkNotNull("type can't be null", type);
        return new EntityFunction() {
            @Override
            public boolean apply(final Entity entity) throws WorldEditException {
                EntityType registryType = entity.getFacet(EntityType.class);
                if (registryType != null) {
                    if (type.matches(registryType)) {
                        if (Fawe.isMainThread()) {
                            entity.remove();
                        } else {
                            SetQueue.IMP.addTask(new Runnable() {
                                @Override
                                public void run() {
                                    entity.remove();
                                }
                            });
                        }
                        return true;
                    }
                }

                return false;
            }
        };
    }

    public static Class<?> inject() {
        return EntityRemover.class;
    }
}
