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
import com.boydti.fawe.util.MathMan;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.minecraft.util.commands.CommandException;
import com.sk89q.minecraft.util.commands.CommandPermissions;
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
import com.sk89q.worldedit.util.command.binding.Switch;
import com.sk89q.worldedit.util.command.parametric.Optional;
import com.sk89q.worldedit.util.io.file.FilenameException;
import com.sk89q.worldedit.world.registry.WorldData;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;


import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Commands that work with schematic files.
 */
public class SchematicCommands {

    /**
     * 9 schematics per page fits in the MC chat window.
     */
    private static final int SCHEMATICS_PER_PAGE = 9;
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
    @CommandPermissions({ "worldedit.clipboard.load", "worldedit.schematic.load", "worldedit.schematic.upload" })
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
                if (!player.hasPermission("worldedit.schematic.upload")) {
                    BBC.NO_PERM.send(player, "worldedit.schematic.upload");
                    return;
                }
                UUID uuid = UUID.fromString(filename.substring(4));
                URL base = new URL(Settings.WEB.URL);
                URL url = new URL(base, "uploads/" + uuid + ".schematic");
                ReadableByteChannel rbc = Channels.newChannel(url.openStream());
                in = Channels.newInputStream(rbc);
            } else {
                if (!player.hasPermission("worldedit.schematic.load") && !player.hasPermission("worldedit.clipboard.load")) {
                    BBC.NO_PERM.send(player, "worldedit.clipboard.load");
                    return;
                }
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

                try (ClipboardWriter writer = format.getWriter(fos)) {
                    if (writer instanceof StructureFormat) {
                        ((StructureFormat) writer).write(target, holder.getWorldData(), player.getName());
                    } else {
                        writer.write(target, holder.getWorldData());
                    }
                    log.info(player.getName() + " saved " + f.getCanonicalPath());
                    BBC.SCHEMATIC_SAVED.send(player, filename);
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
    public void delete(final Player player, final LocalSession session, final CommandContext args) throws WorldEditException {
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
            actor.print(BBC.getPrefix() + builder.toString());
        }
    }

    @Command(
            aliases = {"list", "all", "ls"},
            desc = "List saved schematics",
            min = 0,
            max = 1,
            flags = "dnp",
            help = "List all schematics in the schematics directory\n" +
                    " -d sorts by date, oldest first\n" +
                    " -n sorts by date, newest first\n" +
                    " -p <page> prints the requested page\n"
    )
    @CommandPermissions("worldedit.schematic.list")
    public void list(Actor actor, CommandContext args, @Switch('p') @Optional("1") int page) throws WorldEditException {
        File dir = worldEdit.getWorkingDirectoryFile(worldEdit.getConfiguration().saveDir);
        List<File> fileList = allFiles(dir);

        if (fileList == null || fileList.isEmpty()) {
            BBC.SCHEMATIC_NONE.send(actor);
            return;
        }

        File[] files = new File[fileList.size()];
        fileList.toArray(files);
        if (args.argsLength() > 0 && MathMan.isInteger(args.getString(0))) {
            page = args.getInteger(0);
        }
        int pageCount = files.length / SCHEMATICS_PER_PAGE + 1;
        if (page < 1) {
            BBC.SCHEMATIC_PAGE.send(actor, ">0");
            return;
        }
        if (page > pageCount) {
            BBC.SCHEMATIC_PAGE.send(actor, "<" + (pageCount + 1));
            return;
        }

        final int sortType = args.hasFlag('d') ? -1 : args.hasFlag('n') ? 1 : 0;
        // cleanup file list
        Arrays.sort(files, new Comparator<File>(){
            @Override
            public int compare(File f1, File f2) {
                int res;
                if (sortType == 0) { // use name by default
                    int p = f1.getParent().compareTo(f2.getParent());
                    if (p == 0) { // same parent, compare names
                        res = f1.getName().compareTo(f2.getName());
                    } else { // different parent, sort by that
                        res = p;
                    }
                } else {
                    res = Long.valueOf(f1.lastModified()).compareTo(f2.lastModified()); // use date if there is a flag
                    if (sortType == 1) res = -res; // flip date for newest first instead of oldest first
                }
                return res;
            }
        });

        List<String> schematics = listFiles(worldEdit.getConfiguration().saveDir, files);
        int offset = (page - 1) * SCHEMATICS_PER_PAGE;

        BBC.SCHEMATIC_LIST.send(actor, page, pageCount);
        StringBuilder build = new StringBuilder();
        int limit = Math.min(offset + SCHEMATICS_PER_PAGE, schematics.size());
        for (int i = offset; i < limit;) {
            build.append(schematics.get(i));
            if (++i != limit) {
                build.append("\n");
            }
        }

        actor.print(BBC.getPrefix() + build.toString());
    }


    private List<File> allFiles(File root) {
        File[] files = root.listFiles();
        if (files == null) return null;
        List<File> fileList = new ArrayList<File>();
        for (File f : files) {
            if (f.isDirectory()) {
                List<File> subFiles = allFiles(f);
                if (subFiles == null) continue; // empty subdir
                fileList.addAll(subFiles);
            } else {
                fileList.add(f);
            }
        }
        return fileList;
    }

    private List<String> listFiles(String prefix, File[] files) {
        File dir = worldEdit.getWorkingDirectoryFile(prefix);
        if (prefix == null) prefix = "";
        List<String> result = new ArrayList<String>();
        for (File file : files) {
            StringBuilder build = new StringBuilder();

            build.append("\u00a72");
            ClipboardFormat format = ClipboardFormat.findByFile(file);
            boolean inRoot = file.getParentFile().getName().equals(prefix);
            if (inRoot) {
                build.append(file.getName());
            } else {
                String relative = dir.toURI().relativize(file.toURI()).getPath();
                String[] split = file.getPath().split(Pattern.quote(prefix + File.separator));
                build.append(relative);
            }
            build.append(": ").append(format == null ? "Unknown" : format.name());
            result.add(build.toString());
        }
        return result;
    }

    public static Class<?> inject() {
        return SchematicCommands.class;
    }
}
