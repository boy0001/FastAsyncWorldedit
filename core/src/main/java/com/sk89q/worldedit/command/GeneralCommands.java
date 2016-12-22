package com.sk89q.worldedit.command;

import com.boydti.fawe.config.BBC;
import com.boydti.fawe.object.extent.DefaultTransformParser;
import com.boydti.fawe.object.extent.ResettableExtent;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalConfiguration;
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
            aliases = { "/limit" },
            usage = "<limit>",
            desc = "Modify block change limit",
            min = 1,
            max = 1
    )
    @CommandPermissions("worldedit.limit")
    public void limit(Player player, LocalSession session, CommandContext args) throws WorldEditException {

        LocalConfiguration config = worldEdit.getConfiguration();
        boolean mayDisable = player.hasPermission("worldedit.limit.unrestricted");

        int limit = Math.max(-1, args.getInteger(0));
        if (!mayDisable && config.maxChangeLimit > -1) {
            if (limit > config.maxChangeLimit) {
                player.printError("Your maximum allowable limit is " + config.maxChangeLimit + ".");
                return;
            }
        }

        session.setBlockChangeLimit(limit);

        if (limit != -1) {
            player.print(BBC.getPrefix() + "Block change limit set to " + limit + ". (Use //limit -1 to go back to the default.)");
        } else {
            player.print(BBC.getPrefix() + "Block change limit set to " + limit + ".");
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
                player.printError("Fast mode already enabled.");
                return;
            }

            session.setFastMode(false);
            player.print(BBC.getPrefix() + "Fast mode disabled.");
        } else {
            if ("off".equals(newState)) {
                player.printError("Fast mode already disabled.");
                return;
            }

            session.setFastMode(true);
            player.print(BBC.getPrefix() + "Fast mode enabled. Lighting in the affected chunks may be wrong and/or you may need to rejoin to see changes.");
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
            player.print(BBC.getPrefix() + "Now placing at pos #1.");
        } else {
            player.print(BBC.getPrefix() + "Now placing at the block you stand in.");
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