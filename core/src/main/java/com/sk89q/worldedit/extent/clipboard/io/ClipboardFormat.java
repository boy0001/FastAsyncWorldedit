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

import com.boydti.fawe.object.FaweOutputStream;
import com.boydti.fawe.object.clipboard.DiskOptimizedClipboard;
import com.boydti.fawe.object.schematic.FaweFormat;
import com.boydti.fawe.object.schematic.PNGWriter;
import com.boydti.fawe.object.schematic.StructureFormat;
import com.boydti.fawe.util.MainUtil;
import com.sk89q.jnbt.NBTConstants;
import com.sk89q.jnbt.NBTInputStream;
import com.sk89q.jnbt.NBTOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import javax.annotation.Nullable;


import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A collection of supported clipboard formats.
 */
public enum ClipboardFormat {

    /**
     * The Schematic format used by many software.
     */
    SCHEMATIC("mcedit", "mce", "schematic") {
        @Override
        public ClipboardReader getReader(InputStream inputStream) throws IOException {
            NBTInputStream nbtStream = new NBTInputStream(new GZIPInputStream(inputStream));
            return new SchematicReader(nbtStream);
        }

        @Override
        public ClipboardWriter getWriter(OutputStream outputStream) throws IOException {
            GZIPOutputStream gzip;
            if (outputStream instanceof GZIPOutputStream) {
                gzip = (GZIPOutputStream) outputStream;
            } else {
                gzip = new GZIPOutputStream(outputStream, true);
            }
            NBTOutputStream nbtStream = new NBTOutputStream(gzip);
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
    },
    /**
     * The structure block format:
     * http://minecraft.gamepedia.com/Structure_block_file_format
     */
    STRUCTURE("structure", "nbt") {
        @Override
        public ClipboardReader getReader(InputStream inputStream) throws IOException {
            NBTInputStream nbtStream = new NBTInputStream(new GZIPInputStream(inputStream));
            return new StructureFormat(nbtStream);
        }

        @Override
        public ClipboardWriter getWriter(OutputStream outputStream) throws IOException {
            GZIPOutputStream gzip;
            if (outputStream instanceof GZIPOutputStream) {
                gzip = (GZIPOutputStream) outputStream;
            } else {
                gzip = new GZIPOutputStream(outputStream, true);
            }
            NBTOutputStream nbtStream = new NBTOutputStream(gzip);
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
    },

    PNG("png") {
        @Override
        public ClipboardReader getReader(InputStream inputStream) throws IOException {
            return null;
        }

        @Override
        public ClipboardWriter getWriter(OutputStream outputStream) throws IOException {
            return new PNGWriter(outputStream);
        }

        @Override
        public String getExtension() {
            return "png";
        }

        @Override
        public boolean isFormat(File file) {
            return file.getName().endsWith(".png");
        }
    },

    /**
     * The FAWE file format:
     *  - Streamable for known dimensions with mode 0
     *  - Streamable for unknown dimensions
     *    - 1: Max size 256 x 256 x 256
     *    - 2: Max size 65535 x 65535 x 65535
     *    - 3: Includes BlockChange information
     *  - O(1) Access to blocks if using compression level 0 and mode 0
     *
     *  DiskOptimizedClipboard: compression/mode -> 0/0
     *  DiskStorageHistory: compression/mode -> Any/3
     *  MemoryOptimizedHistory: compression/mode -> Any/3
     *  FaweFormat: compression/mode -> Any/Any (slower)
     *
     */
    FAWE("fawe") {
        /**
         * Read a clipboard from a compressed stream (the first byte indicates the compression level)
         * @param inputStream the input stream
         * @return
         * @throws IOException
         */
        @Override
        public ClipboardReader getReader(InputStream inputStream) throws IOException {
            return new FaweFormat(MainUtil.getCompressedIS(inputStream));
        }

        /**
         * Write a clipboard to a stream with compression level 8
         * @param outputStream the output stream
         * @return
         * @throws IOException
         */
        @Override
        public ClipboardWriter getWriter(OutputStream outputStream) throws IOException {
            return getWriter(outputStream, 8);
        }

        /**
         * Write a clipboard to a stream
         * @param os
         * @param compression
         * @return
         * @throws IOException
         */
        public ClipboardWriter getWriter(OutputStream os, int compression) throws IOException {
            FaweFormat writer = new FaweFormat(new FaweOutputStream(os));
            writer.compress(compression);
            return writer;
        }

        /**
         * Read or write blocks ids to a file
         * @param file
         * @return
         * @throws IOException
         */
        public DiskOptimizedClipboard getUncompressedReadWrite(File file) throws IOException {
            return new DiskOptimizedClipboard(file);
        }

        /**
         * Read or write block ids to a new file
         * @param width
         * @param height
         * @param length
         * @param file
         * @return
         */
        public DiskOptimizedClipboard createUncompressedReadWrite(int width, int height, int length, File file) {
            return new DiskOptimizedClipboard(width, height, length, file);
        }

        @Override
        public boolean isFormat(File file) {
            return file.getName().endsWith(".fawe") || file.getName().endsWith(".bd");
        }

        @Override
        public String getExtension() {
            return "fawe";
        }
    },

    ;

    private static final Map<String, ClipboardFormat> aliasMap = new HashMap<String, ClipboardFormat>();

    private final String[] aliases;

    /**
     * Create a new instance.
     *
     * @param aliases an array of aliases by which this format may be referred to
     */
    ClipboardFormat(String ... aliases) {
        this.aliases = aliases;
    }

    /**
     * Get a set of aliases.
     *
     * @return a set of aliases
     */
    public Set<String> getAliases() {
        return Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(aliases)));
    }

    /**
     * Create a reader.
     *
     * @param inputStream the input stream
     * @return a reader
     * @throws IOException thrown on I/O error
     */
    public abstract ClipboardReader getReader(InputStream inputStream) throws IOException;

    /**
     * Create a writer.
     *
     * @param outputStream the output stream
     * @return a writer
     * @throws IOException thrown on I/O error
     */
    public abstract ClipboardWriter getWriter(OutputStream outputStream) throws IOException;

    /**
     * Get the file extension used
     * @return file extension string
     */
    public abstract String getExtension();

    /**
     * Return whether the given file is of this format.
     *
     * @param file the file
     * @return true if the given file is of this format
     */
    public abstract boolean isFormat(File file);

    static {
        for (ClipboardFormat format : EnumSet.allOf(ClipboardFormat.class)) {
            for (String key : format.aliases) {
                aliasMap.put(key, format);
            }
        }
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

    public static Class<?> inject() {
        return ClipboardFormat.class;
    }
}