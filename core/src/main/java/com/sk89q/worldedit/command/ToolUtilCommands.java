package com.sk89q.worldedit.command;

import com.boydti.fawe.config.BBC;
import com.boydti.fawe.object.brush.DoubleActionBrushTool;
import com.boydti.fawe.object.extent.DefaultTransformParser;
import com.boydti.fawe.object.extent.ResettableExtent;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.command.tool.BrushTool;
import com.sk89q.worldedit.command.tool.Tool;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.util.command.parametric.Optional;

/**
 * Tool commands.
 */
public class ToolUtilCommands {
    private final WorldEdit we;
    private final DefaultTransformParser transformParser;

    public ToolUtilCommands(WorldEdit we) {
        this.we = we;
        this.transformParser = new DefaultTransformParser(we);
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
            aliases = { "mask", "/mask" },
            usage = "[mask]",
            desc = "Set the brush mask",
            min = 0,
            max = -1
    )
    @CommandPermissions("worldedit.brush.options.mask")
    public void mask(Player player, LocalSession session, EditSession editSession, @Optional CommandContext context) throws WorldEditException {
        Tool tool = session.getTool(player.getItemInHand());
        if (tool == null) {
            player.print(BBC.getPrefix() + BBC.BRUSH_NONE.f());
            return;
        }
        if (context == null || context.argsLength() == 0) {
            if (tool instanceof BrushTool) {
                ((BrushTool) tool).setMask(null);
            } else if (tool instanceof DoubleActionBrushTool) {
                ((DoubleActionBrushTool) tool).setMask(null);
            }
            BBC.BRUSH_MASK_DISABLED.send(player);
        } else {
            ParserContext parserContext = new ParserContext();
            parserContext.setActor(player);
            parserContext.setWorld(player.getWorld());
            parserContext.setSession(session);
            parserContext.setExtent(editSession);
            Mask mask = we.getMaskFactory().parseFromInput(context.getJoinedStrings(0), parserContext);
            if (tool instanceof BrushTool) {
                ((BrushTool) tool).setMask(mask);
            } else if (tool instanceof DoubleActionBrushTool) {
                ((DoubleActionBrushTool) tool).setMask(mask);
            }
            BBC.BRUSH_MASK.send(player);
        }
    }

    @Command(
            aliases = { "smask", "/smask", "/sourcemask", "sourcemask" },
            usage = "[mask]",
            desc = "Set the brush mask",
            min = 0,
            max = -1
    )
    @CommandPermissions("worldedit.brush.options.mask")
    public void smask(Player player, LocalSession session, EditSession editSession, @Optional CommandContext context) throws WorldEditException {
        Tool tool = session.getTool(player.getItemInHand());
        if (tool == null) {
            player.print(BBC.getPrefix() + BBC.BRUSH_NONE.f());
            return;
        }
        if (context == null || context.argsLength() == 0) {
            if (tool instanceof BrushTool) {
                ((BrushTool) tool).setSourceMask(null);
            } else if (tool instanceof DoubleActionBrushTool) {
                ((DoubleActionBrushTool) tool).setMask(null);
            }
            BBC.BRUSH_SOURCE_MASK_DISABLED.send(player);
        } else {
            ParserContext parserContext = new ParserContext();
            parserContext.setActor(player);
            parserContext.setWorld(player.getWorld());
            parserContext.setSession(session);
            parserContext.setExtent(editSession);
            Mask mask = we.getMaskFactory().parseFromInput(context.getJoinedStrings(0), parserContext);
            if (tool instanceof BrushTool) {
                ((BrushTool) tool).setSourceMask(mask);
            } else if (tool instanceof DoubleActionBrushTool) {
                ((DoubleActionBrushTool) tool).setSourceMask(mask);
            }
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
    public void transform(Player player, LocalSession session, EditSession editSession, @Optional CommandContext context) throws WorldEditException {
        Tool tool = session.getTool(player.getItemInHand());
        if (tool == null) {
            return;
        }
        if (context == null || context.argsLength() == 0) {
            if (tool instanceof BrushTool) {
                ((BrushTool) tool).setTransform(null);
            } else if (tool instanceof DoubleActionBrushTool) {
                ((DoubleActionBrushTool) tool).setTransform(null);
            }
            BBC.BRUSH_TRANSFORM_DISABLED.send(player);
        } else {
            ParserContext parserContext = new ParserContext();
            parserContext.setActor(player);
            parserContext.setWorld(player.getWorld());
            parserContext.setSession(session);
            parserContext.setExtent(editSession);
            ResettableExtent transform = transformParser.parseFromInput(context.getJoinedStrings(0), parserContext);
            if (tool instanceof BrushTool) {
                ((BrushTool) tool).setTransform(transform);
            } else if (tool instanceof DoubleActionBrushTool) {
                ((DoubleActionBrushTool) tool).setTransform(transform);
            }
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
    public void material(Player player, LocalSession session, Pattern pattern) throws WorldEditException {
        Tool tool = session.getTool(player.getItemInHand());
        if (tool instanceof BrushTool) {
            ((BrushTool) tool).setMask(null);
        } else if (tool instanceof DoubleActionBrushTool) {
            ((DoubleActionBrushTool) tool).setFill(pattern);
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
        int range = args.getInteger(0);
        Tool tool = session.getTool(player.getItemInHand());
        if (tool instanceof BrushTool) {
            ((BrushTool) tool).setMask(null);
        } else if (tool instanceof DoubleActionBrushTool) {
            ((DoubleActionBrushTool) tool).setRange(range);
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
    public void size(Player player, LocalSession session, CommandContext args) throws WorldEditException {

        int radius = args.getInteger(0);
        we.checkMaxBrushRadius(radius);

        Tool tool = session.getTool(player.getItemInHand());
        if (tool instanceof BrushTool) {
            ((BrushTool) tool).setMask(null);
        } else if (tool instanceof DoubleActionBrushTool) {
            ((DoubleActionBrushTool) tool).setSize(radius);
        }
        BBC.BRUSH_SIZE.send(player);
    }

    public static Class<?> inject() {
        return ToolUtilCommands.class;
    }
}