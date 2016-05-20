package com.boydti.fawe.wrappers;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.object.RunnableVal;
import com.boydti.fawe.util.MainUtil;
import com.boydti.fawe.util.TaskManager;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.EditSessionFactory;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.PlayerDirection;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.WorldVector;
import com.sk89q.worldedit.WorldVectorFace;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.blocks.BlockType;
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

    public Player getParent() {
        return parent;
    }

    @Override
    public World getWorld() {
        return WorldWrapper.wrap((AbstractWorld) parent.getWorld());
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
    public void findFreePosition(final WorldVector searchPos) {
        TaskManager.IMP.sync(new RunnableVal<Boolean>() {
            @Override
            public void run(Boolean value) {
                parent.findFreePosition(searchPos);
            }
        });
    }

    @Override
    public void setOnGround(WorldVector searchPos) {
        parent.setOnGround(searchPos);
    }

    @Override
    public void findFreePosition() {
        TaskManager.IMP.sync(new RunnableVal<Boolean>() {
            @Override
            public void run(Boolean value) {
                parent.findFreePosition();
            }
        });
    }

    @Override
    public boolean ascendLevel() {
        return TaskManager.IMP.sync(new RunnableVal<Boolean>() {
            @Override
            public void run(Boolean value) {
                this.value = parent.ascendLevel();
            }
        });
    }

    @Override
    public boolean descendLevel() {
        return TaskManager.IMP.sync(new RunnableVal<Boolean>() {
            @Override
            public void run(Boolean value) {
                this.value = parent.descendLevel();
            }
        });
    }

    @Override
    public boolean ascendToCeiling(int clearance) {
        return ascendToCeiling(clearance, true);
    }

    @Override
    public boolean ascendToCeiling(int clearance, boolean alwaysGlass) {
        Vector pos = getBlockIn();
        int x = pos.getBlockX();
        int initialY = Math.max(0, pos.getBlockY());
        int y = Math.max(0, pos.getBlockY() + 2);
        int z = pos.getBlockZ();
        World world = getPosition().getWorld();

        // No free space above
        if (world.getBlockType(new Vector(x, y, z)) != 0) {
            return false;
        }

        while (y <= world.getMaxY()) {
            // Found a ceiling!
            if (!BlockType.canPassThrough(world.getBlock(new Vector(x, y, z)))) {
                int platformY = Math.max(initialY, y - 3 - clearance);
                floatAt(x, platformY + 1, z, alwaysGlass);
                return true;
            }

            ++y;
        }

        return false;
    }

    @Override
    public boolean ascendUpwards(int distance) {
        return ascendUpwards(distance, true);
    }

    @Override
    public boolean ascendUpwards(int distance, boolean alwaysGlass) {
        final Vector pos = getBlockIn();
        final int x = pos.getBlockX();
        final int initialY = Math.max(0, pos.getBlockY());
        int y = Math.max(0, pos.getBlockY() + 1);
        final int z = pos.getBlockZ();
        final int maxY = Math.min(getWorld().getMaxY() + 1, initialY + distance);
        final World world = getPosition().getWorld();

        while (y <= world.getMaxY() + 2) {
            if (!BlockType.canPassThrough(world.getBlock(new Vector(x, y, z)))) {
                break; // Hit something
            } else if (y > maxY + 1) {
                break;
            } else if (y == maxY + 1) {
                floatAt(x, y - 1, z, alwaysGlass);
                return true;
            }

            ++y;
        }

        return false;
    }

    @Override
    public void floatAt(final int x, final int y, final int z, final boolean alwaysGlass) {
        EditSessionFactory factory = WorldEdit.getInstance().getEditSessionFactory();
        final EditSession edit = factory.getEditSession(parent.getWorld(), -1, null, this);
        try {
            edit.setBlock(new Vector(x, y - 1, z), new BaseBlock( BlockType.GLASS.getID()));
            LocalSession session = Fawe.get().getWorldEdit().getSession(this);
            if (session != null) {
                session.remember(edit);
            }
        } catch (MaxChangedBlocksException e) {
            MainUtil.handleError(e);
        }
        TaskManager.IMP.sync(new RunnableVal<Object>() {
            @Override
            public void run(Object value) {
                edit.getQueue().next();
                setPosition(new Vector(x + 0.5, y, z + 0.5));
            }
        });
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
    public WorldVector getSolidBlockTrace(final int range) {
        return TaskManager.IMP.sync(new RunnableVal<WorldVector>() {
            @Override
            public void run(WorldVector value) {
                this.value = parent.getSolidBlockTrace(range);
            }
        });
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
    public boolean passThroughForwardWall(final int range) {
        return TaskManager.IMP.sync(new RunnableVal<Boolean>() {
            @Override
            public void run(Boolean value) {
                this.value = parent.passThroughForwardWall(range);
            }
        });
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
