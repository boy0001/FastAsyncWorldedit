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

import com.boydti.fawe.Fawe;
import com.boydti.fawe.config.BBC;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.FaweLimit;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.brush.BlendBall;
import com.boydti.fawe.object.brush.CommandBrush;
import com.boydti.fawe.object.brush.CopyPastaBrush;
import com.boydti.fawe.object.brush.DoubleActionBrushTool;
import com.boydti.fawe.object.brush.ErodeBrush;
import com.boydti.fawe.object.brush.HeightBrush;
import com.boydti.fawe.object.brush.LineBrush;
import com.boydti.fawe.object.brush.RecurseBrush;
import com.boydti.fawe.object.brush.SplineBrush;
import com.boydti.fawe.object.mask.IdMask;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.EmptyClipboardException;
import com.sk89q.worldedit.LocalConfiguration;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.command.tool.BrushTool;
import com.sk89q.worldedit.command.tool.brush.ButcherBrush;
import com.sk89q.worldedit.command.tool.brush.ClipboardBrush;
import com.sk89q.worldedit.command.tool.brush.CylinderBrush;
import com.sk89q.worldedit.command.tool.brush.GravityBrush;
import com.sk89q.worldedit.command.tool.brush.HollowCylinderBrush;
import com.sk89q.worldedit.command.tool.brush.HollowSphereBrush;
import com.sk89q.worldedit.command.tool.brush.SmoothBrush;
import com.sk89q.worldedit.command.tool.brush.SphereBrush;
import com.sk89q.worldedit.command.util.CreatureButcher;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.mask.BlockMask;
import com.sk89q.worldedit.function.pattern.BlockPattern;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.util.command.binding.Switch;
import com.sk89q.worldedit.util.command.parametric.Optional;
import java.io.File;


import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Commands to set brush shape.
 */
public class BrushCommands {

    private final WorldEdit worldEdit;

    /**
     * Create a new instance.
     *
     * @param worldEdit reference to WorldEdit
     */
    public BrushCommands(WorldEdit worldEdit) {
        checkNotNull(worldEdit);
        this.worldEdit = worldEdit;
    }

    @Command(
            aliases = { "blendball", "bb" },
            usage = "[radius]",
            desc = "Choose the blend ball brush",
            help = "Chooses the blend ball brush",
            min = 0,
            max = 1
    )
    @CommandPermissions("worldedit.brush.blendball")
    public void blendBallBrush(Player player, LocalSession session, @Optional("5") double radius) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);
        BrushTool tool = session.getBrushTool(player.getItemInHand());
        tool.setSize(radius);
        tool.setBrush(new BlendBall(), "worldedit.brush.blendball");
        player.print(BBC.getPrefix() + BBC.BRUSH_BLEND_BALL.f(radius));
    }

    @Command(
            aliases = { "erode", "e" },
            usage = "[radius]",
            desc = "Choose the erode brush",
            help = "Chooses the erode brush",
            min = 0,
            max = 1
    )
    @CommandPermissions("worldedit.brush.erode")
    public void erodeBrush(Player player, LocalSession session, @Optional("5") double radius) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);
        DoubleActionBrushTool tool = session.getDoubleActionBrushTool(player.getItemInHand());
        tool.setSize(radius);
        tool.setBrush(new ErodeBrush(), "worldedit.brush.erode");
        player.print(BBC.getPrefix() + BBC.BRUSH_ERODE.f(radius));
    }

    @Command(
            aliases = { "recursive", "recurse", "r" },
            usage = "<pattern-to> [radius]",
            desc = "Choose the recursive brush",
            help = "Chooses the recursive brush\n" +
                    "The -d flag Will apply in depth first order",
            min = 0,
            max = 3
    )
    @CommandPermissions("worldedit.brush.recursive")
    public void recursiveBrush(Player player, LocalSession session, EditSession editSession, Pattern fill, @Optional("2") double radius, @Switch('d') boolean depthFirst) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);
        BrushTool tool = session.getBrushTool(player.getItemInHand());
        tool.setSize(radius);
        tool.setBrush(new RecurseBrush(tool, depthFirst), "worldedit.brush.recursive");
        tool.setMask(new IdMask(editSession));
        tool.setFill(fill);
        player.print(BBC.getPrefix() + BBC.BRUSH_RECURSIVE.f(radius));
    }

    @Command(
            aliases = { "line", "l" },
            usage = "<pattern> [radius]",
            flags = "hsf",
            desc = "Choose the line brush",
            help =
                    "Chooses the line brush.\n" +
                            "The -h flag creates only a shell\n" +
                            "The -s flag selects the clicked point after drawing\n" +
                            "The -f flag creates a flat line",
            min = 1,
            max = 2
    )
    @CommandPermissions("worldedit.brush.line")
    public void lineBrush(Player player, LocalSession session, Pattern fill, @Optional("0") double radius, @Switch('h') boolean shell, @Switch('s') boolean select, @Switch('f') boolean flat) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);
        DoubleActionBrushTool tool = session.getDoubleActionBrushTool(player.getItemInHand());
        tool.setFill(fill);
        tool.setSize(radius);
        tool.setBrush(new LineBrush(shell, select, flat), "worldedit.brush.line");
        player.print(BBC.getPrefix() + BBC.BRUSH_LINE.f(radius));
    }

    @Command(
            aliases = { "spline", "spl" },
            usage = "<pattern>",
            desc = "Choose the spline brush",
            help = "Chooses the spline brush",
            min = 0,
            max = 2
    )
    @CommandPermissions("worldedit.brush.spline")
    public void splineBrush(Player player, LocalSession session, Pattern fill, @Optional("25") double radius) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);
        DoubleActionBrushTool tool = session.getDoubleActionBrushTool(player.getItemInHand());
        tool.setFill(fill);
        tool.setSize(radius);
        tool.setBrush(new SplineBrush(player, session, tool), "worldedit.brush.spline");
        player.print(BBC.getPrefix() + BBC.BRUSH_SPLINE.f(radius));
    }

    @Command(
            aliases = { "sphere", "s" },
            usage = "<pattern> [radius]",
            flags = "h",
            desc = "Choose the sphere brush",
            help =
                    "Chooses the sphere brush.\n" +
                            "The -h flag creates hollow spheres instead.",
            min = 1,
            max = 2
    )
    @CommandPermissions("worldedit.brush.sphere")
    public void sphereBrush(Player player, LocalSession session, Pattern fill, @Optional("2") double radius, @Switch('h') boolean hollow) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);

        BrushTool tool = session.getBrushTool(player.getItemInHand());
        tool.setFill(fill);
        tool.setSize(radius);

        if (hollow) {
            tool.setBrush(new HollowSphereBrush(), "worldedit.brush.sphere");
        } else {
            tool.setBrush(new SphereBrush(), "worldedit.brush.sphere");
        }
        if (fill instanceof BlockPattern) {
            BaseBlock block = ((BlockPattern) fill).getBlock();
            switch (block.getId()) {
                case BlockID.SAND:
                case BlockID.GRAVEL:
                    player.print(BBC.getPrefix() + BBC.BRUSH_SPHERE.f(radius));
                    BBC.BRUSH_TRY_OTHER.send(player);
                    return;
            }
        }
        player.print(BBC.getPrefix() + BBC.BRUSH_SPHERE.f(radius));
        BBC.TIP_BRUSH_COMMAND.or(BBC.TIP_BRUSH_RELATIVE, BBC.TIP_BRUSH_TRANSFORM, BBC.TIP_BRUSH_MASK_SOURCE, BBC.TIP_BRUSH_MASK, BBC.TIP_BRUSH_COPY, BBC.TIP_BRUSH_HEIGHT, BBC.TIP_BRUSH_SPLINE).send(player);
    }

    @Command(
            aliases = { "cylinder", "cyl", "c" },
            usage = "<block> [radius] [height]",
            flags = "h",
            desc = "Choose the cylinder brush",
            help =
                    "Chooses the cylinder brush.\n" +
                            "The -h flag creates hollow cylinders instead.",
            min = 1,
            max = 3
    )
    @CommandPermissions("worldedit.brush.cylinder")
    public void cylinderBrush(Player player, LocalSession session, Pattern fill,
                              @Optional("2") double radius, @Optional("1") int height, @Switch('h') boolean hollow) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);
        worldEdit.checkMaxBrushRadius(height);

        BrushTool tool = session.getBrushTool(player.getItemInHand());
        tool.setFill(fill);
        tool.setSize(radius);

        if (hollow) {
            tool.setBrush(new HollowCylinderBrush(height), "worldedit.brush.cylinder");
        } else {
            tool.setBrush(new CylinderBrush(height), "worldedit.brush.cylinder");
        }
        player.print(BBC.getPrefix() + BBC.BRUSH_SPHERE.f(radius, height));
    }

    @Command(
            aliases = { "clipboard"},
            usage = "",
            desc = "Choose the clipboard brush",
            help =
                    "Chooses the clipboard brush.\n" +
                            "The -a flag makes it not paste air.\n" +
                            "Without the -p flag, the paste will appear centered at the target location. " +
                            "With the flag, then the paste will appear relative to where you had " +
                            "stood relative to the copied area when you copied it."
    )
    @CommandPermissions("worldedit.brush.clipboard")
    public void clipboardBrush(Player player, LocalSession session, @Switch('a') boolean ignoreAir, @Switch('p') boolean usingOrigin) throws WorldEditException {
        ClipboardHolder holder = session.getClipboard();
        Clipboard clipboard = holder.getClipboard();

        Vector size = clipboard.getDimensions();

        worldEdit.checkMaxBrushRadius(size.getBlockX());
        worldEdit.checkMaxBrushRadius(size.getBlockY());
        worldEdit.checkMaxBrushRadius(size.getBlockZ());

        BrushTool tool = session.getBrushTool(player.getItemInHand());
        tool.setBrush(new ClipboardBrush(holder, ignoreAir, usingOrigin), "worldedit.brush.clipboard");
        player.print(BBC.getPrefix() + BBC.BRUSH_CLIPBOARD.s());
    }

    @Command(
            aliases = { "smooth" },
            usage = "[size] [iterations]",
            flags = "n",
            desc = "Choose the terrain softener brush",
            help =
                    "Chooses the terrain softener brush.\n" +
                            "The -n flag makes it only consider naturally occurring blocks.",
            min = 0,
            max = 2
    )
    @CommandPermissions("worldedit.brush.smooth")
    public void smoothBrush(Player player, LocalSession session, EditSession editSession,
                            @Optional("2") double radius, @Optional("4") int iterations, @Switch('n')
                                    boolean naturalBlocksOnly) throws WorldEditException {

        worldEdit.checkMaxBrushRadius(radius);

        FawePlayer fp = FawePlayer.wrap(player);
        FaweLimit limit = Settings.getLimit(fp);
        iterations = Math.min(limit.MAX_ITERATIONS, iterations);
        BrushTool tool = session.getBrushTool(player.getItemInHand());
        tool.setSize(radius);
        tool.setBrush(new SmoothBrush(iterations, naturalBlocksOnly), "worldedit.brush.smooth");

        player.print(BBC.getPrefix() + BBC.BRUSH_SMOOTH.f(radius, iterations, (naturalBlocksOnly ? "natural blocks only" : "any block")));
    }

    @Command(
            aliases = { "ex", "extinguish" },
            usage = "[radius]",
            desc = "Shortcut fire extinguisher brush",
            min = 0,
            max = 1
    )
    @CommandPermissions("worldedit.brush.ex")
    public void extinguishBrush(Player player, LocalSession session, EditSession editSession, @Optional("5") double radius) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);

        BrushTool tool = session.getBrushTool(player.getItemInHand());
        Pattern fill = new BlockPattern(new BaseBlock(0));
        tool.setFill(fill);
        tool.setSize(radius);
        tool.setMask(new BlockMask(editSession, new BaseBlock(BlockID.FIRE)));
        tool.setBrush(new SphereBrush(), "worldedit.brush.ex");
        BBC.BRUSH_EXTINGUISHER.send(player, radius);
        player.print(BBC.getPrefix() + BBC.BRUSH_EXTINGUISHER.f(radius));
    }

    @Command(
            aliases = { "gravity", "grav" },
            usage = "[radius]",
            flags = "h",
            desc = "Gravity brush",
            help =
                    "This brush simulates the affect of gravity.\n" +
                            "The -h flag makes it affect blocks starting at the world's max y, " +
                            "instead of the clicked block's y + radius.",
            min = 0,
            max = 1
    )
    @CommandPermissions("worldedit.brush.gravity")
    public void gravityBrush(Player player, LocalSession session, @Optional("5") double radius, @Switch('h') boolean fromMaxY) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);

        BrushTool tool = session.getBrushTool(player.getItemInHand());
        tool.setSize(radius);
        tool.setBrush(new GravityBrush(fromMaxY, tool), "worldedit.brush.gravity");
        player.print(BBC.getPrefix() + BBC.BRUSH_GRAVITY.f(radius));
    }

    @Command(
            aliases = { "height", "heightmap" },
            usage = "[radius] [file|#clipboard|null] [rotation] [yscale]",
            flags = "h",
            desc = "Height brush",
            help =
                    "This brush raises land.\n",
            min = 1,
            max = 4
    )
    @CommandPermissions("worldedit.brush.height")
    public void heightBrush(Player player, LocalSession session, @Optional("5") double radius, @Optional("") final String filename, @Optional("0") final int rotation, @Optional("1") final double yscale) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);
        File file = new File(Fawe.imp().getDirectory(), "heightmap" + File.separator + (filename.endsWith(".png") ? filename : filename + ".png"));
        BrushTool tool = session.getBrushTool(player.getItemInHand());
        tool.setSize(radius);
        try {
            tool.setBrush(new HeightBrush(file, rotation, yscale, tool, session.getClipboard().getClipboard()), "worldedit.brush.height");
        } catch (EmptyClipboardException ignore) {
            tool.setBrush(new HeightBrush(file, rotation, yscale, tool, null), "worldedit.brush.height");
        }
        player.print(BBC.getPrefix() + BBC.BRUSH_HEIGHT.f(radius));
    }

    @Command(
            aliases = { "copypaste", "copy", "paste", "cp", "copypasta" },
            usage = "[depth]",
            desc = "Copy Paste brush",
            help =
                    "Left click the base of an object to copy.\n" +
                    "Right click to paste",
            min = 0,
            max = 1
    )
    @CommandPermissions("worldedit.brush.copy")
    public void copy(Player player, LocalSession session, @Optional("5") double radius) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);
        DoubleActionBrushTool tool = session.getDoubleActionBrushTool(player.getItemInHand());
        tool.setSize(radius);
        tool.setBrush(new CopyPastaBrush(player, session, tool), "worldedit.brush.copy");
        player.print(BBC.getPrefix() + BBC.BRUSH_COPY.f(radius));
    }

    @Command(
            aliases = { "command", "cmd" },
            usage = "<radius> [cmd1;cmd2...]",
            desc = "Command brush",
            help =
                    "Right click executes the command at the position.\n",
            min = 2,
            max = 99
    )
    @CommandPermissions("worldedit.brush.command")
    public void command(Player player, LocalSession session, @Optional("5") double radius, CommandContext args) throws WorldEditException {
        BrushTool tool = session.getBrushTool(player.getItemInHand());
        String cmd = args.getJoinedStrings(1);
        tool.setBrush(new CommandBrush(player, tool, cmd, radius), "worldedit.brush.copy");
        player.print(BBC.getPrefix() + BBC.BRUSH_COMMAND.f(cmd));
    }

    @Command(
            aliases = { "butcher", "kill" },
            usage = "[radius]",
            flags = "plangbtfr",
            desc = "Butcher brush",
            help = "Kills nearby mobs within the specified radius.\n" +
                    "Flags:\n" +
                    "  -p also kills pets.\n" +
                    "  -n also kills NPCs.\n" +
                    "  -g also kills Golems.\n" +
                    "  -a also kills animals.\n" +
                    "  -b also kills ambient mobs.\n" +
                    "  -t also kills mobs with name tags.\n" +
                    "  -f compounds all previous flags.\n" +
                    "  -r also destroys armor stands.\n" +
                    "  -l currently does nothing.",
            min = 0,
            max = 1
    )
    @CommandPermissions("worldedit.brush.butcher")
    public void butcherBrush(Player player, LocalSession session, CommandContext args) throws WorldEditException {
        LocalConfiguration config = worldEdit.getConfiguration();

        double radius = args.argsLength() > 0 ? args.getDouble(0) : 5;
        double maxRadius = config.maxBrushRadius;
        // hmmmm not horribly worried about this because -1 is still rather efficient,
        // the problem arises when butcherMaxRadius is some really high number but not infinite
        // - original idea taken from https://github.com/sk89q/worldedit/pull/198#issuecomment-6463108
        if (player.hasPermission("worldedit.butcher")) {
            maxRadius = Math.max(config.maxBrushRadius, config.butcherMaxRadius);
        }
        if (radius > maxRadius) {
            player.printError("Maximum allowed brush radius: " + maxRadius);
            return;
        }

        CreatureButcher flags = new CreatureButcher(player);
        flags.fromCommand(args);

        BrushTool tool = session.getBrushTool(player.getItemInHand());
        tool.setSize(radius);
        tool.setBrush(new ButcherBrush(flags), "worldedit.brush.butcher");
        player.print(BBC.getPrefix() + BBC.BRUSH_BUTCHER.f(radius));
    }

    public static Class<?> inject() {
        return BrushCommands.class;
    }
}