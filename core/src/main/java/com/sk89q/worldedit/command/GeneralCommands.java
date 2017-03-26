package com.sk89q.worldedit.command;

import com.boydti.fawe.config.BBC;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.extent.DefaultTransformParser;
import com.boydti.fawe.object.extent.ResettableExtent;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.ItemType;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.util.command.parametric.Optional;


import static com.google.common.base.Preconditions.checkNotNull;

/**
 * General WorldEdit commands.
 */
public class GeneralCommands {

    private final WorldEdit worldEdit;
    private final DefaultTransformParser transformParser;

    /**
     * Create a new instance.
     *
     * @param worldEdit reference to WorldEdit
     */
    public GeneralCommands(WorldEdit worldEdit) {
        checkNotNull(worldEdit);
        this.worldEdit = worldEdit;
        transformParser = new DefaultTransformParser(worldEdit);
    }

    @Command(
            aliases = { "/tips", "tips" },
            desc = "Toggle WorldEdit tips"
    )
    public void tips(Player player, LocalSession session) throws WorldEditException {
        FawePlayer<Object> fp = FawePlayer.wrap(player);
        if (fp.toggle("fawe.tips")) {
            BBC.WORLDEDIT_TOGGLE_TIPS_ON.send(player);
        } else {
            BBC.WORLDEDIT_TOGGLE_TIPS_OFF.send(player);
        }
    }

    @Command(
            aliases = { "/fast" },
            usage = "[on|off]",
            desc = "Toggle fast mode",
            min = 0,
            max = 1
    )
    @CommandPermissions("worldedit.fast")
    public void fast(Player player, LocalSession session, CommandContext args) throws WorldEditException {

        String newState = args.getString(0, null);
        if (session.hasFastMode()) {
            if ("on".equals(newState)) {
                BBC.FAST_ENABLED.send(player);
                return;
            }

            session.setFastMode(false);
            BBC.FAST_DISABLED.send(player);
        } else {
            if ("off".equals(newState)) {
                BBC.FAST_DISABLED.send(player);
                return;
            }

            session.setFastMode(true);
            BBC.FAST_ENABLED.send(player);
        }
    }

    @Command(
            aliases = { "/gmask", "gmask", "globalmask", "/globalmask" },
            usage = "[mask]",
            desc = "Set the global mask",
            min = 0,
            max = -1
    )
    @CommandPermissions("worldedit.global-mask")
    public void gmask(Player player, LocalSession session, EditSession editSession, @Optional CommandContext context) throws WorldEditException {
        if (context == null || context.argsLength() == 0) {
            session.setMask((Mask) null);
            BBC.MASK_DISABLED.send(player);
        } else {
            ParserContext parserContext = new ParserContext();
            parserContext.setActor(player);
            parserContext.setWorld(player.getWorld());
            parserContext.setSession(session);
            parserContext.setExtent(editSession);
            Mask mask = worldEdit.getMaskFactory().parseFromInput(context.getJoinedStrings(0), parserContext);
            session.setMask(mask);
            BBC.MASK.send(player);
        }
    }

    @Command(
            aliases = { "/gsmask", "gsmask", "globalsourcemask", "/globalsourcemask" },
            usage = "[mask]",
            desc = "Set the global source mask",
            min = 0,
            max = -1
    )
    @CommandPermissions("worldedit.global-mask")
    public void gsmask(Player player, LocalSession session, EditSession editSession, @Optional CommandContext context) throws WorldEditException {
        if (context == null || context.argsLength() == 0) {
            session.setSourceMask((Mask) null);
            BBC.SOURCE_MASK_DISABLED.send(player);
        } else {
            ParserContext parserContext = new ParserContext();
            parserContext.setActor(player);
            parserContext.setWorld(player.getWorld());
            parserContext.setSession(session);
            parserContext.setExtent(editSession);
            Mask mask = worldEdit.getMaskFactory().parseFromInput(context.getJoinedStrings(0), parserContext);
            session.setSourceMask(mask);
            BBC.SOURCE_MASK.send(player);
        }
    }

    @Command(
            aliases = { "/gtransform", "gtransform" },
            usage = "[transform]",
            desc = "Set the global transform",
            min = 0,
            max = -1
    )
    @CommandPermissions("worldedit.global-trasnform")
    public void gtransform(Player player, EditSession editSession, LocalSession session, @Optional CommandContext context) throws WorldEditException {
        if (context == null || context.argsLength() == 0) {
            session.setTransform(null);
            BBC.TRANSFORM_DISABLED.send(player);
        } else {
            ParserContext parserContext = new ParserContext();
            parserContext.setActor(player);
            parserContext.setWorld(player.getWorld());
            parserContext.setSession(session);
            parserContext.setExtent(editSession);
            ResettableExtent transform = transformParser.parseFromInput(context.getJoinedStrings(0), parserContext);
            session.setTransform(transform);
            BBC.TRANSFORM.send(player);
        }
    }

    @Command(
            aliases = { "/toggleplace", "toggleplace" },
            usage = "",
            desc = "Switch between your position and pos1 for placement",
            min = 0,
            max = 0
    )
    public void togglePlace(Player player, LocalSession session, CommandContext args) throws WorldEditException {

        if (session.togglePlacementPosition()) {
            BBC.PLACE_ENABLED.send(player);
        } else {
            BBC.PLACE_DISABLED.send(player);
        }
    }

    @Command(
            aliases = { "/searchitem", "/l", "/search", "searchitem" },
            usage = "<query>",
            flags = "bi",
            desc = "Search for an item",
            help =
                    "Searches for an item.\n" +
                            "Flags:\n" +
                            "  -b only search for blocks\n" +
                            "  -i only search for items",
            min = 1,
            max = 1
    )
    public void searchItem(Actor actor, CommandContext args) throws WorldEditException {

        String query = args.getString(0).trim().toLowerCase();
        boolean blocksOnly = args.hasFlag('b');
        boolean itemsOnly = args.hasFlag('i');

        try {
            int id = Integer.parseInt(query);

            ItemType type = ItemType.fromID(id);

            if (type != null) {
                actor.print(BBC.getPrefix() + "#" + type.getID() + " (" + type.getName() + ")");
            } else {
                actor.printError("No item found by ID " + id);
            }

            return;
        } catch (NumberFormatException ignored) {
        }

        if (query.length() <= 2) {
            actor.printError("Enter a longer search string (len > 2).");
            return;
        }

        if (!blocksOnly && !itemsOnly) {
            actor.print(BBC.getPrefix() + "Searching for: " + query);
        } else if (blocksOnly && itemsOnly) {
            actor.printError("You cannot use both the 'b' and 'i' flags simultaneously.");
            return;
        } else if (blocksOnly) {
            actor.print(BBC.getPrefix() + "Searching for blocks: " + query);
        } else {
            actor.print(BBC.getPrefix() + "Searching for items: " + query);
        }

        int found = 0;

        for (ItemType type : ItemType.values()) {
            if (found >= 15) {
                actor.print(BBC.getPrefix() + "Too many results!");
                break;
            }

            if (blocksOnly && type.getID() > 255) {
                continue;
            }

            if (itemsOnly && type.getID() <= 255) {
                continue;
            }

            for (String alias : type.getAliases()) {
                if (alias.contains(query)) {
                    actor.print(BBC.getPrefix() + "#" + type.getID() + " (" + type.getName() + ")");
                    ++found;
                    break;
                }
            }
        }

        if (found == 0) {
            actor.printError("No items found.");
        }
    }

    public static Class<?> inject() {
        return GeneralCommands.class;
    }
}