package com.boydti.fawe.object.schematic;

import com.boydti.fawe.FaweCache;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.FaweInputStream;
import com.boydti.fawe.object.FaweOutputStream;
import com.boydti.fawe.object.io.FastByteArrayInputStream;
import com.boydti.fawe.object.io.FastByteArrayOutputStream;
import com.boydti.fawe.util.MainUtil;
import com.boydti.fawe.util.ReflectionUtils;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.DoubleTag;
import com.sk89q.jnbt.FloatTag;
import com.sk89q.jnbt.IntTag;
import com.sk89q.jnbt.ListTag;
import com.sk89q.jnbt.NamedTag;
import com.sk89q.jnbt.StringTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.MutableBlockVector;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.registry.WorldData;
import com.sk89q.worldedit.world.storage.NBTConversions;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class FaweFormat implements ClipboardReader, ClipboardWriter {
    private static final int MAX_SIZE = Short.MAX_VALUE - Short.MIN_VALUE;

    private FaweInputStream in;
    private FaweOutputStream out;
    private int mode;

    public FaweFormat(FaweInputStream in) {
        this.in = in;
    }

    public FaweFormat(FaweOutputStream out) {
        this.out = out;
    }

    private boolean compressed = false;

    public boolean compress(int i) throws IOException {
        if (compressed) {
            return false;
        }
        compressed = true;
        if (in != null) {
            in = MainUtil.getCompressedIS(in.getParent());
        } else if (out != null) {
            out = MainUtil.getCompressedOS(out.getParent());
        }
        return true;
    }

    public void setWriteMode(int mode) {
        this.mode = mode;
    }

    @Override
    public Clipboard read(WorldData data) throws IOException {
        return read(data, UUID.randomUUID());
    }

    public Clipboard read(WorldData worldData, UUID clipboardId) throws IOException {
        int mode = in.read();

        BlockArrayClipboard clipboard;
        int ox, oy, oz;
        oy = 0;
        boolean from = false;
        boolean small = false;
        boolean knownSize = false;
        switch (mode) {
            case 0:
                knownSize = true;
                break;
            case 1:
                small = true;
                break;
            case 2:
                break;
            case 3:
                from = true;
                break;
            case 4:
                small = true;
                from = true;
                break;
        }
        if (knownSize) {
            int width = in.readUnsignedShort();
            int height = in.readUnsignedShort();
            int length = in.readUnsignedShort();
            ox = in.readShort();
            oy = in.readShort();
            oz = in.readShort();

            Vector origin = new Vector(0, 0, 0);
            CuboidRegion region = new CuboidRegion(origin, origin.add(width, height, length).subtract(Vector.ONE));
            clipboard = new BlockArrayClipboard(region, clipboardId);
            try {
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        for (int z = 0; z < length; z++) {
                            int combined = in.readUnsignedShort();
                            int id = FaweCache.getId(combined);
                            int data = FaweCache.getData(combined);
                            BaseBlock block = FaweCache.getBlock(id, data);
                            clipboard.setBlock(x, y, z, block);
                        }
                    }
                }
            } catch (WorldEditException e) {
                e.printStackTrace();
                return null;
            }
        } else {
            ox = in.readInt();
            oz = in.readInt();
            FaweOutputStream tmp = new FaweOutputStream(new FastByteArrayOutputStream(Settings.IMP.HISTORY.BUFFER_SIZE));
            int width = 0;
            int height = 0;
            int length = 0;
            while (true) {
                int x, y, z;
                if (small) {
                    tmp.write(x = in.read());
                    tmp.write(y = in.read());
                    tmp.write(z = in.read());
                } else {
                    tmp.writeShort((short) (x = in.readUnsignedShort()));
                    tmp.write(y = in.read());
                    tmp.writeShort((short) (z = in.readUnsignedShort()));
                }
                if (from) {
                    in.skip(2);
                }
                short combined;
                tmp.writeShort(combined = in.readShort());
                if (combined == 0 || y == -1) {
                    break;
                }
                if (x > width) {
                    width = x;
                }
                if (y > height) {
                    height = y;
                }
                if (z > length) {
                    length = z;
                }
            }
            Vector origin = new Vector(0, 0, 0);
            CuboidRegion region = new CuboidRegion(origin, origin.add(width, height, length));
            clipboard = new BlockArrayClipboard(region, clipboardId);
            width++;
            height++;
            length++;
            byte[] array = ((ByteArrayOutputStream) tmp.getParent()).toByteArray();
            FaweInputStream part = new FaweInputStream(new FastByteArrayInputStream(array));
            try {
                for (int i = 0; i < array.length; i += 9) {
                    int x, y, z;
                    if (small) {
                        x = in.read();
                        y = in.read();
                        z = in.read();
                    } else {
                        x = in.readUnsignedShort();
                        y = in.read();
                        z = in.readUnsignedShort();
                    }
                    if (from) {
                        in.skip(2);
                    }
                    int combined = in.readShort();
                    int id = FaweCache.getId(combined);
                    int data = FaweCache.getData(combined);
                    BaseBlock block = FaweCache.getBlock(id, data);
                    clipboard.setBlock(x, y, z, block);
                }
            } catch (WorldEditException e) {
                e.printStackTrace();
                return null;
            }
        }
        try {
            NamedTag namedTag;
            while ((namedTag = in.readNBT()) != null) {
                CompoundTag compound = (CompoundTag) namedTag.getTag();
                Map<String, Tag> map = compound.getValue();
                if (map.containsKey("Rotation")) {
                    // Entity
                    String id = compound.getString("id");
                    Location location = NBTConversions.toLocation(clipboard, compound.getListTag("Pos"), compound.getListTag("Rotation"));
                    if (!id.isEmpty()) {
                        BaseEntity state = new BaseEntity(id, compound);
                        clipboard.createEntity(location, state);
                    }
                } else {
                    // Tile
                    int x = compound.getInt("x");
                    int y = compound.getInt("y");
                    int z = compound.getInt("z");
                    clipboard.setTile(x, y, z, compound);

                }
            }
        } catch (Throwable ignore) {
        }
        clipboard.setOrigin(new Vector(ox, oy, oz));
        return clipboard;
    }

    @Override
    public void write(Clipboard clipboard, WorldData worldData) throws IOException {
        write(clipboard, worldData, "FAWE");
    }

    public void write(Clipboard clipboard, WorldData worldData, String owner) throws IOException {
        Region region = clipboard.getRegion();
        int width = region.getWidth();
        int height = region.getHeight();
        int length = region.getLength();

        if (width > MAX_SIZE) {
            throw new IllegalArgumentException("Width of region too large for a .nbt");
        }
        if (height > MAX_SIZE) {
            throw new IllegalArgumentException("Height of region too large for a .nbt");
        }
        if (length > MAX_SIZE) {
            throw new IllegalArgumentException("Length of region too large for a .nbt");
        }

        Vector min = clipboard.getMinimumPoint();
        Vector max = clipboard.getMaximumPoint();
        Vector origin = clipboard.getOrigin().subtract(min);

        // Mode
        out.write(mode);
        ArrayDeque<CompoundTag> tiles = new ArrayDeque<>();
        boolean from = false;
        boolean small = true;
        switch (mode) {
            default:
                throw new IllegalArgumentException("Invalid mode: " + mode);
            case 3:
                from = true;
            case 2:
                small = false;
            case 1: {
                out.writeInt(origin.getBlockX());
                out.writeInt(origin.getBlockZ());
                for (Vector pt : clipboard.getRegion()) {
                    BaseBlock block = clipboard.getBlock(pt);
                    if (block.getId() == 0) {
                        continue;
                    }
                    int x = pt.getBlockX() - min.getBlockX();
                    int y = pt.getBlockY() - min.getBlockY();
                    int z = pt.getBlockZ() - min.getBlockZ();
                    if (block.hasNbtData()) {
                        CompoundTag tile = block.getNbtData();
                        Map<String, Tag> map = ReflectionUtils.getMap(tile.getValue());
                        map.put("id", new StringTag(block.getNbtId()));
                        map.put("x", new IntTag(x));
                        map.put("y", new IntTag(y));
                        map.put("z", new IntTag(z));
                        tiles.add(tile);
                    }
                    if (small) {
                        out.write((byte) x);
                        out.write((byte) y);
                        out.write((byte) z);
                    } else {
                        out.writeShort((short) x);
                        out.write((byte) y);
                        out.writeShort((short) z);
                    }
                    if (from) {
                        out.writeShort((short) 0);
                    }
                    out.writeShort((short) FaweCache.getCombined(block));
                    break;
                }
                int i = (small ? 3 : 5) + (from ? 4 : 2);
                out.write(0, i);
            }
            case 0: {
                // Dimensions
                out.writeShort((short) width);
                out.writeShort((short) height);
                out.writeShort((short) length);
                out.writeShort((short) origin.getBlockX());
                out.writeShort((short) origin.getBlockY());
                out.writeShort((short) origin.getBlockZ());
                MutableBlockVector mutable = new MutableBlockVector(0, 0, 0);
                for (int y = min.getBlockY(); y <= max.getBlockY(); y++) {
                    mutable.mutY(y);
                    for (int x = min.getBlockX(); x <= max.getBlockX(); x++) {
                        mutable.mutX(x);
                        for (int z = min.getBlockZ(); z <= max.getBlockZ(); z++) {
                            mutable.mutZ(z);
                            BaseBlock block = clipboard.getBlock(mutable);
                            if (block.getId() == 0) {
                                out.writeShort((short) 0);
                            } else {
                                out.writeShort((short) FaweCache.getCombined(block));
                                if (block.hasNbtData()) {
                                    CompoundTag tile = block.getNbtData();
                                    Map<String, Tag> map = ReflectionUtils.getMap(tile.getValue());
                                    map.put("id", new StringTag(block.getNbtId()));
                                    map.put("x", new IntTag(x - min.getBlockX()));
                                    map.put("y", new IntTag(y - min.getBlockY()));
                                    map.put("z", new IntTag(z - min.getBlockZ()));
                                    tiles.add(tile);
                                }
                            }
                        }
                    }
                }
                break;
            }
        }
        for (CompoundTag tile : tiles) {
            out.writeNBT("", FaweCache.asTag(tile));
        }
        for (Entity entity : clipboard.getEntities()) {
            BaseEntity state = entity.getState();
            if (state != null) {
                CompoundTag entityTag = state.getNbtData();
                Map<String, Tag> map = ReflectionUtils.getMap(entityTag.getValue());
                map.put("id", new StringTag(state.getTypeId()));
                map.put("Pos", writeVector(entity.getLocation().toVector(), "Pos"));
                map.put("Rotation", writeRotation(entity.getLocation(), "Rotation"));
                out.writeNBT("", entityTag);
            }
        }
        close();
    }

    @Override
    public void close() throws IOException {
        if (in != null) {
            in.close();
        }
        if (out != null) {
            out.flush();
            out.close();
        }
    }

    private Tag writeVector(Vector vector, String name) {
        List<DoubleTag> list = new ArrayList<DoubleTag>();
        list.add(new DoubleTag(vector.getX()));
        list.add(new DoubleTag(vector.getY()));
        list.add(new DoubleTag(vector.getZ()));
        return new ListTag(DoubleTag.class, list);
    }

    private Tag writeRotation(Location location, String name) {
        List<FloatTag> list = new ArrayList<FloatTag>();
        list.add(new FloatTag(location.getYaw()));
        list.add(new FloatTag(location.getPitch()));
        return new ListTag(FloatTag.class, list);
    }
}
