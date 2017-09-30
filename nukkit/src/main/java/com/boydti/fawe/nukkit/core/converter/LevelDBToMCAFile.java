package com.boydti.fawe.nukkit.core.converter;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.jnbt.anvil.MCAChunk;
import com.boydti.fawe.jnbt.anvil.MCAQueue;
import com.boydti.fawe.jnbt.anvil.MCAQueueMap;
import com.boydti.fawe.jnbt.anvil.MutableMCABackedBaseBlock;
import com.boydti.fawe.jnbt.anvil.filters.RemapFilter;
import com.boydti.fawe.object.clipboard.ClipboardRemapper;
import com.boydti.fawe.object.io.PGZIPOutputStream;
import com.boydti.fawe.util.MemUtil;
import com.boydti.fawe.util.ReflectionUtils;
import com.google.common.io.LittleEndianDataInputStream;
import com.sk89q.jnbt.ByteTag;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.IntTag;
import com.sk89q.jnbt.LongTag;
import com.sk89q.jnbt.NBTInputStream;
import com.sk89q.jnbt.NBTOutputStream;
import com.sk89q.jnbt.NamedTag;
import com.sk89q.jnbt.StringTag;
import com.sk89q.worldedit.world.registry.BundledBlockData;
import java.io.DataInput;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.impl.Iq80DBFactory;

public class LevelDBToMCAFile extends MapConverter {

    private final DB db;
    private final ClipboardRemapper remapper;
    private final ForkJoinPool pool;

    public LevelDBToMCAFile(File from, File to) {
        super(from, to);
        try {
            BundledBlockData.getInstance().loadFromResource();
            this.pool = new ForkJoinPool();
            this.remapper = new ClipboardRemapper(ClipboardRemapper.RemapPlatform.PE, ClipboardRemapper.RemapPlatform.PC);
            int bufferSize = (int) Math.min(Integer.MAX_VALUE, Math.max((long) (MemUtil.getFreeBytes() * 0.8), 134217728));
            this.db = Iq80DBFactory.factory.open(new File(from, "db"),
                    new Options()
                            .createIfMissing(false)
                            .verifyChecksums(false)
                            .blockSize(262144) // 256K
                            .cacheSize(bufferSize) // 8MB
            );
            try {
                this.db.suspendCompactions();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() {
        try {
            pool.shutdown();
            pool.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
            db.close();
            Fawe.debug("Done!");
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    public void accept(ConverterFrame app) {
        try {
            // World name
            String worldName;
            File levelName = new File(folderFrom, "levelname.txt");
            if (levelName.exists()) {
                byte[] encoded = Files.readAllBytes(levelName.toPath());
                worldName = new String(encoded);
            } else {
                worldName = folderFrom.toString();
            }
            File worldOut = new File(folderTo, worldName);

            // Level dat
            File levelDat = new File(folderFrom, "level.dat");
            copyLevelDat(levelDat);


            // Chunks
            MCAQueue queue = new MCAQueue(worldName, new File(worldOut, "region"), true);
            RemapFilter filter = new RemapFilter(this.remapper);
            db.forEach(entry -> {
                byte[] key = entry.getKey();
                Tag tag = Tag.get(key);
                if (tag == null) {
                    if (key.length > 8) {
                        String name = new String(key);
                    }
                    return;
                }
                int cx = tag.getX(key);
                int cz = tag.getZ(key);
                byte[] value = entry.getValue();
                MCAChunk chunk = (MCAChunk) queue.getFaweChunk(cx, cz);

                switch (tag) {
                    case Data2D:
                        // height
                        ByteBuffer buffer = ByteBuffer.wrap(value);
                        int[] heightArray = chunk.getHeightMapArray();
                        for (int i = 0, j = 0; i < heightArray.length; i++, j += 2) {
                            heightArray[i] = buffer.getShort();
                        }
                        // biome
                        int biomeOffset = (heightArray.length << 1);
                        if (value.length > biomeOffset) {
                            System.arraycopy(value, biomeOffset, chunk.biomes, 0, chunk.biomes.length);
                        }
                        break;
                    case SubChunkPrefix:
                        int layer = key[9];
                        byte[] ids = getOrCreate(chunk.ids, layer, 4096);
                        byte[] data = getOrCreate(chunk.data, layer, 2048);
                        byte[] blockLight = getOrCreate(chunk.blockLight, layer, 2048);
                        byte[] skyLight = getOrCreate(chunk.skyLight, layer, 2048);

                        copySection(ids, value, 1);
                        copySection(data, value, 1 + 4096);
                        copySection(skyLight, value, 1 + 4096 + 2048);
                        copySection(blockLight, value, 1 + 4096 + 2048 + 2048);

                        chunk.filterBlocks(new MutableMCABackedBaseBlock(), filter);
                        break;
                    case BlockEntity:
                        break;
                    case Entity:
                        break;
                    // Ignore
                    case LegacyTerrain:
                    case BiomeState:
                    case Data2DLegacy:
                        System.out.println("Legacy terrain not supported, please update.");
                    case FinalizedState:
                    case PendingTicks:
                    case BlockExtraData:
                    case Version:
                        break;
                }
            });
            MCAQueueMap map = (MCAQueueMap) queue.getFaweQueueMap();
            while (map.next(0, Long.MAX_VALUE));
            queue.clear();
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            close();
            app.prompt("Compaction complete!");
        }
    }

    private void copySection(byte[] dest, byte[] src, int srcPos) {
        if (src.length <= srcPos) return;
        switch (dest.length) {
            case 4096: {
                int index = 0;
                int i1, i2, i3;
                for (int y = 0; y < 16; y++) {
                    i1 = y;
                    for (int z = 0; z < 16; z++) {
                        i2 = i1 + (z << 4);
                        for (int x = 0; x < 16; x++) {
                            i3 = i2 + (x << 8);
                            dest[index] = src[srcPos + i3];
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
                                byte newVal = (byte) ((src[srcPos + (i3 >> 1)] & 0xF) + ((src[srcPos + (i4 >> 1)] & 0xF) << 4));
                                dest[index] = newVal;
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
                                byte newVal = (byte) (((src[srcPos + (i3 >> 1)] & 0xF0) >> 4) + ((src[srcPos + (i4 >> 1)] & 0xF0)));
                                dest[index] = newVal;
                                index++;
                            }
                        }
                    }
                    x++;

                }
                break;
            }
            default:
                System.arraycopy(src, srcPos, dest, 0, dest.length);
        }
    }

    private byte[] getOrCreate(byte[][] arr, int index, int len) {
        byte[] data = arr[index];
        if (data == null) {
            arr[index] = data = new byte[len];
        }
        return data;
    }

    public void copyLevelDat(File in) throws IOException {
        File levelDat = new File(folderTo, "level.dat");
        if (!levelDat.exists()) {
            levelDat.createNewFile();
        }
        try (LittleEndianDataInputStream ledis = new LittleEndianDataInputStream(new FileInputStream(in))) {
            int version = ledis.readInt(); // Ignored
            int length = ledis.readInt(); // Ignored
            NBTInputStream nis = new NBTInputStream((DataInput) ledis);
            NamedTag named = nis.readNamedTag();
            com.sk89q.jnbt.CompoundTag tag = (CompoundTag) named.getTag();
            Map<String, com.sk89q.jnbt.Tag> map = ReflectionUtils.getMap(tag.getValue());

            Map<String, String> gameRules = new HashMap<>();
            gameRules.put("firedamage", "firedamage");
            gameRules.put("falldamage", "falldamage");
            gameRules.put("dofiretick", "doFireTick");
            gameRules.put("drowningdamage", "drowningdamage");
            gameRules.put("doentitydrops", "doEntityDrops");
            gameRules.put("keepinventory", "keepInventory");
            gameRules.put("sendcommandfeedback", "sendCommandFeedback");
            gameRules.put("dodaylightcycle", "doDaylightCycle");
            gameRules.put("commandblockoutput", "commandBlockOutput");
            gameRules.put("domobloot", "doMobLoot");
            gameRules.put("domobspawning", "doMobSpawning");
            gameRules.put("doweathercycle", "doWeatherCycle");
            gameRules.put("mobgriefing", "mobGriefing");
            gameRules.put("dotiledrops", "doTileDrops");

            HashMap<String, com.sk89q.jnbt.Tag> ruleTagValue = new HashMap<>();
            for (Map.Entry<String, String> rule : gameRules.entrySet()) {
                com.sk89q.jnbt.Tag value = map.remove(rule.getKey());
                if (value instanceof ByteTag) {
                    value = new StringTag((Byte) value.getValue() == 1 ? "true" : "false");
                }
                ruleTagValue.put(rule.getValue(), value);
            }

            HashSet<String> allowed = new HashSet<>(Arrays.asList(
                    "lightningTime", "pvp", "LevelName", "Difficulty", "GameType", "Generator", "LastPlayed", "RandomSeed", "StorageVersion", "Time", "commandsEnabled", "currentTick", "rainTime", "SpawnX", "SpawnY", "SpawnZ", "SizeOnDisk"
            ));
            Iterator<Map.Entry<String, com.sk89q.jnbt.Tag>> iterator = map.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, com.sk89q.jnbt.Tag> entry = iterator.next();
                if (!allowed.contains(entry.getKey())) {
                    System.out.println("TODO (Unsupported): " + entry.getKey() + " | " + entry.getValue());
                    iterator.remove();
                }
            }

            {
                map.put("GameRules", new CompoundTag(ruleTagValue));

                map.put("version", new IntTag(19133));
                map.put("DataVersion", new IntTag(1343));
                map.put("initialized", new ByteTag((byte) 1));
                map.putIfAbsent("SizeOnDisk", new LongTag(0));

                // generator
                int generator = tag.getInt("Generator");
                String name;
                switch (generator) {
                    default:
                    case 1:
                        name = "default";
                        break;
                    case 2:
                        name = "flat";
                        break;
                }
                map.put("generatorName", new StringTag(name));
                map.put("generatorOptions", new StringTag(""));
                map.put("generatorVersion", new IntTag(1));
                map.put("Difficulty", new ByteTag((byte) tag.getInt("Difficulty")));
                map.put("DifficultyLocked", new ByteTag((byte) 0));
                map.put("MapFeatures", new ByteTag((byte) 1));
                map.put("allowCommands", new ByteTag(tag.getByte("commandsEnabled")));
                long time = tag.getLong("Time");
                if (time == 0) time = tag.getLong("CurrentTick");
                map.put("Time", new LongTag(time));
                map.put("spawnMobs", new ByteTag((byte) 1));
                Long lastPlayed = tag.getLong("LastPlayed");
                if (lastPlayed != null && lastPlayed < Integer.MAX_VALUE) {
                    lastPlayed = lastPlayed * 1000;
                    map.put("LastPlayed", new LongTag(lastPlayed));
                }

                HashMap<String, com.sk89q.jnbt.Tag> data = new HashMap<>();
                data.put("Data", new CompoundTag(map));
                CompoundTag root = new CompoundTag(data);

                try (NBTOutputStream nos = new NBTOutputStream(new PGZIPOutputStream(new FileOutputStream(levelDat)))) {
                    nos.writeNamedTag("level.dat", root);
                }
            }
        }
    }
}
