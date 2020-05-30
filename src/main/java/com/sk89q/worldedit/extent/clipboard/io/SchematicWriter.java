package com.sk89q.worldedit.extent.clipboard.io;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweCache;
import com.boydti.fawe.jnbt.NBTStreamer;
import com.boydti.fawe.object.clipboard.FaweClipboard;
import com.boydti.fawe.object.clipboard.WorldCopyClipboard;
import com.boydti.fawe.object.io.FastByteArrayOutputStream;
import com.boydti.fawe.object.io.PGZIPOutputStream;
import com.boydti.fawe.util.ReflectionUtils;
import com.google.common.io.ByteStreams;
import com.sk89q.jnbt.ByteArrayTag;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.DoubleTag;
import com.sk89q.jnbt.FloatTag;
import com.sk89q.jnbt.IntTag;
import com.sk89q.jnbt.ListTag;
import com.sk89q.jnbt.NBTConstants;
import com.sk89q.jnbt.NBTOutputStream;
import com.sk89q.jnbt.ShortTag;
import com.sk89q.jnbt.StringTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.MutableBlockVector;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.registry.WorldData;
import java.io.ByteArrayInputStream;
import java.io.DataOutput;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;


import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Writes schematic files based that are compatible with MCEdit and other editors.
 */
public class SchematicWriter implements ClipboardWriter {

    private static final int MAX_SIZE = Short.MAX_VALUE - Short.MIN_VALUE;
    private final NBTOutputStream outputStream;

    /**
     * Create a new schematic writer.
     *
     * @param outputStream the output stream to write to
     */
    public SchematicWriter(NBTOutputStream outputStream) {
        checkNotNull(outputStream);
        this.outputStream = outputStream;
    }

    private static class ForEach extends FaweClipboard.BlockReader {
        int x = -1;
        int y = 0;
        int z = 0;
        int index = -1;

        public int[] yarea;
        public int[] zwidth;
        public byte[] blocks;
        public byte[] blockData;
        public byte[] addBlocks;
        public List<Tag> tileEntities;

        public ForEach(int[] yarea, int[] zwidth, byte[] blocks, byte[] blockData, List<Tag> tileEntities) {
            this.yarea = yarea;
            this.zwidth = zwidth;
            this.blocks = blocks;
            this.blockData = blockData;
            this.tileEntities = tileEntities;
        }

        @Override
        public void run(int x, int y, int z, BaseBlock block) {
            if (this.x == x - 1 && this.y == y && this.z == z) {
                this.x++;
                index++;
            } else {
                index = yarea[this.y = y] + zwidth[this.z = z] + (this.x = x);
            }
            int id = block.getId();
            blocks[index] = (byte) id;
            if (FaweCache.hasData(id)) {
                blockData[index] = (byte) block.getData();
                if (id > 255) {
                    if (addBlocks == null) { // Lazily create section
                        addBlocks = new byte[((blocks.length + 1) >> 1)];
                    }
                    addBlocks[index >> 1] = (byte) (((index & 1) == 0) ? addBlocks[index >> 1] & 0xF0 | (id >> 8) & 0xF : addBlocks[index >> 1] & 0xF | ((id >> 8) & 0xF) << 4);
                }
            }
            CompoundTag rawTag = block.getNbtData();
            if (rawTag != null) {
                Map<String, Tag> values = ReflectionUtils.getMap(rawTag.getValue());
                values.put("id", new StringTag(block.getNbtId()));
                values.put("x", new IntTag(x));
                values.put("y", new IntTag(y));
                values.put("z", new IntTag(z));
                tileEntities.add(rawTag);
            }
        }
    }

    @Override
    public void write(final Clipboard clipboard, WorldData data) throws IOException {
        if (clipboard instanceof BlockArrayClipboard) {
            stream((BlockArrayClipboard) clipboard);
        } else {
            outputStream.writeNamedTag("Schematic", writeTag(clipboard));
            outputStream.flush();
        }
    }

    public void stream(final BlockArrayClipboard clipboard) throws IOException {
        final Region region = clipboard.getRegion();
        final Vector origin = clipboard.getOrigin();
        final Vector min = region.getMinimumPoint();
        final Vector offset = min.subtract(origin);
        final int width = region.getWidth();
        final int height = region.getHeight();
        final int length = region.getLength();
        if (width > MAX_SIZE) {
            throw new IllegalArgumentException("Width of region too large for a .schematic");
        }
        if (height > MAX_SIZE) {
            throw new IllegalArgumentException("Height of region too large for a .schematic");
        }
        if (length > MAX_SIZE) {
            throw new IllegalArgumentException("Length of region too large for a .schematic");
        }
        final DataOutput rawStream = outputStream.getOutputStream();
        outputStream.writeLazyCompoundTag("Schematic", new NBTOutputStream.LazyWrite() {
            private boolean hasAdd = false;
            private boolean hasTile = false;
            private boolean hasData = false;

            @Override
            public void write(NBTOutputStream out) throws IOException {
                int volume = width * height * length;

                out.writeNamedTag("Width", ((short) width));
                out.writeNamedTag("Length", ((short) length));
                out.writeNamedTag("Height", ((short) height));
                out.writeNamedTag("Materials", ("Alpha"));
                out.writeNamedTag("WEOriginX", (min.getBlockX()));
                out.writeNamedTag("WEOriginY", (min.getBlockY()));
                out.writeNamedTag("WEOriginZ", (min.getBlockZ()));
                out.writeNamedTag("WEOffsetX", (offset.getBlockX()));
                out.writeNamedTag("WEOffsetY", (offset.getBlockY()));
                out.writeNamedTag("WEOffsetZ", (offset.getBlockZ()));
                out.writeNamedTag("Platform", Fawe.imp().getPlatform());

                if (clipboard.IMP instanceof WorldCopyClipboard) {
                    List<CompoundTag> tileEntities = new ArrayList<CompoundTag>();
                    FastByteArrayOutputStream ids = new FastByteArrayOutputStream();
                    FastByteArrayOutputStream datas = new FastByteArrayOutputStream();
                    FastByteArrayOutputStream add = new FastByteArrayOutputStream();

                    OutputStream idsOut = new PGZIPOutputStream(ids);
                    OutputStream dataOut = new PGZIPOutputStream(datas);
                    OutputStream addOut = new PGZIPOutputStream(add);

                    byte[] addAcc = new byte[1];

                    clipboard.IMP.forEach(new FaweClipboard.BlockReader() {
                        int index;

                        @Override
                        public void run(int x, int y, int z, BaseBlock block) {
                            try {
                                // id
                                int id = block.getId();
                                idsOut.write(id);
                                // data
                                int data = block.getData();
                                dataOut.write(data);
                                // Nbt
                                CompoundTag nbt = block.getNbtData();
                                if (nbt != null) {
                                    hasTile = true;
                                    tileEntities.add(nbt);
                                }
                                // Add
                                if (id > 255) {
                                    int add = id >> 8;
                                    if (!hasAdd) {
                                        hasAdd = true;
                                        for (int i = 0; i < index >> 1; i++) {
                                            addOut.write(new byte[index >> 1]);
                                        }
                                    }
                                    if ((index & 1) == 1) {
                                        addOut.write(addAcc[0] + (add << 4));
                                        addAcc[0] = 0;
                                    } else {
                                        addAcc[0] = (byte) add;
                                    }
                                }
                                // Index
                                index++;
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }, true);
                    if (addAcc[0] != 0) addOut.write(addAcc[0]);

                    idsOut.close();
                    dataOut.close();
                    addOut.close();

                    out.writeNamedTagName("Blocks", NBTConstants.TYPE_BYTE_ARRAY);
                    out.getOutputStream().writeInt(volume);
                    try (GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(ids.toByteArray()))) {
                        ByteStreams.copy(in, (OutputStream) rawStream);
                    }

                    out.writeNamedTagName("Data", NBTConstants.TYPE_BYTE_ARRAY);
                    out.getOutputStream().writeInt(volume);
                    try (GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(datas.toByteArray()))) {
                        ByteStreams.copy(in, (OutputStream) rawStream);
                    }

                    if (hasAdd) {
                        out.writeNamedTagName("AddBlocks", NBTConstants.TYPE_BYTE_ARRAY);
                        int addLength = (volume + 1) >> 1;
                        out.getOutputStream().writeInt(addLength);
                        try (GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(add.toByteArray()))) {
                            ByteStreams.copy(in, (OutputStream) rawStream);
                        }
                    }
                    if (hasTile) {
                        out.writeNamedTag("TileEntities", new ListTag(CompoundTag.class, tileEntities));
                    } else {
                        out.writeNamedEmptyList("TileEntities");
                    }
                } else {
                    out.writeNamedTagName("Blocks", NBTConstants.TYPE_BYTE_ARRAY);
                    out.getOutputStream().writeInt(volume);
                    clipboard.IMP.streamIds(new NBTStreamer.ByteReader() {
                        @Override
                        public void run(int index, int byteValue) {
                            try {
                                if (byteValue >= 256) {
                                    hasAdd = true;
                                }
                                if (FaweCache.hasData(byteValue)) {
                                    hasData = true;
                                }
                                if (FaweCache.hasNBT(byteValue)) {
                                    hasTile = true;
                                }
                                rawStream.writeByte(byteValue);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    });

                    out.writeNamedTagName("Data", NBTConstants.TYPE_BYTE_ARRAY);
                    out.getOutputStream().writeInt(volume);
                    if (hasData) {
                        clipboard.IMP.streamDatas(new NBTStreamer.ByteReader() {
                            @Override
                            public void run(int index, int byteValue) {
                                try {
                                    rawStream.writeByte(byteValue);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    } else {
                        for (int i = 0; i < volume; i++) {
                            rawStream.write(0);
                        }
                    }

                    if (hasAdd) {
                        out.writeNamedTagName("AddBlocks", NBTConstants.TYPE_BYTE_ARRAY);
                        int addLength = (volume + 1) >> 1;
                        out.getOutputStream().writeInt(addLength);

                        final int[] lastAdd = new int[1];
                        final boolean[] write = new boolean[1];

                        clipboard.IMP.streamIds(new NBTStreamer.ByteReader() {
                            @Override
                            public void run(int index, int byteValue) {
                                if (write[0]) {
                                    try {
                                        rawStream.write(((byteValue >> 8) << 4) + (lastAdd[0]));
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                } else {
                                    lastAdd[0] = byteValue >> 8;
                                }
                                write[0] ^= true;
                            }
                        });
                        if (write[0]) {
                            rawStream.write(lastAdd[0]);
                        }
                    }

                    if (hasTile) {
                        final List<CompoundTag> tileEntities = clipboard.IMP.getTileEntities();
                        out.writeNamedTag("TileEntities", new ListTag(CompoundTag.class, tileEntities));
                    } else {
                        out.writeNamedEmptyList("TileEntities");
                    }
                }

                if (clipboard.IMP.hasBiomes()) {
                    out.writeNamedTagName("Biomes", NBTConstants.TYPE_BYTE_ARRAY);
                    out.getOutputStream().writeInt(width * length); // area
                    clipboard.IMP.streamBiomes(new NBTStreamer.ByteReader() {
                        @Override
                        public void run(int index, int byteValue) {
                            try {
                                rawStream.writeByte(byteValue);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }

                List<Tag> entities = new ArrayList<Tag>();
                for (Entity entity : clipboard.getEntities()) {
                    BaseEntity state = entity.getState();

                    if (state != null) {
                        Map<String, Tag> values = new HashMap<String, Tag>();

                        // Put NBT provided data
                        CompoundTag rawTag = state.getNbtData();
                        if (rawTag != null) {
                            values.putAll(rawTag.getValue());
                        }

                        // Store our location data, overwriting any
                        values.put("id", new StringTag(state.getTypeId()));
                        values.put("Pos", writeVector(entity.getLocation().toVector(), "Pos"));
                        values.put("Rotation", writeRotation(entity.getLocation(), "Rotation"));

                        CompoundTag entityTag = new CompoundTag(values);
                        entities.add(entityTag);
                    }
                }
                if (entities.isEmpty()) {
                    out.writeNamedEmptyList("Entities");
                } else {
                    out.writeNamedTag("Entities", new ListTag(CompoundTag.class, entities));
                }
            }
        });
        outputStream.flush();
    }

    public static CompoundTag writeTag(Clipboard clipboard) {
        Region region = clipboard.getRegion();
        Vector origin = clipboard.getOrigin();
        Vector min = region.getMinimumPoint();
        Vector offset = min.subtract(origin);
        int width = region.getWidth();
        int height = region.getHeight();
        int length = region.getLength();

        if (width > MAX_SIZE) {
            throw new IllegalArgumentException("Width of region too large for a .schematic");
        }
        if (height > MAX_SIZE) {
            throw new IllegalArgumentException("Height of region too large for a .schematic");
        }
        if (length > MAX_SIZE) {
            throw new IllegalArgumentException("Length of region too large for a .schematic");
        }

        // ====================================================================
        // Metadata
        // ====================================================================
        HashMap<String, Tag> schematic = new HashMap<String, Tag>();
        schematic.put("Width", new ShortTag((short) width));
        schematic.put("Length", new ShortTag((short) length));
        schematic.put("Height", new ShortTag((short) height));
        schematic.put("Materials", new StringTag("Alpha"));
        schematic.put("WEOriginX", new IntTag(min.getBlockX()));
        schematic.put("WEOriginY", new IntTag(min.getBlockY()));
        schematic.put("WEOriginZ", new IntTag(min.getBlockZ()));
        schematic.put("WEOffsetX", new IntTag(offset.getBlockX()));
        schematic.put("WEOffsetY", new IntTag(offset.getBlockY()));
        schematic.put("WEOffsetZ", new IntTag(offset.getBlockZ()));
        schematic.put("Platform", new StringTag(Fawe.imp().getPlatform()));

        final byte[] blocks = new byte[width * height * length];
        byte[] addBlocks;
        final byte[] blockData = new byte[width * height * length];
        final List<Tag> tileEntities = new ArrayList<Tag>();
        // Precalculate index vars
        int area = width * length;
        final int[] yarea = new int[height];
        final int[] zwidth = new int[length];
        for (int i = 0; i < height; i++) {
            yarea[i] = i * area;
        }
        for (int i = 0; i < length; i++) {
            zwidth[i] = i * width;
        }
        if (clipboard instanceof BlockArrayClipboard) {
            FaweClipboard faweClip = ((BlockArrayClipboard) clipboard).IMP;
            ForEach forEach = new ForEach(yarea, zwidth, blocks, blockData, tileEntities);
            faweClip.forEach(forEach, false);
            addBlocks = forEach.addBlocks;
        } else {
            final int mx = min.getBlockX();
            final int my = min.getBlockY();
            final int mz = min.getBlockZ();
            MutableBlockVector mutable = new MutableBlockVector(0, 0, 0);
            ForEach forEach = new ForEach(yarea, zwidth, blocks, blockData, tileEntities);
            for (Vector point : region) {
                int x = (point.getBlockX() - mx);
                int y = (point.getBlockY() - my);
                int z = (point.getBlockZ() - mz);
                forEach.run(x, y, z, clipboard.getBlock(point));
            }
            addBlocks = forEach.addBlocks;
        }

        schematic.put("Blocks", new ByteArrayTag(blocks));
        schematic.put("Data", new ByteArrayTag(blockData));
        schematic.put("TileEntities", new ListTag(CompoundTag.class, tileEntities));

        if (addBlocks != null) {
            schematic.put("AddBlocks", new ByteArrayTag(addBlocks));
        }

        List<Tag> entities = new ArrayList<Tag>();
        for (Entity entity : clipboard.getEntities()) {
            BaseEntity state = entity.getState();

            if (state != null) {
                Map<String, Tag> values = new HashMap<String, Tag>();

                // Put NBT provided data
                CompoundTag rawTag = state.getNbtData();
                if (rawTag != null) {
                    values.putAll(rawTag.getValue());
                }

                // Store our location data, overwriting any
                values.put("id", new StringTag(state.getTypeId()));
                values.put("Pos", writeVector(entity.getLocation().toVector(), "Pos"));
                values.put("Rotation", writeRotation(entity.getLocation(), "Rotation"));

                CompoundTag entityTag = new CompoundTag(values);
                entities.add(entityTag);
            }
        }

        schematic.put("Entities", new ListTag(CompoundTag.class, entities));

        CompoundTag schematicTag = new CompoundTag(schematic);
        return schematicTag;
    }

    private static Tag writeVector(Vector vector, String name) {
        List<DoubleTag> list = new ArrayList<DoubleTag>();
        list.add(new DoubleTag(vector.getX()));
        list.add(new DoubleTag(vector.getY()));
        list.add(new DoubleTag(vector.getZ()));
        return new ListTag(DoubleTag.class, list);
    }

    private static Tag writeRotation(Location location, String name) {
        List<FloatTag> list = new ArrayList<FloatTag>();
        list.add(new FloatTag(location.getYaw()));
        list.add(new FloatTag(location.getPitch()));
        return new ListTag(FloatTag.class, list);
    }

    @Override
    public void close() throws IOException {
        outputStream.close();
    }

    public static Class<?> inject() {
        return SchematicWriter.class;
    }
}