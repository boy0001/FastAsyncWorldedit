package com.boydti.fawe;

import com.boydti.fawe.config.BBC;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.FaweLocation;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.RegionWrapper;
import com.boydti.fawe.object.changeset.DiskStorageHistory;
import com.boydti.fawe.regions.FaweMaskManager;
import com.boydti.fawe.util.FaweQueue;
import com.boydti.fawe.util.MemUtil;
import com.boydti.fawe.util.SetQueue;
import com.boydti.fawe.util.TaskManager;
import com.boydti.fawe.util.WEManager;
import com.intellectualcrafters.plot.object.PseudoRandom;
import com.sk89q.jnbt.ByteArrayTag;
import com.sk89q.jnbt.IntTag;
import com.sk89q.jnbt.NBTInputStream;
import com.sk89q.jnbt.ShortTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.world.World;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import org.bukkit.Chunk;
import org.bukkit.Location;

/**
 * The FaweAPI class offers a few useful functions.<br>
 *  - This class is not intended to replace the WorldEdit API<br>
 *  - With FAWE installed, you can use the EditSession and other WorldEdit classes from an async thread.<br>
 *  <br>
 *  FaweAPI.[some method]
 */
public class FaweAPI {

    /**
     * The TaskManager has some useful methods for doing things asynchronously
     * @return TaskManager
     */
    public TaskManager getTaskManager() {
        return TaskManager.IMP;
    }

    /**
     * Wrap some object into a FawePlayer<br>
     *     - org.bukkit.entity.Player
     *     - org.spongepowered.api.entity.living.player
     *     - com.sk89q.worldedit.entity.Player
     *     - String (name)
     *     - UUID (player UUID)
     * @param obj
     * @return
     */
    public FawePlayer wrapPlayer(Object obj) {
        return FawePlayer.wrap(obj);
    }

    /**
     * You can either use a FaweQueue or an EditSession to change blocks<br>
     *     - The FaweQueue skips a bit of overhead so it's faster<br>
     *     - The WorldEdit EditSession can do a lot more<br>
     * Remember to enqueue it when you're done!<br>
     * @see com.boydti.fawe.util.FaweQueue#enqueue()
     * @param worldName The name of the world
     * @param autoqueue If it should start dispatching before you enqueue it.
     * @return
     */
    public FaweQueue createQueue(String worldName, boolean autoqueue) {
        return SetQueue.IMP.getNewQueue(worldName, autoqueue);
    }

    public static World getWorld(String worldName) {
        for (World current : WorldEdit.getInstance().getServer().getWorlds()) {
            if (Fawe.imp().getWorldName(current).equals(worldName)) {
                return current;
            }
        }
        return null;
    }

    /**
     * Get a list of supported protection plugin masks.
     * @return Set of FaweMaskManager
     */
    public Set<FaweMaskManager> getMaskManagers() {
        return new HashSet<>(WEManager.IMP.managers);
    }

    /**
     * Check if the server has more than the configured low memory threshold
     * @return True if the server has limited memory
     */
    public boolean isMemoryLimited() {
        return MemUtil.isMemoryLimited();
    }

    /**
     * If you just need things to look random, use this faster alternative
     * @return PseudoRandom
     */
    public PseudoRandom getFastRandom() {
        return new PseudoRandom();
    }

    /**
     * Get a player's allowed WorldEdit region
     * @param player
     * @return
     */
    public Set<RegionWrapper> getRegions(FawePlayer player) {
        return WEManager.IMP.getMask(player);
    }

    /**
     * Cancel the edit with the following extent<br>
     *     - The extent must be the one being used by an EditSession, otherwise an error may be thrown <br>
     *     - Insert an extent into the EditSession using the EditSessionEvent: http://wiki.sk89q.com/wiki/WorldEdit/API/Hooking_EditSession <br>
     * @see com.sk89q.worldedit.EditSession#getFaweExtent() To get the FaweExtent for an EditSession
     * @param extent
     * @param reason
     */
    public void cancelEdit(Extent extent, BBC reason) {
        try {
            WEManager.IMP.cancelEdit(extent, reason);
        } catch (WorldEditException ignore) {}
    }

    public static void addMaskManager(FaweMaskManager maskMan) {
        WEManager.IMP.managers.add(maskMan);
    }

    /**
     * Get the DiskStorageHistory object representing a File
     * @param file
     * @return
     */
    public DiskStorageHistory getChangeSetFromFile(File file) {
        if (!file.exists() || file.isDirectory()) {
            throw new IllegalArgumentException("Not a file!");
        }
        if (!file.getName().toLowerCase().endsWith(".bd")) {
            throw new IllegalArgumentException("Not a BD file!");
        }
        if (Settings.STORE_HISTORY_ON_DISK) {
            throw new IllegalArgumentException("History on disk not enabled!");
        }
        String[] path = file.getPath().split(File.separator);
        if (path.length < 3) {
            throw new IllegalArgumentException("Not in history directory!");
        }
        String worldName = path[path.length - 3];
        String uuidString = path[path.length - 2];
        World world = getWorld(worldName);
        if (world == null) {
            throw new IllegalArgumentException("Corresponding world does not exist: " + worldName);
        }
        UUID uuid;
        try {
            uuid = UUID.fromString(uuidString);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid UUID from file path: " + uuidString);
        }
        DiskStorageHistory history = new DiskStorageHistory(world, uuid, Integer.parseInt(file.getName().split("\\.")[0]));
        return history;
    }

    /**
     * Used in the RollBack to generate a list of DiskStorageHistory objects<br>
     *      - Note: An edit outside the radius may be included if it overlaps with an edit inside that depends on it.
     * @param origin - The origin location
     * @param user - The uuid (may be null)
     * @param radius - The radius from the origin of the edit
     * @param timediff - The max age of the file in milliseconds
     * @param shallow - If shallow is true, FAWE will only read the first Settings.BUFFER_SIZE bytes to obtain history info<br>
     *                Reading only part of the file will result in unreliable bounds info for large edits
     * @return
     */
    public static List<DiskStorageHistory> getBDFiles(FaweLocation origin, UUID user, int radius, long timediff, boolean shallow) {
        File history = new File(Fawe.imp().getDirectory(), "history" + File.separator + origin.world);
        if (!history.exists()) {
            return new ArrayList<>();
        }
        long now = System.currentTimeMillis();
        ArrayList<File> files = new ArrayList<>();
        for (File userFile : history.listFiles()) {
            if (!userFile.isDirectory()) {
                continue;
            }
            UUID userUUID;
            try {
                userUUID = UUID.fromString(userFile.getName());
            } catch (IllegalArgumentException e) {
                continue;
            }
            if (user != null && !userUUID.equals(user)) {
                continue;
            }
            ArrayList<Integer> ids = new ArrayList<>();
            for (File file : userFile.listFiles()) {
                if (file.getName().endsWith(".bd")) {
                    if (timediff >= Integer.MAX_VALUE || now - file.lastModified() <= timediff) {
                        files.add(file);
                        if (files.size() > 2048) {
                            return null;
                        }
                    }
                }
            }
        }
        World world = origin.getWorld();
        Collections.sort(files, new Comparator<File>() {
            @Override
            public int compare(File a, File b) {
                long value = a.lastModified() - b.lastModified();
                return value == 0 ? 0 : value < 0 ? -1 : 1;
            }
        });
        RegionWrapper bounds = new RegionWrapper(origin.x - radius, origin.x + radius, origin.z - radius, origin.z + radius);
        RegionWrapper boundsPlus = new RegionWrapper(bounds.minX - 64, bounds.maxX + 512, bounds.minZ - 64, bounds.maxZ + 512);
        HashSet<RegionWrapper> regionSet = new HashSet<RegionWrapper>(Arrays.asList(bounds));
        ArrayList<DiskStorageHistory> result = new ArrayList<>();
        for (File file : files) {
            UUID uuid = UUID.fromString(file.getParentFile().getName());
            DiskStorageHistory dsh = new DiskStorageHistory(world, uuid, Integer.parseInt(file.getName().split("\\.")[0]));
            DiskStorageHistory.DiskStorageSummary summary = dsh.summarize(boundsPlus, shallow);
            RegionWrapper region = new RegionWrapper(summary.minX, summary.maxX, summary.minZ, summary.maxZ);
            boolean encompassed = false;
            boolean isIn = false;
            for (RegionWrapper allowed : regionSet) {
                isIn = isIn || allowed.intersects(region);
                if (encompassed = allowed.isIn(region.minX, region.maxX) && allowed.isIn(region.minZ, region.maxZ)) {
                    break;
                }
            }
            if (isIn) {
                result.add(0, dsh);
                if (!encompassed) {
                    regionSet.add(region);
                }
                if (shallow && result.size() > 64) {
                    return result;
                }
            }
        }
        return result;
    }

    /**
     * The DiskStorageHistory class is what FAWE uses to represent the undo on disk.
     * @see com.boydti.fawe.object.changeset.DiskStorageHistory#toEditSession(com.sk89q.worldedit.entity.Player)
     * @param world
     * @param uuid
     * @param index
     * @return
     */
    public DiskStorageHistory getChangeSetFromDisk(World world, UUID uuid, int index) {
        return new DiskStorageHistory(world, uuid, index);
    }

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
     * Fix the lighting in a chunk
     * @param world
     * @param x
     * @param z
     * @param fixAll
     */
    public static void fixLighting(String world, int x, int z, final boolean fixAll) {
        FaweQueue queue = SetQueue.IMP.getNewQueue(world, false);
        queue.fixLighting(queue.getChunk(x, z), fixAll);
    }

    /**
     * Fix the lighting in a chunk
     * @param chunk
     * @param fixAll
     */
    public static void fixLighting(final Chunk chunk, final boolean fixAll) {
        FaweQueue queue = SetQueue.IMP.getNewQueue(chunk.getWorld().getName(), false);
        queue.fixLighting(queue.getChunk(chunk.getX(), chunk.getZ()), fixAll);
    }

    /**
     * If a schematic is too large to be pasted normally<br>
     *  - Skips any block history
     *  - Ignores nbt
     *  - No, technically I haven't added proper streaming yet (WIP)
     * @param file
     * @param loc
     * @return
     */
    public static void streamSchematic(final File file, final Location loc) {
        final FaweLocation fl = new FaweLocation(loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        streamSchematic(file, fl);
    }

    /**
     * If a schematic is too large to be pasted normally<br>
     *  - Skips any block history
     *  - Ignores nbt
     * @param file
     * @param loc
     * @return
     */
    public static void streamSchematic(final File file, final FaweLocation loc) {
        try {
            final FileInputStream is = new FileInputStream(file);
            streamSchematic(is, loc);
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * If a schematic is too large to be pasted normally<br>
     *  - Skips any block history
     *  - Ignores nbt
     * @param url
     * @param loc
     */
    public static void streamSchematic(final URL url, final FaweLocation loc) {
        try {
            final ReadableByteChannel rbc = Channels.newChannel(url.openStream());
            final InputStream is = Channels.newInputStream(rbc);
            streamSchematic(is, loc);
        } catch (final IOException e) {
            e.printStackTrace();
        }
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
     * Set a task to run when the global queue (SetQueue class) is empty
     * @param whenDone
     */
    public static void addTask(final Runnable whenDone) {
        SetQueue.IMP.addTask(whenDone);
    }

    /**
     * Have a task run when the server is low on memory (configured threshold)
     * @param run
     */
    public static void addMemoryLimitedTask(Runnable run) {
        MemUtil.addMemoryLimitedTask(run);
    }

    /**
     * Have a task run when the server is no longer low on memory (configured threshold)
     * @param run
     */
    public static void addMemoryPlentifulTask(Runnable run) {
        MemUtil.addMemoryPlentifulTask(run);
    }

    /**
     * @see BBC
     * @return
     */
    public BBC[] getTranslations() {
        return BBC.values();
    }
}
