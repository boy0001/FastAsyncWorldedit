package com.boydti.fawe.wrappers;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.RunnableVal;
import com.boydti.fawe.util.EditSessionBuilder;
import com.boydti.fawe.util.TaskManager;
import com.sk89q.worldedit.BlockWorldVector;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.LocalWorld;
import com.sk89q.worldedit.PlayerDirection;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.WorldVector;
import com.sk89q.worldedit.WorldVectorFace;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.blocks.BlockType;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.platform.AbstractPlayerActor;
import com.sk89q.worldedit.extent.inventory.BlockBag;
import com.sk89q.worldedit.internal.cui.CUIEvent;
import com.sk89q.worldedit.session.SessionKey;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.util.TargetBlock;
import com.sk89q.worldedit.util.auth.AuthorizationException;
import com.sk89q.worldedit.world.World;
import java.io.File;
import java.util.UUID;
import javax.annotation.Nullable;

public class PlayerWrapper extends AbstractPlayerActor {
    private final Player parent;

    public PlayerWrapper(Player parent) {
        this.parent = parent;
    }

    public static PlayerWrapper wrap(Player parent) {
        if (parent instanceof PlayerWrapper) {
            return (PlayerWrapper) parent;
        }
        return new PlayerWrapper(parent);
    }

    public Player getParent() {
        return parent;
    }

    @Override
    public World getWorld() {
        return WorldWrapper.wrap(parent.getWorld());
    }

    @Override
    public boolean isHoldingPickAxe() {
        return parent.isHoldingPickAxe();
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
                PlayerWrapper.super.findFreePosition(searchPos);
            }
        });
    }

    @Override
    public void setOnGround(final WorldVector searchPos) {
        TaskManager.IMP.sync(new RunnableVal<Boolean>() {
            @Override
            public void run(Boolean value) {
                PlayerWrapper.super.setOnGround(searchPos);
            }
        });
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
        RuntimeException caught = null;
        try {
            EditSession edit = new EditSessionBuilder(parent.getWorld()).player(FawePlayer.wrap(this)).build();
            edit.setBlockFast(new Vector(x, y - 1, z), new BaseBlock(BlockType.GLASS.getID()));
            edit.flushQueue();
            LocalSession session = Fawe.get().getWorldEdit().getSession(this);
            if (session != null) {
                session.remember(edit, true, FawePlayer.wrap(this).getLimit().MAX_HISTORY);
            }
        } catch (RuntimeException e) {
            caught = e;
        }
        TaskManager.IMP.sync(new RunnableVal<Object>() {
            @Override
            public void run(Object value) {
                setPosition(new Vector(x + 0.5, y, z + 0.5));
            }
        });
        if (caught != null) {
            throw caught;
        }
    }

    @Override
    public WorldVector getBlockTrace(final int range, final boolean useLastBlock) {
        return TaskManager.IMP.sync(new RunnableVal<WorldVector>() {
            @Override
            public void run(WorldVector value) {
                TargetBlock tb = new TargetBlock(PlayerWrapper.this, range, 0.2D);
                this.value = useLastBlock ? tb.getAnyTargetBlock() : tb.getTargetBlock();
            }
        });
    }

    @Override
    public WorldVectorFace getBlockTraceFace(final int range, final boolean useLastBlock) {
        return TaskManager.IMP.sync(new RunnableVal<WorldVectorFace>() {
            @Override
            public void run(WorldVectorFace value) {
                TargetBlock tb = new TargetBlock(PlayerWrapper.this, range, 0.2D);
                this.value = useLastBlock ? tb.getAnyTargetBlockFace() : tb.getTargetBlockFace();
            }
        });
    }

    @Override
    public WorldVector getSolidBlockTrace(final int range) {
        return TaskManager.IMP.sync(new RunnableVal<WorldVector>() {
            @Override
            public void run(WorldVector value) {
                TargetBlock tb = new TargetBlock(PlayerWrapper.this, range, 0.2D);
                this.value = tb.getSolidTargetBlock();
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
                int searchDist = 0;
                TargetBlock hitBlox = new TargetBlock(PlayerWrapper.this, range, 0.2D);
                LocalWorld world = PlayerWrapper.this.getPosition().getWorld();
                boolean firstBlock = true;
                int freeToFind = 2;
                boolean inFree = false;

                while (true) {
                    BlockWorldVector block;
                    while ((block = hitBlox.getNextBlock()) != null) {
                        boolean free = BlockType.canPassThrough(world.getBlock(block));
                        if (firstBlock) {
                            firstBlock = false;
                            if (!free) {
                                --freeToFind;
                                continue;
                            }
                        }

                        ++searchDist;
                        if (searchDist > 20) {
                            this.value = false;
                            return;
                        }

                        if (inFree != free && free) {
                            --freeToFind;
                        }

                        if (freeToFind == 0) {
                            PlayerWrapper.this.setOnGround(block);
                            this.value = true;
                            return;
                        }

                        inFree = free;
                    }

                    this.value = false;
                    return;
                }
            }
        });
    }

    @Override
    public void setPosition(Vector pos, float pitch, float yaw) {
        parent.setPosition(pos, pitch, yaw);
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
