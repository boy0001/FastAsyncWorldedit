package com.sk89q.worldedit.extent.clipboard.io;

import com.boydti.fawe.FaweCache;
import com.boydti.fawe.object.RunnableVal2;
import com.boydti.fawe.object.clipboard.FaweClipboard;
import com.boydti.fawe.util.ReflectionUtils;
import com.sk89q.jnbt.ByteArrayTag;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.DoubleTag;
import com.sk89q.jnbt.FloatTag;
import com.sk89q.jnbt.IntTag;
import com.sk89q.jnbt.ListTag;
import com.sk89q.jnbt.NBTOutputStream;
import com.sk89q.jnbt.ShortTag;
import com.sk89q.jnbt.StringTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.registry.WorldData;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


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

    private static class ForEach extends RunnableVal2<Vector, BaseBlock> {
        int x = -1;
        int y = 0;
        int z = 0;
        int index = 0;

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
        public void run(Vector point, BaseBlock block) {
            int x = (int) point.x;
            int y = (int) point.y;
            int z = (int) point.z;
            if (this.x == x - 1 && this.y == y && this.z == z) {
                index++;
                x++;
            } else {
                index = yarea[y] + zwidth[z] + x;
            }
            int id = block.getId();
            blocks[index] = (byte) id;
            if (FaweCache.hasData(id)) {
                blockData[index] = (byte) block.getData();
                if (id > 255) {
                    if (addBlocks == null) { // Lazily create section
                        addBlocks = new byte[(blocks.length >> 1) + 1];
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
    public void write(Clipboard clipboard, WorldData data) throws IOException {
        outputStream.writeNamedTag("Schematic", writeTag(clipboard));
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

        final byte[] blocks = new byte[width * height * length];
        byte[] addBlocks = null;
        final byte[] blockData = new byte[width * height * length];
        final List<Tag> tileEntities = new ArrayList<Tag>();
        // Precalculate index vars
        int area = width * length;
        final int[] yarea = new int[height];
        final int[] zwidth = new int[width];
        for (int i = 0; i < height; i++) {
            yarea[i] = i * area;
        }
        for (int i = 0; i < width; i++) {
            zwidth[i] = i * width;
        }
        if (clipboard instanceof BlockArrayClipboard) {
            FaweClipboard faweClip = ((BlockArrayClipboard) clipboard).IMP;
            ForEach forEach = new ForEach(yarea, zwidth, blocks, blockData, tileEntities);
            faweClip.forEach(forEach, false);
            addBlocks = forEach.addBlocks;
        } else {
            final int mx = (int) min.x;
            final int my = (int) min.y;
            final int mz = (int) min.z;
            Vector mutable = new Vector(0, 0, 0);
            ForEach forEach = new ForEach(yarea, zwidth, blocks, blockData, tileEntities);
            for (Vector point : region) {
                mutable.x = point.x - mx;
                mutable.y = point.y - my;
                mutable.z = point.z - mz;
                forEach.run(mutable, clipboard.getBlock(point));
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