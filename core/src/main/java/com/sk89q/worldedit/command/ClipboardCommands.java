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

import com.boydti.fawe.FaweAPI;
import com.boydti.fawe.config.BBC;
import com.boydti.fawe.object.RunnableVal2;
import com.boydti.fawe.object.clipboard.LazyClipboard;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.sk89q.minecraft.util.commands.Logging;
import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.function.block.BlockReplace;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.internal.annotation.Direction;
import com.sk89q.worldedit.internal.annotation.Selection;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.regions.RegionSelector;
import com.sk89q.worldedit.regions.selector.CuboidRegionSelector;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.util.command.binding.Switch;
import com.sk89q.worldedit.util.command.parametric.Optional;
import java.net.URL;
import java.util.Iterator;
import java.util.List;


import static com.google.common.base.Preconditions.checkNotNull;
import static com.sk89q.minecraft.util.commands.Logging.LogMode.PLACEMENT;
import static com.sk89q.minecraft.util.commands.Logging.LogMode.REGION;

/**
 * Clipboard commands.
 */
public class ClipboardCommands {

    private final WorldEdit worldEdit;

    /**
     * Create a new instance.
     *
     * @param worldEdit reference to WorldEdit
     */
    public ClipboardCommands(WorldEdit worldEdit) {
        checkNotNull(worldEdit);
        this.worldEdit = worldEdit;
    }



    @Command(
            aliases = { "/lazycopy" },
            flags = "em",
            desc = "Lazily copy the selection to the clipboard",
            help = "Lazily copy the selection to the clipboard\n" +
                    "Flags:\n" +
                    "  -e controls whether entities are copied\n" +
                    "  -m sets a source mask so that excluded blocks become air\n" +
                    "WARNING: Pasting entities cannot yet be undone!",
            max = 0
    )
    @CommandPermissions("worldedit.clipboard.lazycopy")
    public void lazyCopy(Player player, LocalSession session, final EditSession editSession,
                         @Selection final Region region, @Switch('e') boolean copyEntities,
                         @Switch('m') Mask mask) throws WorldEditException {

        final Vector origin = region.getMinimumPoint();
        final int mx = origin.getBlockX();
        final int my = origin.getBlockY();
        final int mz = origin.getBlockZ();
        LazyClipboard lazyClipboard = new LazyClipboard() {
            @Override
            public BaseBlock getBlock(int x, int y, int z) {
                return editSession.getLazyBlock(mx + x, my + y, mz + z);
            }

            public BaseBlock getBlockAbs(int x, int y, int z) {
                return editSession.getLazyBlock(x, y, z);
            }

            @Override
            public List<? extends Entity> getEntities() {
                return editSession.getEntities(region);
            }

            @Override
            public void forEach(RunnableVal2<Vector, BaseBlock> task, boolean air) {
                Iterator<BlockVector> iter = region.iterator();
                while (iter.hasNext()) {
                    BlockVector pos = iter.next();
                    BaseBlock block = getBlockAbs((int) pos.x, (int) pos.y, (int) pos.z);
                    if (!air && block == EditSession.nullBlock) {
                        continue;
                    }
                    pos.x -= mx;
                    pos.y -= my;
                    pos.z -= mz;
                    task.run(pos, block);
                }
            }
        };

        BlockArrayClipboard clipboard = new BlockArrayClipboard(region, lazyClipboard);
        clipboard.setOrigin(session.getPlacementPosition(player));
        session.setClipboard(new ClipboardHolder(clipboard, editSession.getWorld().getWorldData()));
        BBC.COMMAND_COPY.send(player, region.getArea());
    }


    @Command(
            aliases = { "/copy" },
            flags = "em",
            desc = "Copy the selection to the clipboard",
            help = "Copy the selection to the clipboard\n" +
                    "Flags:\n" +
                    "  -e controls whether entities are copied\n" +
                    "  -m sets a source mask so that excluded blocks become air\n" +
                    "WARNING: Pasting entities cannot yet be undone!",
            min = 0,
            max = 0
    )
    @CommandPermissions("worldedit.clipboard.copy")
    public void copy(Player player, LocalSession session, EditSession editSession,
                     @Selection Region region, @Switch('e') boolean copyEntities,
                     @Switch('m') Mask mask) throws WorldEditException {

        BlockArrayClipboard clipboard = new BlockArrayClipboard(region, player.getUniqueId());


        clipboard.setOrigin(session.getPlacementPosition(player));
        ForwardExtentCopy copy = new ForwardExtentCopy(editSession, region, clipboard, region.getMinimumPoint());
        if (mask != null) {
            copy.setSourceMask(mask);
        }
        Operations.completeLegacy(copy);
        session.setClipboard(new ClipboardHolder(clipboard, editSession.getWorld().getWorldData()));

        BBC.COMMAND_COPY.send(player, region.getArea());
    }

    @Command(
            aliases = { "/cut" },
            flags = "em",
            usage = "[leave-id]",
            desc = "Cut the selection to the clipboard",
            help = "Copy the selection to the clipboard\n" +
                    "Flags:\n" +
                    "  -e controls whether entities are copied\n" +
                    "  -m sets a source mask so that excluded blocks become air\n" +
                    "WARNING: Cutting and pasting entities cannot yet be undone!",
            min = 0,
            max = 1
    )
    @CommandPermissions("worldedit.clipboard.cut")
    @Logging(REGION)
    public void cut(Player player, LocalSession session, EditSession editSession,
                    @Selection Region region, @Optional("air") Pattern leavePattern, @Switch('e') boolean copyEntities,
                    @Switch('m') Mask mask) throws WorldEditException {

        BlockArrayClipboard clipboard = new BlockArrayClipboard(region, player.getUniqueId());
        clipboard.setOrigin(session.getPlacementPosition(player));
        ForwardExtentCopy copy = new ForwardExtentCopy(editSession, region, clipboard, region.getMinimumPoint());
        copy.setSourceFunction(new BlockReplace(editSession, leavePattern));
        if (mask != null) {
            copy.setSourceMask(mask);
        }
        Operations.completeLegacy(copy);
        session.setClipboard(new ClipboardHolder(clipboard, editSession.getWorld().getWorldData()));

        BBC.COMMAND_CUT.send(player, region.getArea());
    }

    @Command(aliases = { "download" }, desc = "Download your clipboard")
    @Deprecated
    @CommandPermissions({ "worldedit.clipboard.download"})
    public void download(final Player player, final LocalSession session, @Optional("schematic") final String formatName) throws CommandException, WorldEditException {
        final ClipboardFormat format = ClipboardFormat.findByAlias(formatName);
        if (format == null) {
            player.printError("Unknown schematic format: " + formatName);
            return;
        }
        ClipboardHolder holder = session.getClipboard();
        Clipboard clipboard = holder.getClipboard();
        BBC.GENERATING_LINK.send(player, formatName);
        URL url = FaweAPI.upload(clipboard, format);
        if (url == null) {
            BBC.GENERATING_LINK_FAILED.send(player);
        } else {
            BBC.DOWNLOAD_LINK.send(player, url.toString());
        }
    }

    @Command(
            aliases = { "/paste" },
            usage = "",
            flags = "sao",
            desc = "Paste the clipboard's contents",
            help =
                    "Pastes the clipboard's contents.\n" +
                            "Flags:\n" +
                            "  -a skips air blocks\n" +
                            "  -o pastes at the original position\n" +
                            "  -s selects the region after pasting",
            min = 0,
            max = 0
    )
    @CommandPermissions("worldedit.clipboard.paste")
    @Logging(PLACEMENT)
    public void paste(Player player, LocalSession session, final EditSession editSession,
                      @Switch('a') boolean ignoreAirBlocks, @Switch('o') boolean atOrigin,
                      @Switch('s') boolean selectPasted) throws WorldEditException {
        ClipboardHolder holder = session.getClipboard();
        if (holder.getTransform().isIdentity()) {
            place(player, session, editSession, ignoreAirBlocks, atOrigin, selectPasted);
            return;
        }

        Clipboard clipboard = holder.getClipboard();
        Region region = clipboard.getRegion();
        Vector to = atOrigin ? clipboard.getOrigin() : session.getPlacementPosition(player);
        Operation operation = holder
                .createPaste(editSession, editSession.getWorld().getWorldData())
                .to(to)
                .ignoreAirBlocks(ignoreAirBlocks)
                .build();
        Operations.completeLegacy(operation);

        if (selectPasted) {
            Vector max = to.add(region.getMaximumPoint().subtract(region.getMinimumPoint()));
            RegionSelector selector = new CuboidRegionSelector(player.getWorld(), to, max);
            session.setRegionSelector(player.getWorld(), selector);
            selector.learnChanges();
            selector.explainRegionAdjust(player, session);
        }
        BBC.COMMAND_PASTE.send(player, to);
    }

    @Command(
            aliases = { "/place" },
            usage = "",
            flags = "sao",
            desc = "Place the clipboard's contents",
            help =
                    "Places the clipboard's contents.\n" +
                            "Flags:\n" +
                            "  -a skips air blocks\n" +
                            "  -o pastes at the original position\n" +
                            "  -s selects the region after pasting",
            min = 0,
            max = 0
    )

    // Skips all transforms
    @CommandPermissions("worldedit.clipboard.place")
    @Logging(PLACEMENT)
    public void place(Player player, LocalSession session, final EditSession editSession,
                      @Switch('a') boolean ignoreAirBlocks, @Switch('o') boolean atOrigin,
                      @Switch('s') boolean selectPasted) throws WorldEditException {
        ClipboardHolder holder = session.getClipboard();
        Clipboard clipboard = holder.getClipboard();
        Region region = clipboard.getRegion().clone();


        final Vector bot = clipboard.getMinimumPoint();
        final Vector origin = clipboard.getOrigin();
        final Vector to = atOrigin ? origin : session.getPlacementPosition(player);
        // Optimize for BlockArrayClipboard
        if (clipboard instanceof BlockArrayClipboard && region instanceof CuboidRegion) {
            // To is relative to the world origin (player loc + small clipboard offset) (As the positions supplied are relative to the clipboard min)
            final int relx = to.getBlockX() + bot.getBlockX() - origin.getBlockX();
            final int rely = to.getBlockY() + bot.getBlockY() - origin.getBlockY();
            final int relz = to.getBlockZ() + bot.getBlockZ() - origin.getBlockZ();
            BlockArrayClipboard bac = (BlockArrayClipboard) clipboard;
            bac.IMP.forEach(new RunnableVal2<Vector, BaseBlock>() {
                @Override
                public void run(Vector pos, BaseBlock block) {
                    pos.x += relx;
                    pos.y += rely;
                    pos.z += relz;
                    try {
                        editSession.setBlock(pos, block);
                    } catch (MaxChangedBlocksException e) {
                        throw new RuntimeException(e);
                    }
                }
            }, !ignoreAirBlocks);
        } else {
            // To must be relative to the clipboard origin ( player location - clipboard origin ) (as the locations supplied are relative to the world origin)
            final int relx = to.getBlockX() - origin.getBlockX();
            final int rely = to.getBlockY() - origin.getBlockY();
            final int relz = to.getBlockZ() - origin.getBlockZ();
            Iterator<BlockVector> iter = region.iterator();
            while (iter.hasNext()) {
                BlockVector loc = iter.next();
                BaseBlock block = clipboard.getBlock(loc);
                if (block == EditSession.nullBlock && ignoreAirBlocks) {
                    continue;
                }
                loc.x += relx;
                loc.y += rely;
                loc.z += relz;
                editSession.setBlock(loc, block);
            }
        }
        // Entity offset is the paste location subtract the clipboard origin (entity's location is already relative to the world origin)
        final int entityOffsetX = to.getBlockX() - origin.getBlockX();
        final int entityOffsetY = to.getBlockY() - origin.getBlockY();
        final int entityOffsetZ = to.getBlockZ() - origin.getBlockZ();
        // entities
        for (Entity entity : clipboard.getEntities()) {
            Location pos = entity.getLocation();
            Location newPos = new Location(pos.getExtent(), pos.getX() + entityOffsetX, pos.getY() + entityOffsetY, pos.getZ() + entityOffsetZ, pos.getYaw(), pos.getPitch());
            editSession.createEntity(newPos, entity.getState());
        }
        if (selectPasted) {
            Vector max = to.add(region.getMaximumPoint().subtract(region.getMinimumPoint()));
            RegionSelector selector = new CuboidRegionSelector(player.getWorld(), to, max);
            session.setRegionSelector(player.getWorld(), selector);
            selector.learnChanges();
            selector.explainRegionAdjust(player, session);
        }
        BBC.COMMAND_PASTE.send(player, to);
    }

    @Command(
            aliases = { "/rotate" },
            usage = "<y-axis> [<x-axis>] [<z-axis>]",
            desc = "Rotate the contents of the clipboard",
            help = "Non-destructively rotate the contents of the clipboard.\n" +
                    "Angles are provided in degrees and a positive angle will result in a clockwise rotation. " +
                    "Multiple rotations can be stacked. Interpolation is not performed so angles should be a multiple of 90 degrees.\n"
    )
    @CommandPermissions("worldedit.clipboard.rotate")
    public void rotate(Player player, LocalSession session, Double yRotate, @Optional Double xRotate, @Optional Double zRotate) throws WorldEditException {
        if ((yRotate != null && Math.abs(yRotate % 90) > 0.001) ||
                xRotate != null && Math.abs(xRotate % 90) > 0.001 ||
                zRotate != null && Math.abs(zRotate % 90) > 0.001) {
            player.printDebug("Note: Interpolation is not yet supported, so angles that are multiples of 90 is recommended.");
        }

        ClipboardHolder holder = session.getClipboard();
        AffineTransform transform = new AffineTransform();
        transform = transform.rotateY(-(yRotate != null ? yRotate : 0));
        transform = transform.rotateX(-(xRotate != null ? xRotate : 0));
        transform = transform.rotateZ(-(zRotate != null ? zRotate : 0));
        holder.setTransform(holder.getTransform().combine(transform));
        BBC.COMMAND_ROTATE.send(player);
    }

    @Command(
            aliases = { "/flip" },
            usage = "[<direction>]",
            desc = "Flip the contents of the clipboard",
            help =
                    "Flips the contents of the clipboard across the point from which the copy was made.\n",
            min = 0,
            max = 1
    )
    @CommandPermissions("worldedit.clipboard.flip")
    public void flip(Player player, LocalSession session, EditSession editSession,
                     @Optional(Direction.AIM) @Direction Vector direction) throws WorldEditException {
        ClipboardHolder holder = session.getClipboard();
        Clipboard clipboard = holder.getClipboard();
        AffineTransform transform = new AffineTransform();
        transform = transform.scale(direction.positive().multiply(-2).add(1, 1, 1));
        holder.setTransform(holder.getTransform().combine(transform));
        BBC.COMMAND_FLIPPED.send(player);
    }

    @Command(
            aliases = { "/load" },
            usage = "<filename>",
            desc = "Load a schematic into your clipboard",
            min = 0,
            max = 1
    )
    @Deprecated
    @CommandPermissions("worldedit.clipboard.load")
    public void load(Actor actor) {
        actor.printError("This command is no longer used. See //schematic load.");
    }

    @Command(
            aliases = { "/save" },
            usage = "<filename>",
            desc = "Save a schematic into your clipboard",
            min = 0,
            max = 1
    )
    @Deprecated
    @CommandPermissions("worldedit.clipboard.save")
    public void save(Actor actor) {
        actor.printError("This command is no longer used. See //schematic save.");
    }

    @Command(
            aliases = { "clearclipboard" },
            usage = "",
            desc = "Clear your clipboard",
            min = 0,
            max = 0
    )
    @CommandPermissions("worldedit.clipboard.clear")
    public void clearClipboard(Player player, LocalSession session, EditSession editSession) throws WorldEditException {
        session.setClipboard(null);
        BBC.CLIPBOARD_CLEARED.send(player);
    }
    public static Class<?> inject() {
        return ClipboardCommands.class;
    }
}
