package com.sk89q.worldedit.command;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweAPI;
import com.boydti.fawe.config.BBC;
import com.boydti.fawe.object.brush.BrushSettings;
import com.boydti.fawe.object.brush.TargetMode;
import com.boydti.fawe.object.brush.scroll.ScrollAction;
import com.boydti.fawe.object.brush.visualization.VisualMode;
import com.boydti.fawe.object.extent.ResettableExtent;
import com.boydti.fawe.object.io.PGZIPOutputStream;
import com.boydti.fawe.util.MainUtil;
import com.boydti.fawe.util.MathMan;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.command.tool.BrushTool;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.event.platform.CommandEvent;
import com.sk89q.worldedit.extension.factory.DefaultMaskParser;
import com.sk89q.worldedit.extension.factory.DefaultTransformParser;
import com.sk89q.worldedit.extension.factory.HashTagPatternParser;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extension.platform.CommandManager;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.util.command.binding.Switch;
import com.sk89q.worldedit.util.command.parametric.Optional;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.util.zip.GZIPInputStream;

/**
 * Tool commands.
 */
@Command(aliases = {}, desc = "Tool commands")
public class BrushOptionsCommands extends MethodCommands {

    public BrushOptionsCommands(WorldEdit we) {
        super(we);
    }

    @Command(
            aliases = {"/savebrush"},
            usage = "[name]",
            desc = "Save your current brush",
            min = 1
    )
    @CommandPermissions("worldedit.brush.save")
    public void saveBrush(Player player, LocalSession session, String name) throws WorldEditException, IOException {
        BrushTool tool = session.getBrushTool(player);
        if (tool != null) {
            name = FileSystems.getDefault().getPath(name).getFileName().toString();
            File folder = MainUtil.getFile(Fawe.imp().getDirectory(), "brushes");
            name = name.endsWith(".jsgz") ? name : name + ".jsgz";
            File file = new File(folder, player.getUniqueId() + File.separator + name);
            File parent = file.getParentFile();
            if (!parent.exists()) {
                parent.mkdirs();
            }
            file.createNewFile();
            try (DataOutputStream out = new DataOutputStream(new PGZIPOutputStream(new FileOutputStream(file)))) {
                out.writeUTF(tool.toString());
            } catch (Throwable e) {
                e.printStackTrace();
            }
            BBC.SCHEMATIC_SAVED.send(player, name);
        } else {
            BBC.BRUSH_NONE.send(player);
        }
    }

    @Command(
            aliases = {"/loadbrush"},
            desc = "load a brush",
            usage = "[name]",
            min = 1
    )
    @CommandPermissions("worldedit.brush.load")
    public void loadBrush(Player player, LocalSession session, String name) throws WorldEditException, IOException {
        name = FileSystems.getDefault().getPath(name).getFileName().toString();
        File folder = MainUtil.getFile(Fawe.imp().getDirectory(), "brushes");
        name = name.endsWith(".jsgz") ? name : name + ".jsgz";
        File file = new File(folder, player.getUniqueId() + File.separator + name);
        if (!file.exists()) {
            file = new File(folder, name);
        }
        if (!file.exists()) {
            File[] files = folder.listFiles(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    return false;
                }
            });
            BBC.BRUSH_NOT_FOUND.send(player, name);
            return;
        }
        try (DataInputStream in = new DataInputStream(new GZIPInputStream(new FileInputStream(file)))) {
            String json = in.readUTF();
            BrushTool tool = BrushTool.fromString(player, session, json);
            BaseBlock item = player.getBlockInHand();
            session.setTool(item.getId(), item.getData(), tool, player);
            BBC.BRUSH_EQUIPPED.send(player, name);
        } catch (Throwable e) {
            e.printStackTrace();
            BBC.BRUSH_INCOMPATIBLE.send(player);
        }
    }

    @Command(
            aliases = {"/listbrush"},
            desc = "List saved brushes",
            usage = "[mine|<filter>] [page=1]",
            min = 0,
            max = -1,
            flags = "dnp",
            help = "List all brushes in the brush directory\n" +
                    " -p <page> prints the requested page\n"
    )
    @CommandPermissions("worldedit.brush.list")
    public void list(Actor actor, CommandContext args, @Switch('p') @Optional("1") int page) throws WorldEditException {
        File dir = MainUtil.getFile(Fawe.imp().getDirectory(), "brushes");
        UtilityCommands.list(dir, actor, args, page, null, true);
    }

    @Command(
            aliases = {"none", "/none"},
            usage = "",
            desc = "Unbind a bound tool from your current item",
            min = 0,
            max = 0
    )
    public void none(Player player, LocalSession session, CommandContext args) throws WorldEditException {
        session.setTool(null, player);
        BBC.TOOL_NONE.send(player);
    }

    @Command(
            aliases = {"/", ","},
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
            aliases = {"patterns"},
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
            aliases = {"masks"},
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
            aliases = {"transforms"},
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
            aliases = {"primary"},
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
            aliases = {"secondary"},
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
            aliases = {"visualize", "visual", "vis"},
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
            aliases = {"target", "tar"},
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
            aliases = {"targetmask", "tarmask", "tm"},
            usage = "[mask]",
            desc = "Set the targeting mask",
            min = 1,
            max = -1
    )
    public void targetMask(Player player, EditSession editSession, LocalSession session, CommandContext context) throws WorldEditException {
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
        Mask mask = worldEdit.getMaskFactory().parseFromInput(context.getJoinedStrings(0), parserContext);
        tool.setTargetMask(mask);
        BBC.BRUSH_TARGET_MASK_SET.send(player, context.getJoinedStrings(0));
    }

    @Command(
            aliases = {"scroll"},
            usage = "[none|clipboard|mask|pattern|range|size|visual|target]",
            desc = "Toggle between different target modes",
            min = 1,
            max = -1
    )
    public void scroll(Player player, EditSession editSession, LocalSession session, @Optional @Switch('h') boolean offHand, CommandContext args) throws WorldEditException {
        BrushTool bt = session.getBrushTool(player, false);
        if (bt == null) {
            BBC.BRUSH_NONE.send(player);
            return;
        }
        BrushSettings settings = offHand ? bt.getOffHand() : bt.getContext();
        ScrollAction action = ScrollAction.fromArguments(bt, player, session, args.getJoinedStrings(0), true);
        settings.setScrollAction(action);
        if (args.getString(0).equalsIgnoreCase("none")) {
            BBC.BRUSH_SCROLL_ACTION_UNSET.send(player);
        } else if (action != null) {
            String full = args.getJoinedStrings(0);
            settings.addSetting(BrushSettings.SettingType.SCROLL_ACTION, full);
            BBC.BRUSH_SCROLL_ACTION_SET.send(player, full);
        }
    }

    @Command(
            aliases = {"mask", "/mask"},
            usage = "[mask]",
            desc = "Set the brush destination mask",
            min = 0,
            max = -1
    )
    @CommandPermissions("worldedit.brush.options.mask")
    public void mask(Player player, LocalSession session, EditSession editSession, @Optional @Switch('h') boolean offHand, CommandContext context) throws WorldEditException {
        BrushTool tool = session.getBrushTool(player, false);
        if (tool == null) {
            player.print(BBC.getPrefix() + BBC.BRUSH_NONE.f());
            return;
        }
        if (context.argsLength() == 0) {
            BBC.BRUSH_MASK_DISABLED.send(player);
            tool.setMask(null);
            return;
        }
        ParserContext parserContext = new ParserContext();
        parserContext.setActor(player);
        parserContext.setWorld(player.getWorld());
        parserContext.setSession(session);
        parserContext.setExtent(editSession);
        Mask mask = worldEdit.getMaskFactory().parseFromInput(context.getJoinedStrings(0), parserContext);
        BrushSettings settings = offHand ? tool.getOffHand() : tool.getContext();
        settings.addSetting(BrushSettings.SettingType.MASK, context.getString(context.argsLength() - 1));
        settings.setMask(mask);
        BBC.BRUSH_MASK.send(player);
    }

    @Command(
            aliases = {"smask", "/smask", "/sourcemask", "sourcemask"},
            usage = "[mask]",
            desc = "Set the brush source mask",
            help = "Set the brush source mask",
            min = 0,
            max = -1
    )
    @CommandPermissions("worldedit.brush.options.mask")
    public void smask(Player player, LocalSession session, EditSession editSession, @Optional @Switch('h') boolean offHand, CommandContext context) throws WorldEditException {
        BrushTool tool = session.getBrushTool(player, false);
        if (tool == null) {
            player.print(BBC.getPrefix() + BBC.BRUSH_NONE.f());
            return;
        }
        if (context.argsLength() == 0) {
            BBC.BRUSH_SOURCE_MASK_DISABLED.send(player);
            tool.setSourceMask(null);
            return;
        }
        ParserContext parserContext = new ParserContext();
        parserContext.setActor(player);
        parserContext.setWorld(player.getWorld());
        parserContext.setSession(session);
        parserContext.setExtent(editSession);
        Mask mask = worldEdit.getMaskFactory().parseFromInput(context.getJoinedStrings(0), parserContext);
        BrushSettings settings = offHand ? tool.getOffHand() : tool.getContext();
        settings.addSetting(BrushSettings.SettingType.SOURCE_MASK, context.getString(context.argsLength() - 1));
        settings.setSourceMask(mask);
        BBC.BRUSH_SOURCE_MASK.send(player);
    }

    @Command(
            aliases = {"transform"},
            usage = "[transform]",
            desc = "Set the brush transform",
            min = 0,
            max = -1
    )
    @CommandPermissions("worldedit.brush.options.transform")
    public void transform(Player player, LocalSession session, EditSession editSession, @Optional @Switch('h') boolean offHand, CommandContext context) throws WorldEditException {
        BrushTool tool = session.getBrushTool(player, false);
        if (tool == null) {
            player.print(BBC.getPrefix() + BBC.BRUSH_NONE.f());
            return;
        }
        if (context.argsLength() == 0) {
            BBC.BRUSH_TRANSFORM_DISABLED.send(player);
            tool.setTransform(null);
            return;
        }
        ParserContext parserContext = new ParserContext();
        parserContext.setActor(player);
        parserContext.setWorld(player.getWorld());
        parserContext.setSession(session);
        parserContext.setExtent(editSession);
        ResettableExtent transform = Fawe.get().getTransformParser().parseFromInput(context.getJoinedStrings(0), parserContext);
        BrushSettings settings = offHand ? tool.getOffHand() : tool.getContext();
        settings.addSetting(BrushSettings.SettingType.TRANSFORM, context.getString(context.argsLength() - 1));
        settings.setTransform(transform);
        BBC.BRUSH_TRANSFORM.send(player);
    }

    @Command(
            aliases = {"mat", "material"},
            usage = "[pattern]",
            desc = "Set the brush material",
            min = 1,
            max = 1
    )
    @CommandPermissions("worldedit.brush.options.material")
    public void material(Player player, EditSession editSession, LocalSession session, Pattern pattern, @Switch('h') boolean offHand, CommandContext context) throws WorldEditException {
        BrushTool tool = session.getBrushTool(player, false);
        if (tool == null) {
            player.print(BBC.getPrefix() + BBC.BRUSH_NONE.f());
            return;
        }
        if (context.argsLength() == 0) {
            BBC.BRUSH_TRANSFORM_DISABLED.send(player);
            tool.setFill(null);
            return;
        }
        BrushSettings settings = offHand ? tool.getOffHand() : tool.getContext();
        settings.setFill(pattern);
        settings.addSetting(BrushSettings.SettingType.FILL, context.getString(context.argsLength() - 1));
        BBC.BRUSH_MATERIAL.send(player);
    }

    @Command(
            aliases = {"range"},
            usage = "[pattern]",
            desc = "Set the brush range",
            min = 1,
            max = 1
    )
    @CommandPermissions("worldedit.brush.options.range")
    public void range(Player player, LocalSession session, CommandContext args) throws WorldEditException {
        int range = Math.max(0, Math.min(256, args.getInteger(0)));
        BrushTool tool = session.getBrushTool(player, false);
        if (tool == null) {
            player.print(BBC.getPrefix() + BBC.BRUSH_NONE.f());
            return;
        }
        tool.setRange(range);
        BBC.BRUSH_RANGE.send(player);
    }

    @Command(
            aliases = {"size"},
            usage = "[pattern]",
            desc = "Set the brush size",
            min = 1,
            max = 1
    )
    @CommandPermissions("worldedit.brush.options.size")
    public void size(Player player, LocalSession session, CommandContext args, @Switch('h') boolean offHand) throws WorldEditException {
        int radius = args.getInteger(0);
        worldEdit.checkMaxBrushRadius(radius);
        BrushTool tool = session.getBrushTool(player, false);
        if (tool == null) {
            player.print(BBC.getPrefix() + BBC.BRUSH_NONE.f());
            return;
        }
        BrushSettings settings = offHand ? tool.getOffHand() : tool.getContext();
        settings.setSize(radius);
        BBC.BRUSH_SIZE.send(player);
    }

    public static Class<?> inject() {
        return BrushOptionsCommands.class;
    }
}