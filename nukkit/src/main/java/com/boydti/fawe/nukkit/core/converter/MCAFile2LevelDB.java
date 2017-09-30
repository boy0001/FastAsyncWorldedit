package com.boydti.fawe.nukkit.core.converter;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.config.BBC;
import com.boydti.fawe.jnbt.anvil.MCAChunk;
import com.boydti.fawe.jnbt.anvil.MCAFile;
import com.boydti.fawe.jnbt.anvil.MCAFilter;
import com.boydti.fawe.jnbt.anvil.MCAQueue;
import com.boydti.fawe.jnbt.anvil.filters.DelegateMCAFilter;
import com.boydti.fawe.jnbt.anvil.filters.RemapFilter;
import com.boydti.fawe.object.RunnableVal;
import com.boydti.fawe.object.clipboard.ClipboardRemapper;
import com.boydti.fawe.object.collection.ByteStore;
import com.boydti.fawe.object.io.LittleEndianOutputStream;
import com.boydti.fawe.object.number.MutableLong;
import com.boydti.fawe.util.ReflectionUtils;
import com.boydti.fawe.util.StringMan;
import com.sk89q.jnbt.ByteTag;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.IntTag;
import com.sk89q.jnbt.LongTag;
import com.sk89q.jnbt.NBTInputStream;
import com.sk89q.jnbt.NBTOutputStream;
import com.sk89q.jnbt.NamedTag;
import com.sk89q.jnbt.ShortTag;
import com.sk89q.jnbt.StringTag;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.world.registry.BundledBlockData;
import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.zip.GZIPInputStream;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.impl.Iq80DBFactory;

public class MCAFile2LevelDB extends MapConverter {

    private final ByteStore bufFinalizedState = new ByteStore(4);
    private final ByteStore keyStore9 = new ByteStore(9);
    private final ByteStore keyStore10 = new ByteStore(10);
    private final ByteStore bufData2D = new ByteStore(512 + 256);
    private final ByteStore bufSubChunkPrefix = new ByteStore(1 + 4096 + 2058 + 2048 + 2048);

    private final byte[] VERSION = new byte[] { 4 };
    private final byte[] COMPLETE_STATE = new byte[] { 2, 0, 0, 0 };

    private final DB db;
    private final ClipboardRemapper remapper;
    private final ForkJoinPool pool;
    private boolean closed;
    private LongAdder submitted = new LongAdder();

    private boolean remap;

    public MCAFile2LevelDB(File folderFrom, File folderTo) {
        super(folderFrom, folderTo);
        try {
            if (!folderTo.exists()) {
                folderTo.mkdirs();
            }
            String worldName = folderTo.getName();
            try (PrintStream out = new PrintStream(new FileOutputStream(new File(folderTo, "levelname.txt")))) {
                out.print(worldName);
            }
            this.pool = new ForkJoinPool();
            this.remapper = new ClipboardRemapper(ClipboardRemapper.RemapPlatform.PC, ClipboardRemapper.RemapPlatform.PE);
            BundledBlockData.getInstance().loadFromResource();
            this.db = Iq80DBFactory.factory.open(new File(folderTo, "db"),
                    new Options()
                            .createIfMissing(true)
                            .verifyChecksums(false)
                            .blockSize(262144) // 256K
                            .cacheSize(8388608) // 8MB
                            .writeBufferSize(134217728) // >=128MB
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public MCAFilter<MutableLong> toFilter() {
        RemapFilter filter = new RemapFilter(ClipboardRemapper.RemapPlatform.PC, ClipboardRemapper.RemapPlatform.PE);
        DelegateMCAFilter<MutableLong> delegate = new DelegateMCAFilter<MutableLong>(filter) {
            @Override
            public void finishFile(MCAFile file, MutableLong cache) {
                file.forEachChunk(new RunnableVal<MCAChunk>() {
                    @Override
                    public void run(MCAChunk value) {
                        try {
                            write(value, !file.getFile().getName().endsWith(".mcapm"));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
                file.clear();
            }
        };
        return delegate;
    }


    @Override
    public void accept(ConverterFrame app) {
        MCAFilter filter = toFilter();
        MCAQueue queue = new MCAQueue(null, new File(folderFrom, "region"), true);
        MCAFilter result = queue.filterWorld(filter);

        File levelDat = new File(folderFrom, "level.dat");
        if (levelDat.exists()) {
            try {
                copyLevelDat(levelDat);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        close();
        app.prompt(
                "Conversion complete!\n" +
                        " - The world save is still being compacted, but you can close the program anytime\n" +
                        " - There will be another prompt when this finishes\n" +
                        "\n" +
                        "What is not converted?\n" +
                        " - Inventory is not copied\n" +
                        " - Some block nbt may not copy\n" +
                        " - Any custom generator settings may not work\n" +
                        " - May not match up with new terrain"
        );
        compact();
        app.prompt("Compaction complete!");
    }

    @Override
    public void close() {
        try {
            if (closed == (closed = true)) return;
            Fawe.debug("Collecting threads");
            pool.shutdown();
            pool.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
            Fawe.debug("Closing");
            db.close();
            Fawe.debug("Done! (but still compacting)");
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public void compact() {
        // Since the library doesn't support it, only way to flush the cache is to loop over everything
        try (DB newDb = Iq80DBFactory.factory.open(new File(folderTo, "db"), new Options()
                .verifyChecksums(false)
                .blockSize(262144) // 256K
                .cacheSize(8388608) // 8MB
                .writeBufferSize(134217728) // >=128MB
        )) {
            newDb.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Fawe.debug("Done compacting!");
    }

    public void copyLevelDat(File in) throws IOException {
        File levelDat = new File(folderTo, "level.dat");
        try (NBTInputStream nis = new NBTInputStream(new GZIPInputStream(new FileInputStream(in)))) {
            if (!levelDat.exists()) {
                levelDat.createNewFile();
            }
            NamedTag named = nis.readNamedTag();
            com.sk89q.jnbt.CompoundTag tag = (CompoundTag) ((CompoundTag) (named.getTag())).getValue().get("Data");
            Map<String, com.sk89q.jnbt.Tag> map = ReflectionUtils.getMap(tag.getValue());

            HashSet<String> allowed = new HashSet<>(Arrays.asList(
            "Difficulty", "GameType", "Generator", "LastPlayed", "RandomSeed", "StorageVersion", "Time", "commandsEnabled", "currentTick", "rainTime", "spawnMobs", "GameRules", "SpawnX", "SpawnY", "SpawnZ"
            ));
            Iterator<Map.Entry<String, com.sk89q.jnbt.Tag>> iterator = map.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, com.sk89q.jnbt.Tag> entry = iterator.next();
                if (!allowed.contains(entry.getKey())) {
                    iterator.remove();
                }
            }
            {
                Map<String, com.sk89q.jnbt.Tag> gameRules = ((CompoundTag) map.remove("GameRules")).getValue();
                for (Map.Entry<String, com.sk89q.jnbt.Tag> entry : gameRules.entrySet()) {
                    String key = entry.getKey().toLowerCase();
                    String value = ((StringTag) entry.getValue()).getValue();
                    if (StringMan.isEqualIgnoreCaseToAny(value, "true", "false")) {
                        map.put(key, new ByteTag((byte) (value.equals("true") ? 1 : 0)));
                    }
                }
                map.put("LevelName", new StringTag(folderTo.getName()));
                map.put("StorageVersion", new IntTag(5));
                Byte difficulty = tag.getByte("Difficulty");
                map.put("Difficulty", new IntTag(difficulty == null ? 2 : difficulty));
                String generatorName = tag.getString("generatorName");
                map.put("Generator", new IntTag("flat".equalsIgnoreCase(generatorName) ? 2 : 1));
                map.put("commandsEnabled", new ByteTag((byte) 1));
                Long time = tag.getLong("Time");
                map.put("CurrentTick", new LongTag(time == null ? 0L : time));
                map.put("spawnMobs", new ByteTag((byte) 1));
                Long lastPlayed = tag.getLong("LastPlayed");
                if (lastPlayed != null && lastPlayed > Integer.MAX_VALUE) {
                    lastPlayed = lastPlayed / 1000;
                    map.put("LastPlayed", new LongTag(lastPlayed));
                }
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (NBTOutputStream nos = new NBTOutputStream((DataOutput) new LittleEndianOutputStream(baos))) {
                nos.writeNamedTag("Name", tag);
            }
            LittleEndianOutputStream leos = new LittleEndianOutputStream(new FileOutputStream(levelDat));
            leos.writeInt(5);
            leos.writeInt(baos.toByteArray().length);
            leos.write(baos.toByteArray());
            leos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void write(MCAChunk chunk, boolean remap) throws IOException {
        submitted.add(1);
        if ((submitted.longValue() & 127) == 0) {
            pool.awaitQuiescence(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
            long queued = pool.getQueuedTaskCount() + pool.getQueuedSubmissionCount();
            if (queued > 127) {
                System.gc();
                while (queued > 64) {
                    try {
                        Thread.sleep(5);
                        queued = pool.getQueuedTaskCount() + pool.getQueuedSubmissionCount();
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }
        }
        pool.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    update(getKey(chunk, Tag.Version), VERSION);
                    update(getKey(chunk, Tag.FinalizedState), COMPLETE_STATE);

                    ByteBuffer data2d = ByteBuffer.wrap(bufData2D.get());
                    int[] heightMap = chunk.getHeightMapArray();
                    for (int i = 0; i < heightMap.length; i++) {
                        data2d.putShort((short) heightMap[i]);
                    }
                    if (chunk.biomes != null) {
                        System.arraycopy(chunk.biomes, 0, data2d.array(), 512, 256);
                    }
                    update(getKey(chunk, Tag.Data2D), data2d.array());

                    if (!chunk.tiles.isEmpty()) {
                        List<com.sk89q.jnbt.Tag> tiles = new ArrayList<>();
                        for (Map.Entry<Short, CompoundTag> entry : chunk.getTiles().entrySet()) {
                            CompoundTag tag = entry.getValue();
                            tiles.add(transform(tag));
                        }
                        update(getKey(chunk, Tag.BlockEntity), write(tiles));
                    }

                    if (!chunk.entities.isEmpty()) {
                        List<com.sk89q.jnbt.Tag> entities = new ArrayList<>();
                        for (com.sk89q.jnbt.CompoundTag tag : chunk.getEntities()) {
                            entities.add(transform(tag));
                        }
                        update(getKey(chunk, Tag.Entity), write(entities));
                    }

                    int maxLayer = chunk.ids.length - 1;
                    while (maxLayer >= 0 && chunk.ids[maxLayer] == null) maxLayer--;
                    if (maxLayer >= 0) {
                        byte[] key = getSectionKey(chunk, 0);
                        for (int layer = 0; layer <= maxLayer; layer++) {
                            // Set layer
                            key[9] = (byte) layer;
                            byte[] value = bufSubChunkPrefix.get();
                            byte[] ids = chunk.ids[layer];
                            if (ids == null) {
                                Arrays.fill(value, (byte) 0);
                            } else {
                                byte[] data = chunk.data[layer];
                                byte[] skyLight = chunk.skyLight[layer];
                                byte[] blockLight = chunk.blockLight[layer];

                                if (remap) {
                                    copySection(ids, value, 1);
                                    copySection(data, value, 1 + 4096);
                                    copySection(skyLight, value, 1 + 4096 + 2048);
                                    copySection(blockLight, value, 1 + 4096 + 2048 + 2048);
                                } else {
                                    System.arraycopy(ids, 0, value, 1, ids.length);
                                    System.arraycopy(data, 0, value, 1 + 4096, data.length);
                                    System.arraycopy(skyLight, 0, value, 1 + 4096 + 2048, skyLight.length);
                                    System.arraycopy(blockLight, 0, value, 1 + 4096 + 2048 + 2048, blockLight.length);
                                }
                            }
                            update(key, value);
                        }
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void update(byte[] key, byte[] value) {
        db.put(key.clone(), value.clone());
    }

    private void copySection(byte[] src, byte[] dest, int destPos) {
        switch (src.length) {
            case 4096: {
                int index = 0;
                int i1, i2, i3;
                for (int y = 0; y < 16; y++) {
                    i1 = y;
                    for (int z = 0; z < 16; z++) {
                        i2 = i1 + (z << 4);
                        for (int x = 0; x < 16; x++) {
                            i3 = i2 + (x << 8);
                            dest[destPos + i3] = src[index];
                            index++;
                        }
                    }
                }
                break;
            }
            case 2048: {
                int index = 0;
                int i1, i2, i3, i4;
                for (int x = 0; x < 16;) {
                    {
                        i1 = x;
                        for (int z = 0; z < 16; z++) {
                            i2 = i1 + (z << 4);
                            for (int y = 0; y < 16; y += 2) {
                                i3 = i2 + (y << 8);
                                i4 = i2 + ((y + 1) << 8);
                                byte newVal = (byte) ((src[i3 >> 1] & 0xF) + ((src[i4 >> 1] & 0xF) << 4));
                                dest[destPos + index] = newVal;
                                index++;
                            }
                        }
                    }
                    x++;
                    {
                        i1 = x;
                        for (int z = 0; z < 16; z++) {
                            i2 = i1 + (z << 4);
                            for (int y = 0; y < 16; y += 2) {
                                i3 = i2 + (y << 8);
                                i4 = i2 + ((y + 1) << 8);
                                byte newVal = (byte) (((src[i3 >> 1] & 0xF0) >> 4) + ((src[i4 >> 1] & 0xF0)));
                                dest[destPos + index] = newVal;
                                index++;
                            }
                        }
                    }
                    x++;

                }
                break;
            }
            default:
                System.arraycopy(src, 0, dest, destPos, src.length);
        }
    }

    private byte[] write(Collection<com.sk89q.jnbt.Tag> tags) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        NBTOutputStream nos = new NBTOutputStream(baos);
        nos.setLittleEndian();
        for (com.sk89q.jnbt.Tag tag : tags) {
            nos.writeNamedTag("", tag);
        }
        nos.close();
        return baos.toByteArray();
    }

    private String convertId(String input) {
        input = input.replace("minecraft:", "");
        StringBuilder result = new StringBuilder();
        boolean toUpper = false;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (i == 0) toUpper = true;
            if (c == '_') {
                toUpper = true;
            } else {
                result.append(toUpper ? Character.toUpperCase(c) : c);
                toUpper = false;
            }
        }
        return result.toString();
    }

    private CompoundTag transform(CompoundTag tag) {
        try {
            String id = tag.getString("id");
            if (id != null) {
                Map<String, com.sk89q.jnbt.Tag> map = ReflectionUtils.getMap(tag.getValue());
                id = convertId(id);
                map.put("id", new StringTag(id));
                { // Convert items
                    com.sk89q.jnbt.ListTag items = tag.getListTag("Items");
                    for (CompoundTag item : (List<CompoundTag>) (List) items.getValue()) {
                        String itemId = item.getString("id");
                        BaseBlock state = BundledBlockData.getInstance().findByState(itemId);
                        if (state != null) {
                            int legacy = state.getId();
                            ReflectionUtils.getMap(item.getValue()).put("id", new ShortTag((short) legacy));
                        }
                    }
                }
                switch (id) {
                    case "MobSpawner": {
                        map.clear();
                        break;
                    }
                    case "Sign": {
                        for (int line = 1; line <= 4; line++) {
                            String key = "Text" + line;
                            String text = tag.getString(key);
                            if (text != null && text.startsWith("{")) {
                                map.put(key, new StringTag(BBC.jsonToString(text)));
                            }

                        }
                    }
                }
            }
        } catch (Throwable e) {
            Fawe.debug("Error converting tag: " + tag);
            e.printStackTrace();
        }
        return tag;
    }

    private byte[] getSectionKey(MCAChunk chunk, int layer) {
        byte[] key = Tag.SubChunkPrefix.fill(chunk.getX(), chunk.getZ(), keyStore10.get());
        key[9] = (byte) layer;
        return key;
    }

    private byte[] getKey(MCAChunk chunk, Tag tag) {

        return tag.fill(chunk.getX(), chunk.getZ(), keyStore9.get());
    }
}
