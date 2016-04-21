package com.boydti.fawe;

import com.boydti.fawe.object.FaweLocation;
import com.boydti.fawe.util.FaweQueue;
import com.boydti.fawe.util.SetQueue;
import com.boydti.fawe.util.TaskManager;
import com.sk89q.jnbt.ByteArrayTag;
import com.sk89q.jnbt.IntTag;
import com.sk89q.jnbt.NBTInputStream;
import com.sk89q.jnbt.ShortTag;
import com.sk89q.jnbt.Tag;
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

/**
 * The FaweAPI class offers a few useful functions.<br>
 *  - This class is not intended to replace the WorldEdit API<br>
 *  <br>
 *  FaweAPI.[some method]
 */
public class FaweAPI {

    private FaweAPI() {}

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

    public static void fixLighting(String world, int x, int z, final boolean fixAll) {
        FaweQueue queue = SetQueue.IMP.getNewQueue(world, false);
        queue.fixLighting(queue.getChunk(x, z), fixAll);
    }


    public static void fixLighting(final Chunk chunk, final boolean fixAll) {
        FaweQueue queue = SetQueue.IMP.getNewQueue(chunk.getWorld().getName(), false);
        queue.fixLighting(queue.getChunk(chunk.getX(), chunk.getZ()), fixAll);
    }

    /**
     * If a schematic is too large to be pasted normally<br>
     *  - Skips any block history
     *  - Ignores some block data
     *  - No, it's not streaming it from disk, but it is a lot faster
     * @param file
     * @param loc
     * @return
     */
    public static void streamSchematicAsync(final File file, final Location loc) {
        final FaweLocation fl = new FaweLocation(loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
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
                    final FileInputStream is = new FileInputStream(file);
                    streamSchematic(is, loc);
                } catch (final IOException e) {
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
                    final ReadableByteChannel rbc = Channels.newChannel(url.openStream());
                    final InputStream is = Channels.newInputStream(rbc);
                    streamSchematic(is, loc);
                } catch (final IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * If a schematic is too large to be pasted normally<br>
     *  - Skips any block history
     *  - Ignores some block data
     *  - Not actually streaming from disk, but it does skip a lot of overhead
     * @param is
     * @param loc
     * @throws IOException
     */
    public static void streamSchematic(final InputStream is, final FaweLocation loc) throws IOException {
        final NBTInputStream stream = new NBTInputStream(new GZIPInputStream(is));
        Tag tag = stream.readNamedTag().getTag();
        stream.close();

        Map<String, Tag> tagMap = (Map<String, Tag>) tag.getValue();

        final short width = ShortTag.class.cast(tagMap.get("Width")).getValue();
        final short length = ShortTag.class.cast(tagMap.get("Length")).getValue();
        final short height = ShortTag.class.cast(tagMap.get("Height")).getValue();
        byte[] ids = ByteArrayTag.class.cast(tagMap.get("Blocks")).getValue();
        byte[] datas = ByteArrayTag.class.cast(tagMap.get("Data")).getValue();

        final String world = loc.world;

        final int x_offset = loc.x + IntTag.class.cast(tagMap.get("WEOffsetX")).getValue();
        final int y_offset = loc.y + IntTag.class.cast(tagMap.get("WEOffsetY")).getValue();
        final int z_offset = loc.z + IntTag.class.cast(tagMap.get("WEOffsetZ")).getValue();

        tagMap = null;
        tag = null;

        FaweQueue queue = SetQueue.IMP.getNewQueue(loc.world, true);

        for (int y = 0; y < height; y++) {
            final int yy = y_offset + y;
            if (yy > 255) {
                continue;
            }
            final int i1 = y * width * length;
            for (int z = 0; z < length; z++) {
                final int i2 = (z * width) + i1;
                final int zz = z_offset + z;
                for (int x = 0; x < width; x++) {
                    final int i = i2 + x;
                    final int xx = x_offset + x;
                    final short id = (short) (ids[i] & 0xFF);
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
                            queue.setBlock(xx, yy, zz, id, (byte) 0);
                            break;
                        default: {
                            queue.setBlock(xx, yy, zz, id, datas[i]);
                            break;
                        }
                    }
                }
            }
        }

        queue.enqueue();

        ids = null;
        datas = null;
    }

    /**
     * Set a task to run when the async queue is empty
     * @param whenDone
     */
    public static void addTask(final Runnable whenDone) {
        SetQueue.IMP.addTask(whenDone);
    }
}
