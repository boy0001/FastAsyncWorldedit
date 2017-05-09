package com.sk89q.worldedit.command;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweAPI;
import com.boydti.fawe.config.BBC;
import com.boydti.fawe.object.brush.TargetMode;
import com.boydti.fawe.object.brush.scroll.ScrollClipboard;
import com.boydti.fawe.object.brush.scroll.ScrollMask;
import com.boydti.fawe.object.brush.scroll.ScrollPattern;
import com.boydti.fawe.object.brush.scroll.ScrollRange;
import com.boydti.fawe.object.brush.scroll.ScrollSize;
import com.boydti.fawe.object.brush.scroll.ScrollTarget;
import com.boydti.fawe.object.brush.visualization.VisualMode;
import com.boydti.fawe.object.extent.ResettableExtent;
import com.boydti.fawe.util.MathMan;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalConfiguration;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.command.tool.BrushTool;
import com.sk89q.worldedit.command.tool.Tool;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.event.platform.CommandEvent;
import com.sk89q.worldedit.extension.factory.DefaultMaskParser;
import com.sk89q.worldedit.extension.factory.DefaultTransformParser;
import com.sk89q.worldedit.extension.factory.HashTagPatternParser;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.extension.platform.CommandManager;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.util.command.binding.Switch;
import com.sk89q.worldedit.util.command.parametric.Optional;
import java.io.IOException;

/**
 * Tool commands.
 */
@Command(aliases = {}, desc = "Tool commands")
public class ToolUtilCommands extends MethodCommands {

    public ToolUtilCommands(WorldEdit we) {
        super(we);
    }

    @Command(
            aliases = { "/", "," },
            usage = "[on|off]",
            desc = "Toggle the super pickaxe function",
            min = 0,
            max = 1
    )
    @CommandPermissions("worldedit.superpickaxe")
    public void togglePickaxe(Player player, LocalSession session, CommandContext args) throws WorldEditException {
        String newState = args.getString(0, null);
        if (session.hasSuperPickAxe()) {
            if ("on".equals(newState)) {
                BBC.SUPERPICKAXE_ENABLED.send(player);
                return;
            }

            session.disableSuperPickAxe();
            BBC.SUPERPICKAXE_DISABLED.send(player);
        } else {
            if ("off".equals(newState)) {

                BBC.SUPERPICKAXE_DISABLED.send(player);
                return;
            }
            session.enableSuperPickAxe();
            BBC.SUPERPICKAXE_ENABLED.send(player);
        }
    }

    @Command(
            aliases = { "patterns" },
            usage = "[page=1|search|pattern]",
            desc = "View help about patterns",
            help = "Patterns determine what blocks are placed\n" +
                    " - Use [brackets] for arguments\n" +
                    " - Use , to OR multiple\n" +
                    "e.g. #surfacespread[10][#existing],andesite\n" +
                    "More Info: https://git.io/vSPmA",
            min = 1
    )
    public void patterns(Player player, LocalSession session, CommandContext args) throws WorldEditException {
        HashTagPatternParser parser = FaweAPI.getParser(HashTagPatternParser.class);
        if (parser != null) {
            UtilityCommands.help(args, worldEdit, player, "/" + getCommand().aliases()[0] + " ", parser.getDispatcher());
        }
    }

    @Command(
            aliases = { "masks" },
            usage = "[page=1|search|mask]",
            desc = "View help about masks",
            help = "Masks determine if a block can be placed\n" +
                    " - Use [brackets] for arguments\n" +
                    " - Use , to OR multiple\n" +
                    " - Use & to AND multiple\n" +
                    "e.g. >[stone,dirt],#light[0][5],$jungle\n" +
                    "More Info: https://git.io/v9r4K",
            min = 1
    )
    public void masks(Player player, LocalSession session, CommandContext args) throws WorldEditException {
        DefaultMaskParser parser = FaweAPI.getParser(DefaultMaskParser.class);
        if (parser != null) {
            UtilityCommands.help(args, worldEdit, player, "/" + getCommand().aliases()[0] + " ", parser.getDispatcher());
        }
    }

    @Command(
            aliases = { "transforms" },
            usage = "[page=1|search|transform]",
            desc = "View help about transforms",
            help = "Transforms modify how a block is placed\n" +
                    " - Use [brackets] for arguments\n" +
                    " - Use , to OR multiple\n" +
                    " - Use & to AND multiple\n" +
                    "More Info: https://git.io/v9KHO",
            min = 1
    )
    public void transforms(Player player, LocalSession session, CommandContext args) throws WorldEditException {
        DefaultTransformParser parser = Fawe.get().getTransformParser();
        if (parser != null) {
            UtilityCommands.help(args, worldEdit, player, "/" + getCommand().aliases()[0] + " ", parser.getDispatcher());
        }
    }

    @Command(
            aliases = { "primary" },
            usage = "[brush arguments]",
            desc = "Set the right click brush",
            help = "Set the right click brush",
            min = 1
    )
    public void primary(Player player, LocalSession session, CommandContext args) throws WorldEditException {
        BaseBlock item = player.getBlockInHand();
        BrushTool tool = session.getBrushTool(player, false);
        session.setTool(item.getId(), item.getData(), null, player);
        String cmd = "brush " + args.getJoinedStrings(0);
        CommandEvent event = new CommandEvent(player, cmd);
        CommandManager.getInstance().handleCommandOnCurrentThread(event);
        BrushTool newTool = session.getBrushTool(item.getId(), item.getData(), player, false);
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
        BaseBlock item = player.getBlockInHand();
        BrushTool tool = session.getBrushTool(player, false);
        session.setTool(item.getId(), item.getData(), null, player);
        String cmd = "brush " + args.getJoinedStrings(0);
        CommandEvent event = new CommandEvent(player, cmd);
        CommandManager.getInstance().handleCommandOnCurrentThread(event);
        BrushTool newTool = session.getBrushTool(item.getId(), item.getData(), player, false);
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
        BrushTool tool = session.getBrushTool(player, false);
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
        BrushTool tool = session.getBrushTool(player, false);
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
        BrushTool tool = session.getBrushTool(player, false);
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
                    ClipboardHolder[] clipboards = ClipboardFormat.SCHEMATIC.loadAllFromInput(player, player.getWorld().getWorldData(), filename, true);
                    if (clipboards == null) {
                        return;
                    }
                    tool.setScrollAction(new ScrollClipboard(tool, session, clipboards));
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
            aliases = { "mask", "/mask" },
            usage = "[mask]",
            desc = "Set the brush destination mask",
            min = 0,
            max = -1
    )
    @CommandPermissions("worldedit.brush.options.mask")
    public void mask(Player player, LocalSession session, EditSession editSession, @Optional CommandContext context, @Switch('h') boolean offHand) throws WorldEditException {
        Tool tool = session.getTool(player);
        if (tool == null) {
            player.print(BBC.getPrefix() + BBC.BRUSH_NONE.f());
            return;
        }
        Mask mask;
        if (context == null || context.argsLength() == 0) {
            mask = null;
        } else {
            ParserContext parserContext = new ParserContext();
            parserContext.setActor(player);
            parserContext.setWorld(player.getWorld());
            parserContext.setSession(session);
            parserContext.setExtent(editSession);
            mask = worldEdit.getMaskFactory().parseFromInput(context.getJoinedStrings(0), parserContext);
        }
        if (tool instanceof BrushTool) {
            BrushTool bt = (BrushTool) tool;
            if (offHand) {
                bt.getSecondary().mask = mask;
            } else {
                ((BrushTool) tool).setMask(mask);
            }
        }
        if (mask == null) {
            BBC.BRUSH_MASK_DISABLED.send(player);
        } else {
            BBC.BRUSH_MASK.send(player);
        }
    }

    @Command(
            aliases = { "smask", "/smask", "/sourcemask", "sourcemask" },
            usage = "[mask]",
            desc = "Set the brush source mask",
            help = "Set the brush source mask",
            min = 0,
            max = -1
    )
    @CommandPermissions("worldedit.brush.options.mask")
    public void smask(Player player, LocalSession session, EditSession editSession, @Optional CommandContext context, @Switch('h') boolean offHand) throws WorldEditException {
        Tool tool = session.getTool(player);
        if (tool == null) {
            player.print(BBC.getPrefix() + BBC.BRUSH_NONE.f());
            return;
        }
        Mask mask;
        if (context == null || context.argsLength() == 0) {
            mask = null;
        } else {
            ParserContext parserContext = new ParserContext();
            parserContext.setActor(player);
            parserContext.setWorld(player.getWorld());
            parserContext.setSession(session);
            parserContext.setExtent(editSession);
            mask = worldEdit.getMaskFactory().parseFromInput(context.getJoinedStrings(0), parserContext);
        }
        if (tool instanceof BrushTool) {
            BrushTool bt = (BrushTool) tool;
            if (offHand) {
                bt.getSecondary().sourceMask = mask;
            } else {
                ((BrushTool) tool).setSourceMask(mask);
            }
        }
        if (mask == null) {
            BBC.BRUSH_SOURCE_MASK_DISABLED.send(player);
        } else {
            BBC.BRUSH_SOURCE_MASK.send(player);
        }
    }

    @Command(
            aliases = { "transform" },
            usage = "[transform]",
            desc = "Set the brush transform",
            min = 0,
            max = -1
    )
    @CommandPermissions("worldedit.brush.options.transform")
    public void transform(Player player, LocalSession session, EditSession editSession, @Optional CommandContext context, @Switch('h') boolean offHand) throws WorldEditException {
        Tool tool = session.getTool(player);
        if (tool == null) {
            return;
        }
        ResettableExtent transform;
        if (context == null || context.argsLength() == 0) {
            transform = null;
        } else {
            ParserContext parserContext = new ParserContext();
            parserContext.setActor(player);
            parserContext.setWorld(player.getWorld());
            parserContext.setSession(session);
            parserContext.setExtent(editSession);
            transform = Fawe.get().getTransformParser().parseFromInput(context.getJoinedStrings(0), parserContext);
        }
        if (tool instanceof BrushTool) {
            BrushTool bt = (BrushTool) tool;
            if (offHand) {
                bt.getSecondary().transform = transform;
            } else {
                ((BrushTool) tool).setTransform(transform);
            }
        }
        if (transform == null) {
            BBC.BRUSH_TRANSFORM_DISABLED.send(player);
        } else {
            BBC.BRUSH_TRANSFORM.send(player);
        }
    }

    @Command(
            aliases = { "mat", "material" },
            usage = "[pattern]",
            desc = "Set the brush material",
            min = 1,
            max = 1
    )
    @CommandPermissions("worldedit.brush.options.material")
    public void material(Player player, EditSession editSession, LocalSession session, Pattern pattern, @Switch('h') boolean offHand) throws WorldEditException {
        Tool tool = session.getTool(player);
        if (tool instanceof BrushTool) {
            BrushTool bt = (BrushTool) tool;
            if (offHand) {
                bt.getSecondary().material = pattern;
            } else {
                ((BrushTool) tool).setFill(pattern);
            }
        }
        BBC.BRUSH_MATERIAL.send(player);
    }

    @Command(
            aliases = { "range" },
            usage = "[pattern]",
            desc = "Set the brush range",
            min = 1,
            max = 1
    )
    @CommandPermissions("worldedit.brush.options.range")
    public void range(Player player, LocalSession session, CommandContext args) throws WorldEditException {
        int range = Math.max(0, Math.min(256, args.getInteger(0)));
        Tool tool = session.getTool(player);
        if (tool instanceof BrushTool) {
            BrushTool bt = (BrushTool) tool;
            ((BrushTool) tool).setRange(range);
        }
        BBC.BRUSH_RANGE.send(player);
    }

    @Command(
            aliases = { "size" },
            usage = "[pattern]",
            desc = "Set the brush size",
            min = 1,
            max = 1
    )
    @CommandPermissions("worldedit.brush.options.size")
    public void size(Player player, LocalSession session, CommandContext args, @Switch('h') boolean offHand) throws WorldEditException {

        int radius = args.getInteger(0);
        worldEdit.checkMaxBrushRadius(radius);

        Tool tool = session.getTool(player);
        if (tool instanceof BrushTool) {
            BrushTool bt = (BrushTool) tool;
            if (offHand) {
                bt.getSecondary().size = radius;
            } else {
                ((BrushTool) tool).setSize(radius);
            }
        }
        BBC.BRUSH_SIZE.send(player);
    }

    public static Class<?> inject() {
        return ToolUtilCommands.class;
    }
}