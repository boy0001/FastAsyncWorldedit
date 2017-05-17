package com.thevoxelbox.voxelsniper;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class RangeBlockHelper {
    private static final int MAXIMUM_WORLD_HEIGHT = 255;
    private static final double DEFAULT_PLAYER_VIEW_HEIGHT = 1.65D;
    private static final double DEFAULT_LOCATION_VIEW_HEIGHT = 0.0D;
    private static final double DEFAULT_STEP = 0.2D;
    private static final int DEFAULT_RANGE = 250;
    private Location playerLoc;
    private double rotX;
    private double rotY;
    private double viewHeight;
    private double rotXSin;
    private double rotXCos;
    private double rotYSin;
    private double rotYCos;
    private double length;
    private double hLength;
    private double step;
    private double range;
    private double playerX;
    private double playerY;
    private double playerZ;
    private double xOffset;
    private double yOffset;
    private double zOffset;
    private int lastX;
    private int lastY;
    private int lastZ;
    private int targetX;
    private int targetY;
    private int targetZ;
    private World world;

    public RangeBlockHelper(Location location) {
        this.init(location, 250.0D, 0.2D, 0.0D);
    }

    public RangeBlockHelper(Location location, int range, double step) {
        this.world = location.getWorld();
        this.init(location, (double)range, step, 0.0D);
    }

    public RangeBlockHelper(Player player, int range, double step) {
        if (player != null) {
            this.world = VoxelSniper.getInstance().getSniperManager().getSniperForPlayer(player).getWorld();
        }
        this.init(player.getLocation(), (double)range, step, 1.65D);
    }

    public RangeBlockHelper(Player player, World world) {
        if (player != null && (world == null || player.getWorld().getName().equals(world.getName()))) {
            this.world = VoxelSniper.getInstance().getSniperManager().getSniperForPlayer(player).getWorld();
        } else {
            this.world = world;
        }
        this.init(player.getLocation(), 250.0D, 0.2D, 1.65D);
    }

    public RangeBlockHelper(Player player, World world, double range) {
        if (player != null && (world == null || player.getWorld().getName().equals(world.getName()))) {
            this.world = VoxelSniper.getInstance().getSniperManager().getSniperForPlayer(player).getWorld();
        } else {
            this.world = world;
        }
        this.init(player.getLocation(), range, 0.2D, 1.65D);
        this.fromOffworld();
    }

    public final void fromOffworld() {
        if(this.targetY <= 255) {
            if(this.targetY < 0) {
                while(this.targetY < 0 && this.length <= this.range) {
                    this.lastX = this.targetX;
                    this.lastY = this.targetY;
                    this.lastZ = this.targetZ;

                    while(true) {
                        this.length += this.step;
                        this.hLength = this.length * this.rotYCos;
                        this.yOffset = this.length * this.rotYSin;
                        this.xOffset = this.hLength * this.rotXCos;
                        this.zOffset = this.hLength * this.rotXSin;
                        this.targetX = (int)Math.floor(this.xOffset + this.playerX);
                        this.targetY = (int)Math.floor(this.yOffset + this.playerY);
                        this.targetZ = (int)Math.floor(this.zOffset + this.playerZ);
                        if(this.length > this.range || this.targetX != this.lastX || this.targetY != this.lastY || this.targetZ != this.lastZ) {
                            break;
                        }
                    }
                }
            }
        } else {
            while(this.targetY > 255 && this.length <= this.range) {
                this.lastX = this.targetX;
                this.lastY = this.targetY;
                this.lastZ = this.targetZ;

                while(true) {
                    this.length += this.step;
                    this.hLength = this.length * this.rotYCos;
                    this.yOffset = this.length * this.rotYSin;
                    this.xOffset = this.hLength * this.rotXCos;
                    this.zOffset = this.hLength * this.rotXSin;
                    this.targetX = (int)Math.floor(this.xOffset + this.playerX);
                    this.targetY = (int)Math.floor(this.yOffset + this.playerY);
                    this.targetZ = (int)Math.floor(this.zOffset + this.playerZ);
                    if(this.length > this.range || this.targetX != this.lastX || this.targetY != this.lastY || this.targetZ != this.lastZ) {
                        break;
                    }
                }
            }
        }

    }

    public final Block getCurBlock() {
        return this.length <= this.range && this.targetY <= 255 && this.targetY >= 0?this.world.getBlockAt(this.targetX, this.targetY, this.targetZ):null;
    }

    public final Block getFaceBlock() {
        while(this.getNextBlock() != null && this.getCurBlock().getTypeId() == 0) {
            ;
        }

        if(this.getCurBlock() != null) {
            return this.getLastBlock();
        } else {
            return null;
        }
    }

    public final Block getLastBlock() {
        return this.lastY <= 255 && this.lastY >= 0?this.world.getBlockAt(this.lastX, this.lastY, this.lastZ):null;
    }

    public final Block getNextBlock() {
        this.lastX = this.targetX;
        this.lastY = this.targetY;
        this.lastZ = this.targetZ;

        do {
            this.length += this.step;
            this.hLength = this.length * this.rotYCos;
            this.yOffset = this.length * this.rotYSin;
            this.xOffset = this.hLength * this.rotXCos;
            this.zOffset = this.hLength * this.rotXSin;
            this.targetX = (int)Math.floor(this.xOffset + this.playerX);
            this.targetY = (int)Math.floor(this.yOffset + this.playerY);
            this.targetZ = (int)Math.floor(this.zOffset + this.playerZ);
        } while(this.length <= this.range && this.targetX == this.lastX && this.targetY == this.lastY && this.targetZ == this.lastZ);

        return this.length <= this.range && this.targetY <= 255 && this.targetY >= 0?this.world.getBlockAt(this.targetX, this.targetY, this.targetZ):null;
    }

    public final Block getRangeBlock() {
        this.fromOffworld();
        return this.length > this.range?null:this.getRange();
    }

    public final Block getTargetBlock() {
        this.fromOffworld();

        while(this.getNextBlock() != null && this.getCurBlock().getTypeId() == 0) {
            ;
        }

        return this.getCurBlock();
    }

    public final void setCurBlock(int type) {
        if(this.getCurBlock() != null) {
            this.world.getBlockAt(this.targetX, this.targetY, this.targetZ).setTypeId(type);
        }

    }

    public final void setFaceBlock(int type) {
        while(this.getNextBlock() != null && this.getCurBlock().getTypeId() == 0) {
            ;
        }

        if(this.getCurBlock() != null) {
            this.world.getBlockAt(this.targetX, this.targetY, this.targetZ).setTypeId(type);
        }

    }

    public final void setLastBlock(int type) {
        if(this.getLastBlock() != null) {
            this.world.getBlockAt(this.lastX, this.lastY, this.lastZ).setTypeId(type);
        }

    }

    public final void setTargetBlock(int type) {
        while(this.getNextBlock() != null && this.getCurBlock().getTypeId() == 0) {
            ;
        }

        if(this.getCurBlock() != null) {
            this.world.getBlockAt(this.targetX, this.targetY, this.targetZ).setTypeId(type);
        }

    }

    private Block getRange() {
        this.lastX = this.targetX;
        this.lastY = this.targetY;
        this.lastZ = this.targetZ;

        do {
            this.length += this.step;
            this.hLength = this.length * this.rotYCos;
            this.yOffset = this.length * this.rotYSin;
            this.xOffset = this.hLength * this.rotXCos;
            this.zOffset = this.hLength * this.rotXSin;
            this.targetX = (int)Math.floor(this.xOffset + this.playerX);
            this.targetY = (int)Math.floor(this.yOffset + this.playerY);
            this.targetZ = (int)Math.floor(this.zOffset + this.playerZ);
        } while(this.length <= this.range && this.targetX == this.lastX && this.targetY == this.lastY && this.targetZ == this.lastZ);

        return this.world.getBlockTypeIdAt(this.targetX, this.targetY, this.targetZ) != 0?this.world.getBlockAt(this.targetX, this.targetY, this.targetZ):(this.length <= this.range && this.targetY <= 255 && this.targetY >= 0?this.getRange():this.world.getBlockAt(this.lastX, this.lastY, this.lastZ));
    }

    private void init(Location location, double range, double step, double viewHeight) {
        this.playerLoc = location;
        this.viewHeight = viewHeight;
        this.playerX = this.playerLoc.getX();
        this.playerY = this.playerLoc.getY() + this.viewHeight;
        this.playerZ = this.playerLoc.getZ();
        this.range = range;
        this.step = step;
        this.length = 0.0D;
        this.rotX = (double)((this.playerLoc.getYaw() + 90.0F) % 360.0F);
        this.rotY = (double)(this.playerLoc.getPitch() * -1.0F);
        this.rotYCos = Math.cos(Math.toRadians(this.rotY));
        this.rotYSin = Math.sin(Math.toRadians(this.rotY));
        this.rotXCos = Math.cos(Math.toRadians(this.rotX));
        this.rotXSin = Math.sin(Math.toRadians(this.rotX));
        this.targetX = (int)Math.floor(this.playerLoc.getX());
        this.targetY = (int)Math.floor(this.playerLoc.getY() + this.viewHeight);
        this.targetZ = (int)Math.floor(this.playerLoc.getZ());
        this.lastX = this.targetX;
        this.lastY = this.targetY;
        this.lastZ = this.targetZ;
    }
}
