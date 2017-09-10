package com.boydti.fawe.jnbt;

import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.RunnableVal2;
import com.boydti.fawe.object.clipboard.CPUOptimizedClipboard;
import com.boydti.fawe.object.clipboard.DiskOptimizedClipboard;
import com.boydti.fawe.object.clipboard.FaweClipboard;
import com.boydti.fawe.object.clipboard.MemoryOptimizedClipboard;
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

public class SchematicStreamer extends NBTStreamer {
    private final UUID uuid;

    public SchematicStreamer(NBTInputStream stream, UUID uuid) {
        super(stream);
        this.uuid = uuid;
        clipboard = new BlockArrayClipboard(new CuboidRegion(new Vector(0, 0, 0), new Vector(0, 0, 0)), fc);
    }

    public void addBlockReaders() {
        NBTStreamReader initializer = new NBTStreamReader<Integer, Integer>() {
            @Override
            public void run(Integer length, Integer type) {
                setupClipboard(length);
            }
        };
        NBTStreamReader initializer2 = new NBTStreamReader<Integer, Integer>() {
            @Override
            public void run(Integer length, Integer type) {
                setupClipboard(length*2);
            }
        };
        addReader("Schematic.Blocks.?", initializer);
        addReader("Schematic.Data.?", initializer);
        addReader("Schematic.AddBlocks.?", initializer2);
        addReader("Schematic.Blocks.#", new ByteReader() {
            @Override
            public void run(int index, int value) {
                if (value != 0) fc.setId(index, value);
            }
        });
        addReader("Schematic.Data.#", new ByteReader() {
            @Override
            public void run(int index, int value) {
                if (value != 0) fc.setData(index, value);
            }
        });
        addReader("Schematic.AddBlocks.#", new ByteReader() {
            @Override
            public void run(int index, int value) {
                if (value != 0) {
                    int first = value & 0x0F;
                    int second = (value & 0xF0) >> 4;
                    int gIndex = index << 1;
                    if (first != 0) fc.setAdd(gIndex, first);
                    if (second != 0) fc.setAdd(gIndex + 1, second);
                }
            }
        });
        ByteReader biomeReader = new ByteReader() {
            @Override
            public void run(int index, int value) {
                fc.setBiome(index, value);
            }
        };
        NBTStreamReader<Integer, Integer> initializer23 = new NBTStreamReader<Integer, Integer>() {
            @Override
            public void run(Integer value1, Integer value2) {
                if (fc == null) setupClipboard(length * width * height);
            }
        };
        addReader("Schematic.AWEBiomes.?", initializer23);
        addReader("Schematic.Biomes.?", initializer23);
        addReader("Schematic.AWEBiomes.#", biomeReader); // AWE stores as an int[]
        addReader("Schematic.Biomes.#", biomeReader); // FAWE stores as a byte[] (4x smaller)

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
                fc.createEntity(clipboard, positionTag.asDouble(0), positionTag.asDouble(1), positionTag.asDouble(2), (float) directionTag.asDouble(0), (float) directionTag.asDouble(1), state);
            }
        });
    }

    public void addDimensionReaders() {
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
        addReader("Schematic.WEOriginX", new RunnableVal2<Integer, Integer>() {
            @Override
            public void run(Integer index, Integer value) {
                originX = (value);
            }
        });
        addReader("Schematic.WEOriginY", new RunnableVal2<Integer, Integer>() {
            @Override
            public void run(Integer index, Integer value) {
                originY = (value);
            }
        });
        addReader("Schematic.WEOriginZ", new RunnableVal2<Integer, Integer>() {
            @Override
            public void run(Integer index, Integer value) {
                originZ = (value);
            }
        });
        addReader("Schematic.WEOffsetX", new RunnableVal2<Integer, Integer>() {
            @Override
            public void run(Integer index, Integer value) {
                offsetX = (value);
            }
        });
        addReader("Schematic.WEOffsetY", new RunnableVal2<Integer, Integer>() {
            @Override
            public void run(Integer index, Integer value) {
                offsetY = (value);
            }
        });
        addReader("Schematic.WEOffsetZ", new RunnableVal2<Integer, Integer>() {
            @Override
            public void run(Integer index, Integer value) {
                offsetZ = (value);
            }
        });
    }

    private int height;
    private int width;
    private int length;

    private int originX;
    private int originY;
    private int originZ;

    private int offsetX;
    private int offsetY;
    private int offsetZ;

    private BlockArrayClipboard clipboard;
    private FaweClipboard fc;

    private FaweClipboard setupClipboard(int size) {
        if (fc != null) {
            if (fc.getDimensions().getX() == 0) {
                fc.setDimensions(new Vector(size, 1, 1));
            }
            return fc;
        }
        if (Settings.IMP.CLIPBOARD.USE_DISK) {
            return fc = new DiskOptimizedClipboard(size, 1, 1, uuid);
        } else if (Settings.IMP.CLIPBOARD.COMPRESSION_LEVEL == 0) {
            return fc = new CPUOptimizedClipboard(size, 1, 1);
        } else {
            return fc = new MemoryOptimizedClipboard(size, 1, 1);
        }
    }

    public Vector getOrigin() {
        return new Vector(originX, originY, originZ);
    }

    public Vector getOffset() {
        return new Vector(offsetX, offsetY, offsetZ);
    }

    public Vector getDimensions() {
        return new Vector(width, height, length);
    }

    public void setClipboard(FaweClipboard clipboard) {
        this.fc = clipboard;
    }

    public Clipboard getClipboard() throws IOException {
        try {
            addDimensionReaders();
            addBlockReaders();
            readFully();
            Vector min = new Vector(originX, originY, originZ);
            Vector offset = new Vector(offsetX, offsetY, offsetZ);
            Vector origin = min.subtract(offset);
            Vector dimensions = new Vector(width, height, length);
            fc.setDimensions(dimensions);
            CuboidRegion region = new CuboidRegion(min, min.add(width, height, length).subtract(Vector.ONE));
            clipboard.init(region, fc);
            clipboard.setOrigin(origin);
            return clipboard;
        } catch (Throwable e) {
            if (fc != null) {
                fc.close();
            }
            throw e;
        }
    }
}
