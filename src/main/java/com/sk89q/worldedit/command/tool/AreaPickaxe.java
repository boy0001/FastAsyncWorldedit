package com.sk89q.worldedit.command.tool;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalConfiguration;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extension.platform.Platform;
import com.sk89q.worldedit.world.World;

/**
 * A super pickaxe mode that will remove blocks in an area.
 */
public class AreaPickaxe implements BlockTool {

    private static final BaseBlock air = new BaseBlock(0);
    private int range;

    public AreaPickaxe(int range) {
        this.range = range;
    }

    @Override
    public boolean canUse(Actor player) {
        return player.hasPermission("worldedit.superpickaxe.area");
    }

    @Override
    public boolean actPrimary(Platform server, LocalConfiguration config, Player player, LocalSession session, com.sk89q.worldedit.util.Location clicked) {
        int ox = clicked.getBlockX();
        int oy = clicked.getBlockY();
        int oz = clicked.getBlockZ();
        int initialType = ((World) clicked.getExtent()).getBlockType(clicked.toVector());

        if (initialType == 0) {
            return true;
        }

        if (initialType == BlockID.BEDROCK && !player.canDestroyBedrock()) {
            return true;
        }

        EditSession editSession = session.createEditSession(player);
        editSession.getSurvivalExtent().setToolUse(config.superPickaxeManyDrop);

        for (int x = ox - range; x <= ox + range; ++x) {
            for (int z = oz - range; z <= oz + range; ++z) {
                for (int y = oy + range; y >= oy - range; --y) {
                    if (editSession.getLazyBlock(x, y, z).getId() != initialType) {
                        continue;
                    }
                    editSession.setBlock(x, y, z, air);
                }
            }
        }
        editSession.flushQueue();
        session.remember(editSession);

        return true;
    }

    public static Class<?> inject() {
        return AreaPickaxe.class;
    }
}