package com.boydti.fawe.jnbt;

import com.boydti.fawe.object.RunnableVal2;
import com.boydti.fawe.object.clipboard.DiskOptimizedClipboard;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.ListTag;
import com.sk89q.jnbt.NBTInputStream;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.regions.CuboidRegion;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class SchematicStreamer extends NBTStreamer {
    private final UUID uuid;

    public SchematicStreamer(NBTInputStream stream, UUID uuid) throws IOException {
        super(stream);
        this.uuid = uuid;
        addReader("Schematic.Height", new RunnableVal2<Integer, Short>() {
            @Override
            public void run(Integer index, Short value) {
                height = (value);
            }
        });
        addReader("Schematic.Width", new RunnableVal2<Integer, Short>() {
            @Override
            public void run(Integer index, Short value) {
                width = (value);
            }
        });
        addReader("Schematic.Length", new RunnableVal2<Integer, Short>() {
            @Override
            public void run(Integer index, Short value) {
                length = (value);
            }
        });
        final AtomicInteger originX = new AtomicInteger();
        final AtomicInteger originY = new AtomicInteger();
        final AtomicInteger originZ = new AtomicInteger();
        addReader("Schematic.WEOriginX", new RunnableVal2<Integer, Integer>() {
            @Override
            public void run(Integer index, Integer value) {
                originX.set(value);
            }
        });
        addReader("Schematic.WEOriginY", new RunnableVal2<Integer, Integer>() {
            @Override
            public void run(Integer index, Integer value) {
                originY.set(value);
            }
        });
        addReader("Schematic.WEOriginZ", new RunnableVal2<Integer, Integer>() {
            @Override
            public void run(Integer index, Integer value) {
                originZ.set(value);
            }
        });
        final AtomicInteger offsetX = new AtomicInteger();
        final AtomicInteger offsetY = new AtomicInteger();
        final AtomicInteger offsetZ = new AtomicInteger();
        addReader("Schematic.WEOffsetX", new RunnableVal2<Integer, Integer>() {
            @Override
            public void run(Integer index, Integer value) {
                offsetX.set(value);
            }
        });
        addReader("Schematic.WEOffsetY", new RunnableVal2<Integer, Integer>() {
            @Override
            public void run(Integer index, Integer value) {
                offsetY.set(value);
            }
        });
        addReader("Schematic.WEOffsetZ", new RunnableVal2<Integer, Integer>() {
            @Override
            public void run(Integer index, Integer value) {
                offsetZ.set(value);
            }
        });
        // Blocks
        RunnableVal2<Integer, Integer> initializer = new RunnableVal2<Integer, Integer>() {
            @Override
            public void run(Integer length, Integer type) {
                setupClipboard(length);
            }
        };
        addReader("Schematic.Blocks.?", initializer);
        addReader("Schematic.Data.?", initializer);
        addReader("Schematic.AddBlocks.?", initializer);
        addReader("Schematic.Blocks.#", new RunnableVal2<Integer, Byte>() {
            int i;
            @Override
            public void run(Integer index, Byte value) {
                fc.setId(i++, value);
            }
        });
        addReader("Schematic.Data.#", new RunnableVal2<Integer, Byte>() {
            int i;
            @Override
            public void run(Integer index, Byte value) {
                fc.setData(i++, value);
            }
        });
        addReader("Schematic.AddBlocks.#", new RunnableVal2<Integer, Byte>() {
            int i;
            @Override
            public void run(Integer index, Byte value) {
                fc.setAdd(i++, value);
            }
        });
        // Tiles
        addReader("Schematic.TileEntities.#", new RunnableVal2<Integer, CompoundTag>() {
            @Override
            public void run(Integer index, CompoundTag value) {
                if (fc == null) {
                    setupClipboard(0);
                }
                int x = value.getInt("x");
                int y = value.getInt("y");
                int z = value.getInt("z");
                fc.setTile(x, y, z, value);
            }
        });
        // Entities
        addReader("Schematic.Entities.#", new RunnableVal2<Integer, CompoundTag>() {
            @Override
            public void run(Integer index, CompoundTag compound) {
                if (fc == null) {
                    setupClipboard(0);
                }
                String id = compound.getString("id");
                if (id.isEmpty()) {
                    return;
                }
                ListTag positionTag = compound.getListTag("Pos");
                ListTag directionTag = compound.getListTag("Rotation");
                BaseEntity state = new BaseEntity(id, compound);
                fc.createEntity(null, positionTag.asDouble(0), positionTag.asDouble(1), positionTag.asDouble(2), (float) directionTag.asDouble(0), (float) directionTag.asDouble(1), state);
            }
        });
        readFully();
        Vector min = new Vector(originX.get(), originY.get(), originZ.get());
        Vector offset = new Vector(offsetX.get(), offsetY.get(), offsetZ.get());
        Vector origin = min.subtract(offset);
        Vector dimensions = new Vector(width, height, length);
        fc.setDimensions(dimensions);
        CuboidRegion region = new CuboidRegion(min, min.add(width, height, length).subtract(Vector.ONE));
        clipboard = new BlockArrayClipboard(region, fc);
        clipboard.setOrigin(origin);
    }

    private int height;
    private int width;
    private int length;

    private Clipboard clipboard;
    private DiskOptimizedClipboard fc;

    private DiskOptimizedClipboard setupClipboard(int size) {
        if (fc != null) {
            if (fc.getDimensions().getX() == 0) {
                fc.setDimensions(new Vector(size, 1, 1));
            }
            return fc;
        }
        return fc = new DiskOptimizedClipboard(size, 1, 1, uuid);
    }

    public Clipboard getClipboard() {
        return clipboard;
    }
}
