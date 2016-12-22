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

package com.sk89q.worldedit.command;

import com.boydti.fawe.config.BBC;
import com.boydti.fawe.object.brush.InspectBrush;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.sk89q.worldedit.LocalConfiguration;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.blocks.ItemType;
import com.sk89q.worldedit.command.tool.BlockDataCyler;
import com.sk89q.worldedit.command.tool.BlockReplacer;
import com.sk89q.worldedit.command.tool.DistanceWand;
import com.sk89q.worldedit.command.tool.FloatingTreeRemover;
import com.sk89q.worldedit.command.tool.FloodFillTool;
import com.sk89q.worldedit.command.tool.LongRangeBuildTool;
import com.sk89q.worldedit.command.tool.QueryTool;
import com.sk89q.worldedit.command.tool.TreePlanter;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.patterns.Pattern;
import com.sk89q.worldedit.util.TreeGenerator;
import com.sk89q.worldedit.util.command.parametric.Optional;

public class ToolCommands {
    private final WorldEdit we;

    public ToolCommands(WorldEdit we) {
        this.we = we;
    }

    @Command(
            aliases = { "inspect" },
            usage = "",
            desc = "Inspect edits within a radius",
            help =
                    "Chooses the inspect brush",
            min = 0,
            max = 0
    )
    @CommandPermissions("worldedit.tool.inspect")
    public void inspectBrush(Player player, LocalSession session, @Optional("1") double radius) throws WorldEditException {
        session.setTool(player.getItemInHand(), new InspectBrush());
        BBC.TOOL_INSPECT.send(player, ItemType.toHeldName(player.getItemInHand()));
    }

    @Command(
            aliases = { "none" },
            usage = "",
            desc = "Unbind a bound tool from your current item",
            min = 0,
            max = 0
    )
    public void none(Player player, LocalSession session, CommandContext args) throws WorldEditException {
        session.setTool(player.getItemInHand(), null);
        BBC.TOOL_NONE.send(player);
    }

    @Command(
            aliases = { "info" },
            usage = "",
            desc = "Block information tool",
            min = 0,
            max = 0
    )
    @CommandPermissions("worldedit.tool.info")
    public void info(Player player, LocalSession session, CommandContext args) throws WorldEditException {
        session.setTool(player.getItemInHand(), new QueryTool());
        BBC.TOOL_INFO.send(player, ItemType.toHeldName(player.getItemInHand()));
    }

    @Command(
            aliases = { "tree" },
            usage = "[type]",
            desc = "Tree generator tool",
            min = 0,
            max = 1
    )
    @CommandPermissions("worldedit.tool.tree")
    @SuppressWarnings("deprecation")
    public void tree(Player player, LocalSession session, CommandContext args) throws WorldEditException {

        TreeGenerator.TreeType type = args.argsLength() > 0 ?
                type = TreeGenerator.lookup(args.getString(0))
                : TreeGenerator.TreeType.TREE;

        if (type == null) {
            BBC.TOOL_TREE_ERROR.send(player, args.getString(0));
            return;
        }

        session.setTool(player.getItemInHand(), new TreePlanter(new TreeGenerator(type)));
        BBC.TOOL_TREE.send(player, ItemType.toHeldName(player.getItemInHand()));
    }

    @Command(
            aliases = { "repl" },
            usage = "<block>",
            desc = "Block replacer tool",
            min = 1,
            max = 1
    )
    @CommandPermissions("worldedit.tool.replacer")
    public void repl(Player player, LocalSession session, CommandContext args) throws WorldEditException {
        BaseBlock targetBlock = we.getBlock(player, args.getString(0));
        session.setTool(player.getItemInHand(), new BlockReplacer(targetBlock));
        BBC.TOOL_REPL.send(player, ItemType.toHeldName(player.getItemInHand()));
    }

    @Command(
            aliases = { "cycler" },
            usage = "",
            desc = "Block data cycler tool",
            min = 0,
            max = 0
    )
    @CommandPermissions("worldedit.tool.data-cycler")
    public void cycler(Player player, LocalSession session, CommandContext args) throws WorldEditException {

        session.setTool(player.getItemInHand(), new BlockDataCyler());
        BBC.TOOL_CYCLER.send(player, ItemType.toHeldName(player.getItemInHand()));
    }

    @Command(
            aliases = { "floodfill", "flood" },
            usage = "<pattern> <range>",
            desc = "Flood fill tool",
            min = 2,
            max = 2
    )
    @CommandPermissions("worldedit.tool.flood-fill")
    public void floodFill(Player player, LocalSession session, CommandContext args) throws WorldEditException {
        LocalConfiguration config = we.getConfiguration();
        int range = args.getInteger(1);

        if (range > config.maxSuperPickaxeSize) {
            BBC.TOOL_FLOOD_FILL_RANGE_ERROR.send(player, config.maxSuperPickaxeSize);
            return;
        }

        Pattern pattern = we.getBlockPattern(player, args.getString(0));
        session.setTool(player.getItemInHand(), new FloodFillTool(range, pattern));
        BBC.TOOL_FLOOD_FILL.send(player, ItemType.toHeldName(player.getItemInHand()));
    }

    @Command(
            aliases = { "deltree" },
            usage = "",
            desc = "Floating tree remover tool",
            min = 0,
            max = 0
    )
    @CommandPermissions("worldedit.tool.deltree")
    public void deltree(Player player, LocalSession session, CommandContext args) throws WorldEditException {
        session.setTool(player.getItemInHand(), new FloatingTreeRemover());
        BBC.TOOL_DELTREE.send(player, ItemType.toHeldName(player.getItemInHand()));
    }

    @Command(
            aliases = { "farwand" },
            usage = "",
            desc = "Wand at a distance tool",
            min = 0,
            max = 0
    )
    @CommandPermissions("worldedit.tool.farwand")
    public void farwand(Player player, LocalSession session, CommandContext args) throws WorldEditException {
        session.setTool(player.getItemInHand(), new DistanceWand());
        BBC.TOOL_FARWAND.send(player, ItemType.toHeldName(player.getItemInHand()));
    }

    @Command(
            aliases = { "lrbuild", "/lrbuild" },
            usage = "<leftclick block> <rightclick block>",
            desc = "Long-range building tool",
            min = 2,
            max = 2
    )
    @CommandPermissions("worldedit.tool.lrbuild")
    public void longrangebuildtool(Player player, LocalSession session, CommandContext args) throws WorldEditException {

        BaseBlock secondary = we.getBlock(player, args.getString(0));
        BaseBlock primary = we.getBlock(player, args.getString(1));
        session.setTool(player.getItemInHand(), new LongRangeBuildTool(primary, secondary));
        BBC.TOOL_LRBUILD_BOUND.send(player, ItemType.toHeldName(player.getItemInHand()));
        BBC.TOOL_LRBUILD_INFO.send(player, ItemType.toName(secondary.getType()), ItemType.toName(primary.getType()));
    }

    public static Class<?> inject() {
        return ToolCommands.class;
    }
}