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
import com.boydti.fawe.object.brush.CircleBrush;
import com.boydti.fawe.object.brush.CommandBrush;
import com.boydti.fawe.object.brush.CopyPastaBrush;
import com.boydti.fawe.object.brush.ErodeBrush;
import com.boydti.fawe.object.brush.FlattenBrush;
import com.boydti.fawe.object.brush.HeightBrush;
import com.boydti.fawe.object.brush.LineBrush;
import com.boydti.fawe.object.brush.RaiseBrush;
import com.boydti.fawe.object.brush.RecurseBrush;
import com.boydti.fawe.object.brush.SplineBrush;
import com.boydti.fawe.object.brush.TargetMode;
import com.boydti.fawe.object.brush.heightmap.ScalableHeightMap;
import com.boydti.fawe.object.brush.scroll.ScrollClipboard;
import com.boydti.fawe.object.brush.scroll.ScrollMask;
import com.boydti.fawe.object.brush.scroll.ScrollPattern;
import com.boydti.fawe.object.brush.scroll.ScrollRange;
import com.boydti.fawe.object.brush.scroll.ScrollSize;
import com.boydti.fawe.object.brush.scroll.ScrollTarget;
import com.boydti.fawe.object.brush.visualization.VisualMode;
import com.boydti.fawe.object.clipboard.LazyClipboardHolder;
import com.boydti.fawe.object.io.FastByteArrayOutputStream;
import com.boydti.fawe.object.mask.IdMask;
import com.boydti.fawe.util.MathMan;
import com.google.common.io.ByteSource;
import com.google.common.io.Files;
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
import com.sk89q.worldedit.event.platform.CommandEvent;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.extension.platform.CommandManager;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.function.mask.BlockMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.pattern.BlockPattern;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.util.command.binding.Switch;
import com.sk89q.worldedit.util.command.parametric.Optional;
import com.sk89q.worldedit.world.registry.WorldData;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


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
            aliases = { "primary" },
            usage = "[brush arguments]",
            desc = "Set the right click brush",
            help = "Set the right click brush",
            min = 1
    )
    public void primary(Player player, LocalSession session, CommandContext args) throws WorldEditException {
        int item = player.getItemInHand();
        BrushTool tool = session.getBrushTool(item, player, false);
        session.setTool(item, null);
        String cmd = "brush " + args.getJoinedStrings(0);
        CommandEvent event = new CommandEvent(player, cmd);
        CommandManager.getInstance().handleCommandOnCurrentThread(event);
        BrushTool newTool = session.getBrushTool(item, player, false);
        if (newTool != null && tool != null) {
            newTool.setSecondary(tool.getSecondary());
        }
    }

    @Command(
            aliases = { "secondary" },
            usage = "[brush arguments]",
            desc = "Set the left click brush",
            help = "Set the left click brush",
            min = 1
    )
    public void secondary(Player player, LocalSession session, CommandContext args) throws WorldEditException {
        int item = player.getItemInHand();
        BrushTool tool = session.getBrushTool(item, player, false);
        session.setTool(item, null);
        String cmd = "brush " + args.getJoinedStrings(0);
        CommandEvent event = new CommandEvent(player, cmd);
        CommandManager.getInstance().handleCommandOnCurrentThread(event);
        BrushTool newTool = session.getBrushTool(item, player, false);
        if (newTool != null && tool != null) {
            newTool.setPrimary(tool.getPrimary());
        }
    }

    @Command(
            aliases = { "visualize", "visual", "vis" },
            usage = "[mode]",
            desc = "Toggle between different visualization modes",
            min = 0,
            max = 1
    )
    public void visual(Player player, LocalSession session, @Optional("0") int mode) throws WorldEditException {
        int item = player.getItemInHand();
        BrushTool tool = session.getBrushTool(item, player, false);
        if (tool == null) {
            BBC.BRUSH_NONE.send(player);
            return;
        }
        VisualMode[] modes = VisualMode.values();
        VisualMode newMode = modes[MathMan.wrap(mode, 0, modes.length - 1)];
        tool.setVisualMode(newMode);
        BBC.BRUSH_VISUAL_MODE_SET.send(player, newMode);
    }

    @Command(
            aliases = { "target", "tar" },
            usage = "[mode]",
            desc = "Toggle between different target modes",
            min = 0,
            max = 1
    )
    public void target(Player player, LocalSession session, @Optional("0") int mode) throws WorldEditException {
        int item = player.getItemInHand();
        BrushTool tool = session.getBrushTool(item, player, false);
        if (tool == null) {
            BBC.BRUSH_NONE.send(player);
            return;
        }
        TargetMode[] modes = TargetMode.values();
        TargetMode newMode = modes[MathMan.wrap(mode, 0, modes.length - 1)];
        tool.setTargetMode(newMode);
        BBC.BRUSH_TARGET_MODE_SET.send(player, newMode);
    }


    @Command(
            aliases = { "scroll" },
            usage = "[none|clipboard|mask|pattern|range|size|visual|target]",
            desc = "Toggle between different target modes",
            min = 1,
            max = -1
    )
    public void scroll(Player player, EditSession editSession, LocalSession session, CommandContext args) throws WorldEditException {
        int item = player.getItemInHand();
        BrushTool tool = session.getBrushTool(item, player, false);
        if (tool == null) {
            BBC.BRUSH_NONE.send(player);
            return;
        }
        ParserContext parserContext = new ParserContext();
        parserContext.setActor(player);
        parserContext.setWorld(player.getWorld());
        parserContext.setSession(session);
        parserContext.setExtent(editSession);
        final LocalConfiguration config = this.worldEdit.getConfiguration();
        switch (args.getString(0).toLowerCase()) {
            case "none":
                tool.setScrollAction(null);
                break;
            case "clipboard":
                if (args.argsLength() != 2) {
                    BBC.COMMAND_SYNTAX.send(player, "clipboard [file]");
                    return;
                }
                String filename = args.getString(1);
                try {
                    WorldData worldData = player.getWorld().getWorldData();
                    if (filename.startsWith("http")) {
                        URL url = new URL(filename);
                        URL webInterface = new URL(Settings.IMP.WEB.URL);
                        if (!url.getHost().equalsIgnoreCase(webInterface.getHost())) {
                            BBC.WEB_UNAUTHORIZED.send(player, url);
                            return;
                        }
                        List<LazyClipboardHolder> clipboards = new ArrayList<>();
                        try (ReadableByteChannel rbc = Channels.newChannel(url.openStream())) {
                            try (InputStream in = Channels.newInputStream(rbc)) {
                                try (ZipInputStream zip = new ZipInputStream(in)) {
                                    ZipEntry entry;
                                    byte[] buffer = new byte[8192];
                                    while ((entry = zip.getNextEntry()) != null) {
                                        if (entry.getName().endsWith(".schematic")) {
                                            FastByteArrayOutputStream out = new FastByteArrayOutputStream();
                                            int len = 0;
                                            while ((len = zip.read(buffer)) > 0) {
                                                out.write(buffer, 0, len);
                                            }
                                            byte[] array = out.toByteArray();
                                            ByteSource source = ByteSource.wrap(array);
                                            LazyClipboardHolder clipboard = new LazyClipboardHolder(source, ClipboardFormat.SCHEMATIC, worldData, null);
                                            clipboards.add(clipboard);
                                        }
                                    }
                                }
                            }
                        }
                        tool.setScrollAction(new ScrollClipboard(tool, session, clipboards.toArray(new LazyClipboardHolder[clipboards.size()])));
                    } else {
                        if (filename.contains("../") && !player.hasPermission("worldedit.schematic.load.other")) {
                            BBC.NO_PERM.send(player, "worldedit.schematic.load.other");
                            return;
                        }
                        File dir = new File(this.worldEdit.getWorkingDirectoryFile(config.saveDir), player.getUniqueId() + File.separator + filename);
                        if (!dir.exists()) {
                            if (!filename.contains("/") && !filename.contains("\\")) {
                                dir = new File(this.worldEdit.getWorkingDirectoryFile(config.saveDir), filename);
                            }
                        }
                        if (!dir.exists() || !dir.isDirectory()) {
                            BBC.SCHEMATIC_NOT_FOUND.send(player, filename);
                            return;
                        }
                        File[] files = dir.listFiles(new FileFilter() {
                            @Override
                            public boolean accept(File pathname) {
                                return pathname.getName().endsWith(".schematic");
                            }
                        });
                        if (files.length < 1) {
                            BBC.SCHEMATIC_NOT_FOUND.send(player, filename);
                            return;
                        }
                        LazyClipboardHolder[] clipboards = new LazyClipboardHolder[files.length];
                        for (int i = 0; i < files.length; i++) {
                            File file = files[i];
                            ByteSource source = Files.asByteSource(file);
                            clipboards[i] = new LazyClipboardHolder(source, ClipboardFormat.SCHEMATIC, worldData, null);
                        }
                        tool.setScrollAction(new ScrollClipboard(tool, session, clipboards));
                    }
                    break;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            case "mask":
                if (args.argsLength() < 2) {
                    BBC.COMMAND_SYNTAX.send(player, "mask [mask 1] [mask 2] [mask 3]...");
                    return;
                }
                Mask[] masks = new Mask[args.argsLength() - 1];
                for (int i = 1; i < args.argsLength(); i++) {
                    String arg = args.getString(i);
                    masks[i - 1] = worldEdit.getMaskFactory().parseFromInput(arg, parserContext);
                }
                tool.setScrollAction(new ScrollMask(tool, masks));
                break;
            case "pattern":
                if (args.argsLength() < 2) {
                    BBC.COMMAND_SYNTAX.send(player, "pattern [pattern 1] [pattern 2] [pattern 3]...");
                    return;
                }
                Pattern[] patterns = new Pattern[args.argsLength() - 1];
                for (int i = 1; i < args.argsLength(); i++) {
                    String arg = args.getString(i);
                    patterns[i - 1] = worldEdit.getPatternFactory().parseFromInput(arg, parserContext);
                }
                tool.setScrollAction(new ScrollPattern(tool, patterns));
                break;
            case "range":
                tool.setScrollAction(new ScrollRange(tool));
                break;
            case "size":
                tool.setScrollAction(new ScrollSize(tool));
                break;
            case "target":
                tool.setScrollAction(new ScrollTarget(tool));
                break;
            default:
                BBC.COMMAND_SYNTAX.send(player, "[none|clipboard|mask|pattern|range|size|visual|target]");
                return;

        }
        BBC.BRUSH_SCROLL_ACTION_SET.send(player, args.getJoinedStrings(0));
    }

    @Command(
            aliases = { "blendball", "bb", "blend" },
            usage = "[radius]",
            desc = "Choose the blend ball brush",
            help = "Chooses the blend ball brush",
            min = 0,
            max = 1
    )
    @CommandPermissions("worldedit.brush.blendball")
    public void blendBallBrush(Player player, LocalSession session, @Optional("5") double radius) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);
        BrushTool tool = session.getBrushTool(player.getItemInHand(), player);
        tool.setSize(radius);
        tool.setBrush(new BlendBall(), "worldedit.brush.blendball", player);
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
        BrushTool tool = session.getBrushTool(player.getItemInHand(), player);
        tool.setSize(radius);
        tool.setBrush(new ErodeBrush(), "worldedit.brush.erode", player);
        player.print(BBC.getPrefix() + BBC.BRUSH_ERODE.f(radius));
    }

    @Command(
            aliases = { "pull" },
            usage = "[radius]",
            desc = "Choose the raise brush",
            help = "Chooses the raise brush",
            min = 0,
            max = 1
    )
    @CommandPermissions("worldedit.brush.pull")
    public void pullBrush(Player player, LocalSession session, @Optional("5") double radius) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);
        BrushTool tool = session.getBrushTool(player.getItemInHand(), player);
        tool.setSize(radius);
        tool.setBrush(new RaiseBrush(), "worldedit.brush.pull", player);
        player.print(BBC.getPrefix() + BBC.BRUSH_ERODE.f(radius));
    }

    @Command(
            aliases = { "circle" },
            usage = "<pattern> [radius]",
            desc = "Choose the circle brush",
            help = "Chooses the circle brush.",
            min = 1,
            max = 2
    )
    @CommandPermissions("worldedit.brush.sphere")
    public void circleBrush(Player player, LocalSession session, Pattern fill, @Optional("5") double radius) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);
        BrushTool tool = session.getBrushTool(player.getItemInHand(), player);
        tool.setSize(radius);
        tool.setFill(fill);
        tool.setBrush(new CircleBrush(tool, player), "worldedit.brush.circle", player);
        player.print(BBC.getPrefix() + BBC.BRUSH_CIRCLE.f(radius));
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
        BrushTool tool = session.getBrushTool(player.getItemInHand(), player);
        tool.setSize(radius);
        tool.setBrush(new RecurseBrush(tool, depthFirst), "worldedit.brush.recursive", player);
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
        BrushTool tool = session.getBrushTool(player.getItemInHand(), player);
        tool.setFill(fill);
        tool.setSize(radius);
        tool.setBrush(new LineBrush(shell, select, flat), "worldedit.brush.line", player);
        player.print(BBC.getPrefix() + BBC.BRUSH_LINE.f(radius));
    }

    @Command(
            aliases = { "spline", "spl", "curve" },
            usage = "<pattern>",
            desc = "Choose the spline brush",
            help = "Chooses the spline brush",
            min = 0,
            max = 2
    )
    @CommandPermissions("worldedit.brush.spline")
    public void splineBrush(Player player, LocalSession session, Pattern fill, @Optional("25") double radius) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);
        BrushTool tool = session.getBrushTool(player.getItemInHand(), player);
        tool.setFill(fill);
        tool.setSize(radius);
        tool.setBrush(new SplineBrush(player, session, tool), "worldedit.brush.spline", player);
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

        BrushTool tool = session.getBrushTool(player.getItemInHand(), player);
        tool.setFill(fill);
        tool.setSize(radius);

        if (hollow) {
            tool.setBrush(new HollowSphereBrush(), "worldedit.brush.sphere", player);
        } else {
            tool.setBrush(new SphereBrush(), "worldedit.brush.sphere", player);
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
        if (!FawePlayer.wrap(player).hasPermission("fawe.tips")) BBC.TIP_BRUSH_COMMAND.or(BBC.TIP_BRUSH_RELATIVE, BBC.TIP_BRUSH_TRANSFORM, BBC.TIP_BRUSH_MASK_SOURCE, BBC.TIP_BRUSH_MASK, BBC.TIP_BRUSH_COPY, BBC.TIP_BRUSH_HEIGHT, BBC.TIP_BRUSH_SPLINE).send(player);
    }

//    @Command(
//            aliases = { "test" },
//            usage = "<pattern> [radius] [count] [distance]",
//            flags = "h",
//            desc = "Choose the sphere brush",
//            help =
//                    "Chooses the sphere brush.\n" +
//                            "The -h flag creates hollow spheres instead.",
//            min = 1,
//            max = -1
//    )
//    @CommandPermissions("worldedit.brush.test")
//    public void testBrush(Player player, LocalSession session, Pattern fill, @Optional("10") double radius, @Optional("10") int count, @Optional("10") int distance) throws WorldEditException {
//        worldEdit.checkMaxBrushRadius(radius);
//
//        BrushTool tool = session.getBrushTool(player.getItemInHand(), player);
//        tool.setFill(fill);
//        tool.setSize(radius);
//        tool.setBrush(new Test(count), "worldedit.brush.test");
//        player.print("equiped");
//    }

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

        BrushTool tool = session.getBrushTool(player.getItemInHand(), player);
        tool.setFill(fill);
        tool.setSize(radius);

        if (hollow) {
            tool.setBrush(new HollowCylinderBrush(height), "worldedit.brush.cylinder", player);
        } else {
            tool.setBrush(new CylinderBrush(height), "worldedit.brush.cylinder", player);
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

        BrushTool tool = session.getBrushTool(player.getItemInHand(), player);
        tool.setBrush(new ClipboardBrush(holder, ignoreAir, usingOrigin), "worldedit.brush.clipboard", player);
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
        FaweLimit limit = Settings.IMP.getLimit(fp);
        iterations = Math.min(limit.MAX_ITERATIONS, iterations);
        BrushTool tool = session.getBrushTool(player.getItemInHand(), player);
        tool.setSize(radius);
        tool.setBrush(new SmoothBrush(iterations, naturalBlocksOnly), "worldedit.brush.smooth", player);

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

        BrushTool tool = session.getBrushTool(player.getItemInHand(), player);
        Pattern fill = new BlockPattern(new BaseBlock(0));
        tool.setFill(fill);
        tool.setSize(radius);
        tool.setMask(new BlockMask(editSession, new BaseBlock(BlockID.FIRE)));
        tool.setBrush(new SphereBrush(), "worldedit.brush.ex", player);
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

        BrushTool tool = session.getBrushTool(player.getItemInHand(), player);
        tool.setSize(radius);
        tool.setBrush(new GravityBrush(fromMaxY, tool), "worldedit.brush.gravity", player);
        player.print(BBC.getPrefix() + BBC.BRUSH_GRAVITY.f(radius));
    }

    @Command(
            aliases = { "height", "heightmap" },
            usage = "[radius] [file|#clipboard|null] [rotation] [yscale]",
            flags = "h",
            desc = "Height brush",
            help =
                    "This brush raises and lowers land.\n",
            min = 1,
            max = 4
    )
    @CommandPermissions("worldedit.brush.height")
    public void heightBrush(Player player, LocalSession session, @Optional("5") double radius, @Optional("") final String filename, @Optional("0") final int rotation, @Optional("1") final double yscale) throws WorldEditException {
        terrainBrush(player, session, radius, filename, rotation, yscale, false, ScalableHeightMap.Shape.CONE);
    }

    @Command(
            aliases = { "cliff", "flatcylinder" },
            usage = "[radius] [file|#clipboard|null] [rotation] [yscale]",
            flags = "h",
            desc = "Cliff brush",
            help =
                    "This brush flattens terrain and creates cliffs.\n",
            min = 1,
            max = 4
    )
    @CommandPermissions("worldedit.brush.height")
    public void cliffBrush(Player player, LocalSession session, @Optional("5") double radius, @Optional("") final String filename, @Optional("0") final int rotation, @Optional("1") final double yscale) throws WorldEditException {
        terrainBrush(player, session, radius, filename, rotation, yscale, true, ScalableHeightMap.Shape.CYLINDER);
    }

    @Command(
            aliases = { "flatten", "flatmap", "flat" },
            usage = "[radius] [file|#clipboard|null] [rotation] [yscale]",
            flags = "h",
            desc = "Flatten brush",
            help =
                    "This brush raises and lowers land towards the clicked point\n",
            min = 1,
            max = 4
    )
    @CommandPermissions("worldedit.brush.height")
    public void flattenBrush(Player player, LocalSession session, @Optional("5") double radius, @Optional("") final String filename, @Optional("0") final int rotation, @Optional("1") final double yscale) throws WorldEditException {
        terrainBrush(player, session, radius, filename, rotation, yscale, true, ScalableHeightMap.Shape.CONE);
    }

    private void terrainBrush(Player player, LocalSession session, double radius, String filename, int rotation, double yscale, boolean flat, ScalableHeightMap.Shape shape) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);
        String filenamePng = (filename.endsWith(".png") ? filename : filename + ".png");
        File file = new File(Fawe.imp().getDirectory(), "heightmap" + File.separator + filenamePng);
        InputStream stream = null;
        if (!file.exists()) {
            if (!filename.equals("#clipboard") && filename.length() >= 7) {
                try {
                    URL url;
                    if (filename.startsWith("http")) {
                        url = new URL(filename);
                        if (!url.getHost().equals("i.imgur.com")) {
                            throw new FileNotFoundException(filename);
                        }
                    } else {
                        url = new URL("https://i.imgur.com/" + filenamePng);
                    }
                    ReadableByteChannel rbc = Channels.newChannel(url.openStream());
                    stream = Channels.newInputStream(rbc);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        } else if (!filename.equalsIgnoreCase("#clipboard")){
            try {
                stream = new FileInputStream(file);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        BrushTool tool = session.getBrushTool(player.getItemInHand(), player);
        tool.setSize(radius);
        if (flat) {
            try {
                tool.setBrush(new FlattenBrush(stream, rotation, yscale, tool, filename.equalsIgnoreCase("#clipboard") ? session.getClipboard().getClipboard() : null, shape), "worldedit.brush.height", player);
            } catch (EmptyClipboardException ignore) {
                tool.setBrush(new FlattenBrush(stream, rotation, yscale, tool, null, shape), "worldedit.brush.height", player);
            }
        } else {
            try {
                tool.setBrush(new HeightBrush(stream, rotation, yscale, tool, filename.equalsIgnoreCase("#clipboard") ? session.getClipboard().getClipboard() : null), "worldedit.brush.height", player);
            } catch (EmptyClipboardException ignore) {
                tool.setBrush(new HeightBrush(stream, rotation, yscale, tool, null), "worldedit.brush.height", player);
            }
        }
        player.print(BBC.getPrefix() + BBC.BRUSH_HEIGHT.f(radius));
    }



    @Command(
            aliases = { "copypaste", "copy", "paste", "cp", "copypasta" },
            usage = "[depth]",
            desc = "Copy Paste brush",
            help =
                    "Left click the base of an object to copy.\n" +
                    "Right click to paste\n" +
                    "The -r flag Will apply random rotation on paste",
            min = 0,
            max = 1
    )
    @CommandPermissions("worldedit.brush.copy")
    public void copy(Player player, LocalSession session, @Optional("5") double radius, @Switch('r') boolean rotate) throws WorldEditException {
        worldEdit.checkMaxBrushRadius(radius);
        BrushTool tool = session.getBrushTool(player.getItemInHand(), player);
        tool.setSize(radius);
        tool.setBrush(new CopyPastaBrush(tool, session, rotate), "worldedit.brush.copy", player);
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
        BrushTool tool = session.getBrushTool(player.getItemInHand(), player);
        String cmd = args.getJoinedStrings(1);
        tool.setBrush(new CommandBrush(tool, cmd, radius), "worldedit.brush.copy", player);
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
            BBC.TOOL_RADIUS_ERROR.send(player, maxRadius);
            return;
        }

        CreatureButcher flags = new CreatureButcher(player);
        flags.fromCommand(args);

        BrushTool tool = session.getBrushTool(player.getItemInHand(), player);
        tool.setSize(radius);
        tool.setBrush(new ButcherBrush(flags), "worldedit.brush.butcher", player);
        player.print(BBC.getPrefix() + BBC.BRUSH_BUTCHER.f(radius));
    }

    public static Class<?> inject() {
        return BrushCommands.class;
    }
}