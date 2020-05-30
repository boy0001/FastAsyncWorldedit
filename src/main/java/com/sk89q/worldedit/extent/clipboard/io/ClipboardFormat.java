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

package com.sk89q.worldedit.extent.clipboard.io;

import com.boydti.fawe.config.BBC;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.jnbt.NBTStreamer;
import com.boydti.fawe.object.FaweOutputStream;
import com.boydti.fawe.object.RunnableVal;
import com.boydti.fawe.object.clipboard.*;
import com.boydti.fawe.object.io.FastByteArrayOutputStream;
import com.boydti.fawe.object.io.PGZIPOutputStream;
import com.boydti.fawe.object.io.ResettableFileInputStream;
import com.boydti.fawe.object.schematic.FaweFormat;
import com.boydti.fawe.object.schematic.PNGWriter;
import com.boydti.fawe.object.schematic.Schematic;
import com.boydti.fawe.object.schematic.StructureFormat;
import com.boydti.fawe.util.MainUtil;
import com.boydti.fawe.util.ReflectionUtils;
import com.google.common.io.ByteSource;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.sk89q.jnbt.NBTConstants;
import com.sk89q.jnbt.NBTInputStream;
import com.sk89q.jnbt.NBTOutputStream;
import com.sk89q.worldedit.LocalConfiguration;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.event.extent.PlayerSaveClipboardEvent;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.ClipboardFormats;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.registry.WorldData;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.annotation.Nullable;


import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A collection of supported clipboard formats.
 */
public enum ClipboardFormat {

    /**
     * The Schematic format used by many software.
     */
    SCHEMATIC(new AbstractClipboardFormat("SCHEMATIC", "mcedit", "mce", "schematic") {
        @Override
        public ClipboardReader getReader(InputStream inputStream) throws IOException {
            if (inputStream instanceof FileInputStream) {
                inputStream = new ResettableFileInputStream((FileInputStream) inputStream);
            }
            BufferedInputStream buffered = new BufferedInputStream(inputStream);
            NBTInputStream nbtStream = new NBTInputStream(new BufferedInputStream(new GZIPInputStream(buffered)));
            SchematicReader input = new SchematicReader(nbtStream);
            input.setUnderlyingStream(inputStream);
            return input;
        }

        @Override
        public ClipboardWriter getWriter(OutputStream outputStream) throws IOException {
            OutputStream gzip;
            if (outputStream instanceof PGZIPOutputStream || outputStream instanceof GZIPOutputStream) {
                gzip = outputStream;
            } else {
                outputStream = new BufferedOutputStream(outputStream);
                PGZIPOutputStream pigz = new PGZIPOutputStream(outputStream);
                gzip = pigz;
            }
            NBTOutputStream nbtStream = new NBTOutputStream(new BufferedOutputStream(gzip));
            return new SchematicWriter(nbtStream);
        }

        @Override
        public boolean isFormat(File file) {
            DataInputStream str = null;
            try {
                str = new DataInputStream(new GZIPInputStream(new FileInputStream(file)));
                if ((str.readByte() & 0xFF) != NBTConstants.TYPE_COMPOUND) {
                    return false;
                }
                byte[] nameBytes = new byte[str.readShort() & 0xFFFF];
                str.readFully(nameBytes);
                String name = new String(nameBytes, NBTConstants.CHARSET);
                return name.equals("Schematic");
            } catch (IOException e) {
                return false;
            } finally {
                if (str != null) {
                    try {
                        str.close();
                    } catch (IOException ignored) {
                    }
                }
            }
        }

        @Override
        public String getExtension() {
            return "schematic";
        }
    }),

    /**
     * The structure block format:
     * http://minecraft.gamepedia.com/Structure_block_file_format
     */
    STRUCTURE(new AbstractClipboardFormat("STRUCTURE", "structure", "nbt") {
        @Override
        public ClipboardReader getReader(InputStream inputStream) throws IOException {
            inputStream = new BufferedInputStream(inputStream);
            NBTInputStream nbtStream = new NBTInputStream(new BufferedInputStream(new GZIPInputStream(inputStream)));
            return new StructureFormat(nbtStream);
        }

        @Override
        public ClipboardWriter getWriter(OutputStream outputStream) throws IOException {
            outputStream = new BufferedOutputStream(outputStream);
            OutputStream gzip;
            if (outputStream instanceof PGZIPOutputStream || outputStream instanceof GZIPOutputStream) {
                gzip = outputStream;
            } else {
                PGZIPOutputStream pigz = new PGZIPOutputStream(outputStream);
                gzip = pigz;
            }
            NBTOutputStream nbtStream = new NBTOutputStream(new BufferedOutputStream(gzip));
            return new StructureFormat(nbtStream);
        }

        @Override
        public boolean isFormat(File file) {
            return file.getName().endsWith(".nbt");
        }

        @Override
        public String getExtension() {
            return "nbt";
        }
    }),

    /**
     * Isometric PNG writer
     */
    PNG(new AbstractClipboardFormat("PNG", "png", "image") {

        @Override
        public ClipboardReader getReader(InputStream inputStream) throws IOException {
            return null;
        }

        @Override
        public ClipboardWriter getWriter(OutputStream outputStream) throws IOException {
            return new PNGWriter(new BufferedOutputStream(outputStream));
        }

        @Override
        public boolean isFormat(File file) {
            return file.getName().endsWith(".png");
        }

        @Override
        public String getExtension() {
            return "png";
        }
    }),

    /**
     * The FAWE file format:
     * - Streamable for known dimensions with mode 0
     * - Streamable for unknown dimensions
     * - 1: Max size 256 x 256 x 256
     * - 2: Max size 65535 x 65535 x 65535
     * - 3: Includes BlockChange information
     * - O(1) Access to blocks if using compression level 0 and mode 0
     * <p>
     * DiskOptimizedClipboard: compression/mode -> 0/0
     * DiskStorageHistory: compression/mode -> Any/3
     * MemoryOptimizedHistory: compression/mode -> Any/3
     * FaweFormat: compression/mode -> Any/Any (slower)
     */
    FAWE(new AbstractClipboardFormat("FAWE", "fawe") {
        @Override
        public ClipboardReader getReader(InputStream inputStream) throws IOException {
            return new FaweFormat(MainUtil.getCompressedIS(inputStream));
        }

        @Override
        public ClipboardWriter getWriter(OutputStream outputStream) throws IOException {
            return getWriter(outputStream, 8);
        }

        @Override
        public boolean isFormat(File file) {
            return file.getName().endsWith(".fawe") || file.getName().endsWith(".bd");
        }

        @Override
        public String getExtension() {
            return "fawe";
        }

        public ClipboardWriter getWriter(OutputStream os, int compression) throws IOException {
            FaweFormat writer = new FaweFormat(new FaweOutputStream(os));
            writer.compress(compression);
            return writer;
        }

        public DiskOptimizedClipboard getUncompressedReadWrite(File file) throws IOException {
            return new DiskOptimizedClipboard(file);
        }

        public DiskOptimizedClipboard createUncompressedReadWrite(int width, int height, int length, File file) {
            return new DiskOptimizedClipboard(width, height, length, file);
        }
    }),;

    private static final Map<String, ClipboardFormat> aliasMap;

    static {
        aliasMap = new ConcurrentHashMap<>(8, 0.9f, 1);
        for (ClipboardFormat emum : ClipboardFormat.values()) {
            for (String alias : emum.getAliases()) {
                aliasMap.put(alias, emum);
            }
        }
    }

    private IClipboardFormat format;

    ClipboardFormat() {

    }

    ClipboardFormat(IClipboardFormat format) {
        this.format = format;
    }

    public URL uploadPublic(final Clipboard clipboard, String category, String user) {
        // summary
        // blocks
        HashMap<String, Object> map = new HashMap<String, Object>();
        Vector dimensions = clipboard.getDimensions();
        map.put("width", dimensions.getX());
        map.put("height", dimensions.getY());
        map.put("length", dimensions.getZ());
        map.put("creator", user);
        if (clipboard instanceof BlockArrayClipboard) {
            FaweClipboard fc = ((BlockArrayClipboard) clipboard).IMP;
            final int[] ids = new int[4096];
            fc.streamIds(new NBTStreamer.ByteReader() {
                @Override
                public void run(int index, int byteValue) {
                    ids[byteValue]++;
                }
            });
            Map<Integer, Integer> blocks = new HashMap<Integer, Integer>();
            for (int i = 0; i < ids.length; i++) {
                if (ids[i] != 0) {
                    blocks.put(i, ids[i]);
                }
            }
            map.put("blocks", blocks);
        }
        Gson gson = new Gson();
        String json = gson.toJson(map);
        return MainUtil.upload(Settings.IMP.WEB.ASSETS, false, json, category, null, new RunnableVal<OutputStream>() {
            @Override
            public void run(OutputStream value) {
                write(value, clipboard);
            }
        });
    }

    public static MultiClipboardHolder loadAllFromInput(Actor player, WorldData worldData, String input, boolean message) throws IOException {
        checkNotNull(player);
        checkNotNull(input);
        ClipboardFormat format = null;
        WorldEdit worldEdit = WorldEdit.getInstance();
        LocalConfiguration config = worldEdit.getConfiguration();
        if (input.startsWith("url:")) {
            if (!player.hasPermission("worldedit.schematic.load.web")) {
                if (message) BBC.NO_PERM.send(player, "worldedit.schematic.load.web");
                return null;
            }
            URL base = new URL(Settings.IMP.WEB.URL);
            input = new URL(base, "uploads/" + input.substring(4) + ".schematic").toString();
        }
        if (input.startsWith("http")) {
            if (!player.hasPermission("worldedit.schematic.load.asset")) {
                if (message) BBC.NO_PERM.send(player, "worldedit.schematic.load.asset");
                return null;
            }
            URL url = new URL(input);
            URL webInterface = new URL(Settings.IMP.WEB.ASSETS);
            if (!url.getHost().equalsIgnoreCase(webInterface.getHost())) {
                if (message) BBC.WEB_UNAUTHORIZED.send(player, url);
                return null;
            }
            MultiClipboardHolder clipboards = loadAllFromUrl(url, worldData);
            return clipboards;
        } else {
            if (Settings.IMP.PATHS.PER_PLAYER_SCHEMATICS && Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}").matcher(input).find() && !player.hasPermission("worldedit.schematic.load.other")) {
                BBC.NO_PERM.send(player, "worldedit.schematic.load.other");
                return null;
            }
            File working = worldEdit.getWorkingDirectoryFile(config.saveDir);
            File dir = Settings.IMP.PATHS.PER_PLAYER_SCHEMATICS ? new File(working, player.getUniqueId().toString()) : working;
            File f;
            if (input.startsWith("#")) {
                String[] extensions;
                if (format != null) {
                    extensions = new String[] { format.getExtension() };
                } else {
                    extensions = ClipboardFormats.getFileExtensionArray();
                }
                f = player.openFileOpenDialog(extensions);
                if (f == null || !f.exists()) {
                    if (message) player.printError("Schematic " + input + " does not exist! (" + f + ")");
                    return null;
                }
            } else {
                if (Settings.IMP.PATHS.PER_PLAYER_SCHEMATICS && Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}").matcher(input).find() && !player.hasPermission("worldedit.schematic.load.other")) {
                    if (message) BBC.NO_PERM.send(player, "worldedit.schematic.load.other");
                    return null;
                }
                if (format == null && input.matches(".*\\.[\\w].*")) {
                    String extension = input.substring(input.lastIndexOf('.') + 1, input.length());
                    format = ClipboardFormat.findByExtension(extension);
                }
                f = MainUtil.resolve(dir, input, format, true);
            }
            if (f == null || !f.exists()) {
                if (!input.contains("../")) {
                    dir = worldEdit.getWorkingDirectoryFile(config.saveDir);
                    f = MainUtil.resolve(dir, input, format, true);
                }
            }
            if (f == null || !f.exists() || !MainUtil.isInSubDirectory(working, f)) {
                if (message) player.printError("Schematic " + input + " does not exist! (" + ((f == null) ? false : f.exists()) + "|" + f + "|" + (f == null ? false : !MainUtil.isInSubDirectory(working, f)) + ")");
                return null;
            }
            if (format == null && f.isFile()) {
                format = ClipboardFormat.findByFile(f);
                if (format == null) {
                    BBC.CLIPBOARD_INVALID_FORMAT.send(player, f.getName());
                    return null;
                }
            }
            if (!f.exists()) {
                if (message) BBC.SCHEMATIC_NOT_FOUND.send(player, input);
                return null;
            }
            if (!f.isDirectory()) {
                ByteSource source = Files.asByteSource(f);
                URI uri = f.toURI();
                return new MultiClipboardHolder(uri, worldData, new LazyClipboardHolder(f.toURI(), source, format, worldData, null));
            }
            URIClipboardHolder[] clipboards = loadAllFromDirectory(f, worldData);
            if (clipboards.length < 1) {
                if (message) BBC.SCHEMATIC_NOT_FOUND.send(player, input);
                return null;
            }
            return new MultiClipboardHolder(f.toURI(), worldData, clipboards);
        }
    }

    public static URIClipboardHolder[] loadAllFromDirectory(File dir, WorldData worldData) {
        HashSet<String> extensions = new HashSet<>(Arrays.asList(ClipboardFormats.getFileExtensionArray()));
        File[] files = dir.listFiles(pathname -> {
            String input = pathname.getName();
            String extension = input.substring(input.lastIndexOf('.') + 1, input.length());
            return (extensions.contains(extension.toLowerCase()));
        });
        LazyClipboardHolder[] clipboards = new LazyClipboardHolder[files.length];
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            ByteSource source = Files.asByteSource(file);
            ClipboardFormat format = ClipboardFormat.findByFile(file);
            clipboards[i] = new LazyClipboardHolder(file.toURI(), source, format, worldData, null);
        }
        return clipboards;
    }

    public static MultiClipboardHolder loadAllFromUrl(URL url, WorldData worldData) throws IOException {
        List<LazyClipboardHolder> clipboards = new ArrayList<>();
        try (ReadableByteChannel rbc = Channels.newChannel(url.openStream())) {
            try (InputStream in = Channels.newInputStream(rbc)) {
                try (ZipInputStream zip = new ZipInputStream(in)) {
                    ZipEntry entry;
                    byte[] buffer = new byte[8192];
                    while ((entry = zip.getNextEntry()) != null) {
                        String filename = entry.getName();
                        String extension = filename.substring(filename.lastIndexOf('.') + 1, filename.length());
                        ClipboardFormat format = findByExtension(filename);
                        if (format != null) {
                            FastByteArrayOutputStream out = new FastByteArrayOutputStream();
                            int len = 0;
                            while ((len = zip.read(buffer)) > 0) {
                                out.write(buffer, 0, len);
                            }
                            byte[] array = out.toByteArray();
                            ByteSource source = ByteSource.wrap(array);
                            LazyClipboardHolder clipboard = new LazyClipboardHolder(url.toURI(), source, format, worldData, null);
                            clipboards.add(clipboard);
                        }
                    }
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
            }
        }
        LazyClipboardHolder[] arr = clipboards.toArray(new LazyClipboardHolder[clipboards.size()]);
        try {
            MultiClipboardHolder multi = new MultiClipboardHolder(url.toURI(), worldData);
            for (LazyClipboardHolder h : arr) multi.add(h);
            return multi;
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private void write(OutputStream value, Clipboard clipboard) {
        try {
            try (PGZIPOutputStream gzip = new PGZIPOutputStream(value)) {
                try (ClipboardWriter writer = format.getWriter(gzip)) {
                    writer.write(clipboard, null);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public URL uploadAnonymous(final Clipboard clipboard) {
        return MainUtil.upload(null, null, format.getExtension(), new RunnableVal<OutputStream>() {
            @Override
            public void run(OutputStream value) {
                write(value, clipboard);
            }
        });
    }

    public IClipboardFormat getFormat() {
        return format;
    }

    /**
     * Get a set of aliases.
     *
     * @return a set of aliases
     */
    public Set<String> getAliases() {
        return format.getAliases();
    }

    /**
     * Create a reader.
     *
     * @param inputStream the input stream
     * @return a reader
     * @throws IOException thrown on I/O error
     */
    public ClipboardReader getReader(InputStream inputStream) throws IOException {
        return format.getReader(inputStream);
    }

    /**
     * Create a writer.
     *
     * @param outputStream the output stream
     * @return a writer
     * @throws IOException thrown on I/O error
     */
    public ClipboardWriter getWriter(OutputStream outputStream) throws IOException {
        return format.getWriter(outputStream);
    }

    /**
     * Set the player's clipboard
     * @param player
     * @param uri
     * @param in
     * @return the held clipboard
     * @throws IOException
     */
    public ClipboardHolder hold(Player player, URI uri, InputStream in) throws IOException {
        checkNotNull(player);
        checkNotNull(uri);
        checkNotNull(in);

        final ClipboardReader reader = getReader(in);

        final WorldData worldData = player.getWorld().getWorldData();
        final Clipboard clipboard;

        LocalSession session = WorldEdit.getInstance().getSessionManager().get(player);
        session.setClipboard(null);
        if (reader instanceof SchematicReader) {
            clipboard = ((SchematicReader) reader).read(player.getWorld().getWorldData(), player.getUniqueId());
        } else if (reader instanceof StructureFormat) {
            clipboard = ((StructureFormat) reader).read(player.getWorld().getWorldData(), player.getUniqueId());
        } else {
            clipboard = reader.read(player.getWorld().getWorldData());
        }
        URIClipboardHolder holder = new URIClipboardHolder(uri, clipboard, worldData);
        session.setClipboard(holder);
        return holder;
    }

    public Schematic load(File file) throws IOException {
        return load(new FileInputStream(file));
    }

    public Schematic load(InputStream stream) throws IOException {
        return new Schematic(this.getReader(stream).read(null));
    }

    /**
     * Get the file extension used
     *
     * @return file extension string
     */
    public String getExtension() {
        return format.getExtension();
    }

    /**
     * Return whether the given file is of this format.
     *
     * @param file the file
     * @return true if the given file is of this format
     */
    public boolean isFormat(File file) {
        return format.isFormat(file);
    }

    /**
     * Find the clipboard format named by the given alias.
     *
     * @param alias the alias
     * @return the format, otherwise null if none is matched
     */
    @Nullable
    public static ClipboardFormat findByAlias(String alias) {
        checkNotNull(alias);
        return aliasMap.get(alias.toLowerCase().trim());
    }

    @Nullable
    public static ClipboardFormat findByExtension(String extension) {
        checkNotNull(extension);
        extension = extension.toLowerCase();
        for (ClipboardFormat format : values()) {
            if (format.getExtension().equalsIgnoreCase(extension)) {
                return format;
            }
        }
        return null;
    }

    /**
     * Detect the format given a file.
     *
     * @param file the file
     * @return the format, otherwise null if one cannot be detected
     */
    @Nullable
    public static ClipboardFormat findByFile(File file) {
        checkNotNull(file);
        for (ClipboardFormat format : EnumSet.allOf(ClipboardFormat.class)) {
            if (format.isFormat(file)) {
                return format;
            }
        }

        return null;
    }

    public static ClipboardFormat addFormat(IClipboardFormat instance) {
        ClipboardFormat newEnum = ReflectionUtils.addEnum(ClipboardFormat.class, instance.getName());
        newEnum.format = instance;
        for (String alias : newEnum.getAliases()) {
            aliasMap.put(alias, newEnum);
        }
        return newEnum;
    }

    public static Class<?> inject() {
        return ClipboardFormat.class;
    }
}