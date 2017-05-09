package com.boydti.fawe;

import com.boydti.fawe.config.BBC;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.example.NMSMappedFaweQueue;
import com.boydti.fawe.example.NMSRelighter;
import com.boydti.fawe.object.FaweLocation;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.FaweQueue;
import com.boydti.fawe.object.PseudoRandom;
import com.boydti.fawe.object.RegionWrapper;
import com.boydti.fawe.object.changeset.DiskStorageHistory;
import com.boydti.fawe.object.schematic.Schematic;
import com.boydti.fawe.regions.FaweMaskManager;
import com.boydti.fawe.util.EditSessionBuilder;
import com.boydti.fawe.util.MainUtil;
import com.boydti.fawe.util.MemUtil;
import com.boydti.fawe.util.SetQueue;
import com.boydti.fawe.util.TaskManager;
import com.boydti.fawe.util.WEManager;
import com.boydti.fawe.wrappers.WorldWrapper;
import com.sk89q.jnbt.ByteArrayTag;
import com.sk89q.jnbt.IntTag;
import com.sk89q.jnbt.NBTInputStream;
import com.sk89q.jnbt.ShortTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extension.factory.DefaultMaskParser;
import com.sk89q.worldedit.extension.factory.DefaultTransformParser;
import com.sk89q.worldedit.extension.factory.HashTagPatternParser;
import com.sk89q.worldedit.extension.platform.CommandManager;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.internal.registry.AbstractFactory;
import com.sk89q.worldedit.internal.registry.InputParser;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.AbstractWorld;
import com.sk89q.worldedit.world.World;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
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
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * The FaweAPI class offers a few useful functions.<br>
 *  - This class is not intended to replace the WorldEdit API<br>
 *  - With FAWE installed, you can use the EditSession and other WorldEdit classes from an async thread.<br>
 *  <br>
 *  FaweAPI.[some method]
 */
public class FaweAPI {
    /**
     * Offers a lot of options for building an EditSession
     * @see com.boydti.fawe.util.EditSessionBuilder
     * @param world
     * @return A new EditSessionBuilder
     */
    public static EditSessionBuilder getEditSessionBuilder(World world) {
        return new EditSessionBuilder(world);
    }

    /**
     * The TaskManager has some useful methods for doing things asynchronously
     * @return TaskManager
     */
    public static TaskManager getTaskManager() {
        return TaskManager.IMP;
    }

    /**
     * Add a custom mask for use in e.g {@literal //mask #id:<input>}
     * @see com.sk89q.worldedit.command.MaskCommands
     * @param methods The class with a bunch of mask methods
     * @return true if the mask was registered
     */
    public static boolean registerMasks(Object methods) {
        DefaultMaskParser parser = getParser(DefaultMaskParser.class);
        if (parser != null) parser.register(methods);
        return parser != null;
    }

    /**
     * Add a custom material for use in e.g {@literal //material #id:<input>}
     * @see com.sk89q.worldedit.command.PatternCommands
     * @param methods The class with a bunch of pattern methods
     * @return true if the mask was registered
     */
    public static boolean registerPatterns(Object methods) {
        HashTagPatternParser parser = getParser(HashTagPatternParser.class);
        if (parser != null) parser.register(methods);
        return parser != null;
    }

    /**
     * Add a custom transform for use in
     * @see com.sk89q.worldedit.command.TransformCommands
     * @param methods The class with a bunch of transform methods
     * @return true if the transform was registered
     */
    public static boolean registerTransforms(Object methods) {
        DefaultTransformParser parser = Fawe.get().getTransformParser();
        if (parser != null) parser.register(methods);
        return parser != null;
    }

    public static <T> T getParser(Class<T> parserClass) {
        try {
            Field field = AbstractFactory.class.getDeclaredField("parsers");
            field.setAccessible(true);
            ArrayList<InputParser> parsers = new ArrayList<>();
            parsers.addAll((List<InputParser>) field.get(WorldEdit.getInstance().getMaskFactory()));
            parsers.addAll((List<InputParser>) field.get(WorldEdit.getInstance().getBlockFactory()));
            parsers.addAll((List<InputParser>) field.get(WorldEdit.getInstance().getItemFactory()));
            parsers.addAll((List<InputParser>) field.get(WorldEdit.getInstance().getPatternFactory()));
            for (InputParser parser : parsers) {
                if (parserClass.isAssignableFrom(parser.getClass())) {
                    return (T) parser;
                }
            }
            return null;
        } catch (Throwable e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    /**
     * Create a command with the provided aliases and register all methods of the class as sub commands.<br>
     *  - You should try to register commands during startup
     *  - If no aliases are specified, all methods become root commands
     * @param clazz The class containing all the sub command methods
     * @param aliases The aliases to give the command (or none)
     */
    public static void registerCommands(Object clazz, String... aliases) {
        CommandManager.getInstance().registerCommands(clazz, aliases);
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
    public static FawePlayer wrapPlayer(Object obj) {
        return FawePlayer.wrap(obj);
    }

    public static FaweQueue createQueue(String worldName, boolean autoqueue) {
        return SetQueue.IMP.getNewQueue(worldName, true, autoqueue);
    }

    /**
     * You can either use a FaweQueue or an EditSession to change blocks<br>
     *     - The FaweQueue skips a bit of overhead so it's faster<br>
     *     - The WorldEdit EditSession can do a lot more<br>
     * Remember to enqueue it when you're done!<br>
     * @see com.boydti.fawe.object.FaweQueue#enqueue()
     * @param world The name of the world
     * @param autoqueue If it should start dispatching before you enqueue it.
     * @return
     */
    public static FaweQueue createQueue(World world, boolean autoqueue) {
        return SetQueue.IMP.getNewQueue(world, true, autoqueue);
    }

    public static World getWorld(String worldName) {
        List<? extends World> worlds = WorldEdit.getInstance().getServer().getWorlds();
        for (World current : worlds) {
            if (Fawe.imp().getWorldName(current).equals(worldName)) {
                return WorldWrapper.wrap((AbstractWorld) current);
            }
        }
        for (World current : worlds) {
            if (current.getName().equals(worldName)) {
                return WorldWrapper.wrap((AbstractWorld) current);
            }
        }
        return null;
    }

    /**
     * Upload the clipboard to the configured web interface
     * @param clipboard The clipboard (may not be null)
     * @param format The format to use (some formats may not be supported)
     * @return The download URL or null
     */
    public static URL upload(final Clipboard clipboard, final ClipboardFormat format) {
        return format.uploadAnonymous(clipboard);
    }

    /**
     * Just forwards to ClipboardFormat.SCHEMATIC.load(file)
     * @see com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat
     * @see com.boydti.fawe.object.schematic.Schematic
     * @param file
     * @return
     */
    public static Schematic load(File file) throws IOException {
        return ClipboardFormat.SCHEMATIC.load(file);
    }

    /**
     * Get a list of supported protection plugin masks.
     * @return Set of FaweMaskManager
     */
    public static Set<FaweMaskManager> getMaskManagers() {
        return new HashSet<>(WEManager.IMP.managers);
    }

    /**
     * Check if the server has more than the configured low memory threshold
     * @return True if the server has limited memory
     */
    public static boolean isMemoryLimited() {
        return MemUtil.isMemoryLimited();
    }

    /**
     * Use ThreadLocalRandom instead
     * @return
     */
    @Deprecated
    public static PseudoRandom getFastRandom() {
        return new PseudoRandom();
    }

    /**
     * Get a player's allowed WorldEdit region
     * @param player
     * @return
     */
    public static RegionWrapper[] getRegions(FawePlayer player) {
        return WEManager.IMP.getMask(player);
    }

    /**
     * Cancel the edit with the following extent<br>
     *     - The extent must be the one being used by an EditSession, otherwise an error may be thrown <br>
     *     - Insert an extent into the EditSession using the EditSessionEvent: http://wiki.sk89q.com/wiki/WorldEdit/API/Hooking_EditSession <br>
     * @see com.sk89q.worldedit.EditSession#getRegionExtent() To get the FaweExtent for an EditSession
     * @param extent
     * @param reason
     */
    public static void cancelEdit(Extent extent, BBC reason) {
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
    public static DiskStorageHistory getChangeSetFromFile(File file) {
        if (!file.exists() || file.isDirectory()) {
            throw new IllegalArgumentException("Not a file!");
        }
        if (!file.getName().toLowerCase().endsWith(".bd")) {
            throw new IllegalArgumentException("Not a BD file!");
        }
        if (Settings.IMP.HISTORY.USE_DISK) {
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
     * @param shallow - If shallow is true, FAWE will only read the first Settings.IMP.BUFFER_SIZE bytes to obtain history info<br>
     *                Reading only part of the file will result in unreliable bounds info for large edits
     * @return
     */
    public static List<DiskStorageHistory> getBDFiles(FaweLocation origin, UUID user, int radius, long timediff, boolean shallow) {
        File history = MainUtil.getFile(Fawe.imp().getDirectory(), Settings.IMP.PATHS.HISTORY + File.separator + origin.world);
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
                String aName = a.getName();
                String bName = b.getName();
                int aI = Integer.parseInt(aName.substring(0, aName.length() - 3));
                int bI = Integer.parseInt(bName.substring(0, bName.length() - 3));
                long value = aI - bI;
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
     * @see com.boydti.fawe.object.changeset.DiskStorageHistory#toEditSession(com.boydti.fawe.object.FawePlayer)
     * @param world
     * @param uuid
     * @param index
     * @return
     */
    public static DiskStorageHistory getChangeSetFromDisk(World world, UUID uuid, int index) {
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

    @Deprecated
    public static int fixLighting(String world, Region selection) {
        return fixLighting(world, selection, FaweQueue.RelightMode.ALL);
    }

    @Deprecated
    public static int fixLighting(String world, Region selection, final FaweQueue.RelightMode mode) {
        return fixLighting(world, selection, null, mode);
    }

    @Deprecated
    public static int fixLighting(String world, Region selection, @Nullable FaweQueue queue, final FaweQueue.RelightMode mode) {
        return fixLighting(getWorld(world), selection, queue, mode);
    }

    /**
     * Fix the lighting in a selection<br>
     *  - First removes all lighting, then relights
     *  - Relights in parallel (if enabled) for best performance<br>
     *  - Also resends chunks<br>
     * @param world
     * @param selection (assumes cuboid)
     * @return
     */
    public static int fixLighting(World world, Region selection, @Nullable FaweQueue queue, final FaweQueue.RelightMode mode) {
        final Vector bot = selection.getMinimumPoint();
        final Vector top = selection.getMaximumPoint();

        final int minX = bot.getBlockX() >> 4;
        final int minZ = bot.getBlockZ() >> 4;

        final int maxX = top.getBlockX() >> 4;
        final int maxZ = top.getBlockZ() >> 4;

        int count = 0;
        if (queue == null) {
            queue = SetQueue.IMP.getNewQueue(world, true, false);
        }
        // Remove existing lighting first
        if (queue instanceof NMSMappedFaweQueue) {
            final NMSMappedFaweQueue nmsQueue = (NMSMappedFaweQueue) queue;
            NMSRelighter relighter = new NMSRelighter(nmsQueue);
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z ++) {
                    relighter.addChunk(x, z, null, 65535);
                    count++;
                }
            }
            if (mode != FaweQueue.RelightMode.NONE) {
                boolean sky = nmsQueue.hasSky();
                if (sky) {
                    relighter.fixSkyLighting();
                }
                relighter.fixBlockLighting();
            } else {
                relighter.removeLighting();
            }
            relighter.sendChunks();
        }
        return count;
    }

    /**
     * If a schematic is too large to be pasted normally<br>
     *  - Skips any block history
     *  - Ignores nbt
     * @param file
     * @param loc
     * @return
     */
    @Deprecated
    public static void streamSchematic(final File file, final FaweLocation loc) {
        try {
            final FileInputStream is = new FileInputStream(file);
            streamSchematic(is, loc);
        } catch (final IOException e) {
            MainUtil.handleError(e);
        }
    }

    /**
     * @deprecated Since I haven't finished it yet
     * If a schematic is too large to be pasted normally<br>
     *  - Skips any block history
     *  - Ignores nbt
     * @param url
     * @param loc
     */
    @Deprecated
    public static void streamSchematic(final URL url, final FaweLocation loc) {
        try {
            final ReadableByteChannel rbc = Channels.newChannel(url.openStream());
            final InputStream is = Channels.newInputStream(rbc);
            streamSchematic(is, loc);
        } catch (final IOException e) {
            MainUtil.handleError(e);
        }
    }

    /**
     * @deprecated Since I haven't finished it yet
     * If a schematic is too large to be pasted normally<br>
     *  - Skips any block history
     *  - Ignores some block data
     *  - Not actually streaming from disk, but it does skip a lot of overhead
     * @param is
     * @param loc
     * @throws IOException
     */
    @Deprecated
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

        FaweQueue queue = SetQueue.IMP.getNewQueue(getWorld(loc.world), true, true);

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
                    queue.setBlock(xx, yy, zz, id, datas[i]);
                }
            }
        }

        queue.enqueue();
    }

    /**
     * Set a task to run when the global queue (SetQueue class) is empty
     * @param whenDone
     */
    public static void addTask(final Runnable whenDone) {
        SetQueue.IMP.addEmptyTask(whenDone);
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
    public static BBC[] getTranslations() {
        return BBC.values();
    }

    /**
     * @deprecated
     * @see #getEditSessionBuilder(com.sk89q.worldedit.world.World)
     */
    @Deprecated
    public static EditSession getNewEditSession(@Nonnull FawePlayer player) {
        if (player == null) {
            throw new IllegalArgumentException("Player may not be null");
        }
        return player.getNewEditSession();
    }

    /**
     * @deprecated
     * @see #getEditSessionBuilder(com.sk89q.worldedit.world.World)
     */
    @Deprecated
    public static EditSession getNewEditSession(World world) {
        return WorldEdit.getInstance().getEditSessionFactory().getEditSession(world, -1);
    }

}
