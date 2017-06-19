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
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.FaweLimit;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.clipboard.ReadOnlyClipboard;
import com.boydti.fawe.object.clipboard.WorldCutClipboard;
import com.boydti.fawe.object.exception.FaweException;
import com.boydti.fawe.object.io.FastByteArrayOutputStream;
import com.boydti.fawe.object.schematic.Schematic;
import com.boydti.fawe.util.ImgurUtility;
import com.boydti.fawe.util.MaskTraverser;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.sk89q.minecraft.util.commands.Logging;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.function.block.BlockReplace;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.Masks;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.internal.annotation.Direction;
import com.sk89q.worldedit.internal.annotation.Selection;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.math.transform.Transform;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.regions.RegionSelector;
import com.sk89q.worldedit.regions.selector.CuboidRegionSelector;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.util.command.binding.Switch;
import com.sk89q.worldedit.util.command.parametric.Optional;
import java.io.IOException;
import java.net.URL;


import static com.google.common.base.Preconditions.checkNotNull;
import static com.sk89q.minecraft.util.commands.Logging.LogMode.PLACEMENT;
import static com.sk89q.minecraft.util.commands.Logging.LogMode.REGION;

/**
 * Clipboard commands.
 */
@Command(aliases = {}, desc = "Related commands to copy and pasting blocks: [More Info](http://wiki.sk89q.com/wiki/WorldEdit/Clipboard)")
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
    public void lazyCopy(Player player, LocalSession session, EditSession editSession,
                         @Selection final Region region, @Switch('e') boolean copyEntities,
                         @Switch('m') Mask mask) throws WorldEditException {
        Vector min = region.getMinimumPoint();
        Vector max = region.getMaximumPoint();
        long volume = (((long)max.getX() - (long)min.getX() + 1) * ((long)max.getY() - (long)min.getY() + 1) * ((long)max.getZ() - (long)min.getZ() + 1));
        FaweLimit limit = FawePlayer.wrap(player).getLimit();
        if (volume >= limit.MAX_CHECKS) {
            throw new FaweException(BBC.WORLDEDIT_CANCEL_REASON_MAX_CHECKS);
        }
        session.setClipboard(null);
        final Vector origin = region.getMinimumPoint();
        final int mx = origin.getBlockX();
        final int my = origin.getBlockY();
        final int mz = origin.getBlockZ();
        ReadOnlyClipboard lazyClipboard = ReadOnlyClipboard.of(editSession, region);

        BlockArrayClipboard clipboard = new BlockArrayClipboard(region, lazyClipboard);
        clipboard.setOrigin(session.getPlacementPosition(player));
        session.setClipboard(new ClipboardHolder(clipboard, editSession.getWorldData()));
        BBC.COMMAND_COPY.send(player, region.getArea());
        if (!FawePlayer.wrap(player).hasPermission("fawe.tips")) BBC.TIP_PASTE.or(BBC.TIP_LAZYCOPY, BBC.TIP_DOWNLOAD, BBC.TIP_ROTATE, BBC.TIP_COPYPASTE, BBC.TIP_REPLACE_MARKER, BBC.TIP_COPY_PATTERN).send(player);
    }


    @Command(
            aliases = { "/copy", "/c" },
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
        Vector min = region.getMinimumPoint();
        Vector max = region.getMaximumPoint();
        long volume = (((long)max.getX() - (long)min.getX() + 1) * ((long)max.getY() - (long)min.getY() + 1) * ((long)max.getZ() - (long)min.getZ() + 1));
        FaweLimit limit = FawePlayer.wrap(player).getLimit();
        if (volume >= limit.MAX_CHECKS) {
            throw new FaweException(BBC.WORLDEDIT_CANCEL_REASON_MAX_CHECKS);
        }
        session.setClipboard(null);
        BlockArrayClipboard clipboard = new BlockArrayClipboard(region, player.getUniqueId());
        session.setClipboard(new ClipboardHolder(clipboard, editSession.getWorldData()));

        clipboard.setOrigin(session.getPlacementPosition(player));
        ForwardExtentCopy copy = new ForwardExtentCopy(editSession, region, clipboard, region.getMinimumPoint());
        Mask sourceMask = editSession.getSourceMask();
        if (sourceMask != null) {
            new MaskTraverser(sourceMask).reset(editSession);
            copy.setSourceMask(sourceMask);
            editSession.setSourceMask(null);
        }
        if (mask != null && mask != Masks.alwaysTrue()) {
            copy.setSourceMask(mask);
        }
        Operations.completeLegacy(copy);
        BBC.COMMAND_COPY.send(player, region.getArea());
        if (!FawePlayer.wrap(player).hasPermission("fawe.tips")) BBC.TIP_PASTE.or(BBC.TIP_DOWNLOAD, BBC.TIP_ROTATE, BBC.TIP_COPYPASTE, BBC.TIP_REPLACE_MARKER, BBC.TIP_COPY_PATTERN).send(player);
    }

    @Command(
            aliases = { "/lazycut" },
            flags = "em",
            desc = "Lazily cut the selection to the clipboard",
            help = "Lazily cut the selection to the clipboard\n" +
                    "Flags:\n" +
                    "  -e controls whether entities are cut\n" +
                    "  -m sets a source mask so that excluded blocks become air\n" +
                    "WARNING: Pasting entities cannot yet be undone!",
            max = 0
    )
    @CommandPermissions("worldedit.clipboard.lazycut")
    public void lazyCut(Player player, LocalSession session, EditSession editSession,
                         @Selection final Region region, @Switch('e') boolean copyEntities,
                         @Switch('m') Mask mask) throws WorldEditException {
        Vector min = region.getMinimumPoint();
        Vector max = region.getMaximumPoint();
        long volume = (((long)max.getX() - (long)min.getX() + 1) * ((long)max.getY() - (long)min.getY() + 1) * ((long)max.getZ() - (long)min.getZ() + 1));
        FaweLimit limit = FawePlayer.wrap(player).getLimit();
        if (volume >= limit.MAX_CHECKS) {
            throw new FaweException(BBC.WORLDEDIT_CANCEL_REASON_MAX_CHECKS);
        }
        if (volume >= limit.MAX_CHANGES) {
            throw new FaweException(BBC.WORLDEDIT_CANCEL_REASON_MAX_CHANGES);
        }
        session.setClipboard(null);
        final Vector origin = region.getMinimumPoint();
        final int mx = origin.getBlockX();
        final int my = origin.getBlockY();
        final int mz = origin.getBlockZ();
        ReadOnlyClipboard lazyClipboard = new WorldCutClipboard(editSession, region);
        BlockArrayClipboard clipboard = new BlockArrayClipboard(region, lazyClipboard);
        clipboard.setOrigin(session.getPlacementPosition(player));
        session.setClipboard(new ClipboardHolder(clipboard, editSession.getWorldData()));
        BBC.COMMAND_CUT_LAZY.send(player, region.getArea());
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
        Vector min = region.getMinimumPoint();
        Vector max = region.getMaximumPoint();
        long volume = (((long)max.getX() - (long)min.getX() + 1) * ((long)max.getY() - (long)min.getY() + 1) * ((long)max.getZ() - (long)min.getZ() + 1));
        FaweLimit limit = FawePlayer.wrap(player).getLimit();
        if (volume >= limit.MAX_CHECKS) {
            throw new FaweException(BBC.WORLDEDIT_CANCEL_REASON_MAX_CHECKS);
        }
        if (volume >= limit.MAX_CHANGES) {
            throw new FaweException(BBC.WORLDEDIT_CANCEL_REASON_MAX_CHANGES);
        }
        session.setClipboard(null);
        BlockArrayClipboard clipboard = new BlockArrayClipboard(region, player.getUniqueId());
        clipboard.setOrigin(session.getPlacementPosition(player));
        ForwardExtentCopy copy = new ForwardExtentCopy(editSession, region, clipboard, region.getMinimumPoint());
        copy.setSourceFunction(new BlockReplace(editSession, leavePattern));
        Mask sourceMask = editSession.getSourceMask();
        if (sourceMask != null) {
            new MaskTraverser(sourceMask).reset(editSession);
            copy.setSourceMask(sourceMask);
            editSession.setSourceMask(null);
        }
        if (mask != null) {
            copy.setSourceMask(mask);
        }
        Operations.completeLegacy(copy);
        session.setClipboard(new ClipboardHolder(clipboard, editSession.getWorldData()));

        BBC.COMMAND_CUT_SLOW.send(player, region.getArea());
        if (!FawePlayer.wrap(player).hasPermission("fawe.tips")) BBC.TIP_LAZYCUT.send(player);
    }

    @Command(aliases = { "download" }, desc = "Downloads your clipboard through the configured web interface")
    @Deprecated
    @CommandPermissions({ "worldedit.clipboard.download"})
    public void download(final Player player, final LocalSession session, @Optional("schematic") final String formatName) throws CommandException, WorldEditException {
        final ClipboardFormat format = ClipboardFormat.findByAlias(formatName);
        if (format == null) {
            BBC.CLIPBOARD_INVALID_FORMAT.send(player, formatName);
            return;
        }
        ClipboardHolder holder = session.getClipboard();
        Clipboard clipboard = holder.getClipboard();
        final Transform transform = holder.getTransform();
        final Clipboard target;
        // If we have a transform, bake it into the copy
        if (!transform.isIdentity()) {
            final FlattenedClipboardTransform result = FlattenedClipboardTransform.transform(clipboard, transform, holder.getWorldData());
            target = new BlockArrayClipboard(result.getTransformedRegion(), player.getUniqueId());
            target.setOrigin(clipboard.getOrigin());
            Operations.completeLegacy(result.copyTo(target));
        } else {
            target = clipboard;
        }
        BBC.GENERATING_LINK.send(player, formatName);
        URL url;
        switch (format) {
            case PNG:
                try {
                    FastByteArrayOutputStream baos = new FastByteArrayOutputStream(Short.MAX_VALUE);
                    ClipboardWriter writer = format.getWriter(baos);
                    writer.write(target, null);
                    baos.flush();
                    url = ImgurUtility.uploadImage(baos.toByteArray());
                } catch (IOException e) {
                    e.printStackTrace();
                    url = null;
                }
                break;
            case SCHEMATIC:
                if (Settings.IMP.WEB.URL.isEmpty()) {
                    BBC.SETTING_DISABLE.send(player, "web.url");
                    return;
                }
                url = FaweAPI.upload(target, format);
                break;
            default:
                url = null;
                break;
        }
        if (url == null) {
            BBC.GENERATING_LINK_FAILED.send(player);
        } else {
            BBC.DOWNLOAD_LINK.send(player, url.toString());
        }
    }

    @Command(
            aliases = { "asset", "createasset", "makeasset" },
            usage = "[category]",
            desc = "Create an asset",
            help = "Saves your clipboard to the asset web interface",
            min = 1,
            max = 1
    )
    @CommandPermissions({ "worldedit.clipboard.asset"})
    public void asset(final Player player, final LocalSession session, String category) throws CommandException, WorldEditException {
        final ClipboardFormat format = ClipboardFormat.SCHEMATIC;
        ClipboardHolder holder = session.getClipboard();
        Clipboard clipboard = holder.getClipboard();
        final Transform transform = holder.getTransform();
        final Clipboard target;
        // If we have a transform, bake it into the copy
        if (!transform.isIdentity()) {
            final FlattenedClipboardTransform result = FlattenedClipboardTransform.transform(clipboard, transform, holder.getWorldData());
            target = new BlockArrayClipboard(result.getTransformedRegion(), player.getUniqueId());
            target.setOrigin(clipboard.getOrigin());
            Operations.completeLegacy(result.copyTo(target));
        } else {
            target = clipboard;
        }
        BBC.GENERATING_LINK.send(player, format.name());
        if (Settings.IMP.WEB.ASSETS.isEmpty()) {
            BBC.SETTING_DISABLE.send(player, "web.assets");
            return;
        }
        URL url = format.uploadPublic(target, category.replaceAll("[/|\\\\]", "."), player.getName());
        if (url == null) {
            BBC.GENERATING_LINK_FAILED.send(player);
        } else {
            BBC.DOWNLOAD_LINK.send(player, Settings.IMP.WEB.ASSETS);
        }
    }

    @Command(
            aliases = { "/paste", "/p" },
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
    public void paste(Player player, LocalSession session, EditSession editSession,
                      @Switch('a') boolean ignoreAirBlocks, @Switch('o') boolean atOrigin,
                      @Switch('s') boolean selectPasted) throws WorldEditException {
        ClipboardHolder holder = session.getClipboard();
        if (holder.getTransform().isIdentity() && editSession.getSourceMask() == null) {
            place(player, session, editSession, ignoreAirBlocks, atOrigin, selectPasted);
            return;
        }
        Clipboard clipboard = holder.getClipboard();
        Region region = clipboard.getRegion();
        Vector to = atOrigin ? clipboard.getOrigin() : session.getPlacementPosition(player);
        Operation operation = holder
                .createPaste(editSession, editSession.getWorldData())
                .to(to)
                .ignoreAirBlocks(ignoreAirBlocks)
                .build();
        Operations.completeLegacy(operation);

        if (selectPasted) {
            Vector clipboardOffset = clipboard.getRegion().getMinimumPoint().subtract(clipboard.getOrigin());
            Vector realTo = to.add(new Vector(holder.getTransform().apply(clipboardOffset)));
            Vector max = realTo.add(new Vector(holder.getTransform().apply(region.getMaximumPoint().subtract(region.getMinimumPoint()))));
            RegionSelector selector = new CuboidRegionSelector(player.getWorld(), realTo, max);
            session.setRegionSelector(player.getWorld(), selector);
            selector.learnChanges();
            selector.explainRegionAdjust(player, session);
        }
        BBC.COMMAND_PASTE.send(player, to);
        if (!FawePlayer.wrap(player).hasPermission("fawe.tips")) BBC.TIP_COPYPASTE.or(BBC.TIP_SOURCE_MASK, BBC.TIP_REPLACE_MARKER).send(player, to);
    }

    @Command(
            aliases = { "/place" },
            usage = "",
            flags = "sao",
            desc = "Place the clipboard's contents without applying transformations (e.g. rotate)",
            help =
                    "Places the clipboard's contents without applying transformations (e.g. rotate).\n" +
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
                      @Switch('a') final boolean ignoreAirBlocks, @Switch('o') boolean atOrigin,
                      @Switch('s') boolean selectPasted) throws WorldEditException {
        ClipboardHolder holder = session.getClipboard();
        final Clipboard clipboard = holder.getClipboard();
        final Vector origin = clipboard.getOrigin();
        final Vector to = atOrigin ? origin : session.getPlacementPosition(player);

        Schematic schem = new Schematic(clipboard);
        schem.paste(editSession, to, !ignoreAirBlocks);

        Region region = clipboard.getRegion().clone();
        if (selectPasted) {
            Vector max = to.add(region.getMaximumPoint().subtract(region.getMinimumPoint()));
            RegionSelector selector = new CuboidRegionSelector(player.getWorld(), to, max);
            session.setRegionSelector(player.getWorld(), selector);
            selector.learnChanges();
            selector.explainRegionAdjust(player, session);
        }
        BBC.COMMAND_PASTE.send(player, to);
        FawePlayer<Object> fp = FawePlayer.wrap(player);
        if (!fp.hasPermission("fawe.tips")) {
            BBC.TIP_COPYPASTE.send(fp);
        }
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
        if (!FawePlayer.wrap(player).hasPermission("fawe.tips")) BBC.TIP_FLIP.or(BBC.TIP_DEFORM, BBC.TIP_TRANSFORM).send(player);
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
    public void flip(Player player, LocalSession session,
                     @Optional(Direction.AIM) @Direction Vector direction) throws WorldEditException {
        ClipboardHolder holder = session.getClipboard();
        Clipboard clipboard = holder.getClipboard();
        AffineTransform transform = new AffineTransform();
        transform = transform.scale(direction.positive().multiply(-2).add(1, 1, 1));
        holder.setTransform(holder.getTransform().combine(transform));
        BBC.COMMAND_FLIPPED.send(player);
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
