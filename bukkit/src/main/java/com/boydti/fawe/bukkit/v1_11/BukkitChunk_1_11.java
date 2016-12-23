package com.boydti.fawe.bukkit.v1_11;

import com.boydti.fawe.Fawe;
import com.boydti.fawe.FaweCache;
import com.boydti.fawe.bukkit.v0.BukkitQueue_0;
import com.boydti.fawe.example.CharFaweChunk;
import com.boydti.fawe.object.FaweChunk;
import com.boydti.fawe.object.FaweQueue;
import com.boydti.fawe.util.MainUtil;
import com.boydti.fawe.util.ReflectionUtils;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.ListTag;
import com.sk89q.jnbt.LongTag;
import com.sk89q.jnbt.StringTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.internal.Constants;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.v1_11_R1.Block;
import net.minecraft.server.v1_11_R1.BlockPosition;
import net.minecraft.server.v1_11_R1.ChunkSection;
import net.minecraft.server.v1_11_R1.DataBits;
import net.minecraft.server.v1_11_R1.DataPalette;
import net.minecraft.server.v1_11_R1.DataPaletteBlock;
import net.minecraft.server.v1_11_R1.DataPaletteGlobal;
import net.minecraft.server.v1_11_R1.Entity;
import net.minecraft.server.v1_11_R1.EntityPlayer;
import net.minecraft.server.v1_11_R1.EntityTypes;
import net.minecraft.server.v1_11_R1.IBlockData;
import net.minecraft.server.v1_11_R1.MinecraftKey;
import net.minecraft.server.v1_11_R1.NBTTagCompound;
import net.minecraft.server.v1_11_R1.TileEntity;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_11_R1.CraftChunk;
import org.bukkit.event.entity.CreatureSpawnEvent;

public class BukkitChunk_1_11 extends CharFaweChunk<Chunk, com.boydti.fawe.bukkit.v1_11.BukkitQueue_1_11> {

    public DataPaletteBlock[] sectionPalettes;

    public static Map<String, Class<? extends Entity>> entityKeys;

    /**
     * A FaweSections object represents a chunk and the blocks that you wish to change in it.
     *
     * @param parent
     * @param x
     * @param z
     */
    public BukkitChunk_1_11(FaweQueue parent, int x, int z) {
        super(parent, x, z);
    }

    public BukkitChunk_1_11(FaweQueue parent, int x, int z, char[][] ids, short[] count, short[] air, byte[] heightMap) {
        super(parent, x, z, ids, count, air, heightMap);
    }

    @Override
    public CharFaweChunk copy(boolean shallow) {
        BukkitChunk_1_11 copy;
        if (shallow) {
            copy = new BukkitChunk_1_11(getParent(), getX(), getZ(), ids, count, air, heightMap);
            copy.biomes = biomes;
            copy.chunk = chunk;
        } else {
            copy = new BukkitChunk_1_11(getParent(), getX(), getZ(), (char[][]) MainUtil.copyNd(ids), count.clone(), air.clone(), heightMap.clone());
            copy.biomes = biomes;
            copy.chunk = chunk;
            copy.biomes = biomes.clone();
            copy.chunk = chunk;
        }
        if (sectionPalettes != null) {
            copy.sectionPalettes = new DataPaletteBlock[16];
            try {
                Field fieldBits = DataPaletteBlock.class.getDeclaredField("b");
                fieldBits.setAccessible(true);
                Field fieldPalette = DataPaletteBlock.class.getDeclaredField("c");
                fieldPalette.setAccessible(true);
                Field fieldSize = DataPaletteBlock.class.getDeclaredField("e");
                fieldSize.setAccessible(true);
                for (int i = 0; i < sectionPalettes.length; i++) {
                    DataPaletteBlock current = sectionPalettes[i];
                    if (current == null) {
                        continue;
                    }
                    // Clone palette
                    DataPalette currentPalette = (DataPalette) fieldPalette.get(current);
                    if (!(currentPalette instanceof DataPaletteGlobal)) {
                        current.a(128, null);
                    }
                    DataPaletteBlock paletteBlock = newDataPaletteBlock();
                    currentPalette = (DataPalette) fieldPalette.get(current);
                    if (!(currentPalette instanceof DataPaletteGlobal)) {
                        throw new RuntimeException("Palette must be global!");
                    }
                    fieldPalette.set(paletteBlock, currentPalette);
                    // Clone size
                    fieldSize.set(paletteBlock, fieldSize.get(current));
                    // Clone palette
                    DataBits currentBits = (DataBits) fieldBits.get(current);
                    DataBits newBits = new DataBits(1, 0);
                    for (Field field : DataBits.class.getDeclaredFields()) {
                        field.setAccessible(true);
                        Object currentValue = field.get(currentBits);
                        if (currentValue instanceof long[]) {
                            currentValue = ((long[]) currentValue).clone();
                        }
                        field.set(newBits, currentValue);
                    }
                    fieldBits.set(paletteBlock, newBits);
                    copy.sectionPalettes[i] = paletteBlock;
                }
            } catch (Throwable e) {
                MainUtil.handleError(e);
            }
        }
        return copy;
    }

    @Override
    public Chunk getNewChunk() {
        return ((com.boydti.fawe.bukkit.v1_11.BukkitQueue_1_11) getParent()).getWorld().getChunkAt(getX(), getZ());
    }

    public DataPaletteBlock newDataPaletteBlock() {
        try {
            return new DataPaletteBlock();
        } catch (Throwable e) {
            try {
                Constructor<DataPaletteBlock> constructor = DataPaletteBlock.class.getDeclaredConstructor(IBlockData[].class);
                return constructor.newInstance((Object) null);
            } catch (Throwable e2) {
                throw new RuntimeException(e2);
            }
        }
    }

    public void optimize() {
        if (sectionPalettes != null) {
            return;
        }
        char[][] arrays = getCombinedIdArrays();
        IBlockData lastBlock = null;
        char lastChar = Character.MAX_VALUE;
        for (int layer = 0; layer < 16; layer++) {
            if (getCount(layer) > 0) {
                if (sectionPalettes == null) {
                    sectionPalettes = new DataPaletteBlock[16];
                }
                DataPaletteBlock palette = newDataPaletteBlock();
                char[] blocks = getIdArray(layer);
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        for (int x = 0; x < 16; x++) {
                            char combinedId = blocks[FaweCache.CACHE_J[y][z][x]];
                            if (combinedId > 1) {
                                palette.setBlock(x, y, z, Block.getById(combinedId >> 4).fromLegacyData(combinedId & 0xF));
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public void start() {
        getChunk().load(true);
    }

    @Override
    public FaweChunk call() {
        try {
            final Chunk chunk = this.getChunk();
            final World world = chunk.getWorld();
            int bx = this.getX() << 4;
            int bz = this.getZ() << 4;
            final boolean flag = world.getEnvironment() == World.Environment.NORMAL;
            net.minecraft.server.v1_11_R1.Chunk nmsChunk = ((CraftChunk) chunk).getHandle();
            nmsChunk.f(true); // Set Modified
            nmsChunk.mustSave = true;
            net.minecraft.server.v1_11_R1.World nmsWorld = nmsChunk.world;
            ChunkSection[] sections = nmsChunk.getSections();
            final Collection<Entity>[] entities = (Collection<Entity>[]) getParent().getEntitySlices.invoke(nmsChunk);
            Map<BlockPosition, TileEntity> tiles = nmsChunk.getTileEntities();
            // Set heightmap
            getParent().setHeightMap(this, heightMap);
            // Remove entities
            for (int i = 0; i < entities.length; i++) {
                int count = this.getCount(i);
                if (count == 0) {
                    continue;
                } else if (count >= 4096) {
                    Collection<Entity> ents = entities[i];
                    if (!ents.isEmpty()) {
                        synchronized (BukkitQueue_0.adapter) {
                            ents.clear();
                        }
                    }
                } else {
                    Collection<Entity> ents = entities[i];
                    if (!ents.isEmpty()) {
                        char[] array = this.getIdArray(i);
                        ents = new ArrayList<>(entities[i]);
                        synchronized (BukkitQueue_0.adapter) {
                            for (Entity entity : ents) {
                                if (entity instanceof EntityPlayer) {
                                    continue;
                                }
                                int x = ((int) Math.round(entity.locX) & 15);
                                int z = ((int) Math.round(entity.locZ) & 15);
                                int y = (int) Math.round(entity.locY);
                                if (array == null || y < 0 || y > 255) {
                                    continue;
                                }
                                if (y < 0 || y > 255 || array[FaweCache.CACHE_J[y][z][x]] != 0) {
                                    nmsWorld.removeEntity(entity);
                                }
                            }
                        }
                    }
                }
            }
            HashSet<UUID> entsToRemove = this.getEntityRemoves();
            if (!entsToRemove.isEmpty()) {
                synchronized (BukkitQueue_0.adapter) {
                    for (int i = 0; i < entities.length; i++) {
                        Collection<Entity> ents = new ArrayList<>(entities[i]);
                        for (Entity entity : ents) {
                            if (entsToRemove.contains(entity.getUniqueID())) {
                                nmsWorld.removeEntity(entity);
                            }
                        }
                    }
                }
            }
            // Set entities
            Set<CompoundTag> entitiesToSpawn = this.getEntities();
            Set<UUID> createdEntities = new HashSet<>();
            if (!entitiesToSpawn.isEmpty()) {
                synchronized (BukkitQueue_0.adapter) {
                    for (CompoundTag nativeTag : entitiesToSpawn) {
                        Map<String, Tag> entityTagMap = ReflectionUtils.getMap(nativeTag.getValue());
                        StringTag idTag = (StringTag) entityTagMap.get("Id");
                        ListTag posTag = (ListTag) entityTagMap.get("Pos");
                        ListTag rotTag = (ListTag) entityTagMap.get("Rotation");
                        if (idTag == null || posTag == null || rotTag == null) {
                            Fawe.debug("Unknown entity tag: " + nativeTag);
                            continue;
                        }
                        double x = posTag.getDouble(0);
                        double y = posTag.getDouble(1);
                        double z = posTag.getDouble(2);
                        float yaw = rotTag.getFloat(0);
                        float pitch = rotTag.getFloat(1);
                        String id = idTag.getValue();
                        if (entityKeys == null) {
                            entityKeys = new HashMap<>();
                            for (MinecraftKey key : EntityTypes.a()) {
                                String currentId = EntityTypes.a(key);
                                Class<? extends Entity> clazz = EntityTypes.b.get(key);
                                entityKeys.put(currentId, clazz);
                                entityKeys.put(key.a(), clazz);
                            }
                        }
                        Class<? extends Entity> clazz = entityKeys.get(id);
                        if (clazz != null) {
                            Entity entity = EntityTypes.a(clazz, nmsWorld);
                            if (entity != null) {
                                UUID uuid = entity.getUniqueID();
                                entityTagMap.put("UUIDMost", new LongTag(uuid.getMostSignificantBits()));
                                entityTagMap.put("UUIDLeast", new LongTag(uuid.getLeastSignificantBits()));
                                if (nativeTag != null) {
                                    NBTTagCompound tag = (NBTTagCompound) BukkitQueue_1_11.methodFromNative.invoke(BukkitQueue_1_11.adapter, nativeTag);
                                    for (String name : Constants.NO_COPY_ENTITY_NBT_FIELDS) {
                                        tag.remove(name);
                                    }
                                    entity.f(tag);
                                }
                                entity.setLocation(x, y, z, yaw, pitch);
                                nmsWorld.addEntity(entity, CreatureSpawnEvent.SpawnReason.CUSTOM);
                                createdEntities.add(entity.getUniqueID());
                            }
                        }
                    }
                }
            }
            // Change task?
            if (getParent().getChangeTask() != null) {
                BukkitChunk_1_11 previous = getParent().getPrevious(this, sections, tiles, entities, createdEntities, false);
                getParent().getChangeTask().run(previous, this);
            }
            // Trim tiles
            Iterator<Map.Entry<BlockPosition, TileEntity>> iterator = tiles.entrySet().iterator();
            HashMap<BlockPosition, TileEntity> toRemove = null;
            while (iterator.hasNext()) {
                Map.Entry<BlockPosition, TileEntity> tile = iterator.next();
                BlockPosition pos = tile.getKey();
                int lx = pos.getX() & 15;
                int ly = pos.getY();
                int lz = pos.getZ() & 15;
                int j = FaweCache.CACHE_I[ly][lz][lx];
                char[] array = this.getIdArray(j);
                if (array == null) {
                    continue;
                }
                int k = FaweCache.CACHE_J[ly][lz][lx];
                if (array[k] != 0) {
                    if (toRemove == null) {
                        toRemove = new HashMap<>();
                    }
                    toRemove.put(tile.getKey(), tile.getValue());
                }
            }
            if (toRemove != null) {
                for (Map.Entry<BlockPosition, TileEntity> entry : toRemove.entrySet()) {
                    BlockPosition bp = entry.getKey();
                    TileEntity tile = entry.getValue();
                    tiles.remove(bp);
                    tile.z();
                    nmsWorld.s(bp);
                    tile.invalidateBlockCache();
                }

            }
            // Set blocks
            for (int j = 0; j < sections.length; j++) {
                int count = this.getCount(j);
                if (count == 0) {
                    continue;
                }
                int air = this.getAir(j);
                final char[] array = this.getIdArray(j);
                if (array == null) {
                    continue;
                }
                ChunkSection section = sections[j];
                if (section == null) {
                    if (count == air) {
                        continue;
                    }
                    if (this.sectionPalettes != null && this.sectionPalettes[j] != null) {
                        section = sections[j] = getParent().newChunkSection(j << 4, flag, null);
                        getParent().setPalette(section, this.sectionPalettes[j]);
                        getParent().setCount(0, count - this.getAir(j), section);
                        continue;
                    } else {
                        sections[j] = getParent().newChunkSection(j << 4, flag, array);
                    }
                    continue;
                } else if (count >= 4096) {
                    if (air >= 4096) {
                        sections[j] = null;
                        continue;
                    }
                    if (this.sectionPalettes != null && this.sectionPalettes[j] != null) {
                        getParent().setPalette(section, this.sectionPalettes[j]);
                        getParent().setCount(0, count - this.getAir(j), section);
                        continue;
                    } else {
                        sections[j] = getParent().newChunkSection(j << 4, flag, array);
                    }
                    continue;
                }
                int by = j << 4;
                DataPaletteBlock nibble = section.getBlocks();
                int nonEmptyBlockCount = 0;
                IBlockData existing;
                for (int y = 0; y < 16; y++) {
                    short[][] i1 = FaweCache.CACHE_J[y];
                    for (int z = 0; z < 16; z++) {
                        short[] i2 = i1[z];
                        for (int x= 0; x < 16; x++) {
                            char combinedId = array[i2[x]];
                            switch (combinedId) {
                                case 0:
                                    continue;
                                case 1:
                                    existing = nibble.a(x, y, z);
                                    if (existing != BukkitQueue_1_11.air) {
                                        if (existing.d() > 0) {
                                            getParent().getRelighter().addLightUpdate(bx + x, by + y, bz + z);
                                        }
                                        nonEmptyBlockCount--;
                                    }
                                    nibble.setBlock(x, y, z, BukkitQueue_1_11.air);
                                    continue;
                                default:
                                    existing = nibble.a(x, y, z);
                                    if (existing != BukkitQueue_1_11.air) {
                                        if (existing.d() > 0) {
                                            getParent().getRelighter().addLightUpdate(bx + x, by + y, bz + z);
                                        }
                                    } else {
                                        nonEmptyBlockCount++;
                                    }
                                    nibble.setBlock(x, y, z, getParent().IBD_CACHE[(int) combinedId]);
                            }
                        }
                    }
                }
                getParent().setCount(0, getParent().getNonEmptyBlockCount(section) + nonEmptyBlockCount, section);
            }
            // Set biomes
            int[][] biomes = this.biomes;
            if (biomes != null) {
                for (int x = 0; x < 16; x++) {
                    int[] array = biomes[x];
                    if (array == null) {
                        continue;
                    }
                    for (int z = 0; z < 16; z++) {
                        int biome = array[z];
                        if (biome == 0) {
                            continue;
                        }
                        nmsChunk.getBiomeIndex()[((z & 0xF) << 4 | x & 0xF)] = (byte) biome;
                    }
                }
            }
            // Set tiles
            Map<Short, CompoundTag> tilesToSpawn = this.getTiles();
            for (Map.Entry<Short, CompoundTag> entry : tilesToSpawn.entrySet()) {
                CompoundTag nativeTag = entry.getValue();
                short blockHash = entry.getKey();
                int x = (blockHash >> 12 & 0xF) + bx;
                int y = (blockHash & 0xFF);
                int z = (blockHash >> 8 & 0xF) + bz;
                BlockPosition pos = new BlockPosition(x, y, z); // Set pos
                TileEntity tileEntity = nmsWorld.getTileEntity(pos);
                if (tileEntity != null) {
                    NBTTagCompound tag = (NBTTagCompound) com.boydti.fawe.bukkit.v1_11.BukkitQueue_1_11.methodFromNative.invoke(com.boydti.fawe.bukkit.v1_11.BukkitQueue_1_11.adapter, nativeTag);
                    tileEntity.a(tag); // ReadTagIntoTile
                }
            }
        } catch (Throwable e) {
            MainUtil.handleError(e);
        }
        return this;
    }
}
