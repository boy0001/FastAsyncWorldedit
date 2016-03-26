package com.boydti.fawe;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;

import com.boydti.fawe.object.ChunkLoc;
import com.boydti.fawe.object.FaweChunk;
import com.boydti.fawe.object.FaweLocation;
import com.boydti.fawe.util.SetQueue;
import com.boydti.fawe.util.TaskManager;
import com.sk89q.jnbt.ByteArrayTag;
import com.sk89q.jnbt.IntTag;
import com.sk89q.jnbt.NBTInputStream;
import com.sk89q.jnbt.ShortTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.world.biome.BaseBiome;

/**
 * The FaweAPI class offers a few useful functions.<br>
 *  - This class is not intended to replace the WorldEdit API<br>
 *  <br>
 *  FaweAPI.[some method]
 */
public class FaweAPI {
    
    /**
     * Compare two versions
     * @param version
     * @param major
     * @param minor
     * @param minor2
     * @return true if version is >= major, minor, minor2
     */
    public static boolean checkVersion(final int[] version, final int major, final int minor, final int minor2) {
        return (version[0] > major) || ((version[0] == major) && (version[1] > minor)) || ((version[0] == major) && (version[1] == minor) && (version[2] >= minor2));
    }
    
    /**
     * Set a block at a location asynchronously
     * @param loc
     * @param m
     */
    public static void setBlockAsync(final Location loc, final Material m) {
        SetQueue.IMP.setBlock(loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), (short) m.getId());
    }
    
    /**
     * Set a block at a location asynchronously
     * @param world
     * @param x
     * @param y
     * @param z
     * @param id
     * @param data
     */
    public static void setBlockAsync(final String world, final int x, final int y, final int z, final short id, final byte data) {
        SetQueue.IMP.setBlock(world, x, y, z, id, data);
    }
    
    /**
     * Set a biome at a location asynchronously
     * @param world
     * @param x
     * @param z
     * @param id
     * @param data
     */
    public static void setBiomeAsync(final String world, final int x, final int z, BaseBiome biome) {
        SetQueue.IMP.setBiome(world, x, z, biome);
    }

    /**
     * Set a biome at a location asynchronously
     * @param loc
     * @param biome
     */
    public static void setBiomeAsync(Location loc, BaseBiome biome) {
        SetQueue.IMP.setBiome(loc.getWorld().getName(), loc.getBlockX(), loc.getBlockZ(), biome);
    }
    
    /**
     * This will return a FaweChunk object that can be modified.<br>
     *  - The FaweChunk object can be reused if you want identical changes across chunks<br>
     *  - This is additive modification.<br>
     *  - First use {@link FaweChunk#fill(int, byte)} (e.g. with air) for absolute modification<br>
     * When ready, use {@link #setChunk(FaweChunk, ChunkLoc)}
     * @return
     */
    public static FaweChunk<?> createChunk() {
        return SetQueue.IMP.queue.getChunk(new ChunkLoc(null, 0, 0));
    }
    
    /**
     * @see #createChunk()
     * @param data
     * @param location
     */
    public static void setChunkAsync(FaweChunk<?> data, ChunkLoc location) {
        data.setChunkLoc(location);
        data.addToQueue();
    }
    
    /**
     * @see #createChunk()
     * @param data
     * @param chunk
     */
    public static void setChunkAsync(FaweChunk<?> data, Chunk chunk) {
        ChunkLoc loc = new ChunkLoc(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
        data.setChunkLoc(loc);
        data.addToQueue();
    }
    
    /**
     * Fix the lighting at a chunk location.<br>
     *  - The fixAll parameter determines if extensive relighting should occur (slow)
     * @param loc
     */
    public static void fixLighting(ChunkLoc loc, boolean fixAll) {
        SetQueue.IMP.queue.fixLighting(SetQueue.IMP.queue.getChunk(loc), fixAll);
    }
    
    /**
     * Fix the lighting at a chunk.<br>
     *  - The fixAll parameter determines if extensive relighting should occur (slow)
     * @param chunk
     */
    public static void fixLighting(Chunk chunk, boolean fixAll) {
        ChunkLoc loc = new ChunkLoc(chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
        SetQueue.IMP.queue.fixLighting(SetQueue.IMP.queue.getChunk(loc), fixAll);
    }
    
    /**
     * If a schematic is too large to be pasted normally<br>
     *  - Skips any block history
     *  - Ignores some block data
     * @param file
     * @param loc
     * @return
     */
    public static void streamSchematicAsync(final File file, final Location loc) {
        FaweLocation fl = new FaweLocation(loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        streamSchematicAsync(file, fl);
    }
    
    /**
     * If a schematic is too large to be pasted normally<br>
     *  - Skips any block history
     *  - Ignores some block data
     * @param file
     * @param loc
     * @return
     */
    public static void streamSchematicAsync(final File file, final FaweLocation loc) {
        TaskManager.IMP.async(new Runnable() {
            @Override
            public void run() {
                try {
                    FileInputStream is = new FileInputStream(file);
                    streamSchematic(is, loc);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }
    
    /**
     * If a schematic is too large to be pasted normally<br>
     *  - Skips any block history
     *  - Ignores some block data
     * @param url
     * @param loc
     */
    public static void streamSchematicAsync(final URL url, final FaweLocation loc) {
        TaskManager.IMP.async(new Runnable() {
            @Override
            public void run() {
                try {
                    ReadableByteChannel rbc = Channels.newChannel(url.openStream());
                    final InputStream is = Channels.newInputStream(rbc);
                    streamSchematic(is, loc);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }
    
    /**
     * If a schematic is too large to be pasted normally<br>
     *  - Skips any block history
     *  - Ignores some block data
     * @param is
     * @param loc
     * @throws IOException
     */
    public static void streamSchematic(InputStream is, FaweLocation loc) throws IOException {
        NBTInputStream stream = new NBTInputStream(new GZIPInputStream(is));
        Tag tag = stream.readTag();
        stream.close();

        Map<String, Tag> tagMap = (Map<String, Tag>) tag.getValue();

        short width = ShortTag.class.cast(tagMap.get("Width")).getValue();
        short length = ShortTag.class.cast(tagMap.get("Length")).getValue();
        short height = ShortTag.class.cast(tagMap.get("Height")).getValue();
        byte[] ids = ByteArrayTag.class.cast(tagMap.get("Blocks")).getValue();
        byte[] datas = ByteArrayTag.class.cast(tagMap.get("Data")).getValue();
        
        String world = loc.world;
        
        int x_offset = loc.x + IntTag.class.cast(tagMap.get("WEOffsetX")).getValue();
        int y_offset = loc.y + IntTag.class.cast(tagMap.get("WEOffsetY")).getValue();
        int z_offset = loc.z + IntTag.class.cast(tagMap.get("WEOffsetZ")).getValue();
        
        tagMap = null;
        tag = null;

        for (int y = 0; y < height; y++) {
            final int yy = y_offset + y;
            if (yy > 255) {
                continue;
            }
            final int i1 = y * width * length;
            for (int z = 0; z < length; z++) {
                final int i2 = (z * width) + i1;
                int zz = z_offset + z;
                for (int x = 0; x < width; x++) {
                    final int i = i2 + x;
                    int xx = x_offset + x;
                    short id = (short) (ids[i] & 0xFF);
                    switch (id) {
                        case 0:
                        case 2:
                        case 4:
                        case 13:
                        case 14:
                        case 15:
                        case 20:
                        case 21:
                        case 22:
                        case 30:
                        case 32:
                        case 37:
                        case 39:
                        case 40:
                        case 41:
                        case 42:
                        case 45:
                        case 46:
                        case 47:
                        case 48:
                        case 49:
                        case 51:
                        case 56:
                        case 57:
                        case 58:
                        case 60:
                        case 7:
                        case 8:
                        case 9:
                        case 10:
                        case 11:
                        case 73:
                        case 74:
                        case 78:
                        case 79:
                        case 80:
                        case 81:
                        case 82:
                        case 83:
                        case 85:
                        case 87:
                        case 88:
                        case 101:
                        case 102:
                        case 103:
                        case 110:
                        case 112:
                        case 113:
                        case 121:
                        case 122:
                        case 129:
                        case 133:
                        case 165:
                        case 166:
                        case 169:
                        case 170:
                        case 172:
                        case 173:
                        case 174:
                        case 181:
                        case 182:
                        case 188:
                        case 189:
                        case 190:
                        case 191:
                        case 192:
                            setBlockAsync(world, xx, yy, zz, id, (byte) 0);
                            break;
                        default: {
                            setBlockAsync(world, xx, yy, zz, id, datas[i]);
                            break;
                        }
                    }
                }
            }
        }
        
        ids = null;
        datas = null;
        System.gc();
        System.gc();
    }

    /**
     * Set a task to run when the async queue is empty
     * @param whenDone
     */
    public static void addTask(final Runnable whenDone) {
        SetQueue.IMP.addTask(whenDone);
    }
}
