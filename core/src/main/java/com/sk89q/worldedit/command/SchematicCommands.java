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
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.schematic.StructureFormat;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalConfiguration;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.extent.clipboard.io.SchematicReader;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.transform.Transform;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.util.command.parametric.Optional;
import com.sk89q.worldedit.util.io.file.FilenameException;
import com.sk89q.worldedit.util.io.file.FilenameResolutionException;
import com.sk89q.worldedit.world.registry.WorldData;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Arrays;
import java.util.Comparator;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;


import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Commands that work with schematic files.
 */
public class SchematicCommands {

    private static final Logger log = Logger.getLogger(SchematicCommands.class.getCanonicalName());
    private final WorldEdit worldEdit;

    /**
     * Create a new instance.
     *
     * @param worldEdit reference to WorldEdit
     */
    public SchematicCommands(final WorldEdit worldEdit) {
        checkNotNull(worldEdit);
        this.worldEdit = worldEdit;
    }

    @Command(aliases = { "load" }, usage = "[<format>] <filename>", desc = "Load a schematic into your clipboard")
    @Deprecated
    @CommandPermissions({ "worldedit.clipboard.load", "worldedit.schematic.load" })
    public void load(final Player player, final LocalSession session, @Optional("schematic") final String formatName, final String filename) throws FilenameException {
        final LocalConfiguration config = this.worldEdit.getConfiguration();
        final ClipboardFormat format = ClipboardFormat.findByAlias(formatName);
        if (format == null) {
            player.printError("Unknown schematic format: " + formatName);
            return;
        }
        InputStream in = null;
        try {
            if (filename.startsWith("url:")) {
                UUID uuid = UUID.fromString(filename.substring(4));
                URL base = new URL(Settings.WEB.URL);
                URL url = new URL(base, "uploads/" + uuid + ".schematic");
                ReadableByteChannel rbc = Channels.newChannel(url.openStream());
                in = Channels.newInputStream(rbc);
            } else {
                final File dir = this.worldEdit.getWorkingDirectoryFile(config.saveDir);
                final File f = this.worldEdit.getSafeOpenFile(player, dir, filename, format.getExtension(), format.getExtension());
                if (!f.exists()) {
                    player.printError("Schematic " + filename + " does not exist!");
                    return;
                }
                final String filePath = f.getCanonicalPath();
                final String dirPath = dir.getCanonicalPath();
                if (!filePath.substring(0, dirPath.length()).equals(dirPath)) {
                    player.printError("Clipboard file could not read or it does not exist.");
                }
                in = new FileInputStream(f);
            }
            in = new BufferedInputStream(in);
            final ClipboardReader reader = format.getReader(in);
            final WorldData worldData = player.getWorld().getWorldData();
            final Clipboard clipboard;
            if (reader instanceof SchematicReader) {
                clipboard = ((SchematicReader) reader).read(player.getWorld().getWorldData(), player.getUniqueId());
            } else if (reader instanceof StructureFormat) {
                clipboard = ((StructureFormat) reader).read(player.getWorld().getWorldData(), player.getUniqueId());
            } else {
                clipboard = reader.read(player.getWorld().getWorldData());
            }
            session.setClipboard(new ClipboardHolder(clipboard, worldData));
            BBC.SCHEMATIC_LOADED.send(player, filename);
        } catch (IllegalArgumentException  e) {
            player.printError("Unknown filename: " + filename);
        } catch (IOException e) {
            player.printError("File could not be read or it does not exist: " + e.getMessage());
            log.log(Level.WARNING, "Failed to load a saved clipboard", e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignored) {}
            }
        }
    }

    @Command(aliases = { "save" }, usage = "[<format>] <filename>", desc = "Save a schematic into your clipboard")
    @Deprecated
    @CommandPermissions({ "worldedit.clipboard.save", "worldedit.schematic.save" })
    public void save(final Player player, final LocalSession session, @Optional("schematic") final String formatName, final String filename) throws CommandException, WorldEditException {
        final LocalConfiguration config = this.worldEdit.getConfiguration();
        final ClipboardFormat format = ClipboardFormat.findByAlias(formatName);
        if (format == null) {
            player.printError("Unknown schematic format: " + formatName);
            return;
        }

        final File dir = this.worldEdit.getWorkingDirectoryFile(config.saveDir);
        final File f = this.worldEdit.getSafeSaveFile(player, dir, filename, format.getExtension(), format.getExtension());
        final File parent = f.getParentFile();
        if ((parent != null) && !parent.exists()) {
            if (!parent.mkdirs()) {
                log.info("Could not create folder for schematics!");
                return;
            }
        }
        try {
            if (!f.exists()) {
                f.createNewFile();
            }
            try (FileOutputStream fos = new FileOutputStream(f)) {
                final ClipboardHolder holder = session.getClipboard();
                final Clipboard clipboard = holder.getClipboard();
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

                try (BufferedOutputStream bos = new BufferedOutputStream(fos)) {
                    try (ClipboardWriter writer = format.getWriter(bos)) {
                        if (writer instanceof StructureFormat) {
                            ((StructureFormat) writer).write(target, holder.getWorldData(), player.getName());
                        } else {
                            writer.write(target, holder.getWorldData());
                        }
                        log.info(player.getName() + " saved " + f.getCanonicalPath());
                        BBC.SCHEMATIC_SAVED.send(player, filename);
                    }
                }
            }
        } catch (IllegalArgumentException  e) {
            player.printError("Unknown filename: " + filename);
        } catch (IOException e) {
            player.printError("Schematic could not written: " + e.getMessage());
            log.log(Level.WARNING, "Failed to write a saved clipboard", e);
        }
    }

    @Command(aliases = { "delete", "d" }, usage = "<filename>", desc = "Delete a saved schematic", help = "Delete a schematic from the schematic list", min = 1, max = 1)
    @CommandPermissions("worldedit.schematic.delete")
    public void delete(final Player player, final LocalSession session, final EditSession editSession, final CommandContext args) throws WorldEditException {
        final LocalConfiguration config = this.worldEdit.getConfiguration();
        final String filename = args.getString(0);

        final File dir = this.worldEdit.getWorkingDirectoryFile(config.saveDir);
        final File f = this.worldEdit.getSafeSaveFile(player, dir, filename, "schematic", "schematic");
        if (!f.exists()) {
            player.printError("Schematic " + filename + " does not exist!");
            return;
        }
        if (!f.delete()) {
            player.printError("Deletion of " + filename + " failed! Maybe it is read-only.");
            return;
        }
        BBC.SCHEMATIC_DELETE.send(player, filename);
    }

    @Command(aliases = { "formats", "listformats", "f" }, desc = "List available formats", max = 0)
    @CommandPermissions("worldedit.schematic.formats")
    public void formats(final Actor actor) throws WorldEditException {
        BBC.SCHEMATIC_FORMAT.send(actor);
        StringBuilder builder;
        boolean first = true;
        for (final ClipboardFormat format : ClipboardFormat.values()) {
            builder = new StringBuilder();
            builder.append(format.name()).append(": ");
            for (final String lookupName : format.getAliases()) {
                if (!first) {
                    builder.append(", ");
                }
                builder.append(lookupName);
                first = false;
            }
            first = true;
            actor.print(builder.toString());
        }
    }

    @Command(aliases = { "list", "all", "ls" }, desc = "List saved schematics", max = 0, flags = "dn", help = "List all schematics in the schematics directory\n"
    + " -d sorts by date, oldest first\n"
    + " -n sorts by date, newest first\n")
    @CommandPermissions("worldedit.schematic.list")
    public void list(final Actor actor, final CommandContext args) throws WorldEditException {
        final File dir = this.worldEdit.getWorkingDirectoryFile(this.worldEdit.getConfiguration().saveDir);
        final File[] files = dir.listFiles(new FileFilter() {
            @Override
            public boolean accept(final File file) {
                // sort out directories from the schematic list
                // if WE supports sub-directories in the future,
                // this will have to be changed
                return file.isFile();
            }
        });
        if (files == null) {
            throw new FilenameResolutionException(dir.getPath(), "Schematics directory invalid or not found.");
        }

        final int sortType = args.hasFlag('d') ? -1 : args.hasFlag('n') ? 1 : 0;
        // cleanup file list
        Arrays.sort(files, new Comparator<File>() {
            @Override
            public int compare(final File f1, final File f2) {
                // this should no longer happen, as directory-ness is checked before
                // however, if a directory slips through, this will break the contract
                // of comparator transitivity
                if (!f1.isFile() || !f2.isFile()) {
                    return -1;
                }
                // http://stackoverflow.com/questions/203030/best-way-to-list-files-in-java-sorted-by-date-modified
                int result = sortType == 0 ? f1.getName().compareToIgnoreCase(f2.getName()) : // use name by default
                    Long.valueOf(f1.lastModified()).compareTo(f2.lastModified()); // use date if there is a flag
                if (sortType == 1) {
                    result = -result; // flip date for newest first instead of oldest first
                }
                return result;
            }
        });
        BBC.SCHEMATIC_LIST.send(actor);
        actor.print(this.listFiles("", files));
    }

    private String listFiles(final String prefix, final File[] files) {
        final StringBuilder build = new StringBuilder();
        for (final File file : files) {
            if (file.isDirectory()) {
                build.append(this.listFiles(prefix + file.getName() + "/", file.listFiles()));
                continue;
            }

            if (!file.isFile()) {
                continue;
            }

            build.append("\n\u00a79");
            final ClipboardFormat format = ClipboardFormat.findByFile(file);
            build.append(prefix).append(file.getName()).append(": ").append(format == null ? "Unknown" : format.name());
        }
        return build.toString();
    }

    public static Class<?> inject() {
        return SchematicCommands.class;
    }
}
