package com.boydti.fawe.wrappers;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.object.FaweLocation;
import com.boydti.fawe.object.FawePlayer;
import com.google.common.base.Charsets;
import com.sk89q.worldedit.LocalPlayer;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldVector;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.event.platform.CommandEvent;
import com.sk89q.worldedit.extension.platform.CommandManager;
import com.sk89q.worldedit.extent.inventory.BlockBag;
import com.sk89q.worldedit.session.SessionKey;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.World;
import java.util.UUID;
import javax.annotation.Nullable;

/**
 * Only really useful for executing commands from console<br>
 *     - The API itself doesn't any fake player anywhere
 */
public class FakePlayer extends LocalPlayer {
    private static FakePlayer CONSOLE;

    public static FakePlayer getConsole() {
        if (CONSOLE == null) {
            CONSOLE = new FakePlayer("#CONSOLE", null);
        }
        return CONSOLE;
    }

    private final String name;
    private final UUID uuid;
    private World world;
    private Location pos;

    public FakePlayer(String name, UUID uuid) {
        this.name = name;
        this.uuid = uuid == null ? UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(Charsets.UTF_8)) : uuid;
        this.world = WorldEdit.getInstance().getServer().getWorlds().get(0);
        this.pos = new Location(world, 0, 0, 0);
    }

    private FawePlayer fp;

    public FawePlayer toFawePlayer() {
        if (fp == null) {
            fp = new FawePlayer(this) {
                @Override
                public void sendTitle(String head, String sub) {}

                @Override
                public void resetTitle() {}

                @Override
                public String getName() {
                    return name;
                }

                @Override
                public UUID getUUID() {
                    return uuid;
                }

                @Override
                public boolean hasPermission(String perm) {
                    return FakePlayer.this.hasPermission(perm) || (Boolean) getMeta("perm." + perm, false);
                }

                @Override
                public void setPermission(String perm, boolean flag) {
                    setMeta("perm." + perm, true);
                }

                @Override
                public void sendMessage(String message) {
                    FakePlayer.this.print(message);
                }

                @Override
                public void executeCommand(String substring) {
                    CommandManager.getInstance().handleCommand(new CommandEvent(FakePlayer.this, substring));
                }

                @Override
                public FaweLocation getLocation() {
                    Location loc = FakePlayer.this.getLocation();
                    return new FaweLocation(loc.getExtent().toString(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
                }

                @Override
                public Player getPlayer() {
                    return FakePlayer.this;
                }
            };
        }
        return fp;
    }

    @Override
    public World getWorld() {
        return world;
    }

    @Override
    public int getItemInHand() {
        return 0;
    }

    @Override
    public void giveItem(int type, int amount) {}

    @Override
    public BlockBag getInventoryBlockBag() {
        return null;
    }

    @Override
    public WorldVector getPosition() {
        return new WorldVector(pos);
    }

    @Override
    public double getPitch() {
        return pos.getPitch();
    }

    @Override
    public double getYaw() {
        return pos.getYaw();
    }

    @Override
    public void setPosition(Vector pos, float pitch, float yaw) {
        this.pos = new Location(world, pos, yaw, pitch);
    }

    @Nullable
    @Override
    public BaseEntity getState() {
        return null;
    }

    @Override
    public Location getLocation() {
        return pos;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void printRaw(String msg) {
        Fawe.debug(msg);
    }

    @Override
    public void printDebug(String msg) {
        Fawe.debug(msg);
    }

    @Override
    public void print(String msg) {
        Fawe.debug(msg);
    }

    @Override
    public void printError(String msg) {
        Fawe.debug(msg);
    }

    private FakeSessionKey key;

    @Override
    public SessionKey getSessionKey() {
        if (key == null) {
            key = new FakeSessionKey(uuid, name);
        }
        return key;
    }

    @Nullable
    @Override
    public <T> T getFacet(Class<? extends T> cls) {
        return null;
    }

    @Override
    public UUID getUniqueId() {
        return uuid;
    }

    @Override
    public String[] getGroups() {
        return new String[0];
    }

    @Override
    public boolean hasPermission(String permission) {
        return true;
    }

    private static class FakeSessionKey implements SessionKey {
        private final UUID uuid;
        private final String name;
        private FakeSessionKey(UUID uuid, String name) {
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
            return true;
        }

        @Override
        public boolean isPersistent() {
            return true;
        }
    }
}
