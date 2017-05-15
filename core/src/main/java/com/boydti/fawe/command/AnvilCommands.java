package com.boydti.fawe.command;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweCache;
import com.boydti.fawe.config.BBC;
import com.boydti.fawe.jnbt.NBTStreamer;
import com.boydti.fawe.jnbt.anvil.MCAChunk;
import com.boydti.fawe.jnbt.anvil.MCAClipboard;
import com.boydti.fawe.jnbt.anvil.MCAFile;
import com.boydti.fawe.jnbt.anvil.MCAFilter;
import com.boydti.fawe.jnbt.anvil.MCAFilterCounter;
import com.boydti.fawe.jnbt.anvil.MCAQueue;
import com.boydti.fawe.object.FawePlayer;
import com.boydti.fawe.object.FaweQueue;
import com.boydti.fawe.object.RegionWrapper;
import com.boydti.fawe.object.RunnableVal;
import com.boydti.fawe.object.RunnableVal2;
import com.boydti.fawe.object.RunnableVal4;
import com.boydti.fawe.object.mask.FaweBlockMatcher;
import com.boydti.fawe.object.number.MutableLong;
import com.boydti.fawe.util.ArrayUtil;
import com.boydti.fawe.util.SetQueue;
import com.boydti.fawe.util.StringMan;
import com.sk89q.minecraft.util.commands.Command;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.MutableBlockVector;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.blocks.BlockType;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.function.pattern.RandomPattern;
import com.sk89q.worldedit.internal.annotation.Selection;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.command.binding.Switch;
import com.sk89q.worldedit.util.command.parametric.Optional;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;


import static com.google.common.base.Preconditions.checkNotNull;

@Command(aliases = {"anvil", "/anvil"}, desc = "Manipulate billions of blocks: [More Info](https://github.com/boy0001/FastAsyncWorldedit/wiki/Anvil-API)")
public class AnvilCommands {

    private final WorldEdit worldEdit;

    /**
     * Create a new instance.
     *
     * @param worldEdit reference to WorldEdit
     */
    public AnvilCommands(WorldEdit worldEdit) {
        checkNotNull(worldEdit);
        this.worldEdit = worldEdit;
    }

    @Command(
            aliases = {"replaceall", "rea", "repall"},
            usage = "<folder> [from-block] <to-block>",
            desc = "Replace all blocks in the selection with another",
            flags = "d",
            min = 2,
            max = 4
    )
    @CommandPermissions("worldedit.anvil.replaceall")
    public void replaceAll(Player player, EditSession editSession, String folder, @Optional String from, String to, @Switch('d') boolean useData) throws WorldEditException {
        final FaweBlockMatcher matchFrom;
        if (from == null) {
            matchFrom = FaweBlockMatcher.NOT_AIR;
        } else {
            if (from.contains(":")) {
                useData = true; //override d flag, if they specified data they want it
            }
            matchFrom = FaweBlockMatcher.fromBlocks(worldEdit.getBlocks(player, from, true), useData);
        }
        final FaweBlockMatcher matchTo = FaweBlockMatcher.setBlocks(worldEdit.getBlocks(player, to, true));
        File root = new File(folder + File.separator + "region");
        MCAQueue queue = new MCAQueue(folder, root, true);
        MCAFilterCounter counter = queue.filterWorld(new MCAFilterCounter() {
            @Override
            public void applyBlock(int x, int y, int z, BaseBlock block, MutableLong ignore) {
                if (matchFrom.apply(block)) matchTo.apply(block);
            }
        });
        player.print(BBC.getPrefix() + BBC.VISITOR_BLOCK.format(counter.getTotal()));
    }

    @Command(
            aliases = {"deleteallold"},
            usage = "<folder> <age-ticks> [file-age=60000]",
            desc = "Delete all chunks which haven't been occupied for `age-ticks` and have been accessed since `file-age` (ms) after creation",
            min = 2,
            max = 3
    )
    @CommandPermissions("worldedit.anvil.deleteallold")
    public void deleteAllOld(Player player, String folder, int inhabitedTicks, @Optional("60000") int fileAgeMillis) throws WorldEditException {
        FaweQueue defaultQueue = SetQueue.IMP.getNewQueue(folder, true, false);
        MCAQueue queue = new MCAQueue(folder, defaultQueue.getSaveFolder(), defaultQueue.hasSky());
        MCAFilterCounter result = queue.filterWorld(new MCAFilterCounter() {
            @Override
            public MCAFile applyFile(MCAFile mca) {
                File file = mca.getFile();
                try {
                    BasicFileAttributes attr = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
                    long creation = attr.creationTime().toMillis();
                    long modified = attr.lastModifiedTime().toMillis();
                    if (modified - creation < fileAgeMillis && modified > creation) {
                        mca.setDeleted(true);
                        get().add(512 * 512 * 256);
                        return null;
                    }
                } catch (IOException | UnsupportedOperationException ignore) {}
                try {
                    ForkJoinPool pool = new ForkJoinPool();
                    mca.init();
                    mca.forEachSortedChunk(new RunnableVal4<Integer, Integer, Integer, Integer>() {
                        @Override
                        public void run(Integer x, Integer z, Integer offset, Integer size) {
                            try {
                                byte[] bytes = mca.getChunkCompressedBytes(offset);
                                if (bytes == null) return;
                                    Runnable task = new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            mca.streamChunk(offset, new RunnableVal<NBTStreamer>() {
                                                @Override
                                                public void run(NBTStreamer value) {
                                                    value.addReader(".Level.InhabitedTime", new RunnableVal2<Integer, Long>() {
                                                        @Override
                                                        public void run(Integer index, Long value) {
                                                            if (value <= inhabitedTicks) {
                                                                MCAChunk chunk = new MCAChunk(queue, x, z);
                                                                chunk.setDeleted(true);
                                                                synchronized (mca) {
                                                                    mca.setChunk(chunk);
                                                                }
                                                                get().add(16 * 16 * 256);
                                                            }
                                                        }
                                                    });
                                                }
                                            });
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                };
                                pool.submit(task);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                    pool.awaitQuiescence(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
                    mca.close(pool);
                    pool.shutdown();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }
        });
        player.print(BBC.getPrefix() + BBC.VISITOR_BLOCK.format(result.getTotal()));
    }

    @Command(
            aliases = {"replaceallpattern", "reap", "repallpat"},
            usage = "<folder> [from-block] <to-pattern>",
            desc = "Replace all blocks in the selection with another",
            flags = "dm",
            min = 2,
            max = 4
    )
    @CommandPermissions("worldedit.anvil.replaceall")
    public void replaceAllPattern(Player player, String folder, @Optional String from, final Pattern to, @Switch('d') boolean useData, @Switch('m') boolean useMap) throws WorldEditException {
        FaweQueue defaultQueue = SetQueue.IMP.getNewQueue(folder, true, false);
        MCAQueue queue = new MCAQueue(folder, defaultQueue.getSaveFolder(), defaultQueue.hasSky());
        MCAFilterCounter counter;
        if (useMap) {
            List<String> split = StringMan.split(from, ',');
            if (to instanceof RandomPattern) {
                Pattern[] patterns = ((RandomPattern) to).getPatterns().toArray(new Pattern[0]);
                if (patterns.length == split.size()) {
                    Pattern[] map = new Pattern[Character.MAX_VALUE + 1];
                    for (int i = 0; i < split.size(); i++) {
                        Pattern pattern = patterns[i];
                        String arg = split.get(i);
                        ArrayList<BaseBlock> blocks = new ArrayList<BaseBlock>();
                        for (String arg2 : arg.split(",")) {
                            BaseBlock block = worldEdit.getBlock(player, arg, true);
                            if (!useData && !arg2.contains(":")) {
                                block = new BaseBlock(block.getId(), -1);
                            }
                            blocks.add(block);
                        }
                        for (BaseBlock block : blocks) {
                            if (block.getData() != -1) {
                                int combined = FaweCache.getCombined(block);
                                map[combined] = pattern;
                            } else {
                                for (int data = 0; data < 16; data++) {
                                    int combined = FaweCache.getCombined(block.getId(), data);
                                    map[combined] = pattern;
                                }
                            }
                        }
                    }

                    counter = queue.filterWorld(new MCAFilterCounter() {
                        private final MutableBlockVector mutable = new MutableBlockVector(0, 0, 0);
                        @Override
                        public void applyBlock(int x, int y, int z, BaseBlock block, MutableLong ignore) {
                            int id = block.getId();
                            int data = FaweCache.hasData(id) ? block.getData() : 0;
                            int combined = FaweCache.getCombined(id, data);
                            Pattern p = map[combined];
                            if (p != null) {
                                BaseBlock newBlock = p.apply(x, y, z);
                                int currentId = block.getId();
                                if (FaweCache.hasNBT(currentId)) {
                                    block.setNbtData(null);
                                }
                                block.setId(newBlock.getId());
                                block.setData(newBlock.getData());
                            }
                        }
                    });
                } else {
                    player.print(BBC.getPrefix() + "Mask:Pattern must be a 1:1 match");
                    return;
                }
            } else {
                player.print(BBC.getPrefix() + "Must be a pattern list!");
                return;
            }
        } else {
            final FaweBlockMatcher matchFrom;
            if (from == null) {
                matchFrom = FaweBlockMatcher.NOT_AIR;
            } else {
                if (from.contains(":")) {
                    useData = true; //override d flag, if they specified data they want it
                }
                matchFrom = FaweBlockMatcher.fromBlocks(worldEdit.getBlocks(player, from, true), useData);
            }
            counter = queue.filterWorld(new MCAFilterCounter() {
                @Override
                public void applyBlock(int x, int y, int z, BaseBlock block, MutableLong ignore) {
                    if (matchFrom.apply(block)) {
                        BaseBlock newBlock = to.apply(x, y, z);
                        int currentId = block.getId();
                        if (FaweCache.hasNBT(currentId)) {
                            block.setNbtData(null);
                        }
                        block.setId(newBlock.getId());
                        block.setData(newBlock.getData());
                    }
                }
            });
        }
        player.print(BBC.getPrefix() + BBC.VISITOR_BLOCK.format(counter.getTotal()));
    }

    @Command(
            aliases = {"countall"},
            usage = "<folder> [hasSky] <id>",
            desc = "Count all blocks in a world",
            flags = "d",
            min = 2,
            max = 3
    )
    @CommandPermissions("worldedit.anvil.countallstone")
    public void countAll(Player player, EditSession editSession, String folder, String arg, @Switch('d') boolean useData) throws WorldEditException {
        File root = new File(folder + File.separator + "region");
        MCAQueue queue = new MCAQueue(folder, root, true);
        if (arg.contains(":")) {
            useData = true; //override d flag, if they specified data they want it
        }
        Set<BaseBlock> searchBlocks = worldEdit.getBlocks(player, arg, true);
        final boolean[] allowedId = new boolean[FaweCache.getId(Character.MAX_VALUE)];
        for (BaseBlock block : searchBlocks) {
            allowedId[block.getId()] = true;
        }
        MCAFilterCounter filter;
        if (useData) { // Optimize for both cases
            final boolean[] allowed = new boolean[Character.MAX_VALUE];
            for (BaseBlock block : searchBlocks) {
                allowed[FaweCache.getCombined(block)] = true;
            }
            filter = new MCAFilterCounter() {
                @Override
                public MCAChunk applyChunk(MCAChunk chunk, MutableLong count) {
                    for (int layer = 0; layer < chunk.ids.length; layer++) {
                        byte[] ids = chunk.ids[layer];
                        if (ids == null) {
                            continue;
                        }
                        byte[] datas = chunk.data[layer];
                        for (int i = 0; i < ids.length; i++) {
                            int id = ids[i] & 0xFF;
                            if (!allowedId[id]) {
                                continue;
                            }
                            int combined = (id) << 4;
                            if (FaweCache.hasData(id)) {
                                combined += chunk.getNibble(i, datas);
                            }
                            if (allowed[combined]) {
                                count.increment();
                            }
                        }
                    }
                    return null;
                }
            };
        } else {
            filter = new MCAFilterCounter() {
                @Override
                public MCAChunk applyChunk(MCAChunk chunk, MutableLong count) {
                    for (int layer = 0; layer < chunk.ids.length; layer++) {
                        byte[] ids = chunk.ids[layer];
                        if (ids != null) {
                            for (byte i : ids) {
                                if (allowedId[i & 0xFF]) {
                                    count.increment();
                                }
                            }
                        }
                    }
                    return null;
                }
            };
        }
        queue.filterWorld(filter);
        player.print(BBC.getPrefix() + BBC.SELECTION_COUNT.format(filter.getTotal()));
    }

    @Command(
            aliases = {"distr"},
            desc = "Replace all blocks in the selection with another"
    )
    @CommandPermissions("worldedit.anvil.distr")
    public void distr(Player player, EditSession editSession, @Selection Region selection, @Switch('d') boolean useData) throws WorldEditException {
        long total = 0;
        long[] count;
        MCAFilter<long[]> counts;
        if (useData) {
            counts = runWithSelection(player, editSession, selection, new MCAFilter<long[]>() {
                @Override
                public void applyBlock(int x, int y, int z, BaseBlock block, long[] counts) {
                    counts[block.getCombined()]++;
                }
                @Override
                public long[] init() {
                    return new long[Character.MAX_VALUE + 1];
                }
            });
            count = new long[Character.MAX_VALUE + 1];
        } else {
            counts = runWithSelection(player, editSession, selection, new MCAFilter<long[]>() {
                @Override
                public void applyBlock(int x, int y, int z, BaseBlock block, long[] counts) {
                    counts[block.getId()]++;
                }
                @Override
                public long[] init() {
                    return new long[4096];
                }
            });
            count = new long[4096];
        }
        for (long[] value : counts) {
            for (int i = 0; i < value.length; i++) {
                count[i] += value[i];
                total += value[i];
            }
        }
        ArrayList<long[]> map = new ArrayList<>();
        for (int i = 0; i < count.length; i++) {
            if (count[i] != 0) map.add(new long[] { i, count[i]});
        }
        Collections.sort(map, new Comparator<long[]>() {
            @Override
            public int compare(long[] a, long[] b) {
                long vA = a[1];
                long vB = b[1];
                return (vA < vB) ? -1 : ((vA == vB) ? 0 : 1);
            }
        });
        if (useData) {
            for (long[] c : map) {
                BaseBlock block = FaweCache.CACHE_BLOCK[(int) c[0]];
                String name = BlockType.fromID(block.getId()).getName();
                String str = String.format("%-7s (%.3f%%) %s #%d:%d",
                        String.valueOf(c[1]),
                        ((c[1] * 10000) / total) / 100d,
                        name == null ? "Unknown" : name,
                        block.getType(), block.getData());
                player.print(BBC.getPrefix() + str);
            }
        } else {
            for (long[] c : map) {
                BlockType block = BlockType.fromID((int) c[0]);
                String str = String.format("%-7s (%.3f%%) %s #%d",
                        String.valueOf(c[1]),
                        ((c[1] * 10000) / total) / 100d,
                        block == null ? "Unknown" : block.getName(), c[0]);
                player.print(BBC.getPrefix() + str);
            }
        }
    }

    private <G, T extends MCAFilter<G>> T runWithSelection(Player player, EditSession editSession, Region selection, T filter) {
        if (!(selection instanceof CuboidRegion)) {
            BBC.NO_REGION.send(player);
            return null;
        }
        CuboidRegion cuboid = (CuboidRegion) selection;
        RegionWrapper wrappedRegion = new RegionWrapper(cuboid.getMinimumPoint(), cuboid.getMaximumPoint());
        String worldName = Fawe.imp().getWorldName(editSession.getWorld());
        FaweQueue tmp = SetQueue.IMP.getNewQueue(worldName, true, false);
        File folder = tmp.getSaveFolder();
        MCAQueue queue = new MCAQueue(worldName, folder, tmp.hasSky());
        player.print(BBC.getPrefix() + "Safely unloading regions...");
        tmp.setMCA(new Runnable() {
            @Override
            public void run() {
                player.print(BBC.getPrefix() + "Performing operation...");
                queue.filterRegion(filter, wrappedRegion);
                player.print(BBC.getPrefix() + "Safely loading regions...");
            }
        }, wrappedRegion, true);
        return filter;
    }

    @Command(
            aliases = {"replace"},
            usage = "[from-block] <to-block>",
            desc = "Replace all blocks in the selection with another"
    )
    @CommandPermissions("worldedit.anvil.replace")
    public void replace(Player player, EditSession editSession, @Selection Region selection, @Optional String from, String to, @Switch('d') boolean useData) throws WorldEditException {
        final FaweBlockMatcher matchFrom;
        if (from == null) {
            matchFrom = FaweBlockMatcher.NOT_AIR;
        } else {
            if (from.contains(":")) {
                useData = true; //override d flag, if they specified data they want it
            }
            matchFrom = FaweBlockMatcher.fromBlocks(worldEdit.getBlocks(player, from, true), useData);
        }
        final FaweBlockMatcher matchTo = FaweBlockMatcher.setBlocks(worldEdit.getBlocks(player, to, true));
        MCAFilterCounter filter = runWithSelection(player, editSession, selection, new MCAFilterCounter() {
            @Override
            public void applyBlock(int x, int y, int z, BaseBlock block, MutableLong count) {
                if (matchFrom.apply(block)) {
                    matchTo.apply(block);
                    count.increment();
                }
            }
        });
        if (filter != null) {
            player.print(BBC.getPrefix() + BBC.VISITOR_BLOCK.format(filter.getTotal()));
        }
    }

    @Command(
            aliases = {"removelayers"},
            usage = "<id>",
            desc = "Removes matching chunk layers",
            help = "Remove if all the selected layers in a chunk match the provided id"
    )
    @CommandPermissions("worldedit.anvil.removelayer")
    public void removeLayers(Player player, EditSession editSession, @Selection Region selection, int id) throws WorldEditException {
        Vector min = selection.getMinimumPoint();
        Vector max = selection.getMaximumPoint();
        int minY = min.getBlockY();
        int maxY = max.getBlockY();
        final int startLayer = minY >> 4;
        final int endLayer = maxY >> 4;
        MCAFilterCounter filter = runWithSelection(player, editSession, selection, new MCAFilterCounter() {
            @Override
            public MCAChunk applyChunk(MCAChunk chunk, MutableLong cache) {
                for (int layer = startLayer; layer <= endLayer; layer++) {
                    byte[] ids = chunk.ids[layer];
                    if (ids == null) {
                        return null;
                    }
                    int startY = Math.max(minY, layer << 4) & 15;
                    int endY = Math.min(maxY, 15 + (layer << 4)) & 15;
                    for (int y = startY; y <= endY; y++) {
                        int indexStart = y << 8;
                        int indexEnd = indexStart + 255;
                        for (int index = indexStart; index <= indexEnd; index++) {
                            if (ids[index] != id) {
                                return null;
                            }
                        }
                    }
                    for (int y = startY; y <= endY; y++) {
                        int indexStart = y << 8;
                        int indexEnd = indexStart + 255;
                        ArrayUtil.fill(ids, indexStart, indexEnd + 1, (byte) 0);
                    }
                    chunk.setModified();
                }
                return null;
            }
        });
        if (filter != null) {
            player.print(BBC.getPrefix() + BBC.VISITOR_BLOCK.format(filter.getTotal()));
        }
    }


    @Command(
            aliases = {"copy"},
            desc = "Lazily copy chunks to your anvil clipboard"
    )
    @CommandPermissions("worldedit.anvil.copychunks")
    public void copy(Player player, LocalSession session, EditSession editSession, @Selection Region selection) throws WorldEditException {
        if (!(selection instanceof CuboidRegion)) {
            BBC.NO_REGION.send(player);
            return;
        }
        CuboidRegion cuboid = (CuboidRegion) selection;
        String worldName = Fawe.imp().getWorldName(editSession.getWorld());
        FaweQueue tmp = SetQueue.IMP.getNewQueue(worldName, true, false);
        File folder = tmp.getSaveFolder();
        MCAQueue queue = new MCAQueue(worldName, folder, tmp.hasSky());
        Vector origin = session.getPlacementPosition(player);
        MCAClipboard clipboard = new MCAClipboard(queue, cuboid, origin);
        FawePlayer fp = FawePlayer.wrap(player);
        fp.setMeta(FawePlayer.METADATA_KEYS.ANVIL_CLIPBOARD, clipboard);
        BBC.COMMAND_COPY.send(player, selection.getArea());
    }

    @Command(
            aliases = {"paste"},
            desc = "Paste chunks from your anvil clipboard",
            help =
                    "Paste the chunks from your anvil clipboard.\n" +
                            "The -c will align the paste to the chunks.",
            flags = "c"

    )
    @CommandPermissions("worldedit.anvil.pastechunks")
    public void paste(Player player, LocalSession session, EditSession editSession, @Switch('c') boolean alignChunk) throws WorldEditException {
        FawePlayer fp = FawePlayer.wrap(player);
        MCAClipboard clipboard = fp.getMeta(FawePlayer.METADATA_KEYS.ANVIL_CLIPBOARD);
        if (clipboard == null) {
            fp.sendMessage(BBC.getPrefix() + "You must first copy to your clipboard");
            return;
        }
        CuboidRegion cuboid = clipboard.getRegion();
        RegionWrapper copyRegion = new RegionWrapper(cuboid.getMinimumPoint(), cuboid.getMaximumPoint());
        final Vector offset = player.getPosition().subtract(clipboard.getOrigin());
        if (alignChunk) {
            offset.setComponents((offset.getBlockX() >> 4) << 4, offset.getBlockY(), (offset.getBlockZ() >> 4) << 4);
        }
        int oX = offset.getBlockX();
        int oZ = offset.getBlockZ();
        RegionWrapper pasteRegion = new RegionWrapper(copyRegion.minX + oX, copyRegion.maxX + oX, copyRegion.minZ + oZ, copyRegion.maxZ + oZ);
        String pasteWorldName = Fawe.imp().getWorldName(editSession.getWorld());
        FaweQueue tmpTo = SetQueue.IMP.getNewQueue(pasteWorldName, true, false);
        FaweQueue tmpFrom = SetQueue.IMP.getNewQueue(clipboard.getQueue().getWorldName(), true, false);
        File folder = tmpTo.getSaveFolder();
        MCAQueue copyQueue = clipboard.getQueue();
        MCAQueue pasteQueue = new MCAQueue(pasteWorldName, folder, tmpTo.hasSky());
        player.print(BBC.getPrefix() + "Safely unloading regions...");
        tmpTo.setMCA(new Runnable() {
            @Override
            public void run() {
                tmpFrom.setMCA(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            player.print(BBC.getPrefix() + "Performing operation...");
                            pasteQueue.pasteRegion(copyQueue, copyRegion, offset);
                            player.print(BBC.getPrefix() + "Safely loading regions...");
                        } catch (Throwable e) {
                            e.printStackTrace();
                        }
                    }
                }, copyRegion, false);
            }
        }, pasteRegion, true);
        player.print("Done!");
    }
}